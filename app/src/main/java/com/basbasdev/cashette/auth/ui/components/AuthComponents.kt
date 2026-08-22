package com.basbasdev.cashette.auth.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.basbasdev.cashette.BuildConfig
import com.basbasdev.cashette.ui.theme.CashetteShape

/**
 * The web centres a max-w-sm card on the background. On a phone that card would be a
 * box drawn around the whole screen, so the same rhythm runs full-bleed instead.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            content()
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    error: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            isError = error != null,
            shape = CashetteShape.Field,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CashetteShape.Pill,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (loading) {
            // The Expressive shape-morphing indicator, not a spinner.
            Box(contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.size(24.dp))
            }
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AuthTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AuthFormError(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Text(
            text = message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

/** Supabase speaks in API strings; users need a sentence they can act on. */
fun Throwable.toAuthMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("Invalid login credentials", true) -> "Email or password is incorrect."
        raw.contains("Email not confirmed", true) -> "Confirm your email address first."
        raw.contains("User already registered", true) ||
            raw.contains("already been registered", true) -> "That email already has an account."
        raw.contains("Password should be", true) -> "Password must be at least 6 characters."
        raw.contains("same as the old", true) -> "That is already your password."
        raw.contains("rate limit", true) || raw.contains("too many", true) ->
            "Too many attempts. Wait a minute and try again."
        this is java.net.UnknownHostException ||
            this is java.net.ConnectException ||
            this is java.net.SocketTimeoutException -> "Can't reach Cashette. Check your connection."
        // Dev-only misconfiguration: the backend is on plain HTTP and the build has no
        // cleartext exemption. Never shown in release, where HTTPS is required anyway.
        raw.contains("CLEARTEXT", true) ->
            "Can't reach Cashette. (Cleartext HTTP blocked — check the debug network config.)"
        raw.isBlank() -> "Something went wrong. Please try again."
        // A raw exception string is a developer's tool, not an answer for the user.
        else -> if (BuildConfig.DEBUG) raw else "Something went wrong. Please try again."
    }
}
