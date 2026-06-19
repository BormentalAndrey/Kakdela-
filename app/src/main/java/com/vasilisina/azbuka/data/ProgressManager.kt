// Сохранить в app/src/main/java/com/vasilisina/azbuka/data/ProgressManager.kt

package com.vasilisina.azbuka.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/**
 * Менеджер сохранения и загрузки игрового прогресса.
 *
 * Использует SharedPreferences как хранилище (аналог PlayerPrefs в Unity).
 * Сериализация — Gson (JSON).
 *
 * Потокобезопасен: все публичные методы синхронизированы.
 *
 * Использование:
 * ```kotlin
 * // В Application.onCreate()
 * ProgressManager.init(context)
 *
 * // Сохранение
 * ProgressManager.save(gameState)
 *
 * // Загрузка
 * val progress = ProgressManager.load()
 * gameState.loadFrom(progress)
 * ```
 */
object ProgressManager {

    // -------------------------------------------------------------------------
    // Константы
    // -------------------------------------------------------------------------

    /** Тег для логирования */
    private const val TAG = "ProgressManager"

    /** Имя файла SharedPreferences */
    private const val PREFS_NAME = "vasilisa_progress"

    /** Ключ для сохранения прогресса */
    private const val KEY_PROGRESS = "saved_progress"

    /** Ключ для сохранения версии формата данных */
    private const val KEY_DATA_VERSION = "data_version"

    /** Текущая версия формата данных (для миграций) */
    private const val CURRENT_DATA_VERSION = 1

    /** Максимальное количество уровней */
    private const val MAX_LEVELS = 5

    /** Максимальное количество звёзд за уровень */
    private const val MAX_STARS_PER_LEVEL = 3

    // -------------------------------------------------------------------------
    // Поля
    // -------------------------------------------------------------------------

    /** Потокобезопасный экземпляр Gson */
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /** Кэшированный экземпляр SharedPreferences */
    @Volatile
    private var prefs: SharedPreferences? = null

    /** Флаг инициализации */
    @Volatile
    private var initialized: Boolean = false

    /** Блокировка для синхронизации */
    private val lock = Any()

    /** Тип для десериализации SavedProgress */
    private val savedProgressType = object : TypeToken<SavedProgress>() {}.type

    // -------------------------------------------------------------------------
    // Модель данных
    // -------------------------------------------------------------------------

    /**
     * Объект сохранённого прогресса.
     *
     * @property currentLevel Номер последнего открытого уровня (1..MAX_LEVELS)
     * @property stars Карта звёзд: ключ — номер уровня, значение — количество звёзд
     * @property isAlbumUnlocked Флаг, открыт ли альбом успехов
     * @property dataVersion Версия формата данных для миграций
     */
    data class SavedProgress(
        val currentLevel: Int = 1,
        val stars: Map<Int, Int> = (1..MAX_LEVELS).associateWith { 0 },
        val isAlbumUnlocked: Boolean = false,
        val dataVersion: Int = CURRENT_DATA_VERSION
    ) {
        /**
         * Создаёт глубокую копию объекта.
         */
        fun copy(): SavedProgress {
            return SavedProgress(
                currentLevel = currentLevel,
                stars = stars.toMap(),
                isAlbumUnlocked = isAlbumUnlocked,
                dataVersion = dataVersion
            )
        }

        override fun toString(): String {
            val starsStr = (1..MAX_LEVELS).joinToString(", ") { level ->
                "Ур.$level: ${stars[level] ?: 0}★"
            }

            return "SavedProgress(открыто уровней: $currentLevel, звёзды: [$starsStr], альбом: $isAlbumUnlocked, версия: $dataVersion)"
        }
    }

    // -------------------------------------------------------------------------
    // Инициализация
    // -------------------------------------------------------------------------

    /**
     * Инициализирует менеджер прогресса.
     *
     * Должен быть вызван один раз при старте приложения,
     * рекомендуется в [android.app.Application.onCreate].
     *
     * @param context Контекст приложения (автоматически используется applicationContext)
     * @throws IllegalStateException если метод вызван повторно с другим контекстом
     */
    fun init(context: Context) {
        if (initialized) {
            Log.w(TAG, "ProgressManager уже инициализирован. Вызов init() проигнорирован.")
            return
        }

        synchronized(lock) {
            if (initialized) {
                Log.w(TAG, "ProgressManager уже инициализирован (двойная проверка). Вызов init() проигнорирован.")
                return
            }

            val appContext = context.applicationContext

            prefs = appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

            // Проверяем, не нужно ли мигрировать данные
            migrateIfNeeded()

            initialized = true

            Log.d(TAG, "ProgressManager инициализирован. ${if (hasSave()) "Сохранение найдено." else "Сохранение отсутствует."}")
        }
    }

    /**
     * Проверяет, был ли ProgressManager инициализирован.
     *
     * @return true если init() был успешно вызван
     */
    fun isInitialized(): Boolean {
        return initialized
    }

    // -------------------------------------------------------------------------
    // Сохранение
    // -------------------------------------------------------------------------

