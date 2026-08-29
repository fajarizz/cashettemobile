package com.basbasdev.cashette.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recording an expense changes Home's hero, History's list, and an account balance on
 * two other screens. The web solves this with `window.location.reload()`; a native app
 * has to say which screens went stale.
 *
 * One counter, bumped after every write. Screens collect it and reload. Coarse on
 * purpose — the payloads are small, and a per-entity graph would be more machinery than
 * the problem deserves.
 */
@Singleton
class LedgerRefresh @Inject constructor() {

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun invalidate() = _revision.update { it + 1 }
}
