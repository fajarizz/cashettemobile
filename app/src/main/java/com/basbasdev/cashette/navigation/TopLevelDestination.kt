package com.basbasdev.cashette.navigation

import androidx.annotation.DrawableRes
import com.basbasdev.cashette.R

/**
 * The bottom bar. Four destinations, each a noun the user recognises and a place they
 * go to do something Home cannot do — nothing here is a menu of other screens.
 *
 * Budget, Subscriptions, Analytics, Accounts, Pockets, Debt and Settings sit one level
 * down, reached from the Home card or Money section that already previews them.
 */
enum class TopLevelDestination(
    val route: Any,
    @DrawableRes val icon: Int,
    val label: String,
    /** Chat's composer owns the bottom edge; a FAB over it would collide. */
    val showsFab: Boolean,
) {
    HOME(HomeRoute, R.drawable.ic_home, "Home", showsFab = true),
    CHAT(ChatRoute, R.drawable.ic_chat, "Chat", showsFab = false),
    HISTORY(HistoryRoute, R.drawable.ic_history, "History", showsFab = true),
    MONEY(MoneyRoute, R.drawable.ic_money, "Money", showsFab = true),
}
