// Сохранить в app/src/main/java/com/vasilisina/azbuka/audio/AudioPlayer.kt

package com.vasilisina.azbuka.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Потокобезопасный менеджер аудио для игры «Василисина азбука».
 *
 * Управляет:
 * - Фоновой музыкой через [MediaPlayer]
 * - Звуковыми эффектами (SFX) через [SoundPool]
 *
 * Особенности:
 * - Потокобезопасность для всех публичных методов
 * - Автоматическое восстановление после ошибок
 * - Отслеживание состояния загрузки звуков
 * - Раздельная регулировка громкости музыки и эффектов
 *
 * Использование:
 * ```kotlin
 * // Инициализация (в Application.onCreate)
 * AudioPlayer.init(context)
 *
 * // Загрузка звуков
 * AudioPlayer.loadSFX(context, R.raw.sfx_click, "click")
 *
 * // Воспроизведение музыки
 * AudioPlayer.playMusic(context, R.raw.music_main)
 *
 * // Воспроизведение эффекта
 * AudioPlayer.playSFX("click")
 *
 * // Остановка при выходе
 * AudioPlayer.release()
 * ```
 */
object AudioPlayer {

    // -------------------------------------------------------------------------
    // Константы
    // -------------------------------------------------------------------------

    /** Тег для логирования */
    private const val TAG = "AudioPlayer"

    /** Максимальное количество одновременных потоков SoundPool */
    private const val MAX_SFX_STREAMS = 8

    /** Приоритет звуковых эффектов */
    private const val SFX_PRIORITY = 1

    /** Скорость воспроизведения SFX (1.0 — нормальная) */
    private const val SFX_RATE = 1f

    // -------------------------------------------------------------------------
    // Поля
    // -------------------------------------------------------------------------

    /** Плеер фоновой музыки */
    @Volatile
    private var musicPlayer: MediaPlayer? = null

    /** Пул звуковых эффектов */
    @Volatile
    private var soundPool: SoundPool? = null

    /** Маппинг ключ → SoundPool ID */
    private val soundIds = ConcurrentHashMap<String, Int>()

    /** Статус загрузки звуков: SoundPool ID → загружен */
    private val loadedSoundIds = ConcurrentHashMap<Int, Boolean>()

    /** Ресурс текущей играющей музыки */
    @Volatile
    private var currentMusicRes: Int? = null

    /** Громкость музыки (0.0–1.0) */
    @Volatile
    private var musicVolume: Float = 1f

    /** Громкость звуковых эффектов (0.0–1.0) */
    @Volatile
    private var sfxVolume: Float = 1f

    /** Флаг инициализации */
    private val isInitialized = AtomicBoolean(false)

    /** Блокировка для операций с MediaPlayer */
    private val musicLock = Any()

    // -------------------------------------------------------------------------
    // Инициализация и освобождение
    // -------------------------------------------------------------------------

