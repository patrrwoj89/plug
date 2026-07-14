package com.tvhub.skeleton.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Watchlist : Screen("watchlist")
    data object Settings : Screen("settings")

    data class Detail(val id: String) : Screen("detail/$id")
    data class Player(val id: String) : Screen("player/$id")
}
