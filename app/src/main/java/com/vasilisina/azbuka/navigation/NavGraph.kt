// Сохранить в app/src/main/java/com/vasilisina/azbuka/navigation/NavGraph.kt

package com.vasilisina.azbuka.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.menu.MainMenuScreen
import com.vasilisina.azbuka.ui.map.MapScreen
import com.vasilisina.azbuka.ui.album.ProgressAlbumScreen
import com.vasilisina.azbuka.ui.fairytale.FairyTaleScreen
import com.vasilisina.azbuka.ui.levels.alphabet.AlphabetLessonScreen
import com.vasilisina.azbuka.ui.levels.counting.CountingLessonScreen
import com.vasilisina.azbuka.ui.levels.keyboard.KeyboardLessonScreen
import com.vasilisina.azbuka.ui.levels.logic.FindOddOneScreen
import com.vasilisina.azbuka.ui.levels.logic.RiddlesScreen
import com.vasilisina.azbuka.ui.levels.final.FinalSceneScreen

object Routes {
    const val MAIN_MENU = "main_menu"
    const val MAP = "map"
    const val ALBUM = "album"
    const val FAIRY_TALE = "fairy_tale"
    const val LEVEL_1 = "level_1"  // Алфавит — Москва
    const val LEVEL_2 = "level_2"  // Счёт — Тула
    const val LEVEL_3 = "level_3"  // Печать — Вологда
    const val LEVEL_4 = "level_4"  // Логика — Казань
    const val LEVEL_5 = "level_5"  // Загадки — Ярославль
    const val LEVEL_6 = "level_6"  // Финал — Владивосток
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {
        // Главное меню
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onPlay = { navController.navigate(Routes.MAP) { launchSingleTop = true } },
                onAlbum = { if (GameState.isAlbumUnlocked) navController.navigate(Routes.ALBUM) { launchSingleTop = true } },
                onFairyTale = { navController.navigate(Routes.FAIRY_TALE) { launchSingleTop = true } },
                onQuit = { android.os.Process.killProcess(android.os.Process.myPid()) }
            )
        }

        // Сказка
        composable(Routes.FAIRY_TALE) {
            FairyTaleScreen(onBack = { navController.popBackStack() })
        }

        // Карта
        composable(Routes.MAP) {
            MapScreen(
                onLevelSelected = { level ->
                    val route = when (level) {
                        1 -> Routes.LEVEL_1; 2 -> Routes.LEVEL_2; 3 -> Routes.LEVEL_3
                        4 -> Routes.LEVEL_4; 5 -> Routes.LEVEL_5; 6 -> Routes.LEVEL_6
                        else -> return@MapScreen
                    }
                    if (GameState.isLevelUnlocked(level)) navController.navigate(route) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Альбом
        composable(Routes.ALBUM) { ProgressAlbumScreen(onBack = { navController.popBackStack() }) }

        // Уровень 1: Алфавит
        composable(Routes.LEVEL_1) { AlphabetLessonScreen(level = 1, onComplete = { returnToMap(navController) }) }

        // Уровень 2: Счёт
        composable(Routes.LEVEL_2) { CountingLessonScreen(level = 2, onComplete = { returnToMap(navController) }) }

        // Уровень 3: Печать
        composable(Routes.LEVEL_3) { KeyboardLessonScreen(level = 3, onComplete = { returnToMap(navController) }) }

        // Уровень 4: Логика
        composable(Routes.LEVEL_4) { FindOddOneScreen(level = 4, onComplete = { returnToMap(navController) }) }

        // Уровень 5: Загадки
        composable(Routes.LEVEL_5) { RiddlesScreen(level = 5, onComplete = { returnToMap(navController) }) }

        // Уровень 6: Финал
        composable(Routes.LEVEL_6) {
            FinalSceneScreen(level = 6, onComplete = {
                navController.navigate(Routes.ALBUM) { popUpTo(Routes.MAIN_MENU) { inclusive = false }; launchSingleTop = true }
            })
        }
    }
}

private fun returnToMap(navController: NavHostController) {
    navController.navigate(Routes.MAP) { popUpTo(Routes.MAP) { inclusive = true }; launchSingleTop = true }
}
