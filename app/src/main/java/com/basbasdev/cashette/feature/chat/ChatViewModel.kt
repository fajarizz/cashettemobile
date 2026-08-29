package com.basbasdev.cashette.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basbasdev.cashette.auth.data.AuthRepository
import com.basbasdev.cashette.data.ChatApi
import com.basbasdev.cashette.data.model.ConfirmRequestDto
import com.basbasdev.cashette.data.model.ParseDto
import com.basbasdev.cashette.data.toDataMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * "yes" to a pending confirmation is a confirmation, not a new question. Mirrors the
 * web's list so the two assistants accept the same words, Indonesian included.
 */
private val CONFIRM_WORDS = setOf(
    "yes", "ya", "iya", "yep", "yup", "sure", "ok", "okay",
    "confirm", "confirmed", "do it", "go ahead", "lanjut", "gas",
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: ChatApi,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** Paging cursor: the created_at of the oldest turn currently held. */
    private var oldest: String? = null

    init {
        loadHistory()
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            runCatching { api.models() }.onSuccess { models ->
                _state.update {
                    it.copy(
                        availableModels = models,
                        selectedModel = it.selectedModel ?: models.firstOrNull(),
                    )
                }
            }
        }
    }

    fun selectModel(model: com.basbasdev.cashette.data.model.ModelInfoDto) {
        _state.update { it.copy(selectedModel = model) }
    }

    private fun loadHistory() {
        val userId = auth.currentUserId ?: return
        viewModelScope.launch {
            runCatching { api.history(userId, limit = 20) }.fold(
                onSuccess = { page ->
                    val messages = page.messages.orEmpty()
                    oldest = messages.firstOrNull()?.createdAt
                    _state.update {
                        it.copy(
                            turns = messages.map(::toTurn),
                            hasMore = page.hasMore,
                            loadingHistory = false,
                            historyError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(loadingHistory = false, historyError = error.toDataMessage())
                    }
                },
            )
        }
    }

    fun retryHistory() {
        _state.update { it.copy(loadingHistory = true, historyError = null) }
        loadHistory()
        loadModels()
    }

    fun loadMore() {
        val userId = auth.currentUserId ?: return
        val cursor = oldest ?: return
        if (_state.value.loadingMore || !_state.value.hasMore) return

        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching { api.history(userId, limit = 20, before = cursor) }.fold(
                onSuccess = { page ->
                    val older = page.messages.orEmpty()
                    oldest = older.firstOrNull()?.createdAt ?: oldest
                    _state.update {
                        it.copy(
                            turns = older.map(::toTurn) + it.turns,
                            hasMore = page.hasMore,
                            loadingMore = false,
                        )
                    }
                },
                onFailure = { _state.update { it.copy(loadingMore = false) } },
            )
        }
    }

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.sending) return

        val pending = _state.value.turns.lastOrNull { it.awaitingAnswer }
        if (message.lowercase() in CONFIRM_WORDS && pending?.parse != null) {
            append(ChatTurn(newId(), Author.USER, message))
            confirm(pending.id, pending.parse)
            return
        }

        val currentModel = _state.value.selectedModel?.id

        append(ChatTurn(newId(), Author.USER, message))
        _state.update { it.copy(thinking = true, sending = true) }

        viewModelScope.launch {
            runCatching { api.parse(message, currentModel) }.fold(
                onSuccess = { parse ->
                    append(
                        ChatTurn(
                            id = newId(),
                            author = Author.ASSISTANT,
                            text = parse.replyMessage?.takeIf { it.isNotBlank() } ?: "Got it.",
                            parse = parse.takeIf { it.hasCard },
                        ),
                    )
                },
                onFailure = { error ->
                    append(
                        ChatTurn(
                            id = newId(),
                            author = Author.ASSISTANT,
                            text = error.toDataMessage(),
                            failed = true,
                        ),
                    )
                },
            )
            _state.update { it.copy(thinking = false, sending = false) }
        }
    }

    fun confirm(turnId: String, parse: ParseDto) {
        val intentId = parse.parsedIntentId ?: return

        // Optimistic: the card settles immediately so the buttons cannot be tapped twice
        // while the request is in flight. Rolled back below if the server refuses.
        setTurn(turnId) { it.copy(confirmed = true) }
        _state.update { it.copy(thinking = true) }

        viewModelScope.launch {
            runCatching {
                api.confirm(
                    ConfirmRequestDto(
                        parsedIntentId = intentId,
                        accountId = parse.accountId,
                        toAccountId = parse.toAccountId,
                        amount = parse.amount,
                        tax = parse.tax,
                    ),
                )
            }.fold(
                onSuccess = { reply ->
                    append(ChatTurn(newId(), Author.ASSISTANT, reply))
                },
                onFailure = { error ->
                    setTurn(turnId) { it.copy(confirmed = false) }
                    append(
                        ChatTurn(
                            id = newId(),
                            author = Author.ASSISTANT,
                            text = error.toDataMessage(),
                            failed = true,
                        ),
                    )
                },
            )
            _state.update { it.copy(thinking = false) }
        }
    }

    fun cancel(turnId: String) = setTurn(turnId) { it.copy(cancelled = true) }

    private fun append(turn: ChatTurn) = _state.update { it.copy(turns = it.turns + turn) }

    private fun setTurn(id: String, transform: (ChatTurn) -> ChatTurn) = _state.update { current ->
        current.copy(turns = current.turns.map { if (it.id == id) transform(it) else it })
    }

    private fun toTurn(dto: com.basbasdev.cashette.data.model.ChatHistoryMessageDto) = ChatTurn(
        id = dto.id,
        author = if (dto.role == "user") Author.USER else Author.ASSISTANT,
        text = dto.message,
        parse = dto.parseData?.takeIf { it.hasCard },
        fromHistory = true,
    )

    private fun newId() = UUID.randomUUID().toString()
}
