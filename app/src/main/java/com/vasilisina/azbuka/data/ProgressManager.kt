// Сохранить в app/src/main/java/com/vasilisina/azbuka/data/ProgressManager.kt

package com.vasilisina.azbuka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

object ProgressManager {

    private const val TAG = "ProgressManager"
    private const val PREFS_NAME = "vasilisa_progress"
    private const val KEY_PROGRESS = "saved_progress"
    private const val KEY_DATA_VERSION = "data_version"
    private const val CURRENT_DATA_VERSION = 1
    private const val MAX_LEVELS = 6
    private const val MAX_STARS_PER_LEVEL = 3

    private val gson: Gson = GsonBuilder().create()

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var initialized: Boolean = false

    private val lock = Any()

    // ✅ Явный TypeToken с сохранением generic-сигнатуры
    private val savedProgressType = object : TypeToken<SavedProgress>() {}.type

    data class SavedProgress(
        val currentLevel: Int = 1,
        val stars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { 0 },
        val isAlbumUnlocked: Boolean = false,
        val dataVersion: Int = CURRENT_DATA_VERSION
    ) {
        fun copy(): SavedProgress = SavedProgress(currentLevel, stars.toMap(), isAlbumUnlocked, dataVersion)
    }

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            migrateIfNeeded()
            initialized = true
        }
    }

    fun isInitialized(): Boolean = initialized

    fun save(gameState: GameState) {
        ensureInitialized()
        val p = prefs ?: return
        try {
            val json = gson.toJson(gameStateToProgress(gameState))
            p.edit().putString(KEY_PROGRESS, json).putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
        } catch (e: Exception) { Log.e(TAG, "Ошибка сохранения", e) }
    }

    fun saveImmediately(gameState: GameState): Boolean {
        ensureInitialized()
        val p = prefs ?: return false
        return try {
            val json = gson.toJson(gameStateToProgress(gameState))
            p.edit().putString(KEY_PROGRESS, json).putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).commit()
        } catch (e: Exception) { Log.e(TAG, "Ошибка синхронного сохранения", e); false }
    }

    fun load(): SavedProgress {
        ensureInitialized()
        val p = prefs ?: return SavedProgress()
        val json = p.getString(KEY_PROGRESS, null) ?: return SavedProgress()
        return try {
            val loaded = gson.fromJson<SavedProgress>(json, savedProgressType) ?: return SavedProgress()
            sanitizeProgress(loaded)
        } catch (e: JsonSyntaxException) { Log.e(TAG, "Повреждён JSON", e); clear(); SavedProgress() }
        catch (e: Exception) { Log.e(TAG, "Ошибка загрузки", e); SavedProgress() }
    }

    fun loadAndApply(gameState: GameState): Boolean {
        return try { gameState.loadFrom(load()); true } catch (e: Exception) { false }
    }

    fun clear() {
        ensureInitialized()
        prefs?.edit()?.remove(KEY_PROGRESS)?.remove(KEY_DATA_VERSION)?.apply()
    }

    fun hasSave(): Boolean = prefs?.contains(KEY_PROGRESS) ?: false

    fun getSavedProgressCopy(): SavedProgress = load().copy()

    fun isSaveValid(): Boolean = hasSave() && try { load().dataVersion > 0 } catch (_: Exception) { false }

    private fun gameStateToProgress(gameState: GameState): SavedProgress {
        val safeStars = mutableMapOf<Int, Int>()
        for (level in 1..MAX_LEVELS) safeStars[level] = gameState.getStars(level).coerceIn(0, MAX_STARS_PER_LEVEL)
        return SavedProgress(gameState.currentLevel.coerceIn(1, MAX_LEVELS), safeStars.toMap(), gameState.isAlbumUnlocked, CURRENT_DATA_VERSION)
    }

    private fun sanitizeProgress(progress: SavedProgress): SavedProgress {
        var needsFix = false
        val safeLevel = if (progress.currentLevel in 1..MAX_LEVELS) progress.currentLevel else { needsFix = true; 1 }
        val safeStars = mutableMapOf<Int, Int>()
        for (level in 1..MAX_LEVELS) {
            val raw = progress.stars[level] ?: 0
            if (raw !in 0..MAX_STARS_PER_LEVEL) needsFix = true
            safeStars[level] = raw.coerceIn(0, MAX_STARS_PER_LEVEL)
        }
        var safeAlbum = progress.isAlbumUnlocked
        if (safeAlbum && (safeStars[MAX_LEVELS] ?: 0) == 0) { safeAlbum = false; needsFix = true }
        return if (needsFix) {
            val fixed = SavedProgress(safeLevel, safeStars.toMap(), safeAlbum, CURRENT_DATA_VERSION)
            saveFixedProgress(fixed)
            fixed
        } else progress
    }

    private fun saveFixedProgress(progress: SavedProgress) {
        try { prefs?.edit()?.putString(KEY_PROGRESS, gson.toJson(progress))?.putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)?.apply() } catch (_: Exception) {}
    }

    private fun ensureInitialized() {
        if (!initialized) throw IllegalStateException("ProgressManager не инициализирован. Вызовите init(context).")
    }

    private fun migrateIfNeeded() {
        val p = prefs ?: return
        val savedVersion = p.getInt(KEY_DATA_VERSION, 0)
        if (savedVersion < CURRENT_DATA_VERSION) {
            p.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
        }
    }
}
