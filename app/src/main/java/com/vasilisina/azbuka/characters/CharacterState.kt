// Сохранить в app/src/main/java/com/vasilisina/azbuka/characters/CharacterState.kt

package com.vasilisina.azbuka.characters

/**
 * Перечисление эмоций персонажей игры «Василисина азбука».
 *
 * Каждая эмоция соответствует набору спрайтов в `res/drawable/`:
 * - `vasilisa_happy` / `kuzya_happy`
 * - `vasilisa_sad`   / `kuzya_sad`
 * - `vasilisa_clap`  / `kuzya_clap`
 * - `vasilisa_idle`  / `kuzya_idle`
 *
 * @property isAnimated  `true`, если эмоция подразумевает анимацию (например, хлопки).
 * @property isPositive  `true`, если эмоция позитивная (радость, аплодисменты).
 * @property isNegative  `true`, если эмоция негативная (грусть).
 */
enum class CharacterEmotion {

    /** Радость / улыбка */
    HAPPY,

    /** Грусть / огорчение */
    SAD,

    /** Аплодисменты / хлопки (анимированное состояние) */
    CLAP,

    /** Нейтральное / idle-состояние */
    IDLE;

    // -------------------------------------------------------------------------
    // Свойства эмоции
    // -------------------------------------------------------------------------

    /**
     * Является ли эмоция анимированной.
     * Используется для запуска `AnimationDrawable` или `Animatable`.
     */
    val isAnimated: Boolean
        get() = this == CLAP

    /**
     * Является ли эмоция позитивной (HAPPY или CLAP).
     */
    val isPositive: Boolean
        get() = this == HAPPY || this == CLAP

    /**
     * Является ли эмоция негативной (SAD).
     */
    val isNegative: Boolean
        get() = this == SAD

    /**
     * Является ли эмоция нейтральной (IDLE).
     */
    val isNeutral: Boolean
        get() = this == IDLE

    /**
     * Человекочитаемое имя эмоции (для озвучки / субтитров).
     */
    val displayName: String
        get() = when (this) {
            HAPPY -> "Радость"
            SAD   -> "Грусть"
            CLAP  -> "Аплодисменты"
            IDLE  -> "Спокойствие"
        }

    /**
     * Emoji-индикатор эмоции (для отладки / заглушек).
     */
    val emoji: String
        get() = when (this) {
            HAPPY -> "\uD83D\uDE0A"  // 😊
            SAD   -> "\uD83D\uDE22"  // 😢
            CLAP  -> "\uD83D\uDC4F"  // 👏
            IDLE  -> "\uD83D\uDE36"  // 😶
        }
}

// -------------------------------------------------------------------------
// CharacterState
// -------------------------------------------------------------------------

/**
 * Иммутабельное состояние персонажа.
 *
 * Инкапсулирует:
 * - имя персонажа (Василиса / Кузя)
 * - текущую эмоцию
 * - флаг видимости
 *
 * Все методы-модификаторы возвращают **новый** экземпляр (паттерн «immutable copy»).
 * Это гарантирует, что состояние можно безопасно использовать в `StateFlow` / `MutableState`.
 *
 * @property name     Уникальное имя персонажа (не может быть пустым).
 * @property emotion  Текущая эмоция (по умолчанию [CharacterEmotion.IDLE]).
 * @property isVisible Виден ли персонаж на экране (по умолчанию `true`).
 */
