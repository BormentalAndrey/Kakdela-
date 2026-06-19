// Сохранить в app/src/main/java/com/vasilisina/azbuka/data/GameState.kt

package com.vasilisina.azbuka.data

/**
 * Глобальное состояние игры — потокобезопасный синглтон.
 *
 * Хранит:
 * - текущий прогресс (открытые уровни, звёзды)
 * - флаг доступа к альбому успехов
 *
 * Синхронизируется с SharedPreferences через [ProgressManager].
 *
 * Использование:
 * ```kotlin
 * // Инициализация (выполняется один раз в Application)
 * GameState.init(context)
 *
 * // Завершение уровня
 * GameState.completeLevel(level = 1, earnedStars = 3)
 *
 * // Проверка доступа
 * if (GameState.isLevelUnlocked(2)) { ... }
 * ```
 */
object GameState {

    // -------------------------------------------------------------------------
    // Константы
    // -------------------------------------------------------------------------

    /** Максимальное количество уровней в игре */
    const val MAX_LEVELS = 5

    /** Максимальное количество звёзд за один уровень */
    const val MAX_STARS_PER_LEVEL = 3

    /** Максимально возможное суммарное количество звёзд */
    const val TOTAL_MAX_STARS = MAX_LEVELS * MAX_STARS_PER_LEVEL

    // -------------------------------------------------------------------------
    // Состояние
    // -------------------------------------------------------------------------

    /**
     * Номер последнего открытого уровня (1..MAX_LEVELS).
     * Игрок может проходить уровень, только если level <= currentLevel.
     */
    @Volatile
    var currentLevel: Int = 1
        private set

    /**
     * Звёзды за каждый уровень.
     * Ключ — номер уровня (1..MAX_LEVELS), значение — количество звёзд (0..MAX_STARS_PER_LEVEL).
     */
    @Volatile
    var stars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { 0 }
        private set

    /**
     * Флаг, показывающий, открыт ли альбом успехов.
     * Становится true после первого успешного прохождения последнего уровня.
     */
    @Volatile
    var isAlbumUnlocked: Boolean = false
        private set

    /** Признак того, что синглтон был проинициализирован */
    @Volatile
    private var isInitialized: Boolean = false

    // -------------------------------------------------------------------------
    // Инициализация
    // -------------------------------------------------------------------------

    /**
     * Инициализирует состояние, загружая сохранённый прогресс.
     * Должен быть вызван один раз при старте приложения.
     *
     * @param progressManager Экземпляр менеджера прогресса с уже вызванным init()
     */
    @Synchronized
    fun init(progressManager: ProgressManager) {
        if (isInitialized) {
            return
        }

        val saved = progressManager.load()
        loadFrom(saved)
        isInitialized = true
    }

    // -------------------------------------------------------------------------
    // Игровые методы
    // -------------------------------------------------------------------------

    /**
     * Отмечает успешное прохождение уровня.
     *
     * Логика:
     * 1. Сохраняет лучший результат по звёздам.
     * 2. Если уровень пройден (хотя бы 1 звезда) — открывает следующий.
     * 3. После прохождения последнего уровня — открывает альбом.
     * 4. Автоматически синхронизирует состояние с SharedPreferences.
     *
     * @param level Номер пройденного уровня (1..MAX_LEVELS)
     * @param earnedStars Количество заработанных звёзд (0..MAX_STARS_PER_LEVEL)
     * @throws IllegalArgumentException если level вне допустимого диапазона
     */
    @Synchronized
    fun completeLevel(level: Int, earnedStars: Int) {
        require(level in 1..MAX_LEVELS) {
            "Номер уровня должен быть от 1 до $MAX_LEVELS, получено: $level"
        }

        val validStars = earnedStars.coerceIn(0, MAX_STARS_PER_LEVEL)
        val previousStars = stars[level] ?: 0

        // Сохраняем лучший результат
        if (validStars > previousStars) {
            val mutableStars = stars.toMutableMap()
            mutableStars[level] = validStars
            stars = mutableStars.toMap()
        }

        // Прогресс возможен только если уровень действительно пройден
        if (validStars > 0) {
            // Открываем следующий уровень (если не последний)
            if (level < MAX_LEVELS) {
                currentLevel = maxOf(currentLevel, level + 1)
            }

            // Финальный уровень открывает альбом
            if (level == MAX_LEVELS) {
                isAlbumUnlocked = true
            }
        }

        // Сохраняем прогресс в SharedPreferences
        ProgressManager.save(this)
    }

    /**
     * Загружает уровень по его индексу.
     * В текущей архитектуре уровень загружается через навигацию,
     * этот метод выполняет валидацию доступа.
     *
     * @param levelIndex Номер уровня для загрузки (1..MAX_LEVELS)
     * @return true если уровень доступен и может быть загружен
     */
    fun loadLevel(levelIndex: Int): Boolean {
        return isLevelUnlocked(levelIndex)
    }

    /**
     * Разблокирует следующий уровень.
     * Может использоваться для отладки или специальных механик.
     *
     * @return Номер только что открытого уровня или -1, если все уже открыты
     */
    @Synchronized
    fun unlockNextLevel(): Int {
        if (currentLevel >= MAX_LEVELS) {
            return -1
        }

        currentLevel += 1
        ProgressManager.save(this)

        return currentLevel
    }

    // -------------------------------------------------------------------------
    // Проверки состояния
    // -------------------------------------------------------------------------

