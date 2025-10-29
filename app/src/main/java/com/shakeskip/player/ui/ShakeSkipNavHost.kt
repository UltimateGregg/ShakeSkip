package com.shakeskip.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shakeskip.player.ui.playback.PlaybackScreen
import com.shakeskip.player.ui.settings.SettingsScreen

private object Destinations {
    const val PLAYBACK = "playback"
    const val SETTINGS = "settings"
}

@Composable
fun ShakeSkipNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.PLAYBACK,
        modifier = modifier
    ) {
        composable(Destinations.PLAYBACK) {
            PlaybackScreen(
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) }
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
