package com.polishmediahub.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polishmediahub.app.ui.components.Sidebar
import com.polishmediahub.app.ui.screens.AdminScreen
import com.polishmediahub.app.ui.screens.AnimeScreen
import com.polishmediahub.app.ui.screens.CustomListDetailScreen
import com.polishmediahub.app.ui.screens.CustomListsScreen
import com.polishmediahub.app.ui.screens.DetailScreen
import com.polishmediahub.app.ui.screens.DownloadsScreen
import com.polishmediahub.app.ui.screens.EpgScreen
import com.polishmediahub.app.ui.screens.EssentialSetupScreen
import com.polishmediahub.app.ui.screens.HomeScreen
import com.polishmediahub.app.ui.screens.LibraryScreen
import com.polishmediahub.app.ui.screens.MusicScreen
import com.polishmediahub.app.ui.screens.PlayerScreen
import com.polishmediahub.app.ui.screens.SearchScreen
import com.polishmediahub.app.ui.screens.SettingsScreen
import com.polishmediahub.app.ui.screens.TorrentsScreen
import com.polishmediahub.app.ui.screens.WatchlistScreen
import com.polishmediahub.app.ui.viewmodel.ProfileViewModel
import com.polishmediahub.app.ui.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun TVApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val activity = androidx.activity.compose.LocalActivity.current
    LaunchedEffect(Unit) {
        activity?.intent?.let { navController.handleDeepLink(it) }
    }

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
            route.startsWith(Screen.Epg.route) -> Screen.Epg
            route.startsWith(Screen.Torrents.route) -> Screen.Torrents
            route.startsWith(Screen.Downloads.route) -> Screen.Downloads
            route.startsWith(Screen.CustomLists.route) -> Screen.CustomLists
            route.startsWith(Screen.Settings.route) -> Screen.Settings
            route.startsWith(Screen.Admin.route) -> Screen.Admin
            route.startsWith(Screen.EssentialSetup.route) -> Screen.EssentialSetup
            route.startsWith("detail/") -> Screen.Detail(
                navBackStackEntry?.arguments?.getString("id") ?: ""
            )
            route.startsWith("player/") -> Screen.Player(
                navBackStackEntry?.arguments?.getString("id") ?: ""
            )
            route.startsWith("custom_list/") -> Screen.CustomListDetail(
                navBackStackEntry?.arguments?.getString("listId") ?: ""
            )
            else -> Screen.Home
        }
    } ?: Screen.Home

    val profileViewModel = hiltViewModel<ProfileViewModel>()
    val currentProfile by profileViewModel.currentProfile.collectAsStateWithLifecycle()
    val profiles by profileViewModel.profiles.collectAsStateWithLifecycle()

    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val firstLaunch by settingsViewModel.isFirstLaunch.collectAsStateWithLifecycle()
    val isFirstLaunch = firstLaunch ?: run {
        Box(modifier = modifier.fillMaxSize())
        return
    }
    val startDestination = if (isFirstLaunch) Screen.EssentialSetup.route else Screen.Home.route

    val hazeState: HazeState = rememberHazeState()
    var sidebarExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = sidebarExpanded || currentScreen == Screen.Home) {
        if (sidebarExpanded) {
            sidebarExpanded = false
        } else if (currentScreen == Screen.Home) {
            sidebarExpanded = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    !sidebarExpanded &&
                    currentScreen !is Screen.Player &&
                    currentScreen != Screen.EssentialSetup
                ) {
                    sidebarExpanded = true
                    true
                } else {
                    false
                }
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.EssentialSetup.route) {
                EssentialSetupScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.EssentialSetup.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
            composable(Screen.Epg.route) {
                EpgScreen(
                    onNavigate = { screen ->
                        when (screen) {
                            is Screen.Player -> navController.navigate(screen.route)
                            else -> navController.navigate(screen.route)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.Torrents.route) {
                TorrentsScreen(
                    onPlay = { navController.navigate(Screen.Player(it.id).route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(modifier = Modifier.fillMaxSize())
            }
            composable(Screen.CustomLists.route) {
                CustomListsScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                route = "custom_list/{listId}",
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) {
                CustomListDetailScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                route = Screen.Settings.route,
                deepLinks = listOf(navDeepLink { uriPattern = "polishmediahub://settings" })
            ) {
                SettingsScreen(onNavigate = { navController.navigate(it.route) })
            }
            composable(Screen.Admin.route) {
                AdminScreen(
                    onNavigate = { navController.navigate(it.route) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "polishmediahub://detail/{id}" })
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

        if (currentScreen != Screen.EssentialSetup) {
            Sidebar(
                current = currentScreen,
                expanded = sidebarExpanded,
                onExpandedChange = { sidebarExpanded = it },
                hazeState = hazeState,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                profile = currentProfile,
                profiles = profiles,
                onSelectProfile = { profileViewModel.selectProfile(it) },
                onVerifyPin = { profile, code -> profileViewModel.verifyPin(profile, code) },
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}
