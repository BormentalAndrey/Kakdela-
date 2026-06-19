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

    @Volatile private var musicPlayer: MediaPlayer? = null
    @Volatile private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<String, Int>()
    private val loadedSoundIds = ConcurrentHashMap<Int, Boolean>()
    @Volatile private var currentMusicRes: Int? = null
    @Volatile private var musicVolume: Float = 1f
    @Volatile private var sfxVolume: Float = 1f
    private val isInitialized = AtomicBoolean(false)
    private val musicLock = Any()

    @Synchronized
    fun init(context: Context) {
        if (isInitialized.get()) return
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
                        } else {
                            loadedSoundIds.remove(sampleId)
                        }
                    }
                }
            isInitialized.set(true)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации", e)
            isInitialized.set(false)
        }
    }

    @Synchronized
    fun release() {
        stopMusic()
        try { soundPool?.release() } catch (_: Exception) {}
        soundPool = null
        soundIds.clear()
        loadedSoundIds.clear()
        currentMusicRes = null
        isInitialized.set(false)
    }

    fun playMusic(context: Context, resId: Int, loop: Boolean = true) {
        synchronized(musicLock) {
            if (currentMusicRes == resId && isMusicPlaying()) return
            stopMusicInternal()
            try {
                val player = MediaPlayer.create(context.applicationContext, resId)
                if (player == null) return
                currentMusicRes = resId
                musicPlayer = player
                player.isLooping = loop
                player.setVolume(musicVolume, musicVolume)
                player.setOnCompletionListener { mp ->
                    if (!mp.isLooping) {
                        synchronized(musicLock) {
                            if (musicPlayer === mp) {
                                releasePlayer(mp)
                                musicPlayer = null
                                currentMusicRes = null
                            }
                        }
                    }
                }
                player.setOnErrorListener { mp, _, _ ->
                    synchronized(musicLock) {
                        if (musicPlayer === mp) {
                            releasePlayer(mp)
                            musicPlayer = null
                            currentMusicRes = null
                        }
                    }
                    true
                }
                player.start()
            } catch (e: Exception) {
                stopMusicInternal()
            }
        }
    }

    fun pauseMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && player.isPlaying) {
                    player.pause()
                }
            } catch (_: Exception) {}
        }
    }

    fun resumeMusic() {
        synchronized(musicLock) {
            try {
                val player = musicPlayer
                if (player != null && !player.isPlaying) {
                    player.start()
                }
            } catch (_: Exception) {}
        }
    }

    fun stopMusic() {
        synchronized(musicLock) { stopMusicInternal() }
    }

    private fun stopMusicInternal() {
        try {
            val player = musicPlayer
            if (player != null) {
                releasePlayer(player)
                musicPlayer = null
                currentMusicRes = null
            }
        } catch (_: Exception) {
            musicPlayer = null
            currentMusicRes = null
        }
    }

    private fun releasePlayer(player: MediaPlayer) {
        try {
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            if (player.isPlaying) player.stop()
            player.reset()
            player.release()
        } catch (_: Exception) {}
    }

    fun isMusicPlaying(): Boolean {
        synchronized(musicLock) {
            val mp = musicPlayer ?: return false
            return try { mp.isPlaying } catch (_: Exception) { false }
        }
    }

    fun loadSFX(context: Context, resId: Int, key: String) {
        val pool = soundPool ?: return
        if (soundIds.containsKey(key)) return
        try {
            val id = pool.load(context.applicationContext, resId, SFX_PRIORITY)
            soundIds[key] = id
            loadedSoundIds[id] = false
        } catch (_: Exception) {}
    }

    fun loadSFXBatch(context: Context, sfxMap: Map<String, Int>) {
        sfxMap.forEach { (key, resId) -> loadSFX(context, resId, key) }
    }

    fun playSFX(key: String) {
        val id = soundIds[key] ?: return
        val pool = soundPool ?: return
        if (loadedSoundIds[id] != true) return
        try { pool.play(id, sfxVolume, sfxVolume, SFX_PRIORITY, 0, SFX_RATE) } catch (_: Exception) {}
    }

    fun isSoundLoaded(key: String): Boolean {
        val id = soundIds[key] ?: return false
        return loadedSoundIds[id] == true
    }

    fun unloadSFX(key: String) {
        synchronized(this) {
            val pool = soundPool ?: return
            val id = soundIds.remove(key) ?: return
            try { pool.unload(id) } catch (_: Exception) {} finally { loadedSoundIds.remove(id) }
        }
    }

    fun unloadAllSFX() {
        synchronized(this) {
            soundIds.keys.toList().forEach { unloadSFX(it) }
        }
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        synchronized(musicLock) {
            try { musicPlayer?.setVolume(musicVolume, musicVolume) } catch (_: Exception) {}
        }
    }

    fun setSfxVolume(volume: Float) { sfxVolume = volume.coerceIn(0f, 1f) }
    fun getMusicVolume(): Float = musicVolume
    fun getSfxVolume(): Float = sfxVolume
    fun isInitialized(): Boolean = isInitialized.get()
    fun getLoadedSfxCount(): Int = soundIds.size

    fun getReadySfxCount(): Int {
        var c = 0
        for (v in loadedSoundIds.values) { if (v) c++ }
        return c
    }
}
