package com.basbasdev.cashette.feature.chat

import com.basbasdev.cashette.data.model.ModelInfoDto
import com.basbasdev.cashette.data.model.ParseDto

enum class Author { USER, ASSISTANT }

data class ChatTurn(
    val id: String,
    val author: Author,
    val text: String,
    val parse: ParseDto? = null,
    val confirmed: Boolean = false,
    val cancelled: Boolean = false,
    val fromHistory: Boolean = false,
    val failed: Boolean = false,
) {
    val settled: Boolean get() = confirmed || cancelled || fromHistory

    val awaitingAnswer: Boolean
        get() = parse?.requiresConfirmation == true && !settled
}

data class ChatUiState(
    val turns: List<ChatTurn> = emptyList(),
    val loadingHistory: Boolean = true,
    val thinking: Boolean = false,
    val sending: Boolean = false,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val historyError: String? = null,
    val availableModels: List<ModelInfoDto> = emptyList(),
    val selectedModel: ModelInfoDto? = null,
) {
    val empty: Boolean get() = turns.isEmpty() && !loadingHistory
}

