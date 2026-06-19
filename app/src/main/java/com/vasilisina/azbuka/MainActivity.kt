// Сохранить в app/src/main/java/com/vasilisina/azbuka/MainActivity.kt

package com.vasilisina.azbuka

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.navigation.NavGraph
import com.vasilisina.azbuka.ui.theme.VasilisaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureDisplay()
        enableEdgeToEdge()
        setContent {
            VasilisaTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AudioPlayer.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        AudioPlayer.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AudioPlayer.release()
        }
    }

    private fun configureDisplay() {
        // ГОРИЗОНТАЛЬНАЯ ориентация
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @Deprecated("Use OnBackPressedCallback", ReplaceWith("OnBackPressedCallback"))
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
