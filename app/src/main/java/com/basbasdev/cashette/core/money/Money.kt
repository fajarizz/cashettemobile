package com.basbasdev.cashette.core.money

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

// The API sends amounts as decimal strings. Parsing them into Double loses cents on
// large IDR figures, so everything downstream of the network layer is BigDecimal.

private val ID = Locale.forLanguageTag("id-ID")

private val GROUPED = DecimalFormat(
    "#,##0",
    DecimalFormatSymbols(ID).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    },
)

/** A malformed or absent amount is zero, never a crash and never a silent NaN. */
fun String?.toAmount(): BigDecimal =
    this?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO

/** `Rp 2.610.000`. IDR has no practical minor unit, so fractions are rounded away. */
fun BigDecimal.toIdr(): String = "Rp ${GROUPED.format(this)}"

/** Same figure, signed, for a ledger row: `+Rp 40.000` / `−Rp 25.000`. */
fun BigDecimal.toSignedIdr(negative: Boolean): String =
    (if (negative) "−" else "+") + toIdr()

/**
 * `Rp 2,6jt`. For chips and axis labels where the full figure would wrap. Never for a
 * hero or a ledger row — a rounded number the user cannot reconcile is worse than one
 * that wraps.
 */
fun BigDecimal.toIdrCompact(): String {
    val v = toDouble()
    val (scaled, suffix) = when {
        v >= 1_000_000_000 -> v / 1_000_000_000 to "m"
        v >= 1_000_000 -> v / 1_000_000 to "jt"
        v >= 1_000 -> v / 1_000 to "rb"
        else -> return toIdr()
    }
    val text = if (scaled >= 10) "%.0f".format(ID, scaled) else "%.1f".format(ID, scaled)
    return "Rp $text$suffix"
}

/**
 * What TalkBack should say. Read literally, `Rp 2.610.000` becomes "R P two point six
 * one zero point zero zero zero", so every money figure needs this as its
 * contentDescription.
 */
fun BigDecimal.toSpokenIdr(): String = "${toBigInteger()} rupiah"

fun BigDecimal.isZero(): Boolean = signum() == 0
