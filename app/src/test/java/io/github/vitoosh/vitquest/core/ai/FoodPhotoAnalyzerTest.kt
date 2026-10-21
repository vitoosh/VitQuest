package io.github.vitoosh.vitquest.core.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.testing.FixedTimeSource
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodPhotoAnalyzerTest {

    /** Stands in for the Keystore, which Robolectric does not provide. */
    private class XorCipher : TokenCipher {
        override fun encrypt(plain: ByteArray) = plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        override fun decrypt(data: ByteArray) = encrypt(data)
    }

    private class FakeHttp : HttpClient {
        val requests = mutableListOf<Pair<String, Map<String, String>>>()
        val replies = ArrayDeque<HttpResponse>()
        var failWith: IOException? = null
        override suspend fun postJson(url: String, headers: Map<String, String>, body: String): HttpResponse {
            failWith?.let { throw it }
            requests += url to headers
            return replies.removeFirst()
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val time = FixedTimeSource(Instant.parse("2026-10-21T05:00:00Z"))
    private val http = FakeHttp()
    private lateinit var sessions: AiSessionStore
    private lateinit var analyzer: FoodPhotoAnalyzer

    private fun session(token: String) = HttpResponse(200, """{"access_token":"$token","refresh_token":"r-$token","expires_in":3600,"user":{"id":"u"}}""")

    private val estimate = HttpResponse(
        200,
        """{"is_food":true,"note":"Cek sambal","items":[
            {"name_id":"Nasi putih","name_en":"White rice","grams":150,"kcal":195,"protein_g":4,"carbs_g":42,"fat_g":0.4,"confidence":"high"},
            {"name_id":"Ayam goreng","name_en":"Fried chicken","grams":90,"kcal":260,"confidence":"medium","extra":"ignored"}]}""",
    )

    @Before
    fun setUp() {
        sessions = AiSessionStore(context, XorCipher()).apply { clear() }
        analyzer = FoodPhotoAnalyzer(AiConfig("https://proj.supabase.co/", "sb_publishable_test"), http, sessions, time)
    }

    @Test
    fun signsInOnceThenReusesTheSession() = runTest {
        http.replies += listOf(session("a"), estimate, estimate)

        val first = analyzer.analyze(byteArrayOf(1, 2, 3), "id") as AnalyzeResult.Success
        analyzer.analyze(byteArrayOf(1, 2, 3), "id")

        assertEquals(listOf("Nasi putih", "Ayam goreng"), first.estimate.items.map { it.nameId })
        assertEquals(EstimatedItem.Confidence.MEDIUM, first.estimate.items[1].confidence)
        assertEquals("Cek sambal", first.estimate.note)
        val urls = http.requests.map { it.first }
        assertEquals(
            listOf(
                "https://proj.supabase.co/auth/v1/signup",
                "https://proj.supabase.co/functions/v1/food-photo",
                "https://proj.supabase.co/functions/v1/food-photo",
            ),
            urls,
        )
        assertEquals("Bearer a", http.requests[1].second["Authorization"])
        assertEquals("sb_publishable_test", http.requests[1].second["apikey"])
    }

    @Test
    fun anExpiredTokenIsRefreshedAndTheCallRetriedOnce() = runTest {
        http.replies += listOf(session("a"), HttpResponse(401, "{}"), session("b"), estimate)

        assertTrue(analyzer.analyze(byteArrayOf(1), "id") is AnalyzeResult.Success)
        assertEquals("https://proj.supabase.co/auth/v1/token?grant_type=refresh_token", http.requests[2].first)
        assertEquals("Bearer b", http.requests[3].second["Authorization"])
        assertEquals("b", sessions.load()?.accessToken)
    }

    @Test
    fun limitsConnectionProblemsAndOddAnswersAreReportedNotThrown() = runTest {
        http.replies += listOf(session("a"), HttpResponse(429, """{"error":"daily_limit"}"""))
        assertEquals(AnalyzeResult.DailyLimitReached, analyzer.analyze(byteArrayOf(1), "id"))

        http.replies += HttpResponse(200, "not json")
        assertEquals(AnalyzeResult.ServerError, analyzer.analyze(byteArrayOf(1), "id"))

        http.replies += HttpResponse(502, """{"error":"model_error"}""")
        assertEquals(AnalyzeResult.ServerError, analyzer.analyze(byteArrayOf(1), "id"))

        http.failWith = IOException("offline")
        assertEquals(AnalyzeResult.Offline, analyzer.analyze(byteArrayOf(1), "id"))
    }

    @Test
    fun withoutServerSettingsNothingIsSent() = runTest {
        val unconfigured = FoodPhotoAnalyzer(AiConfig("", ""), http, sessions, time)
        assertEquals(AnalyzeResult.NotConfigured, unconfigured.analyze(byteArrayOf(1), "id"))
        assertTrue(http.requests.isEmpty())
    }

    @Test
    fun estimatesAreCheckedAgainOnThePhone() {
        val parsed = analyzer.parseEstimate(
            """{"is_food":true,"items":[
                {"name_id":"  Es teh manis ","name_en":"","grams":99999,"kcal":-10,"confidence":"certain"},
                {"name_id":"","name_en":"Nothing","grams":10,"kcal":10},
                {"name_id":"Air","name_en":"Water","grams":0,"kcal":0}]}""",
        )
        val item = parsed.items.single()
        assertEquals("Es teh manis", item.nameId)
        assertEquals("Es teh manis", item.nameEn)
        assertEquals(3_000.0, item.grams, 0.0)
        assertEquals(0.0, item.kcal, 0.0)
        assertEquals(EstimatedItem.Confidence.LOW, item.confidence)
        assertFalse(analyzer.parseEstimate("""{"is_food":true,"items":[]}""").isFood)
    }

    @Test
    fun theStoredSessionIsEncryptedAndDroppedWhenUnreadable() {
        sessions.save(AiSession("secret-token", "refresh", 10))
        val raw = context.noBackupFilesDir.resolve("ai_session.bin").readBytes().toString(Charsets.UTF_8)
        assertFalse(raw.contains("secret-token"))
        // A store with another key (for example after moving to a new phone) cannot read it and starts over.
        val other = AiSessionStore(context, object : TokenCipher {
            override fun encrypt(plain: ByteArray) = plain
            override fun decrypt(data: ByteArray): ByteArray = throw java.security.GeneralSecurityException("wrong key")
        })
        assertEquals(null, other.load())
    }
}
