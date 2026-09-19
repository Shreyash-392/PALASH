package com.palash.voicebridge.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.palash.voicebridge.PalashApp
import com.palash.voicebridge.ui.home.HomeScreen
import com.palash.voicebridge.ui.settings.SettingsScreen
import com.palash.voicebridge.ui.video.VideoScreen
import com.palash.voicebridge.ui.voice.VoiceTranslationScreen
import com.palash.voicebridge.ui.worksheet.WorksheetScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    app: PalashApp
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToVoice = { navController.navigate(Screen.VoiceTranslation.route) },
                onNavigateToWorksheets = { navController.navigate(Screen.Worksheets.route) },
                onNavigateToVideos = { navController.navigate(Screen.VideoLessons.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.VoiceTranslation.route) {
            VoiceTranslationScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Worksheets.route) {
            WorksheetScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.VideoLessons.route) {
            VideoScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
