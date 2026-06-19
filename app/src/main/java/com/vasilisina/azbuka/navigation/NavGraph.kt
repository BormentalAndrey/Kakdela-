// Сохранить в app/src/main/java/com/vasilisina/azbuka/navigation/NavGraph.kt

package com.vasilisina.azbuka.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.menu.MainMenuScreen
import com.vasilisina.azbuka.ui.map.MapScreen
import com.vasilisina.azbuka.ui.album.ProgressAlbumScreen
import com.vasilisina.azbuka.ui.levels.alphabet.AlphabetLessonScreen
import com.vasilisina.azbuka.ui.levels.counting.CountingLessonScreen
import com.vasilisina.azbuka.ui.levels.keyboard.KeyboardLessonScreen
import com.vasilisina.azbuka.ui.levels.logic.FindOddOneScreen
import com.vasilisina.azbuka.ui.levels.final.FinalSceneScreen

// -------------------------------------------------------------------------
// Маршруты навигации
// -------------------------------------------------------------------------

/**
 * Константы маршрутов для навигации.
 *
 * Каждый экран имеет уникальный строковый идентификатор.
 * Используется [NavHostController.navigate] для перехода между экранами.
 */
object Routes {
    /** Главное меню */
    const val MAIN_MENU = "main_menu"

    /** Карта России с городами-уровнями */
    const val MAP = "map"

    /** Альбом успехов */
    const val ALBUM = "album"

    /** Уровень 1: Алфавит (Москва) */
    const val LEVEL_1 = "level_1"

    /** Уровень 2: Счёт (Тула) */
    const val LEVEL_2 = "level_2"

    /** Уровень 3: Печать (Вологда) */
    const val LEVEL_3 = "level_3"

    /** Уровень 4: Логика (Казань) */
    const val LEVEL_4 = "level_4"

    /** Уровень 5: Финал (Владивосток) */
    const val LEVEL_5 = "level_5"
}

// -------------------------------------------------------------------------
// Граф навигации
// -------------------------------------------------------------------------

/**
 * Главный граф навигации приложения.
 *
 * Определяет все возможные маршруты и переходы между экранами.
 * Использует стандартные анимации перехода:
 * - Вперёд: слайд справа налево + fade
 * - Назад: слайд слева направо + fade
 *
 * @param navController Контроллер навигации (обычно [rememberNavController]).
 */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_MENU,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }
            ) + fadeOut()
        }
    ) {
        // -----------------------------------------------------------------
        // Главное меню
        // -----------------------------------------------------------------
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(
                onPlay = {
                    navController.navigate(Routes.MAP) {
                        // Главное меню остаётся в стеке
                        launchSingleTop = true
                    }
                },
                onAlbum = {
                    // Альбом доступен только если открыт
                    if (GameState.isAlbumUnlocked) {
                        navController.navigate(Routes.ALBUM) {
                            launchSingleTop = true
                        }
                    } else {
                        // Если альбом не открыт — показываем заглушку или игнорируем
                        // Можно добавить Toast/снэкбар: «Пройди все уровни!»
                    }
                },
                onQuit = {
                    // Завершаем процесс приложения
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            )
        }

        // -----------------------------------------------------------------
        // Карта
        // -----------------------------------------------------------------
        composable(Routes.MAP) {
            MapScreen(
                onLevelSelected = { level ->
                    val route = when (level) {
                        1 -> Routes.LEVEL_1
                        2 -> Routes.LEVEL_2
                        3 -> Routes.LEVEL_3
                        4 -> Routes.LEVEL_4
                        5 -> Routes.LEVEL_5
                        else -> return@MapScreen
                    }

                    // Проверяем, открыт ли уровень
                    if (GameState.isLevelUnlocked(level)) {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // -----------------------------------------------------------------
        // Альбом успехов
        // -----------------------------------------------------------------
        composable(Routes.ALBUM) {
            ProgressAlbumScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // -----------------------------------------------------------------
        // Уровень 1: Алфавит
        // -----------------------------------------------------------------
        composable(Routes.LEVEL_1) {
            AlphabetLessonScreen(
                level = 1,
                onComplete = { stars ->
                    returnToMap(navController)
                }
            )
        }

        // -----------------------------------------------------------------
        // Уровень 2: Счёт
        // -----------------------------------------------------------------
        composable(Routes.LEVEL_2) {
            CountingLessonScreen(
                level = 2,
                onComplete = { stars ->
                    returnToMap(navController)
                }
            )
        }

        // -----------------------------------------------------------------
        // Уровень 3: Печать (клавиатура)
        // -----------------------------------------------------------------
        composable(Routes.LEVEL_3) {
            KeyboardLessonScreen(
                level = 3,
                onComplete = { stars ->
                    returnToMap(navController)
                }
            )
        }

        // -----------------------------------------------------------------
        // Уровень 4: Логика
        // -----------------------------------------------------------------
        composable(Routes.LEVEL_4) {
            FindOddOneScreen(
                level = 4,
                onComplete = { stars ->
                    returnToMap(navController)
                }
            )
        }

        // -----------------------------------------------------------------
        // Уровень 5: Финал
        // -----------------------------------------------------------------
        composable(Routes.LEVEL_5) {
            FinalSceneScreen(
                level = 5,
                onComplete = { stars ->
                    // После финала переходим в альбом
                    // Очищаем весь стек навигации до главного меню
                    navController.navigate(Routes.ALBUM) {
                        popUpTo(Routes.MAIN_MENU) {
                            inclusive = false // Главное меню остаётся в стеке
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------
// Вспомогательные функции навигации
// -------------------------------------------------------------------------

/**
 * Возвращает пользователя на карту после прохождения уровня.
 *
 * Использует [popUpTo] для удаления экрана уровня из стека,
 * чтобы кнопка «Назад» с карты не возвращала на пройденный уровень.
 *
 * @param navController Контроллер навигации.
 */
private fun returnToMap(navController: NavHostController) {
    navController.navigate(Routes.MAP) {
        // Удаляем экран уровня из стека
        popUpTo(Routes.MAP) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

// -------------------------------------------------------------------------
// Расширения для удобной навигации (опционально)
// -------------------------------------------------------------------------

/**
 * Безопасный переход на указанный маршрут.
 *
 * Проверяет, что маршрут не совпадает с текущим,
 * чтобы избежать двойного добавления в стек.
 *
 * @param route Маршрут для перехода.
 */
fun NavHostController.navigateSafely(route: String) {
    if (currentDestination?.route != route) {
        navigate(route) {
            launchSingleTop = true
        }
    }
}

/**
 * Проверяет, можно ли открыть альбом, и выполняет переход.
 *
 * Если альбом не открыт — возвращает false.
 *
 * @return true если переход выполнен.
 */
fun NavHostController.navigateToAlbumIfUnlocked(): Boolean {
    return if (GameState.isAlbumUnlocked) {
        navigate(Routes.ALBUM) {
            launchSingleTop = true
        }
        true
    } else {
        false
    }
}

/**
 * Переход на уровень с проверкой доступности.
 *
 * @param level Номер уровня (1–5).
 * @return true если уровень открыт и переход выполнен.
 */
fun NavHostController.navigateToLevel(level: Int): Boolean {
    if (!GameState.isLevelUnlocked(level)) return false

    val route = when (level) {
        1 -> Routes.LEVEL_1
        2 -> Routes.LEVEL_2
        3 -> Routes.LEVEL_3
        4 -> Routes.LEVEL_4
        5 -> Routes.LEVEL_5
        else -> return false
    }

    navigate(route) {
        launchSingleTop = true
    }

    return true
}
