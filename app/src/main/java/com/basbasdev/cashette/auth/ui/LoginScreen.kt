package com.basbasdev.cashette.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, formError = null) }

    fun submit() {
        val current = _state.value
        val emailError = if (!current.email.contains("@")) "Enter a valid email" else null
        val passwordError = if (current.password.length < 6) "At least 6 characters" else null

        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            authRepository.signIn(current.email, current.password)
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.toAuthMessage()) }
                }
            // On success the session flips and the nav host swaps graphs; this screen
            // is torn down, so there is nothing to reset.
        }
    }
}

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthScaffold(title = "Welcome back", subtitle = "Sign in to your Cashette account") {
        Column {
            AuthTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = !state.loading,
                error = state.emailError,
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                isPassword = true,
                imeAction = ImeAction.Done,
                enabled = !state.loading,
                error = state.passwordError,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AuthTextLink("Forgot password?", onForgotPassword)
            }

            AuthFormError(state.formError)
            Spacer(Modifier.height(12.dp))

            AuthPrimaryButton(
                text = "Sign in",
                onClick = viewModel::submit,
                loading = state.loading,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "New to Cashette?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AuthTextLink("Create account", onRegister)
            }
        }
    }
}
