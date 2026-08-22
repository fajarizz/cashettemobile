package com.basbasdev.cashette.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.auth.data.ProfileRepository
import com.basbasdev.cashette.auth.ui.components.toAuthMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the user is allowed to see. Mirrors cashetteweb's two gates: the `_app` route
 * guard on the token, then the display-name modal.
 */
sealed interface SessionState {
    /** Restoring a persisted session. Hold the splash — never flash the login screen. */
    data object Loading : SessionState

    data class SignedOut(val message: String? = null) : SessionState

    /** Authenticated, but the profile has no name yet. Blocks the app. */
    data class NeedsDisplayName(val userId: String) : SessionState

    /**
     * Signed in, but the profile could not be read. Distinct from [NeedsDisplayName] on
     * purpose: a backend outage must not look like a missing name, or a user who already
     * has one gets asked for it again every time the network blips.
     */
    data class Unavailable(val message: String) : SessionState

    data class Ready(val userId: String, val displayName: String) : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    /** Bumped after the display-name step so the profile is re-read. */
    private val refreshKey = MutableStateFlow(0)

    private val _recoveryMode = MutableStateFlow(false)

    /** True while a password-recovery deep link is being completed. */
    val recoveryMode: StateFlow<Boolean> = _recoveryMode.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SessionState> =
        combine(authRepository.sessionStatus, refreshKey) { status, _ -> status }
            .mapLatest { resolve(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SessionState.Loading,
            )

    private suspend fun resolve(status: SessionStatus): SessionState = when (status) {
        is SessionStatus.Initializing -> SessionState.Loading

        is SessionStatus.NotAuthenticated -> SessionState.SignedOut()

        is SessionStatus.RefreshFailure -> SessionState.SignedOut(
            "Your session expired. Please sign in again.",
        )

        is SessionStatus.Authenticated -> {
            val userId = status.session.user?.id
            if (userId == null) {
                SessionState.SignedOut()
            } else {
                profileRepository.fetchProfile(userId).fold(
                    onSuccess = { profile ->
                        val name = profile?.fullName?.trim().orEmpty()
                        if (name.isEmpty()) {
                            SessionState.NeedsDisplayName(userId)
                        } else {
                            SessionState.Ready(userId, name)
                        }
                    },
                    onFailure = { SessionState.Unavailable(it.toAuthMessage()) },
                )
            }
        }
    }

    fun onDisplayNameSaved() {
        refreshKey.value += 1
    }

    /** Re-runs the profile lookup after an [SessionState.Unavailable]. */
    fun retry() {
        refreshKey.value += 1
    }

    fun enterRecoveryMode() {
        _recoveryMode.value = true
    }

    fun exitRecoveryMode() {
        _recoveryMode.value = false
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
