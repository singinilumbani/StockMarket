package com.example.stockmarket.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.stockmarket.data.remote.UserDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// A second DataStore file, just for the login. The settings keep their own file.
private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

// Everything we remember about a login
data class StoredSession(val token: String, val user: UserDto)

// Encrypts and decrypts text with a key that lives in the phone's Android Keystore.
// The key never leaves the Keystore, so a copy of the saved file is useless without this phone.
object TokenCipher {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "mse_invest_session_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12          // bytes
    private const val TAG_BITS = 128

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    // Result: base64 text containing a fresh random "iv" followed by the scrambled data
    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val scrambled = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + scrambled, Base64.NO_WRAP)
    }

    // Returns null if the text can't be decrypted (for example after a restore onto another phone)
    fun decrypt(text: String): String? = try {
        val all = Base64.decode(text, Base64.NO_WRAP)
        val iv = all.copyOfRange(0, IV_SIZE)
        val scrambled = all.copyOfRange(IV_SIZE, all.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(scrambled), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

class SessionStorage(context: Context) {

    private val store = context.applicationContext.sessionDataStore
    private val blobKey = stringPreferencesKey("session_blob")
    private val gson = Gson()

    suspend fun save(session: StoredSession) {
        val encrypted = TokenCipher.encrypt(gson.toJson(session))
        store.edit { it[blobKey] = encrypted }
    }

    suspend fun read(): StoredSession? {
        val blob = store.data.first()[blobKey] ?: return null
        val json = TokenCipher.decrypt(blob) ?: return null
        return try {
            gson.fromJson(json, StoredSession::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clear() {
        store.edit { it.remove(blobKey) }
    }
}