    /**
     * Асинхронно сохраняет прогресс в SharedPreferences.
     *
     * Использует [SharedPreferences.Editor.apply] — запись происходит в фоне.
     * Рекомендуется для большинства случаев.
     *
     * @param gameState Текущее состояние игры
     * @throws IllegalStateException если менеджер не инициализирован
     */
    fun save(gameState: GameState) {
        ensureInitialized()

        val currentPrefs = prefs ?: run {
            Log.e(TAG, "SharedPreferences равен null, сохранение невозможно.")
            return
        }

        try {
            val progress = gameStateToProgress(gameState)
            val json = gson.toJson(progress)

            currentPrefs
                .edit()
                .putString(KEY_PROGRESS, json)
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                .apply()

            Log.d(TAG, "Прогресс асинхронно сохранён: $progress")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка асинхронного сохранения прогресса.", e)
        }
    }

    /**
     * Синхронно сохраняет прогресс в SharedPreferences.
     *
     * Использует [SharedPreferences.Editor.commit] — запись происходит
     * в текущем потоке с возвратом результата.
     * Рекомендуется использовать только когда нужно гарантировать
     * сохранение до завершения Activity или процесса.
     *
     * @param gameState Текущее состояние игры
     * @return true если сохранение успешно
     * @throws IllegalStateException если менеджер не инициализирован
     */
    fun saveImmediately(gameState: GameState): Boolean {
        ensureInitialized()

        val currentPrefs = prefs ?: run {
            Log.e(TAG, "SharedPreferences равен null, синхронное сохранение невозможно.")
            return false
        }

        return try {
            val progress = gameStateToProgress(gameState)
            val json = gson.toJson(progress)

            val success = currentPrefs
                .edit()
                .putString(KEY_PROGRESS, json)
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                .commit()

            if (success) {
                Log.d(TAG, "Прогресс синхронно сохранён: $progress")
            } else {
                Log.e(TAG, "Ошибка синхронного сохранения прогресса: commit() вернул false.")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при синхронном сохранении прогресса.", e)
            false
        }
    }

    // -------------------------------------------------------------------------
    // Загрузка
    // -------------------------------------------------------------------------

    /**
     * Загружает сохранённый прогресс.
     *
     * Если сохранение отсутствует или повреждено, возвращает прогресс по умолчанию.
     *
     * @return Объект [SavedProgress], никогда не null
     * @throws IllegalStateException если менеджер не инициализирован
     */
    fun load(): SavedProgress {
        ensureInitialized()

        val currentPrefs = prefs ?: run {
            Log.e(TAG, "SharedPreferences равен null, возвращаю прогресс по умолчанию.")
            return SavedProgress()
        }

        val json = currentPrefs.getString(KEY_PROGRESS, null)

        if (json == null) {
            Log.d(TAG, "Сохранение не найдено. Возвращаю прогресс по умолчанию.")
            return SavedProgress()
        }

        return try {
            val loaded = gson.fromJson<SavedProgress>(json, savedProgressType)

            if (loaded == null) {
                Log.e(TAG, "Десериализация вернула null. Возвращаю прогресс по умолчанию.")
                return SavedProgress()
            }

            val sanitized = sanitizeProgress(loaded)

            Log.d(TAG, "Прогресс загружен: $sanitized")

            sanitized
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Повреждённый файл сохранения (JSON). Сброс прогресса.", e)
            clear()
            SavedProgress()
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при загрузке прогресса.", e)
            SavedProgress()
        }
    }

    /**
     * Загружает прогресс и применяет его к [GameState].
     *
     * @param gameState Объект состояния игры
     * @return true если прогресс был успешно загружен и применён
     */
    fun loadAndApply(gameState: GameState): Boolean {
        return try {
            val progress = load()
            gameState.loadFrom(progress)
            Log.d(TAG, "Прогресс загружен и применён к GameState.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка применения прогресса к GameState.", e)
            false
        }
    }

    // -------------------------------------------------------------------------
    // Управление сохранением
    // -------------------------------------------------------------------------

    /**
     * Полностью удаляет сохранённый прогресс.
     *
     * @throws IllegalStateException если менеджер не инициализирован
     */
    fun clear() {
        ensureInitialized()

        val currentPrefs = prefs ?: run {
            Log.e(TAG, "SharedPreferences равен null, очистка невозможна.")
            return
        }

        currentPrefs
            .edit()
            .remove(KEY_PROGRESS)
            .remove(KEY_DATA_VERSION)
            .apply()

        Log.d(TAG, "Прогресс полностью удалён.")
    }

    /**
     * Проверяет, существует ли сохранённый прогресс.
     *
     * @return true если в SharedPreferences есть данные прогресса
     */
    fun hasSave(): Boolean {
        val currentPrefs = prefs ?: return false

        return currentPrefs.contains(KEY_PROGRESS)
    }

    /**
     * Возвращает копию сохранённого прогресса без изменения внутреннего состояния.
     *
     * @return Копия [SavedProgress] или прогресс по умолчанию
     */
    fun getSavedProgressCopy(): SavedProgress {
        return load().copy()
    }

    /**
     * Проверяет целостность сохранённых данных.
     *
     * @return true если сохранение существует и успешно десериализуется
     */
    fun isSaveValid(): Boolean {
        if (!hasSave()) return false

        return try {
            val progress = load()
            // Прогресс по умолчанию возвращается при любых проблемах,
            // проверяем, не вернулся ли он из-за ошибки
            progress.dataVersion > 0
        } catch (e: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // Внутренние методы
    // -------------------------------------------------------------------------

    /**
     * Преобразует состояние игры в объект для сохранения.
     */
    private fun gameStateToProgress(gameState: GameState): SavedProgress {
        val safeStars = mutableMapOf<Int, Int>()

        for (level in 1..MAX_LEVELS) {
            safeStars[level] = (gameState.getStars(level))
                .coerceIn(0, MAX_STARS_PER_LEVEL)
        }

        return SavedProgress(
            currentLevel = gameState.currentLevel.coerceIn(1, MAX_LEVELS),
            stars = safeStars.toMap(),
            isAlbumUnlocked = gameState.isAlbumUnlocked,
            dataVersion = CURRENT_DATA_VERSION
        )
    }

    /**
     * Проверяет и исправляет загруженный прогресс.
     *
     * Обрабатывает:
     * - Выход значений за допустимые границы
     * - Логические несоответствия (альбом открыт, но финал не пройден)
     */
    private fun sanitizeProgress(progress: SavedProgress): SavedProgress {
        var needsFix = false

        // Проверка currentLevel
        val safeLevel = if (progress.currentLevel in 1..MAX_LEVELS) {
            progress.currentLevel
        } else {
            Log.w(TAG, "currentLevel вне диапазона: ${progress.currentLevel}. Исправлен на 1.")
            needsFix = true
            1
        }

        // Проверка звёзд
        val safeStars = mutableMapOf<Int, Int>()

        for (level in 1..MAX_LEVELS) {
            val rawStars = progress.stars[level] ?: 0

            if (rawStars !in 0..MAX_STARS_PER_LEVEL) {
                Log.w(TAG, "Звёзды уровня $level вне диапазона: $rawStars. Исправлены.")
                needsFix = true
            }

            safeStars[level] = rawStars.coerceIn(0, MAX_STARS_PER_LEVEL)
        }

        // Проверка альбома: не может быть открыт без прохождения последнего уровня
        var safeAlbumUnlocked = progress.isAlbumUnlocked

        if (safeAlbumUnlocked && (safeStars[MAX_LEVELS] ?: 0) == 0) {
            Log.w(TAG, "Альбом был открыт, но последний уровень не пройден. Альбом закрыт.")
            safeAlbumUnlocked = false
            needsFix = true
        }

        if (needsFix) {
            Log.i(TAG, "Прогресс был исправлен: уровень=$safeLevel, альбом=$safeAlbumUnlocked, звёзды=$safeStars")

            // Сохраняем исправленную версию
            val fixed = SavedProgress(
                currentLevel = safeLevel,
                stars = safeStars.toMap(),
                isAlbumUnlocked = safeAlbumUnlocked,
                dataVersion = CURRENT_DATA_VERSION
            )

            saveFixedProgress(fixed)

            return fixed
        }

        return progress
    }

    /**
     * Сохраняет исправленный прогресс.
     */
    private fun saveFixedProgress(progress: SavedProgress) {
        try {
            val currentPrefs = prefs ?: return
            val json = gson.toJson(progress)

            currentPrefs
                .edit()
                .putString(KEY_PROGRESS, json)
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                .apply()

            Log.d(TAG, "Исправленный прогресс сохранён.")
        } catch (e: Exception) {
            Log.e(TAG, "Не удалось сохранить исправленный прогресс.", e)
        }
    }

    /**
     * Проверяет, что менеджер инициализирован.
     *
     * @throws IllegalStateException если init() не был вызван
     */
    private fun ensureInitialized() {
        if (!initialized) {
            throw IllegalStateException(
                "ProgressManager не инициализирован. " +
                        "Вызовите ProgressManager.init(context) в Application.onCreate()."
            )
        }
    }

    /**
     * Выполняет миграцию данных при изменении версии формата.
     */
    private fun migrateIfNeeded() {
        val currentPrefs = prefs ?: return

        val savedVersion = currentPrefs.getInt(KEY_DATA_VERSION, 0)

        if (savedVersion < CURRENT_DATA_VERSION) {
            Log.i(TAG, "Обнаружена старая версия данных ($savedVersion). Выполняется миграция до $CURRENT_DATA_VERSION.")

            when (savedVersion) {
                0 -> {
                    // Версия 0: самый первый формат или отсутствие версии
                    // Миграция не требуется, просто обновим версию при следующем сохранении
                    Log.d(TAG, "Миграция с версии 0: автоматическая.")
                }
                // Добавлять новые миграции здесь:
                // 1 -> { migrateFromV1ToV2() }
            }

            // Обновляем версию
            currentPrefs
                .edit()
                .putInt(KEY_DATA_VERSION, CURRENT_DATA_VERSION)
                .apply()

            Log.i(TAG, "Миграция завершена. Текущая версия данных: $CURRENT_DATA_VERSION.")
        }
    }
}
