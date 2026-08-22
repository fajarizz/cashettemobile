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
import androidx.compose.ui.Alignment
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

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(fullName = v, nameError = null, formError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, formError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null, formError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null, formError = null) }

    fun submit() {
        val s = _state.value
        val nameError = if (s.fullName.trim().length < 2) "Enter your name" else null
        val emailError = if (!s.email.contains("@")) "Enter a valid email" else null
        val passwordError = if (s.password.length < 6) "At least 6 characters" else null
        val confirmError = if (s.password != s.confirmPassword) "Passwords do not match" else null

        if (listOfNotNull(nameError, emailError, passwordError, confirmError).isNotEmpty()) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                )
            }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            authRepository.signUp(s.email, s.password, s.fullName)
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.toAuthMessage()) }
                }
        }
    }
}

@Composable
fun RegisterScreen(
    onSignIn: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AuthScaffold(title = "Create account", subtitle = "Start managing your finances") {
        Column {
            AuthTextField(
                value = state.fullName,
                onValueChange = viewModel::onNameChange,
                label = "Display name",
                enabled = !state.loading,
                error = state.nameError,
            )
            Spacer(Modifier.height(12.dp))
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
                text = "Create account",
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
                    "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AuthTextLink("Sign in", onSignIn)
            }
        }
    }
}
