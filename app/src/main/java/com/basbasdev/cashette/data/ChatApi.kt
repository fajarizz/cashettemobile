package com.basbasdev.cashette.data

import com.basbasdev.cashette.data.model.ChatHistoryDto
import com.basbasdev.cashette.data.model.ConfirmRequestDto
import com.basbasdev.cashette.data.model.ConfirmResponseDto
import com.basbasdev.cashette.data.model.ParseDto
import com.basbasdev.cashette.data.model.ParseRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Both assistant calls wait on inference, which runs 8-15s and occasionally longer under
 * load. The client's default is deliberately tighter than this — a stalled ledger request
 * should fail while the user is still looking at it — so these two raise it for
 * themselves. Hanging up early does not just show an error: it cancels the request
 * context the Go handler is holding, which aborts the model call already in flight.
 */
private const val ASSISTANT_TIMEOUT_MS = 90_000L

@Singleton
class ChatApi @Inject constructor(
    private val client: HttpClient,
    @Named("apiBaseUrl") private val baseUrl: String,
) {
    /** Newest last. `before` pages backwards using the oldest message's created_at. */
    suspend fun history(userId: String, limit: Int = 20, before: String? = null): ChatHistoryDto {
        val response = client.get("$baseUrl/api/chat") {
            parameter("user_id", userId)
            parameter("limit", limit)
            before?.let { parameter("before", it) }
        }
        if (!response.status.isSuccess()) error("Couldn't load your conversation.")
        return response.body()
    }

    /**
     * Sends a message to the assistant. Nothing is committed here — a reply that wants
     * to change money comes back with `requires_confirmation` and waits.
     */
    suspend fun parse(message: String): ParseDto {
        val response = client.post("$baseUrl/api/chat/parse") {
            setBody(ParseRequestDto(message))
            timeout {
                requestTimeoutMillis = ASSISTANT_TIMEOUT_MS
                socketTimeoutMillis = ASSISTANT_TIMEOUT_MS
            }
        }
        if (!response.status.isSuccess()) {
            // The assistant is a shared, rate-limited resource; that is a state the user
            // can act on, not a generic failure.
            if (response.status == HttpStatusCode.TooManyRequests) {
                error("The assistant is busy right now. Give it a moment and try again.")
            }
            error("The assistant couldn't answer that.")
        }
        return response.body()
    }

    /** Commits the pending intent. This is the call that actually moves money. */
    suspend fun confirm(request: ConfirmRequestDto): String {
        val response = client.post("$baseUrl/api/chat/confirm") {
            setBody(request)
            timeout {
                requestTimeoutMillis = ASSISTANT_TIMEOUT_MS
                socketTimeoutMillis = ASSISTANT_TIMEOUT_MS
            }
        }
        val body = runCatching { response.body<ConfirmResponseDto>() }.getOrNull()
        if (!response.status.isSuccess()) {
            error(body?.error ?: "Couldn't confirm that.")
        }
        return body?.message ?: "Done."
    }
}
