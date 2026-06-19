// Сохранить в app/src/main/java/com/vasilisina/azbuka/MainActivity.kt

package com.vasilisina.azbuka

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
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

    @Deprecated("Use OnBackPressedCallback", ReplaceWith("OnBackPressedCallback"))
    override fun onBackPressed() {
        super.onBackPressed()
    }
}
