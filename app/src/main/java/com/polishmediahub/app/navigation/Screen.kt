package com.polishmediahub.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Watchlist : Screen("watchlist")
    data object Anime : Screen("anime")
    data object Music : Screen("music")
    data object Settings : Screen("settings")
    data object Admin : Screen("admin")
    data object Downloads : Screen("downloads")
    data object CustomLists : Screen("custom_lists")

    data class Detail(val id: String) : Screen("detail/$id")
    data class Player(val id: String) : Screen("player/$id")
}
