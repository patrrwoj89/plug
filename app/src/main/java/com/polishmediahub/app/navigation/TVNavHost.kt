package com.polishmediahub.app.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.polishmediahub.app.ui.components.Sidebar
import com.polishmediahub.app.ui.screens.AdminScreen
import com.polishmediahub.app.ui.screens.AnimeScreen
import com.polishmediahub.app.ui.screens.CustomListsScreen
import com.polishmediahub.app.ui.screens.DetailScreen
import com.polishmediahub.app.ui.screens.DownloadsScreen
import com.polishmediahub.app.ui.screens.HomeScreen
import com.polishmediahub.app.ui.screens.LibraryScreen
import com.polishmediahub.app.ui.screens.MusicScreen
import com.polishmediahub.app.ui.screens.PlayerScreen
import com.polishmediahub.app.ui.screens.SearchScreen
import com.polishmediahub.app.ui.screens.SettingsScreen
import com.polishmediahub.app.ui.screens.WatchlistScreen

@Composable
fun TVApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = currentRoute?.let { route ->
        when {
            route.startsWith(Screen.Home.route) -> Screen.Home
            route.startsWith(Screen.Search.route) -> Screen.Search
            route.startsWith(Screen.Library.route) -> Screen.Library
            route.startsWith(Screen.Watchlist.route) -> Screen.Watchlist
            route.startsWith(Screen.Anime.route) -> Screen.Anime
            route.startsWith(Screen.Music.route) -> Screen.Music
            route.startsWith(Screen.Downloads.route) -> Screen.Downloads
            route.startsWith(Screen.CustomLists.route) -> Screen.CustomLists
            route.startsWith(Screen.Settings.route) -> Screen.Settings
            route.startsWith(Screen.Admin.route) -> Screen.Admin
            route.startsWith("detail/") -> Screen.Detail(
                navBackStackEntry?.arguments?.getString("id") ?: ""
            )
            route.startsWith("player/") -> Screen.Player(
                navBackStackEntry?.arguments?.getString("id") ?: ""
            )
            else -> Screen.Home
        }
    } ?: Screen.Home

    Row(modifier = modifier.fillMaxSize()) {
        Sidebar(
            current = currentScreen,
            onNavigate = { screen ->
                navController.navigate(screen.route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.fillMaxHeight()
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Search.route) {
                SearchScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Library.route) {
                LibraryScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Watchlist.route) {
                WatchlistScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Anime.route) {
                AnimeScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Music.route) {
                MusicScreen(
                    onPlay = { track ->
                        track.streamUrl?.let { navController.navigate(Screen.Player(track.id).route) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(modifier = Modifier.fillMaxSize())
            }
            composable(Screen.CustomLists.route) {
                CustomListsScreen(modifier = Modifier.fillMaxSize())
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Admin.route) {
                AdminScreen(modifier = Modifier.fillMaxSize())
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) {
                DetailScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                route = "player/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "polishmediahub://play/{id}" })
            ) {
                PlayerScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
