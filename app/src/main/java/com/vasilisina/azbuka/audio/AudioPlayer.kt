// Сохранить в app/src/main/java/com/vasilisina/azbuka/audio/AudioPlayer.kt

package com.vasilisina.azbuka.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object AudioPlayer {

    private const val TAG = "AudioPlayer"
    private const val MAX_SFX_STREAMS = 8
    private const val SFX_PRIORITY = 1
    private const val SFX_RATE = 1f

    @Volatile
    private var musicPlayer: MediaPlayer? = null

    @Volatile
    private var soundPool: SoundPool? = null

    private val soundIds = ConcurrentHashMap<String, Int>()
    private val loadedSoundIds = ConcurrentHashMap<Int, Boolean>()

    @Volatile
    private var currentMusicRes: Int? = null

    @Volatile
    private var musicVolume: Float = 1f

    @Volatile
    private var sfxVolume: Float = 1f

    private val isInitialized = AtomicBoolean(false)
    private val musicLock = Any()

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

    @Synchronized
    fun release() {
        Log.i(TAG, "Освобождение AudioPlayer...")
        stopMusic()
        try { soundPool?.release() } catch (e: Exception) { Log.e(TAG, "Ошибка при освобождении SoundPool.", e) } finally { soundPool = null }
        soundIds.clear()
        loadedSoundIds.clear()
        currentMusicRes = null
        isInitialized.set(false)
        Log.i(TAG, "AudioPlayer освобождён.")
    }

    fun playMusic(context: Context, resId: Int, loop: Boolean = true) {
        synchronized(musicLock) {
            if (currentMusicRes == resId && isMusicPlaying()) {
                Log.d(TAG, "Музыка $resId уже воспроизводится.")
                return
            }
            stopMusicInternal()
            try {
                val player = MediaPlayer.create(context.applicationContext, resId)
                if (player == null) { Log.e(TAG, "Не удалось создать MediaPlayer для ресурса: $resId"); return }
                currentMusicRes = resId
                musicPlayer = player
                player.isLooping = loop
                player.setVolume(musicVolume, musicVolume)
                player.setOnCompletionListener { mp ->
                    if (!mp.isLooping) {
                        synchronized(musicLock) {
                            if (musicPlayer === mp) { releasePlayer(mp); musicPlayer = null; currentMusicRes = null }
                        }
                    }
                }
                player.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "Ошибка MediaPlayer: what=$what, extra=$extra")
                    synchronized(musicLock) {
                        if (musicPlayer === mp) { releasePlayer(mp); musicPlayer = null; currentMusicRes = null }
                    }
                    true
                }
                player.start()
                Log.d(TAG, "Музыка запущена: resId=$resId, loop=$loop")
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при запуске музыки: resId=$resId", e)
                stopMusicInternal()
            }
        }
    }

    fun pauseMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && player.isPlaying) { player.pause(); Log.d(TAG, "Музыка на паузе.") }
            } catch (e: Exception) { Log.e(TAG, "Ошибка при постановке музыки на паузу.", e) }
        }
    }

    fun resumeMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && !player.isPlaying) { player.start(); Log.d(TAG, "Музыка возобновлена.") }
            } catch (e: Exception) { Log.e(TAG, "Ошибка при возобновлении музыки.", e) }
        }
    }

    fun stopMusic() { synchronized(musicLock) { stopMusicInternal() } }

    private fun stopMusicInternal() {
        try {
            val player = musicPlayer
            if (player != null) { releasePlayer(player); musicPlayer = null; currentMusicRes = null; Log.d(TAG, "Музыка остановлена.") }
        } catch (e: Exception) { Log.e(TAG, "Ошибка при остановке музыки.", e); musicPlayer = null; currentMusicRes = null }
    }

    private fun releasePlayer(player: MediaPlayer) {
        try {
            player.setOnCompletionListener(null); player.setOnErrorListener(null)
            if (player.isPlaying) player.stop()
            player.reset(); player.release()
        } catch (e: Exception) { Log.e(TAG, "Ошибка при освобождении MediaPlayer.", e) }
    }

    // ИСПРАВЛЕНО: явный return Boolean, без 'if' как expression
    fun isMusicPlaying(): Boolean {
        synchronized(musicLock) {
            val mp = musicPlayer
            return if (mp != null) {
                try { mp.isPlaying } catch (e: Exception) { false }
            } else false
        }
    }

    fun loadSFX(context: Context, resId: Int, key: String) {
        val pool = soundPool
        if (pool == null) { Log.e(TAG, "SoundPool не инициализирован. Звук '$key' не загружен."); return }
        if (soundIds.containsKey(key)) { Log.d(TAG, "SFX '$key' уже загружен."); return }
        try {
            val id = pool.load(context.applicationContext, resId, SFX_PRIORITY)
            soundIds[key] = id; loadedSoundIds[id] = false
            Log.d(TAG, "SFX '$key' загружается: resId=$resId, sampleId=$id")
        } catch (e: Exception) { Log.e(TAG, "Ошибка загрузки SFX '$key': resId=$resId", e) }
    }

    fun loadSFXBatch(context: Context, sfxMap: Map<String, Int>) {
        sfxMap.forEach { (key, resId) -> loadSFX(context, resId, key) }
        Log.d(TAG, "Пакетная загрузка ${sfxMap.size} SFX завершена.")
    }

    fun playSFX(key: String) {
        val id = soundIds[key]
        if (id == null) { Log.w(TAG, "SFX '$key' не найден."); return }
        val pool = soundPool
        if (pool == null) { Log.e(TAG, "SoundPool не инициализирован."); return }
        val isLoaded = loadedSoundIds[id] == true
        if (!isLoaded) { Log.w(TAG, "SFX '$key' ещё загружается."); return }
        try {
            val streamId = pool.play(id, sfxVolume, sfxVolume, SFX_PRIORITY, 0, SFX_RATE)
            if (streamId == 0) Log.w(TAG, "SFX '$key' не воспроизведён.")
        } catch (e: Exception) { Log.e(TAG, "Ошибка воспроизведения SFX '$key'.", e) }
    }

    fun isSoundLoaded(key: String): Boolean {
        val id = soundIds[key] ?: return false
        return loadedSoundIds[id] == true
    }

    fun unloadSFX(key: String) {
        synchronized(this) {
            val pool = soundPool ?: return
            val id = soundIds.remove(key) ?: return
            try { pool.unload(id); Log.d(TAG, "SFX '$key' выгружен: sampleId=$id") } catch (e: Exception) { Log.e(TAG, "Ошибка выгрузки SFX '$key'.", e) } finally { loadedSoundIds.remove(id) }
        }
    }

    fun unloadAllSFX() {
        synchronized(this) { val keys = soundIds.keys.toList(); keys.forEach { unloadSFX(it) }; Log.d(TAG, "Все SFX выгружены.") }
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        synchronized(musicLock) { try { musicPlayer?.setVolume(musicVolume, musicVolume) } catch (e: Exception) { Log.e(TAG, "Ошибка громкости.", e) } }
    }

    fun setSfxVolume(volume: Float) { sfxVolume = volume.coerceIn(0f, 1f) }
    fun getMusicVolume(): Float = musicVolume
    fun getSfxVolume(): Float = sfxVolume
    fun isInitialized(): Boolean = isInitialized.get()
    fun getLoadedSfxCount(): Int = soundIds.size

    // ИСПРАВЛЕНО: вместо count { it.value } — явный цикл
    fun getReadySfxCount(): Int {
        var count = 0
        for (v in loadedSoundIds.values) { if (v) count++ }
        return count
    }

    fun logState() {
        Log.i(TAG, buildString {
            appendLine("=== AudioPlayer State ===")
            appendLine("Инициализирован: ${isInitialized.get()}")
            appendLine("Музыка играет: ${isMusicPlaying()}")
            appendLine("resId: ${currentMusicRes ?: "нет"}")
            appendLine("SFX: ${getLoadedSfxCount()} (готово: ${getReadySfxCount()})")
            appendLine("=========================")
        })
    }
}
