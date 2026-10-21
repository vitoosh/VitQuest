package io.github.vitoosh.vitquest.core.ai

import io.github.vitoosh.vitquest.core.time.TimeSource
import java.io.IOException
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One food or drink the AI saw, with its estimate for the visible portion. */
data class EstimatedItem(
    val nameId: String,
    val nameEn: String,
    val grams: Double,
    val kcal: Double,
    val proteinG: Double?,
    val carbsG: Double?,
    val fatG: Double?,
    val confidence: Confidence,
) {
    enum class Confidence { LOW, MEDIUM, HIGH }
}

data class FoodEstimate(val isFood: Boolean, val items: List<EstimatedItem>, val note: String?)

sealed interface AnalyzeResult {
    data class Success(val estimate: FoodEstimate) : AnalyzeResult
    /** The app was built without the Supabase settings. */
    data object NotConfigured : AnalyzeResult
    data object Offline : AnalyzeResult
    data object DailyLimitReached : AnalyzeResult
    data object ServerError : AnalyzeResult
}

/** Sends a photo to the VitQuest food-photo function and reads back an estimate. Never logs anything itself. */
@Singleton
class FoodPhotoAnalyzer @Inject constructor(
    private val config: AiConfig,
    private val http: HttpClient,
    private val sessions: AiSessionStore,
    private val timeSource: TimeSource,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val sessionMutex = Mutex()

    suspend fun analyze(jpeg: ByteArray, language: String): AnalyzeResult {
        if (!config.isConfigured) return AnalyzeResult.NotConfigured
        return try {
            val body = json.encodeToString(
                AnalyzeRequest.serializer(),
                AnalyzeRequest(Base64.getEncoder().encodeToString(jpeg), "image/jpeg", language),
            )
            var session = session(forceNew = false)
            var response = call(session, body)
            if (response.code == HTTP_UNAUTHORIZED) {
                // Expired or revoked: refresh, or sign in again, and retry once.
                session = session(forceNew = true)
                response = call(session, body)
            }
            when (response.code) {
                in 200..299 -> AnalyzeResult.Success(parseEstimate(response.body))
                HTTP_TOO_MANY_REQUESTS -> AnalyzeResult.DailyLimitReached
                else -> AnalyzeResult.ServerError
            }
        } catch (_: IOException) {
            AnalyzeResult.Offline
        } catch (_: AuthException) {
            AnalyzeResult.ServerError
        } catch (_: IllegalArgumentException) {
            // The response was not the JSON we expect (kotlinx.serialization throws subclasses of this).
            AnalyzeResult.ServerError
        }
    }

    private suspend fun call(session: AiSession, body: String): HttpResponse = http.postJson(
        "${config.supabaseUrl}/functions/v1/food-photo",
        mapOf("Authorization" to "Bearer ${session.accessToken}", "apikey" to config.publishableKey),
        body,
    )

    /** A valid session: the stored one, a refreshed one, or a new anonymous sign-in. */
    private suspend fun session(forceNew: Boolean): AiSession = sessionMutex.withLock {
        val now = timeSource.now().epochSecond
        val stored = sessions.load()
        if (!forceNew && stored != null && stored.expiresAtEpochSeconds - EXPIRY_MARGIN_S > now) return stored
        val refreshed = stored?.let { refresh(it.refreshToken, now) }
        val session = refreshed ?: signInAnonymously(now)
        sessions.save(session)
        session
    }

    private suspend fun refresh(refreshToken: String, now: Long): AiSession? {
        val response = http.postJson(
            "${config.supabaseUrl}/auth/v1/token?grant_type=refresh_token",
            mapOf("apikey" to config.publishableKey),
            json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken)),
        )
        return if (response.code in 200..299) parseSession(response.body, now) else null
    }

    private suspend fun signInAnonymously(now: Long): AiSession {
        val response = http.postJson("${config.supabaseUrl}/auth/v1/signup", mapOf("apikey" to config.publishableKey), "{}")
        if (response.code !in 200..299) throw AuthException()
        return parseSession(response.body, now)
    }

    private fun parseSession(body: String, now: Long): AiSession {
        val auth = json.decodeFromString(AuthResponse.serializer(), body)
        return AiSession(auth.accessToken, auth.refreshToken, now + auth.expiresIn)
    }

    /** Values are checked again here, so an odd answer can never produce absurd diary entries. */
    internal fun parseEstimate(body: String): FoodEstimate {
        val response = json.decodeFromString(EstimateResponse.serializer(), body)
        val items = response.items.take(MAX_ITEMS).mapNotNull { item ->
            val nameId = item.nameId.trim().take(MAX_NAME)
            val nameEn = item.nameEn.trim().take(MAX_NAME).ifEmpty { nameId }
            if (nameId.isEmpty() || !item.grams.isFinite() || !item.kcal.isFinite() || item.grams <= 0) return@mapNotNull null
            EstimatedItem(
                nameId = nameId,
                nameEn = nameEn,
                grams = item.grams.coerceIn(1.0, MAX_GRAMS),
                kcal = item.kcal.coerceIn(0.0, MAX_KCAL),
                proteinG = item.proteinG?.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_GRAMS),
                carbsG = item.carbsG?.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_GRAMS),
                fatG = item.fatG?.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_GRAMS),
                confidence = EstimatedItem.Confidence.entries.firstOrNull { it.name.equals(item.confidence, ignoreCase = true) }
                    ?: EstimatedItem.Confidence.LOW,
            )
        }
        return FoodEstimate(response.isFood && items.isNotEmpty(), items, response.note?.trim()?.take(MAX_NOTE)?.ifEmpty { null })
    }

    private class AuthException : Exception()

    @Serializable
    private data class AnalyzeRequest(
        @SerialName("image_base64") val imageBase64: String,
        @SerialName("mime_type") val mimeType: String,
        val language: String,
    )

    @Serializable
    private data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

    @Serializable
    private data class AuthResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("expires_in") val expiresIn: Long,
    )

    @Serializable
    private data class EstimateResponse(
        @SerialName("is_food") val isFood: Boolean = false,
        val items: List<EstimateItem> = emptyList(),
        val note: String? = null,
    )

    @Serializable
    private data class EstimateItem(
        @SerialName("name_id") val nameId: String = "",
        @SerialName("name_en") val nameEn: String = "",
        val grams: Double = 0.0,
        val kcal: Double = 0.0,
        @SerialName("protein_g") val proteinG: Double? = null,
        @SerialName("carbs_g") val carbsG: Double? = null,
        @SerialName("fat_g") val fatG: Double? = null,
        val confidence: String = "low",
    )

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val EXPIRY_MARGIN_S = 60L
        const val MAX_ITEMS = 15
        const val MAX_NAME = 80
        const val MAX_NOTE = 300
        const val MAX_GRAMS = 3_000.0
        const val MAX_KCAL = 5_000.0
    }
}
