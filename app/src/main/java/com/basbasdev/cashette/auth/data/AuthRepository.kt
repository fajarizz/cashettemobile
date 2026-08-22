package com.basbasdev.cashette.auth.data

import com.basbasdev.cashette.di.AppModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    val sessionStatus: StateFlow<SessionStatus> get() = supabase.auth.sessionStatus

    val currentUserId: String? get() = supabase.auth.currentUserOrNull()?.id

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    /**
     * `full_name` rides along as user metadata. The `on_auth_user_created` trigger
     * (migration 000014) reads `raw_user_meta_data->>'full_name'` into `public.profiles`,
     * which is what stops a new account from rendering nameless.
     *
     * Email confirmation is off on this project, so this returns an active session and
     * the caller lands straight in the app.
     */
    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                this.data = buildJsonObject { put("full_name", fullName.trim()) }
            }
            Unit
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        supabase.auth.resetPasswordForEmail(
            email = email.trim(),
            redirectUrl = AppModule.RESET_REDIRECT,
        )
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        supabase.auth.updateUser { password = newPassword }
        Unit
    }

    suspend fun signOut(): Result<Unit> = runCatching { supabase.auth.signOut() }
}
