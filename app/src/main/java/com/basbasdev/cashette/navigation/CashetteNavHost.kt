package com.basbasdev.cashette.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.basbasdev.cashette.auth.SessionState
import com.basbasdev.cashette.auth.SessionViewModel
import com.basbasdev.cashette.auth.ui.DisplayNameScreen
import com.basbasdev.cashette.auth.ui.components.AuthPrimaryButton
import com.basbasdev.cashette.auth.ui.components.AuthScaffold
import com.basbasdev.cashette.auth.ui.components.AuthTextLink
import com.basbasdev.cashette.auth.ui.ForgotPasswordScreen
import com.basbasdev.cashette.auth.ui.LoginScreen
import com.basbasdev.cashette.auth.ui.RegisterScreen
import com.basbasdev.cashette.auth.ui.ResetPasswordScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object ForgotPasswordRoute

@Serializable
object ResetPasswordRoute

/**
 * Session state chooses the graph; screens never navigate across that boundary
 * themselves. Swapping the whole NavHost means a signed-out user has no signed-in
 * back stack left to return to, and vice versa.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CashetteNavHost(sessionViewModel: SessionViewModel = hiltViewModel()) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()
    val recoveryMode by sessionViewModel.recoveryMode.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = session to recoveryMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "session",
    ) { (state, recovering) ->
        when {
            // A recovery deep link authenticates the user, but the only thing they
            // may do with that session is set a new password.
            recovering -> ResetPasswordScreen(onDone = sessionViewModel::exitRecoveryMode)

            state is SessionState.Loading -> Splash()

            state is SessionState.SignedOut ->
                AuthGraph(navController = rememberNavController())

            state is SessionState.NeedsDisplayName ->
                DisplayNameScreen(
                    userId = state.userId,
                    onSaved = sessionViewModel::onDisplayNameSaved,
                    onSignOut = sessionViewModel::signOut,
                )

            state is SessionState.Unavailable ->
                Unavailable(
                    message = state.message,
                    onRetry = sessionViewModel::retry,
                    onSignOut = sessionViewModel::signOut,
                )

            state is SessionState.Ready ->
                AppNavHost(
                    displayName = state.displayName,
                    onSignOut = sessionViewModel::signOut,
                )
        }
    }
}

@Composable
private fun AuthGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(
                onRegister = { navController.navigate(RegisterRoute) },
                onForgotPassword = { navController.navigate(ForgotPasswordRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(onSignIn = { navController.popBackStack() })
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Splash() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    }
}

/**
 * Signed in, but the profile could not be read. Says so and offers a retry, rather
 * than pretending the account has no name.
 */
@Composable
private fun Unavailable(message: String, onRetry: () -> Unit, onSignOut: () -> Unit) {
    AuthScaffold(
        title = "Can't load your account",
        subtitle = message,
    ) {
        Column {
            AuthPrimaryButton(text = "Try again", onClick = onRetry)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AuthTextLink("Sign out", onSignOut)
            }
        }
    }
}
