package com.basbasdev.cashette.feature.chat

import com.basbasdev.cashette.data.model.ParseDto

enum class Author { USER, ASSISTANT }

/**
 * One turn in the conversation. [parse] is attached only when the reply carries
 * something worth rendering as a card.
 *
 * [settled] covers both outcomes and history alike: a card the user already answered,
 * or one restored from a past session, shows its result and offers no buttons. Letting
 * a reloaded conversation re-fire a confirmation would double-record the money.
 */
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
    /** The assistant is composing a reply. */
    val thinking: Boolean = false,
    val sending: Boolean = false,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val historyError: String? = null,
) {
    val empty: Boolean get() = turns.isEmpty() && !loadingHistory
}
