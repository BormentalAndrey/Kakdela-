// Сохранить в app/src/main/java/com/vasilisina/azbuka/data/ProgressManager.kt

package com.vasilisina.azbuka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

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

    // ✅ Вспомогательный класс со String-ключами (без TypeToken)
    private data class SavedProgressJson(
        val currentLevel: Int = 1,
        val stars: Map<String, Int> = emptyMap(),
        val isAlbumUnlocked: Boolean = false,
        val dataVersion: Int = CURRENT_DATA_VERSION
    )

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
            p.edit().putString(KEY_PROGRESS, gson.toJson(gameStateToProgress(gameState)))
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
        } catch (e: Exception) { Log.e(TAG, "Ошибка сохранения", e) }
    }

    fun saveImmediately(gameState: GameState): Boolean {
        ensureInitialized()
        val p = prefs ?: return false
        return try {
            p.edit().putString(KEY_PROGRESS, gson.toJson(gameStateToProgress(gameState)))
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).commit()
        } catch (e: Exception) { false }
    }

    fun load(): SavedProgress {
        ensureInitialized()
        val p = prefs ?: return SavedProgress()
        val json = p.getString(KEY_PROGRESS, null) ?: return SavedProgress()
        return try {
            // ✅ Десериализация через SavedProgressJson (String-ключи)
            val loaded = gson.fromJson(json, SavedProgressJson::class.java) ?: return SavedProgress()
            val starsInt = mutableMapOf<Int, Int>()
            loaded.stars.forEach { (key, value) -> key.toIntOrNull()?.let { starsInt[it] = value } }
            sanitizeProgress(SavedProgress(
                loaded.currentLevel.coerceIn(1, MAX_LEVELS),
                if (starsInt.isEmpty()) (1..MAX_LEVELS).associateWith { 0 } else starsInt.toMap(),
                loaded.isAlbumUnlocked,
                loaded.dataVersion
            ))
        } catch (e: JsonSyntaxException) { Log.e(TAG, "Повреждён JSON", e); clear(); SavedProgress() }
        catch (e: Exception) { Log.e(TAG, "Ошибка загрузки", e); SavedProgress() }
    }

    fun loadAndApply(gameState: GameState): Boolean = try { gameState.loadFrom(load()); true } catch (_: Exception) { false }

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
            try { prefs?.edit()?.putString(KEY_PROGRESS, gson.toJson(fixed))?.putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)?.apply() } catch (_: Exception) {}
            fixed
        } else progress
    }

    private fun ensureInitialized() {
        if (!initialized) throw IllegalStateException("ProgressManager не инициализирован.")
    }

    private fun migrateIfNeeded() {
        val p = prefs ?: return
        if (p.getInt(KEY_DATA_VERSION, 0) < CURRENT_DATA_VERSION) {
            p.edit().putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION).apply()
        }
    }
}
