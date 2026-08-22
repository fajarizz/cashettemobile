package com.basbasdev.cashette.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.ProfileRepository
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

data class DisplayNameUiState(
    val name: String = "",
    val nameError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
)

@HiltViewModel
class DisplayNameViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DisplayNameUiState())
    val state: StateFlow<DisplayNameUiState> = _state.asStateFlow()

    fun onNameChange(v: String) =
        _state.update { it.copy(name = v, nameError = null, formError = null) }

    fun submit(userId: String, onSaved: () -> Unit) {
        val name = _state.value.name.trim()
        if (name.length < 2) {
            _state.update { it.copy(nameError = "At least 2 characters") }
            return
        }

        _state.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            profileRepository.setDisplayName(userId, name)
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                    onSaved()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, formError = error.toAuthMessage()) }
                }
        }
    }
}

/**
 * The gate for an authenticated account with no name. Not dismissible and no back
 * affordance — same contract as the web's DisplayNameModal. Signing out is the only
 * way past it, which is the honest escape hatch.
 */
@Composable
fun DisplayNameScreen(
    userId: String,
    onSaved: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DisplayNameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = true) { /* deliberately inert */ }

    AuthScaffold(
        title = "What should we call you?",
        subtitle = "Pick a display name. You can change it later in Settings.",
    ) {
        Column {
            AuthTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Display name",
                imeAction = ImeAction.Done,
                enabled = !state.loading,
                error = state.nameError,
            )

            AuthFormError(state.formError)
            Spacer(Modifier.height(20.dp))

            AuthPrimaryButton(
                text = "Continue",
                onClick = { viewModel.submit(userId, onSaved) },
                loading = state.loading,
            )

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AuthTextLink("Sign out", onSignOut)
            }
        }
    }
}
