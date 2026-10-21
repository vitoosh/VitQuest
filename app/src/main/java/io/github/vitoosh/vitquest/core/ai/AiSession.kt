package io.github.vitoosh.vitquest.core.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** An anonymous Supabase sign-in. Only used to call the food photo function. */
@Serializable
data class AiSession(val accessToken: String, val refreshToken: String, val expiresAtEpochSeconds: Long)

/** Encrypts small secrets so they are unreadable outside this app on this phone. */
interface TokenCipher {
    fun encrypt(plain: ByteArray): ByteArray

    /** Throws when the data was not made by [encrypt] on this phone (for example after a restore). */
    fun decrypt(data: ByteArray): ByteArray
}

/** AES-GCM with a key that never leaves the Android Keystore. */
@Singleton
class KeystoreTokenCipher @Inject constructor() : TokenCipher {

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        val ivSize = data.firstOrNull()?.toInt() ?: throw GeneralSecurityException("Empty")
        if (ivSize <= 0 || data.size <= 1 + ivSize) throw GeneralSecurityException("Malformed")
        val iv = data.copyOfRange(1, 1 + ivSize)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return cipher.doFinal(data.copyOfRange(1 + ivSize, data.size))
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "vitquest_ai_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/** Keeps the session encrypted in app storage. Not part of backups; a new phone simply signs in again. */
@Singleton
class AiSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cipher: TokenCipher,
) {
    private val file = File(context.noBackupFilesDir, "ai_session.bin")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AiSession? = try {
        if (!file.exists()) {
            null
        } else {
            json.decodeFromString(AiSession.serializer(), cipher.decrypt(file.readBytes()).toString(Charsets.UTF_8))
        }
    } catch (_: Exception) {
        // Unreadable (key gone, file damaged): start a new anonymous session.
        file.delete()
        null
    }

    fun save(session: AiSession) {
        file.writeBytes(cipher.encrypt(json.encodeToString(AiSession.serializer(), session).toByteArray(Charsets.UTF_8)))
    }

    fun clear() {
        file.delete()
    }
}
