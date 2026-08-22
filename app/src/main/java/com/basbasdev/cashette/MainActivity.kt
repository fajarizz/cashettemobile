package com.basbasdev.cashette

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.basbasdev.cashette.auth.SessionViewModel
import com.basbasdev.cashette.navigation.CashetteNavHost
import com.basbasdev.cashette.ui.theme.CashetteTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark-only app: pin the system bars to light icons rather than letting them
        // follow the device theme, or a light-mode phone draws dark icons on olive.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handleAuthDeeplink(intent)

        setContent {
            CashetteTheme {
                CashetteNavHost(sessionViewModel = sessionViewModel)
            }
        }
    }

    // launchMode is singleTask, so a recovery link tapped while the app is running
    // arrives here rather than starting a second activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeeplink(intent)
    }

    /**
     * Supabase password-recovery links come back as cashette://auth#access_token=...
     * handleDeeplinks establishes the session; the recovery flag then pins the UI to
     * the reset screen so this short-lived session cannot be used to browse the app.
     */
    private fun handleAuthDeeplink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != AUTH_SCHEME) return

        supabase.handleDeeplinks(
            intent = intent,
            onSessionSuccess = { sessionViewModel.enterRecoveryMode() },
        )
    }

    private companion object {
        const val AUTH_SCHEME = "cashette"
    }
}
