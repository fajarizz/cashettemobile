package com.basbasdev.cashette.di

import android.content.Context
import com.basbasdev.cashette.BuildConfig
import com.basbasdev.cashette.core.session.EncryptedSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context,
        json: Json,
    ): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            // Password-recovery links come back as cashette://auth. This pair must match
            // the manifest intent filter and the Supabase dashboard's Redirect URLs.
            scheme = AUTH_SCHEME
            host = AUTH_HOST
            sessionManager = EncryptedSessionManager(context, json)
        }
    }

    /**
     * Talks to the Go backend, not to Supabase. Auth is Supabase's job; every row of
     * data belongs to the API, because RLS is deny-all and the anon key opens nothing.
     */
    @Provides
    @Singleton
    fun provideApiClient(
        supabase: SupabaseClient,
        json: Json,
    ): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false

        install(ContentNegotiation) { json(json) }

        defaultRequest {
            contentType(ContentType.Application.Json)
            // Read per request: supabase-kt rotates the access token in the background.
            supabase.auth.currentAccessTokenOrNull()?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }

        // The web patches window.fetch to bounce to /login on a 401; this is the same
        // guarantee. Dropping the session flips SessionState and the nav host swaps
        // graphs, so a stale token can never leave a half-authenticated screen up.
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                    scope.launch { runCatching { supabase.auth.signOut() } }
                }
            }
        }
    }

    @Provides
    @Singleton
    @Named("apiBaseUrl")
    fun provideApiBaseUrl(): String = BuildConfig.API_URL.trimEnd('/')

    const val AUTH_SCHEME = "cashette"
    const val AUTH_HOST = "auth"
    const val RESET_REDIRECT = "$AUTH_SCHEME://$AUTH_HOST"
}
