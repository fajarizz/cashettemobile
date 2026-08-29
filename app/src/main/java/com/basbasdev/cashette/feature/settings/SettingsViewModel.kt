package com.basbasdev.cashette.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.auth.ui.components.toAuthMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val displayName: String = "",
    val initialDisplayName: String = "",
    val updatingProfile: Boolean = false,
    val profileMessage: String? = null,
    val profileError: String? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val updatingPassword: Boolean = false,
    val passwordMessage: String? = null,
    val passwordError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        val email = auth.currentUserEmail.orEmpty()
        val name = auth.currentFullName.orEmpty()
        _state.update {
            it.copy(
                email = email,
                displayName = name,
                initialDisplayName = name,
            )
        }
    }

    fun setDisplayName(name: String) {
        _state.update { it.copy(displayName = name, profileError = null, profileMessage = null) }
    }

    fun saveProfile(onSuccess: (String) -> Unit = {}) {
        val name = _state.value.displayName.trim()
        if (name.length < 2) {
            _state.update { it.copy(profileError = "Name must be at least 2 characters.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(updatingProfile = true, profileError = null, profileMessage = null) }
            auth.updateProfile(name)
                .onSuccess {
                    _state.update {
                        it.copy(
                            updatingProfile = false,
                            initialDisplayName = name,
                            profileMessage = "Display name updated successfully.",
                        )
                    }
                    onSuccess(name)
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            updatingProfile = false,
                            profileError = err.toAuthMessage(),
                        )
                    }
                }
        }
    }

    fun setNewPassword(p: String) {
        _state.update { it.copy(newPassword = p, passwordError = null, passwordMessage = null) }
    }

    fun setConfirmPassword(p: String) {
        _state.update { it.copy(confirmPassword = p, passwordError = null, passwordMessage = null) }
    }

    fun updatePassword() {
        val p1 = _state.value.newPassword
        val p2 = _state.value.confirmPassword

        if (p1.length < 6) {
            _state.update { it.copy(passwordError = "Password must be at least 6 characters.") }
            return
        }
        if (p1 != p2) {
            _state.update { it.copy(passwordError = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(updatingPassword = true, passwordError = null, passwordMessage = null) }
            auth.updatePassword(p1)
                .onSuccess {
                    _state.update {
                        it.copy(
                            updatingPassword = false,
                            newPassword = "",
                            confirmPassword = "",
                            passwordMessage = "Password updated successfully.",
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            updatingPassword = false,
                            passwordError = err.toAuthMessage(),
                        )
                    }
                }
        }
    }
}
