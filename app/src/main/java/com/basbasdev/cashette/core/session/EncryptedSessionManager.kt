package com.basbasdev.cashette.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * supabase-kt's default session manager writes to plain SharedPreferences. The refresh
 * token is long-lived and re-mints access tokens indefinitely, so it goes in
 * EncryptedSharedPreferences (AES256-GCM, key held in the Android Keystore) instead.
 */
class EncryptedSessionManager(
    context: Context,
    private val json: Json,
) : SessionManager {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_SESSION, json.encodeToString(session)).apply()
    }

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val raw = prefs.getString(KEY_SESSION, null) ?: return@withContext null
        // A payload we can no longer read is worse than none: drop it and sign out
        // cleanly rather than crashing on every launch.
        runCatching { json.decodeFromString<UserSession>(raw) }
            .getOrElse {
                prefs.edit().remove(KEY_SESSION).apply()
                null
            }
    }

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private companion object {
        const val FILE_NAME = "cashette_session"
        const val KEY_SESSION = "user_session"
    }
}
