package com.basbasdev.cashette.data

import com.basbasdev.cashette.BuildConfig
import java.io.IOException

/**
 * A section that failed to load is not an auth failure, and must not borrow that
 * vocabulary. Names the problem and the recovery, in one line that fits beside a Retry.
 */
fun Throwable.toDataMessage(): String = when {
    this is IOException ||
        this is java.net.UnknownHostException ||
        this is java.net.ConnectException ||
        this is java.net.SocketTimeoutException -> "Can't reach Cashette."

    message?.contains("(401)") == true -> "Your session expired."
    message?.contains("(5") == true -> "Cashette's server had a problem."

    // A serialization or contract mismatch is a developer's problem, not the user's —
    // but hiding it in debug is how it survives to release.
    else -> if (BuildConfig.DEBUG) "Couldn't load: ${message.orEmpty().take(120)}"
    else "Couldn't load this."
}
