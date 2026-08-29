package com.basbasdev.cashette.navigation

import kotlinx.serialization.Serializable

// Every signed-in destination, mirroring a cashetteweb route. Typed rather than string
// paths so a rename is a compile error instead of a blank screen at runtime.

// ── Top level: the four bottom-bar destinations ──────────────────────────────

@Serializable
object HomeRoute

@Serializable
object ChatRoute

@Serializable
object HistoryRoute

/** The balance sheet: accounts, pockets and debt under one net position. */
@Serializable
object MoneyRoute

// ── One level down: reached from Home or from a Money section ────────────────

@Serializable
data class AccountsRoute(val initialAccountId: String? = null)

@Serializable
object AnalyticsRoute

@Serializable
object BudgetRoute

@Serializable
object DebtRoute

@Serializable
object PocketsRoute

@Serializable
object SettingsRoute

@Serializable
object SubscriptionsRoute

@Serializable
object EdgingRoute
