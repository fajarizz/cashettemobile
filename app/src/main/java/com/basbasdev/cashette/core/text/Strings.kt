package com.basbasdev.cashette.core.text

import java.util.Locale

/**
 * Transaction notes arrive as they were typed or parsed — "sate", "nasi goreng", "kopi".
 * A ledger reads as a list of names, and a column of lowercase entries reads as unfinished.
 *
 * Only the first character is touched. Title-casing every word would turn a written note
 * into a headline ("Bought Coffee At The Station") and flatten names that carry their own
 * capitals, like GoPay or iPhone. Locale is pinned to English so the mapping cannot change
 * under a Turkish locale, where lowercase i uppercases to a dotted İ.
 */
fun String.sentenceCase(): String = replaceFirstChar { it.titlecase(Locale.ENGLISH) }
