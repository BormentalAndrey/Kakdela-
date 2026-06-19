// Сохранить в app/src/main/java/com/vasilisina/azbuka/VasilisaApp.kt

package com.vasilisina.azbuka

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.data.ProgressManager

/**
 * Класс Application для игры «Василисина азбука: Путешествие по России».
 *
 * Отвечает за:
 * - Инициализацию синглтонов ([ProgressManager], [AudioPlayer])
 * - Загрузку сохранённого прогресса
 * - Корректное освобождение ресурсов при завершении приложения
 * - Обработку изменений конфигурации (поворот экрана, тёмная тема и т.д.)
 *
 * Должен быть зарегистрирован в AndroidManifest.xml:
 * ```xml
 * <application
 *     android:name=".VasilisaApp"
 *     ... >
 * ```
 */
class VasilisaApp : Application() {

    companion object {
        private const val TAG = "VasilisaApp"

        /**
         * Удобный доступ к экземпляру Application из любого места.
         * Инициализируется в onCreate().
         */
        @Volatile
        lateinit var instance: VasilisaApp
            private set
    }

    // -------------------------------------------------------------------------
    // Флаги состояния
    // -------------------------------------------------------------------------

    /** Был ли прогресс успешно загружен */
    @Volatile
    var isProgressLoaded: Boolean = false
        private set

    /** Были ли инициализированы все компоненты */
    @Volatile
    var isFullyInitialized: Boolean = false
        private set

    // -------------------------------------------------------------------------
    // Жизненный цикл
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()

        // Сохраняем ссылку для статического доступа
        instance = this

        Log.i(TAG, "========================================")
        Log.i(TAG, "  Василисина азбука запускается...")
        Log.i(TAG, "========================================")

        // Этап 1: Инициализация ProgressManager
        initializeProgressManager()

        // Этап 2: Инициализация AudioPlayer
        initializeAudioPlayer()

        // Этап 3: Загрузка прогресса
        loadSavedProgress()

        // Этап 4: Предзагрузка звуков (опционально)
        preloadCommonSounds()

        isFullyInitialized = true

        Log.i(TAG, "========================================")
        Log.i(TAG, "  Инициализация завершена успешно.")
        Log.i(TAG, "  Прогресс: ${GameState.getCompletionPercent()}%")
        Log.i(TAG, "========================================")
    }

    override fun onTerminate() {
        Log.i(TAG, "Приложение завершается. Освобождение ресурсов...")

        releaseResources()

        super.onTerminate()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Логируем изменения конфигурации для отладки
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

        Log.d(TAG, "Конфигурация изменена: ориентация=$orientation, тема=$nightMode")
    }

    override fun onLowMemory() {
        super.onLowMemory()

        Log.w(TAG, "Система сообщает о нехватке памяти.")

        // Освобождаем некритичные ресурсы
        // (музыка может быть перезапущена при возвращении в игру)
        try {
            AudioPlayer.stopMusic()
            Log.d(TAG, "Фоновая музыка остановлена для экономии памяти.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обработке onLowMemory.", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        // При критическом уровне освобождаем все аудиоресурсы
        if (level >= TRIM_MEMORY_MODERATE) {
            Log.w(TAG, "onTrimMemory: уровень $level. Освобождение аудиоресурсов.")

            try {
                AudioPlayer.stopMusic()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при освобождении аудио в onTrimMemory.", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Инициализация компонентов
    // -------------------------------------------------------------------------

    /**
     * Инициализирует [ProgressManager] с обработкой ошибок.
     */
    private fun initializeProgressManager() {
        try {
            ProgressManager.init(this)
            Log.d(TAG, "ProgressManager инициализирован.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации ProgressManager.", e)
            // Приложение может работать без сохранения прогресса
        }
    }

    /**
     * Инициализирует [AudioPlayer] с обработкой ошибок.
     */
    private fun initializeAudioPlayer() {
        try {
            AudioPlayer.init(this)
            Log.d(TAG, "AudioPlayer инициализирован.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации AudioPlayer.", e)
            // Приложение может работать без звука
        }
    }

    /**
     * Загружает сохранённый прогресс в [GameState].
     */
    private fun loadSavedProgress() {
        try {
            val savedProgress = ProgressManager.load()

            GameState.loadFrom(savedProgress)

            isProgressLoaded = true

            Log.d(TAG, "Прогресс загружен:")
            Log.d(TAG, "  - Открыто уровней: ${savedProgress.currentLevel}/5")
            Log.d(TAG, "  - Звёзды: ${(1..5).joinToString(", ") { "Ур.$it: ${savedProgress.stars[it] ?: 0}" }}")
            Log.d(TAG, "  - Альбом: ${if (savedProgress.isAlbumUnlocked) "открыт" else "закрыт"}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки прогресса. Используется прогресс по умолчанию.", e)
            isProgressLoaded = false
            // GameState уже содержит значения по умолчанию
        }
    }

    /**
     * Предварительно загружает часто используемые звуки.
     */
    private fun preloadCommonSounds() {
        try {
            AudioPlayer.loadSFX(this, R.raw.sfx_click, "click")
            AudioPlayer.loadSFX(this, R.raw.sfx_correct, "correct")
            AudioPlayer.loadSFX(this, R.raw.sfx_wrong, "wrong")
            Log.d(TAG, "Основные звуковые эффекты загружены.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка предзагрузки звуков.", e)
        }
    }

    // -------------------------------------------------------------------------
    // Освобождение ресурсов
    // -------------------------------------------------------------------------

    /**
     * Освобождает все ресурсы при завершении приложения.
     */
    private fun releaseResources() {
        try {
            AudioPlayer.release()
            Log.d(TAG, "AudioPlayer освобождён.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при освобождении AudioPlayer.", e)
        }

        try {
            // Сохраняем прогресс напоследок
            ProgressManager.saveImmediately(GameState)
            Log.d(TAG, "Прогресс сохранён.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при финальном сохранении прогресса.", e)
        }
    }

    // -------------------------------------------------------------------------
    // Публичные утилиты
    // -------------------------------------------------------------------------

    /**
     * Проверяет, доступен ли экземпляр приложения.
     *
     * @return true если [instance] инициализирован.
     */
    fun isInstanceAvailable(): Boolean {
        return ::instance.isInitialized
    }

    /**
     * Выполняет полную перезагрузку состояния игры.
     * Используется для кнопки «Начать заново» в настройках.
     */
    fun resetGameProgress() {
        Log.i(TAG, "Полный сброс прогресса игры.")

        try {
            GameState.reset()
            isProgressLoaded = true
            Log.d(TAG, "Прогресс сброшен успешно.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сбросе прогресса.", e)
        }
    }

    /**
     * Принудительно сохраняет текущий прогресс.
     * Может вызываться из Activity.onPause() для надёжности.
     */
    fun forceSaveProgress() {
        try {
            ProgressManager.saveImmediately(GameState)
            Log.d(TAG, "Прогресс принудительно сохранён.")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка принудительного сохранения.", e)
        }
    }
}