    /**
     * Инициализирует аудиоплеер.
     *
     * Должен быть вызван один раз при старте приложения.
     * Повторные вызовы безопасны и игнорируются.
     *
     * @param context Контекст приложения
     */
    @Synchronized
    fun init(context: Context) {
        if (isInitialized.get()) {
            Log.d(TAG, "AudioPlayer уже инициализирован.")
            return
        }

        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(MAX_SFX_STREAMS)
                .setAudioAttributes(attributes)
                .build()
                .apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            loadedSoundIds[sampleId] = true
                            Log.d(TAG, "SFX загружен: sampleId=$sampleId")
                        } else {
                            loadedSoundIds.remove(sampleId)
                            Log.e(TAG, "Ошибка загрузки SFX: sampleId=$sampleId, status=$status")
                        }
                    }
                }

            isInitialized.set(true)
            Log.i(TAG, "AudioPlayer инициализирован (max streams: $MAX_SFX_STREAMS).")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации AudioPlayer.", e)
            isInitialized.set(false)
        }
    }

    /**
     * Полностью освобождает все аудиоресурсы.
     *
     * Должен быть вызван при завершении приложения (onTerminate, onDestroy).
     */
    @Synchronized
    fun release() {
        Log.i(TAG, "Освобождение AudioPlayer...")

        stopMusic()

        try {
            soundPool?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при освобождении SoundPool.", e)
        } finally {
            soundPool = null
        }

        soundIds.clear()
        loadedSoundIds.clear()
        currentMusicRes = null
        isInitialized.set(false)

        Log.i(TAG, "AudioPlayer освобождён.")
    }

    // -------------------------------------------------------------------------
    // Управление фоновой музыкой
    // -------------------------------------------------------------------------

    /**
     * Воспроизводит фоновую музыку.
     *
     * Если музыка с таким же resId уже играет — вызов игнорируется.
     *
     * @param context Контекст приложения
     * @param resId Ресурс музыки (например, R.raw.music_main)
     * @param loop Зацикливать ли воспроизведение (по умолчанию true)
     */
    fun playMusic(
        context: Context,
        resId: Int,
        loop: Boolean = true
    ) {
        synchronized(musicLock) {
            // Если эта же музыка уже играет — ничего не делаем
            if (currentMusicRes == resId && isMusicPlaying()) {
                Log.d(TAG, "Музыка $resId уже воспроизводится.")
                return
            }

            // Останавливаем предыдущую
            stopMusicInternal()

            try {
                val player = MediaPlayer.create(
                    context.applicationContext,
                    resId
                )

                if (player == null) {
                    Log.e(TAG, "Не удалось создать MediaPlayer для ресурса: $resId")
                    return
                }

                currentMusicRes = resId
                musicPlayer = player

                player.isLooping = loop
                player.setVolume(musicVolume, musicVolume)

                player.setOnCompletionListener { mp ->
                    if (!mp.isLooping) {
                        Log.d(TAG, "Музыка завершилась (без зацикливания).")
                        synchronized(musicLock) {
                            if (musicPlayer === mp) {
                                releasePlayer(mp)
                                musicPlayer = null
                                currentMusicRes = null
                            }
                        }
                    }
                }

                player.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "Ошибка MediaPlayer: what=$what, extra=$extra")
                    synchronized(musicLock) {
                        if (musicPlayer === mp) {
                            releasePlayer(mp)
                            musicPlayer = null
                            currentMusicRes = null
                        }
                    }
                    true // Ошибка обработана
                }

                player.start()

                Log.d(TAG, "Музыка запущена: resId=$resId, loop=$loop")
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при запуске музыки: resId=$resId", e)
                stopMusicInternal()
            }
        }
    }

    /**
     * Ставит фоновую музыку на паузу.
     */
    fun pauseMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && player.isPlaying) {
                    player.pause()
                    Log.d(TAG, "Музыка на паузе.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при постановке музыки на паузу.", e)
            }
        }
    }

    /**
     * Возобновляет фоновую музыку после паузы.
     */
    fun resumeMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && !player.isPlaying) {
                    player.start()
                    Log.d(TAG, "Музыка возобновлена.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при возобновлении музыки.", e)
            }
        }
    }

    /**
     * Останавливает и освобождает фоновую музыку.
     */
    fun stopMusic() {
        synchronized(musicLock) {
            stopMusicInternal()
        }
    }

    /**
     * Внутренний метод остановки музыки (без синхронизации).
     * Должен вызываться только из synchronized(musicLock).
     */
    private fun stopMusicInternal() {
        try {
            val player = musicPlayer
            if (player != null) {
                releasePlayer(player)
                musicPlayer = null
                currentMusicRes = null
                Log.d(TAG, "Музыка остановлена.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при остановке музыки.", e)
            musicPlayer = null
            currentMusicRes = null
        }
    }

    /**
     * Безопасно освобождает MediaPlayer.
     */
    private fun releasePlayer(player: MediaPlayer) {
        try {
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            if (player.isPlaying) {
                player.stop()
            }
            player.reset()
            player.release()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при освобождении MediaPlayer.", e)
        }
    }

    /**
     * Проверяет, играет ли музыка в данный момент.
     *
     * @return true если музыка активна
     */
    fun isMusicPlaying(): Boolean {
        return try {
            synchronized(musicLock) {
                musicPlayer?.isPlaying == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки состояния музыки.", e)
            false
        }
    }

    // -------------------------------------------------------------------------
    // Управление звуковыми эффектами (SFX)
    // -------------------------------------------------------------------------

    /**
     * Загружает звуковой эффект в SoundPool.
     *
     * Загрузка асинхронная. Проверить готовность можно через [isSoundLoaded].
     *
     * @param context Контекст приложения
     * @param resId Ресурс звука (например, R.raw.sfx_click)
     * @param key Уникальный строковый ключ для воспроизведения
     */
    fun loadSFX(
        context: Context,
        resId: Int,
        key: String
    ) {
        val pool = soundPool

        if (pool == null) {
            Log.e(TAG, "SoundPool не инициализирован. Звук '$key' не загружен.")
            return
        }

        // Не загружаем повторно
        if (soundIds.containsKey(key)) {
            Log.d(TAG, "SFX '$key' уже загружен.")
            return
        }

        try {
            val id = pool.load(
                context.applicationContext,
                resId,
                SFX_PRIORITY
            )

            soundIds[key] = id
            loadedSoundIds[id] = false

            Log.d(TAG, "SFX '$key' загружается: resId=$resId, sampleId=$id")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки SFX '$key': resId=$resId", e)
        }
    }

    /**
     * Пакетная загрузка звуковых эффектов.
     *
     * @param context Контекст приложения
     * @param sfxMap Карта: ключ → ресурс (например, mapOf("click" to R.raw.sfx_click))
     */
    fun loadSFXBatch(
        context: Context,
        sfxMap: Map<String, Int>
    ) {
        sfxMap.forEach { (key, resId) ->
            loadSFX(context, resId, key)
        }
        Log.d(TAG, "Пакетная загрузка ${sfxMap.size} SFX завершена.")
    }

    /**
     * Воспроизводит звуковой эффект по ключу.
     *
     * Если звук ещё не загружен — вызов игнорируется с предупреждением в лог.
     *
     * @param key Ключ звука, указанный при загрузке
     */
    fun playSFX(key: String) {
        val id = soundIds[key]

        if (id == null) {
            Log.w(TAG, "SFX '$key' не найден. Возможно, не был загружен через loadSFX().")
            return
        }

        val pool = soundPool

        if (pool == null) {
            Log.e(TAG, "SoundPool не инициализирован. SFX '$key' не воспроизведён.")
            return
        }

        val isLoaded = loadedSoundIds[id] == true

        if (!isLoaded) {
            Log.w(TAG, "SFX '$key' ещё загружается (sampleId=$id). Пропускаем воспроизведение.")
            return
        }

        try {
            val streamId = pool.play(
                id,
                sfxVolume,   // leftVolume
                sfxVolume,   // rightVolume
                SFX_PRIORITY,
                0,           // loop (0 = без повтора)
                SFX_RATE
            )

            if (streamId == 0) {
                Log.w(TAG, "SFX '$key' не воспроизведён: SoundPool переполнен или ошибка.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка воспроизведения SFX '$key'.", e)
        }
    }

    /**
     * Проверяет, загружен ли звук и готов к воспроизведению.
     *
     * @param key Ключ звука
     * @return true если звук полностью загружен
     */
    fun isSoundLoaded(key: String): Boolean {
        val id = soundIds[key] ?: return false
        return loadedSoundIds[id] == true
    }

    /**
     * Выгружает звуковой эффект из SoundPool.
     *
     * @param key Ключ звука
     */
    fun unloadSFX(key: String) {
        synchronized(this) {
            val pool = soundPool ?: return

            val id = soundIds.remove(key) ?: return

            try {
                pool.unload(id)
                Log.d(TAG, "SFX '$key' выгружен: sampleId=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка выгрузки SFX '$key'.", e)
            } finally {
                loadedSoundIds.remove(id)
            }
        }
    }

    /**
     * Выгружает все звуковые эффекты.
     */
    fun unloadAllSFX() {
        synchronized(this) {
            val keys = soundIds.keys.toList()
            keys.forEach { key ->
                unloadSFX(key)
            }
            Log.d(TAG, "Все SFX выгружены (${keys.size} шт.).")
        }
    }

    // -------------------------------------------------------------------------
    // Громкость
    // -------------------------------------------------------------------------

    /**
     * Устанавливает громкость фоновой музыки.
     *
     * @param volume Громкость от 0.0 (тихо) до 1.0 (максимум)
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)

        synchronized(musicLock) {
            try {
                musicPlayer?.setVolume(musicVolume, musicVolume)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка установки громкости музыки.", e)
            }
        }

        Log.d(TAG, "Громкость музыки: ${"%.2f".format(musicVolume)}")
    }

    /**
     * Устанавливает громкость звуковых эффектов.
     *
     * @param volume Громкость от 0.0 (тихо) до 1.0 (максимум)
     */
    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        Log.d(TAG, "Громкость SFX: ${"%.2f".format(sfxVolume)}")
    }

    /**
     * Возвращает текущую громкость музыки.
     */
    fun getMusicVolume(): Float = musicVolume

    /**
     * Возвращает текущую громкость SFX.
     */
    fun getSfxVolume(): Float = sfxVolume

    // -------------------------------------------------------------------------
    // Состояние
    // -------------------------------------------------------------------------

    /**
     * Проверяет, инициализирован ли AudioPlayer.
     */
    fun isInitialized(): Boolean = isInitialized.get()

    /**
     * Возвращает количество загруженных SFX.
     */
    fun getLoadedSfxCount(): Int = soundIds.size

    /**
     * Возвращает количество полностью загруженных и готовых к воспроизведению SFX.
     */
    fun getReadySfxCount(): Int {
        return loadedSoundIds.count { it.value }
    }

    /**
     * Логирует текущее состояние AudioPlayer.
     */
    fun logState() {
        Log.i(TAG, buildString {
            appendLine("=== AudioPlayer State ===")
            appendLine("Инициализирован: ${isInitialized.get()}")
            appendLine("Музыка играет: ${isMusicPlaying()}")
            appendLine("Текущий resId музыки: ${currentMusicRes ?: "нет"}")
            appendLine("Громкость музыки: ${"%.2f".format(musicVolume)}")
            appendLine("Громкость SFX: ${"%.2f".format(sfxVolume)}")
            appendLine("Загружено SFX: ${getLoadedSfxCount()} (готово: ${getReadySfxCount()})")
            appendLine("Ключи SFX: ${soundIds.keys.toList()}")
            appendLine("=========================")
        })
    }
}
