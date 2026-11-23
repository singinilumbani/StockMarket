
package com.example.stockmarket.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.stockmarket.data.remote.AccountDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

// A third DataStore file. The settings and the login each keep their own.
private val Context.accountCacheStore: DataStore<Preferences> by preferencesDataStore(name = "account_cache")

// What we remember. userId says WHOSE data this is.
data class CachedAccount(val userId: Int, val savedAt: Long, val account: AccountDto)
data class CachedRegistration(val userId: Int, val status: String)

// The phone's saved copy of the logged-in user's account, encrypted with the same Keystore key as the login.
// It is only a COPY for showing when offline. The server stays the only place that decides anything.
object AccountCache {

    private var store: DataStore<Preferences>? = null
    private val gson = Gson()
    private val accountKey = stringPreferencesKey("account_blob")
    private val registrationKey = stringPreferencesKey("registration_blob")

    // Call once, when the app starts
    fun init(context: Context) {
        if (store == null) store = context.applicationContext.accountCacheStore
    }

    // ---------- The account: cash, holdings, transactions and orders ----------

    suspend fun saveAccount(userId: Int, savedAt: Long, account: AccountDto) {
        val blob = TokenCipher.encrypt(gson.toJson(CachedAccount(userId, savedAt, account)))
        store?.edit { it[accountKey] = blob }
    }

    suspend fun readAccount(userId: Int): CachedAccount? = try {
        val blob = store?.data?.first()?.get(accountKey)
        val json = blob?.let { TokenCipher.decrypt(it) }
        val cached = json?.let { gson.fromJson(it, CachedAccount::class.java) }
        if (cached != null && cached.userId == userId) cached else null
        cached
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null      // a damaged or unreadable copy is simply not used
    }

    // ---------- The registration STATUS (never the form: ID numbers stay off the disk) ----------

    suspend fun saveRegistration(userId: Int, status: String) {
        val blob = TokenCipher.encrypt(gson.toJson(CachedRegistration(userId, status)))
        store?.edit { it[registrationKey] = blob }
    }

    suspend fun readRegistration(userId: Int): CachedRegistration? = try {
        val blob = store?.data?.first()?.get(registrationKey)
        val json = blob?.let { TokenCipher.decrypt(it) }
        val cached = json?.let { gson.fromJson(it, CachedRegistration::class.java) }
        if (cached != null && cached.userId == userId) cached else null    // never another user's data
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    // Called when the user logs out
    suspend fun clear() {
        store?.edit { it.clear() }
    }
}