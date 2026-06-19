// Сохранить в app/src/main/java/com/vasilisina/azbuka/VasilisaApp.kt

package com.vasilisina.azbuka

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.data.ProgressManager

class VasilisaApp : Application() {

    companion object {
        private const val TAG = "VasilisaApp"

        @Volatile
        private var _instance: VasilisaApp? = null

        val instance: VasilisaApp
            get() = _instance ?: throw IllegalStateException("VasilisaApp не инициализирован")
    }

    @Volatile
    var isProgressLoaded: Boolean = false
        private set

    @Volatile
    var isFullyInitialized: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        _instance = this

        Log.i(TAG, "Василисина азбука запускается...")
        initializeProgressManager()
        initializeAudioPlayer()
        loadSavedProgress()
        preloadCommonSounds()
        isFullyInitialized = true
        Log.i(TAG, "Инициализация завершена. Прогресс: ${GameState.getCompletionPercent()}%")
    }

    override fun onTerminate() {
        Log.i(TAG, "Завершение. Освобождение ресурсов...")
        releaseResources()
        super.onTerminate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val orientation = when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "ландшафт"
            Configuration.ORIENTATION_PORTRAIT -> "портрет"
            else -> "неизвестно"
        }
        val nightMode = when (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> "тёмная"
            Configuration.UI_MODE_NIGHT_NO -> "светлая"
            else -> "неизвестно"
        }
        Log.d(TAG, "Конфигурация: ориентация=$orientation, тема=$nightMode")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Нехватка памяти.")
        try { AudioPlayer.stopMusic() } catch (_: Exception) {}
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.w(TAG, "onTrimMemory: $level")
            try { AudioPlayer.stopMusic() } catch (_: Exception) {}
        }
    }

    private fun initializeProgressManager() {
        try { ProgressManager.init(this) } catch (e: Exception) { Log.e(TAG, "Ошибка ProgressManager", e) }
    }

    private fun initializeAudioPlayer() {
        try { AudioPlayer.init(this) } catch (e: Exception) { Log.e(TAG, "Ошибка AudioPlayer", e) }
    }

    private fun loadSavedProgress() {
        try {
            val saved = ProgressManager.load()
            GameState.loadFrom(saved)
            isProgressLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки прогресса", e)
            isProgressLoaded = false
        }
    }

    private fun preloadCommonSounds() {
        try {
            AudioPlayer.loadSFX(this, R.raw.sfx_click, "click")
            AudioPlayer.loadSFX(this, R.raw.sfx_correct, "correct")
            AudioPlayer.loadSFX(this, R.raw.sfx_wrong, "wrong")
        } catch (_: Exception) {}
    }

    private fun releaseResources() {
        try { AudioPlayer.release() } catch (_: Exception) {}
        try { ProgressManager.saveImmediately(GameState) } catch (_: Exception) {}
    }

    fun isInstanceAvailable(): Boolean = _instance != null

    fun resetGameProgress() {
        try { GameState.reset(); isProgressLoaded = true } catch (_: Exception) {}
    }

    fun forceSaveProgress() {
        try { ProgressManager.saveImmediately(GameState) } catch (_: Exception) {}
    }
}
