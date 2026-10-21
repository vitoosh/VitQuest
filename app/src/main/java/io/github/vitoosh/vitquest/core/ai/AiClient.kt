package io.github.vitoosh.vitquest.core.ai

import io.github.vitoosh.vitquest.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The only place VitQuest talks to the internet, for food photo estimates.
 * Both values come from the build (GitHub secrets in CI) and are public by design:
 * the publishable key only allows what the database policies allow.
 */
@Singleton
class AiConfig(supabaseUrl: String, val publishableKey: String) {
    @Inject constructor() : this(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)

    val supabaseUrl: String = supabaseUrl.trimEnd('/')

    val isConfigured: Boolean get() = supabaseUrl.startsWith("https://") && publishableKey.isNotBlank()
}

data class HttpResponse(val code: Int, val body: String)

/** Minimal JSON-over-HTTPS client, so the app needs no networking library. */
interface HttpClient {
    @Throws(IOException::class)
    suspend fun postJson(url: String, headers: Map<String, String>, body: String): HttpResponse
}

@Singleton
class UrlConnectionHttpClient @Inject constructor() : HttpClient {
    override suspend fun postJson(url: String, headers: Map<String, String>, body: String): HttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                HttpResponse(code, stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty())
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        // Image analysis can take a while on a free tier.
        const val READ_TIMEOUT_MS = 90_000
    }
}