    /**
     * Проверяет, открыт ли указанный уровень.
     *
     * @param level Номер уровня (1..MAX_LEVELS)
     * @return true если уровень доступен для игры
     */
    fun isLevelUnlocked(level: Int): Boolean {
        if (level !in 1..MAX_LEVELS) return false
        return level <= currentLevel
    }

    /**
     * Возвращает количество звёзд за указанный уровень.
     *
     * @param level Номер уровня (1..MAX_LEVELS)
     * @return Количество звёзд (0..MAX_STARS_PER_LEVEL) или 0 для некорректного уровня
     */
    fun getStars(level: Int): Int {
        if (level !in 1..MAX_LEVELS) return 0
        return stars[level] ?: 0
    }

    /**
     * Возвращает суммарное количество звёзд по всем уровням.
     */
    fun getTotalStars(): Int {
        var total = 0
        for (level in 1..MAX_LEVELS) {
            total += stars[level] ?: 0
        }
        return total
    }

    /**
     * Проверяет, полностью ли пройдена игра.
     * Игра считается пройденной, если открыт альбом (пройден последний уровень).
     */
    fun isGameCompleted(): Boolean {
        return isAlbumUnlocked
    }

    /**
     * Проверяет, пройден ли указанный уровень (есть хотя бы 1 звезда).
     *
     * @param level Номер уровня
     * @return true если уровень был пройден
     */
    fun isLevelCompleted(level: Int): Boolean {
        return getStars(level) > 0
    }

    /**
     * Возвращает процент завершения игры (0..100).
     */
    fun getCompletionPercent(): Int {
        val total = getTotalStars()
        return (total * 100) / TOTAL_MAX_STARS
    }

    // -------------------------------------------------------------------------
    // Сериализация
    // -------------------------------------------------------------------------

    /**
     * Загружает прогресс из сохранённых данных.
     * Выполняет валидацию всех полей.
     *
     * @param progress Десериализованный объект прогресса
     */
    @Synchronized
    fun loadFrom(progress: ProgressManager.SavedProgress) {
        currentLevel = progress.currentLevel.coerceIn(1, MAX_LEVELS)

        val safeStars = mutableMapOf<Int, Int>()
        for (level in 1..MAX_LEVELS) {
            safeStars[level] = (progress.stars[level] ?: 0).coerceIn(0, MAX_STARS_PER_LEVEL)
        }
        stars = safeStars.toMap()

        // Альбом может быть открыт, только если финальный уровень пройден
        isAlbumUnlocked = progress.isAlbumUnlocked && (stars[MAX_LEVELS] ?: 0) > 0

        isInitialized = true
    }

    /**
     * Формирует объект для сохранения.
     *
     * @return [ProgressManager.SavedProgress] с текущим состоянием
     */
    fun toSavedProgress(): ProgressManager.SavedProgress {
        return ProgressManager.SavedProgress(
            currentLevel = currentLevel,
            stars = stars,
            isAlbumUnlocked = isAlbumUnlocked
        )
    }

    // -------------------------------------------------------------------------
    // Сброс и отладка
    // -------------------------------------------------------------------------

    /**
     * Полный сброс прогресса.
     * Используется для отладки или кнопки «Начать заново».
     */
    @Synchronized
    fun reset() {
        currentLevel = 1
        stars = (1..MAX_LEVELS).associateWith { 0 }
        isAlbumUnlocked = false
        isInitialized = true

        ProgressManager.save(this)
    }

    /**
     * Устанавливает состояние игры для отладки.
     * Не сохраняет изменения в SharedPreferences.
     *
     * @param level Открытый уровень
     * @param levelStars Карта звёзд
     * @param albumUnlocked Флаг альбома
     */
    @Synchronized
    fun debugSetState(
        level: Int = MAX_LEVELS,
        levelStars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { MAX_STARS_PER_LEVEL },
        albumUnlocked: Boolean = true
    ) {
        currentLevel = level.coerceIn(1, MAX_LEVELS)

        val safeStars = mutableMapOf<Int, Int>()
        for (lvl in 1..MAX_LEVELS) {
            safeStars[lvl] = (levelStars[lvl] ?: 0).coerceIn(0, MAX_STARS_PER_LEVEL)
        }
        stars = safeStars.toMap()

        isAlbumUnlocked = albumUnlocked && (stars[MAX_LEVELS] ?: 0) > 0
    }

    // -------------------------------------------------------------------------
    // Внутренние методы
    // -------------------------------------------------------------------------

    /**
     * Строковое представление состояния для логов.
     */
    override fun toString(): String {
        val starsStr = (1..MAX_LEVELS).joinToString(", ") { level ->
            "Ур.$level: ${"★".repeat(stars[level] ?: 0)}${"☆".repeat(MAX_STARS_PER_LEVEL - (stars[level] ?: 0))}"
        }

        return buildString {
            appendLine("GameState {")
            appendLine("  Открыто уровней: $currentLevel/$MAX_LEVELS")
            appendLine("  Звёзды: $starsStr")
            appendLine("  Всего звёзд: ${getTotalStars()}/$TOTAL_MAX_STARS")
            appendLine("  Альбом: ${if (isAlbumUnlocked) "открыт" else "закрыт"}")
            appendLine("  Прогресс: ${getCompletionPercent()}%")
            appendLine("}")
        }
    }
}
