package com.basbasdev.cashette.auth.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Serializable
data class ProfileDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("currency_default") val currencyDefault: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class UpdateProfileBody(
    @SerialName("full_name") val fullName: String,
)

@Singleton
class ProfileRepository @Inject constructor(
    private val client: HttpClient,
    @Named("apiBaseUrl") private val baseUrl: String,
) {
    /**
     * Success(null) means the server is reachable and genuinely has no profile row.
     * Anything else — transport failure, 5xx, a blocked request — is a *failure*, never
     * a null. Collapsing the two would push a user who already has a name into the
     * display-name gate every time the backend hiccups.
     */
    suspend fun fetchProfile(userId: String): Result<ProfileDto?> = runCatching {
        val response = client.get("$baseUrl/api/profiles/$userId")
        when {
            response.status == HttpStatusCode.NotFound -> null
            response.status.isSuccess() -> response.body<ProfileDto>()
            else -> error("Profile lookup failed (${response.status.value})")
        }
    }

    suspend fun setDisplayName(userId: String, fullName: String): Result<Unit> = runCatching {
        val response = client.put("$baseUrl/api/profiles/$userId") {
            setBody(UpdateProfileBody(fullName.trim()))
        }
        check(response.status.isSuccess()) { "Failed to save your name (${response.status.value})" }
    }
}