data class CharacterState(

    /**
     * Имя персонажа.
     *
     * Допустимые значения: `"Василиса"`, `"Кузя"` (регистр не важен).
     * Не должно быть пустым или состоять только из пробелов.
     */
    val name: String,

    /**
     * Текущая эмоция персонажа.
     */
    val emotion: CharacterEmotion = CharacterEmotion.IDLE,

    /**
     * Флаг видимости персонажа.
     *
     * Если `false`, персонаж не отрисовывается на сцене.
     */
    val isVisible: Boolean = true

) {

    // -------------------------------------------------------------------------
    // Валидация
    // -------------------------------------------------------------------------

    init {
        require(name.isNotBlank()) {
            "Имя персонажа не может быть пустым или состоять только из пробелов."
        }
    }

    // -------------------------------------------------------------------------
    // Вычисляемые свойства
    // -------------------------------------------------------------------------

    /**
     * Имя, очищенное от лишних пробелов по краям.
     */
    val normalizedName: String
        get() = name.trim()

    /**
     * Удобные проверки текущей эмоции.
     */
    val isHappy: Boolean    get() = emotion == CharacterEmotion.HAPPY
    val isSad: Boolean      get() = emotion == CharacterEmotion.SAD
    val isClapping: Boolean get() = emotion == CharacterEmotion.CLAP
    val isIdle: Boolean     get() = emotion == CharacterEmotion.IDLE

    /**
     * Возвращает имя файла спрайта для текущего состояния.
     *
     * Формат: `{нормализованное_имя}_{эмоция}`.
     * Пример: `"vasilisa_happy"`, `"kuzya_clap"`.
     */
    val spriteName: String
        get() = "${normalizedName.lowercase()}_${emotion.name.lowercase()}"

    // -------------------------------------------------------------------------
    // Методы сравнения
    // -------------------------------------------------------------------------

    /**
     * Проверяет, принадлежит ли состояние указанному персонажу.
     *
     * Сравнение без учёта регистра и лишних пробелов.
     *
     * @param characterName Имя для сравнения.
     * @return `true`, если имена совпадают.
     */
    fun isCharacter(characterName: String): Boolean {
        return normalizedName.equals(
            characterName.trim(),
            ignoreCase = true
        )
    }

    // -------------------------------------------------------------------------
    // Immutable-методы создания новых состояний
    // -------------------------------------------------------------------------

    /**
     * Создаёт копию с новой эмоцией.
     *
     * @param emotion Новая эмоция.
     * @return Новый экземпляр [CharacterState].
     */
    fun withEmotion(emotion: CharacterEmotion): CharacterState {
        return copy(emotion = emotion)
    }

    /**
     * Создаёт копию с изменённой видимостью.
     *
     * @param visible `true` — показать, `false` — скрыть.
     * @return Новый экземпляр [CharacterState].
     */
    fun withVisibility(visible: Boolean): CharacterState {
        return copy(isVisible = visible)
    }

    /**
     * Скрыть персонажа (удобный alias для `withVisibility(false)`).
     */
    fun hide(): CharacterState = copy(isVisible = false)

    /**
     * Показать персонажа (удобный alias для `withVisibility(true)`).
     */
    fun show(): CharacterState = copy(isVisible = true)

    /**
     * Переключить видимость персонажа.
     */
    fun toggleVisibility(): CharacterState = copy(isVisible = !isVisible)

    /**
     * Перевести персонажа в нейтральное состояние.
     */
    fun idle(): CharacterState = copy(emotion = CharacterEmotion.IDLE)

    /**
     * Сделать персонажа счастливым.
     */
    fun happy(): CharacterState = copy(emotion = CharacterEmotion.HAPPY)

    /**
     * Сделать персонажа грустным.
     */
    fun sad(): CharacterState = copy(emotion = CharacterEmotion.SAD)

    /**
     * Запустить аплодисменты.
     */
    fun clap(): CharacterState = copy(emotion = CharacterEmotion.CLAP)

    // -------------------------------------------------------------------------
    // Статические фабрики (companion object)
    // -------------------------------------------------------------------------

    companion object {

        /** Предопределённое состояние Василисы по умолчанию */
        val VASILISA_IDLE = CharacterState(
            name = "Василиса",
            emotion = CharacterEmotion.IDLE,
            isVisible = true
        )

        /** Предопределённое состояние Кузи по умолчанию */
        val KUZYA_IDLE = CharacterState(
            name = "Кузя",
            emotion = CharacterEmotion.IDLE,
            isVisible = true
        )

        /**
         * Создаёт состояние Василисы с заданной эмоцией.
         */
        fun vasilisa(emotion: CharacterEmotion = CharacterEmotion.IDLE): CharacterState {
            return CharacterState(name = "Василиса", emotion = emotion)
        }

        /**
         * Создаёт состояние Кузи с заданной эмоцией.
         */
        fun kuzya(emotion: CharacterEmotion = CharacterEmotion.IDLE): CharacterState {
            return CharacterState(name = "Кузя", emotion = emotion)
        }
    }

    // -------------------------------------------------------------------------
    // Переопределённые методы
    // -------------------------------------------------------------------------

    /**
     * Удобное строковое представление для логов.
     */
    override fun toString(): String {
        return "CharacterState(name='$normalizedName', emotion=${emotion.displayName}, visible=$isVisible)"
    }
}
