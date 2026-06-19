// Сохранить в app/src/main/java/com/vasilisina/azbuka/data/GameState.kt

package com.vasilisina.azbuka.data

object GameState {

    // -------------------------------------------------------------------------
    // Константы
    // -------------------------------------------------------------------------

    /** Максимальное количество уровней в игре */
    const val MAX_LEVELS = 6

    /** Максимальное количество звёзд за один уровень */
    const val MAX_STARS_PER_LEVEL = 3

    /** Максимально возможное суммарное количество звёзд */
    const val TOTAL_MAX_STARS = MAX_LEVELS * MAX_STARS_PER_LEVEL

    // -------------------------------------------------------------------------
    // Состояние
    // -------------------------------------------------------------------------

    @Volatile
    var currentLevel: Int = 1
        private set

    @Volatile
    var stars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { 0 }
        private set

    @Volatile
    var isAlbumUnlocked: Boolean = false
        private set

    @Volatile
    private var isInitialized: Boolean = false

    // -------------------------------------------------------------------------
    // Инициализация
    // -------------------------------------------------------------------------

    @Synchronized
    fun init(progressManager: ProgressManager) {
        if (isInitialized) return
        val saved = progressManager.load()
        loadFrom(saved)
        isInitialized = true
    }

    // -------------------------------------------------------------------------
    // Игровые методы
    // -------------------------------------------------------------------------

    @Synchronized
    fun completeLevel(level: Int, earnedStars: Int) {
        require(level in 1..MAX_LEVELS) { "Номер уровня должен быть от 1 до $MAX_LEVELS, получено: $level" }
        val validStars = earnedStars.coerceIn(0, MAX_STARS_PER_LEVEL)
        val previousStars = stars[level] ?: 0
        if (validStars > previousStars) {
            val mutableStars = stars.toMutableMap()
            mutableStars[level] = validStars
            stars = mutableStars.toMap()
        }
        if (validStars > 0) {
            if (level < MAX_LEVELS) currentLevel = maxOf(currentLevel, level + 1)
            if (level == MAX_LEVELS) isAlbumUnlocked = true
        }
        ProgressManager.save(this)
    }

    fun loadLevel(levelIndex: Int): Boolean = isLevelUnlocked(levelIndex)

    @Synchronized
    fun unlockNextLevel(): Int {
        if (currentLevel >= MAX_LEVELS) return -1
        currentLevel += 1
        ProgressManager.save(this)
        return currentLevel
    }

    // -------------------------------------------------------------------------
    // Проверки состояния
    // -------------------------------------------------------------------------

    fun isLevelUnlocked(level: Int): Boolean = level in 1..MAX_LEVELS && level <= currentLevel
    fun getStars(level: Int): Int = if (level in 1..MAX_LEVELS) stars[level] ?: 0 else 0
    fun getTotalStars(): Int = (1..MAX_LEVELS).sumOf { stars[it] ?: 0 }
    fun isGameCompleted(): Boolean = isAlbumUnlocked
    fun isLevelCompleted(level: Int): Boolean = getStars(level) > 0
    fun getCompletionPercent(): Int = (getTotalStars() * 100) / TOTAL_MAX_STARS

    // -------------------------------------------------------------------------
    // Сериализация
    // -------------------------------------------------------------------------

    @Synchronized
    fun loadFrom(progress: ProgressManager.SavedProgress) {
        currentLevel = progress.currentLevel.coerceIn(1, MAX_LEVELS)
        val safeStars = mutableMapOf<Int, Int>()
        for (level in 1..MAX_LEVELS) safeStars[level] = (progress.stars[level] ?: 0).coerceIn(0, MAX_STARS_PER_LEVEL)
        stars = safeStars.toMap()
        isAlbumUnlocked = progress.isAlbumUnlocked && (stars[MAX_LEVELS] ?: 0) > 0
        isInitialized = true
    }

    fun toSavedProgress() = ProgressManager.SavedProgress(currentLevel = currentLevel, stars = stars, isAlbumUnlocked = isAlbumUnlocked)

    // -------------------------------------------------------------------------
    // Сброс и отладка
    // -------------------------------------------------------------------------

    @Synchronized
    fun reset() {
        currentLevel = 1
        stars = (1..MAX_LEVELS).associateWith { 0 }
        isAlbumUnlocked = false
        isInitialized = true
        ProgressManager.save(this)
    }

    @Synchronized
    fun debugSetState(level: Int = MAX_LEVELS, levelStars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { MAX_STARS_PER_LEVEL }, albumUnlocked: Boolean = true) {
        currentLevel = level.coerceIn(1, MAX_LEVELS)
        val safeStars = mutableMapOf<Int, Int>()
        for (lvl in 1..MAX_LEVELS) safeStars[lvl] = (levelStars[lvl] ?: 0).coerceIn(0, MAX_STARS_PER_LEVEL)
        stars = safeStars.toMap()
        isAlbumUnlocked = albumUnlocked && (stars[MAX_LEVELS] ?: 0) > 0
    }

    override fun toString(): String {
        val starsStr = (1..MAX_LEVELS).joinToString(", ") { "Ур.$it: ${"★".repeat(stars[it] ?: 0)}${"☆".repeat(MAX_STARS_PER_LEVEL - (stars[it] ?: 0))}" }
        return "GameState { Уровней: $currentLevel/$MAX_LEVELS, Звёзды: $starsStr, Альбом: $isAlbumUnlocked, Прогресс: ${getCompletionPercent()}% }"
    }
}
