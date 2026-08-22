package com.basbasdev.cashette.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.auth.ui.components.AuthFormError
import com.basbasdev.cashette.auth.ui.components.AuthPrimaryButton
import com.basbasdev.cashette.auth.ui.components.AuthScaffold
import com.basbasdev.cashette.auth.ui.components.AuthTextField
import com.basbasdev.cashette.auth.ui.components.AuthTextLink
import com.basbasdev.cashette.auth.ui.components.toAuthMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val sent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) =
        _state.update { it.copy(email = v, emailError = null, formError = null) }

    fun submit() {
        val s = _state.value
        if (!s.email.contains("@")) {
            _state.update { it.copy(emailError = "Enter a valid email") }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            authRepository.sendPasswordReset(s.email)
                .onSuccess {
                    // Never reveal whether the address has an account.
                    _state.update { it.copy(loading = false, sent = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.toAuthMessage()) }
                }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.sent) {
        AuthScaffold(
            title = "Check your email",
            subtitle = "If an account exists for ${state.email}, a reset link is on its way. " +
                "Open it on this device and it will bring you straight back here.",
        ) {
            AuthPrimaryButton(text = "Back to sign in", onClick = onBack)
        }
        return
    }

    AuthScaffold(
        title = "Reset password",
        subtitle = "We'll email you a link to set a new one",
    ) {
        Column {
            AuthTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !state.loading,
                error = state.emailError,
            )

            AuthFormError(state.formError)
            Spacer(Modifier.height(20.dp))

            AuthPrimaryButton(
                text = "Send reset link",
                onClick = viewModel::submit,
                loading = state.loading,
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AuthTextLink("Back to sign in", onBack)
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onDone: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.done) {
        AuthScaffold(title = "Password updated", subtitle = "You're all set.") {
            AuthPrimaryButton(text = "Continue", onClick = onDone)
        }
        return
    }

    AuthScaffold(title = "Set a new password", subtitle = "Choose something you'll remember") {
        Column {
            AuthTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "New password",
                isPassword = true,
                enabled = !state.loading,
                error = state.passwordError,
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmChange,
                label = "Confirm password",
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = !state.loading,
                error = state.confirmError,
            )

            AuthFormError(state.formError)
            Spacer(Modifier.height(20.dp))

            AuthPrimaryButton(
                text = "Update password",
                onClick = viewModel::submit,
                loading = state.loading,
            )
        }
    }
}

data class ResetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResetPasswordUiState())
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    fun onPasswordChange(v: String) =
        _state.update { it.copy(password = v, passwordError = null, formError = null) }

    fun onConfirmChange(v: String) =
        _state.update { it.copy(confirmPassword = v, confirmError = null, formError = null) }

    fun submit() {
        val s = _state.value
        val passwordError = if (s.password.length < 6) "At least 6 characters" else null
        val confirmError = if (s.password != s.confirmPassword) "Passwords do not match" else null

        if (passwordError != null || confirmError != null) {
            _state.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            authRepository.updatePassword(s.password)
                .onSuccess { _state.update { it.copy(loading = false, done = true) } }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.toAuthMessage()) }
                }
        }
    }
}
