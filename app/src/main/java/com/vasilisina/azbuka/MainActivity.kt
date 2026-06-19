// Сохранить в app/src/main/java/com/vasilisina/azbuka/MainActivity.kt

package com.vasilisina.azbuka

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.navigation.NavGraph
import com.vasilisina.azbuka.ui.theme.VasilisaTheme

/**
 * Главная (и единственная) Activity игры «Василисина азбука».
 *
 * Особенности:
 * - Фиксированная портретная ориентация (игра для детей, ландшафт неудобен)
 * - Edge-to-edge дизайн (контент за системными барами)
 * - Полноэкранный режим с отключённым сном экрана
 * - Обработка жизненного цикла аудио
 * - Сохранение прогресса при сворачивании
 *
 * Регистрация в AndroidManifest.xml:
 * ```xml
 * <activity
 *     android:name=".MainActivity"
 *     android:screenOrientation="portrait"
 *     android:exported="true"
 *     android:launchMode="singleTask">
 *     <intent-filter>
 *         <action android:name="android.intent.action.MAIN" />
 *         <category android:name="android.intent.category.LAUNCHER" />
 *     </intent-filter>
 * </activity>
 * ```
 */
class MainActivity : ComponentActivity() {

    // -------------------------------------------------------------------------
    // Жизненный цикл
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка отображения
        configureDisplay()

        // Edge-to-edge: контент рисуется за системными барами
        enableEdgeToEdge()

        // Установка Composable-контента
        setContent {
            VasilisaTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Возобновляем музыку, если она была на паузе
        // (AudioPlayer сам проверит, нужно ли)
        AudioPlayer.resumeMusic()
    }

    override fun onPause() {
        super.onPause()

        // Ставим музыку на паузу при сворачивании
        AudioPlayer.pauseMusic()

        // Сохраняем прогресс (на случай, если приложение будет убито)
        // Используем синхронное сохранение для гарантии
        val app = application as? VasilisaApp
        app?.forceSaveProgress()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Если это финальное уничтожение (не пересоздание при повороте)
        if (isFinishing) {
            AudioPlayer.release()
        }
    }

    // -------------------------------------------------------------------------
    // Настройка отображения
    // -------------------------------------------------------------------------

    /**
     * Настраивает параметры окна:
     * - Портретная ориентация
     * - Отключение сна экрана (ребёнок может долго думать)
     * - Полноэкранный режим (скрытие системных баров)
     */
    private fun configureDisplay() {
        // Фиксируем портретную ориентацию
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Не даём экрану гаснуть во время игры
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Опционально: полноэкранный режим с沉浸式体验
        // (системные бары скрываются, появляются по свайпу)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+: управление через WindowInsetsController
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 и ниже: флаги SYSTEM_UI
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    // -------------------------------------------------------------------------
    // Обработка кнопки «Назад»
    // -------------------------------------------------------------------------

    /**
     * Переопределяем поведение системной кнопки «Назад».
     *
     * На главном экране — подтверждение выхода (опционально).
     * На других экранах — стандартное поведение (popBackStack).
     */
    @Deprecated(
        "Используйте OnBackPressedCallback из AndroidX.",
        ReplaceWith(
            "OnBackPressedCallback",
            "androidx.activity.OnBackPressedCallback"
        )
    )
    override fun onBackPressed() {
        // Стандартное поведение: навигация обрабатывается NavHost
        // Если нужно добавить диалог подтверждения выхода на главном экране —
        // используйте OnBackPressedDispatcher.addCallback()
        super.onBackPressed()
    }
}
