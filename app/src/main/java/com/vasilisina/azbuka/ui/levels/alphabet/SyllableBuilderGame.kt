// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/alphabet/SyllableBuilderGame.kt

package com.vasilisina.azbuka.ui.levels.alphabet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Размер слота для буквы */
private val SlotSize = 80.dp

/** Размер кнопки с буквой */
private val LetterButtonSize = 70.dp

/** Радиус скругления слотов и кнопок */
private val CornerRadius = 16.dp

/** Толщина обводки слота */
private val SlotBorderWidth = 3.dp

/** Горизонтальный интервал между слотами */
private val SlotSpacing = 16.dp

/** Горизонтальный интервал между кнопками букв */
private val LetterButtonSpacing = 12.dp

/** Отступ между слотами и буквами */
private val SlotToLettersSpacer = 40.dp

/** Отступ между заголовком и слотами */
private val TitleToSlotsSpacer = 24.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ между элементами */
private val ElementSpacing = 8.dp

/** Задержка перед сбросом при ошибке (мс) */
private const val WRONG_ANSWER_RESET_DELAY_MS = 600L

/** Задержка появления финальной анимации (мс) */
private const val SUCCESS_DISPLAY_DELAY_MS = 400L

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Тень слота */
private val SlotElevation = 6.dp

/** Тень кнопки с буквой */
private val LetterButtonElevation = 4.dp

/** Размер шрифта в слоте */
private val SlotFontSize = 36.sp

/** Размер шрифта на кнопке */
private val LetterButtonFontSize = 32.sp

// -------------------------------------------------------------------------
// Игра «Составь слог»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Составь слог».
 *
 * Ребёнок составляет слог из букв, нажимая на букву в нижнем ряду,
 * а затем на слот в верхнем ряду (или наоборот: сначала слот, потом букву).
 *
 * Особенности:
 * - Буквы генерируются из целевого слога вперемешку
 * - Слоты подсвечиваются при наведении (упрощённо — при выборе буквы)
 * - Визуальная обратная связь: зелёный при успехе, красный при ошибке
 * - Автосброс через 600 мс при неправильном ответе
 * - Кнопка «Сбросить» для ручной очистки
 * - Анимация успеха при правильном составлении
 *
 * @param targetSyllable Целевой слог (например, «МА», «ПА», «РУ»).
 * @param onComplete     Колбэк: `true` если слог составлен правильно.
 */
@Composable
fun SyllableBuilderGame(
    targetSyllable: String,
    onComplete: (correct: Boolean) -> Unit
) {
    // Буквы целевого слога вперемешку (с индексами для уникальности)
    val letters = remember(targetSyllable) {
        targetSyllable.toList().mapIndexed { index, char ->
            IndexedLetter(index, char)
        }.shuffled()
    }

    // Множество индексов использованных букв
    val usedLetterIndices = remember { mutableStateListOf<Int>() }

    // Слоты: каждый хранит индекс буквы из letters или null
    val slots = remember {
        mutableStateListOf<Int?>(
            *Array(targetSyllable.length) { null }
        )
    }

    // Выбранная буква (индекс в letters) — для режима «сначала буква, потом слот»
    var selectedLetterIndex by remember { mutableStateOf<Int?>(null) }

    // Флаг завершения (блокирует повторные вызовы onComplete)
    var isCompleted by remember { mutableStateOf(false) }

    // Флаг успеха для анимации
    var showSuccess by remember { mutableStateOf(false) }

    // Текущее собранное слово
    val currentSyllable = slots.mapNotNull { index ->
        index?.let { letters[it].char.toString() }
    }.joinToString("")

    // Проверка завершения
    LaunchedEffect(currentSyllable) {
        if (isCompleted) return@LaunchedEffect
        if (currentSyllable.length != targetSyllable.length) return@LaunchedEffect

        if (currentSyllable == targetSyllable) {
            // Успех!
            isCompleted = true
            AudioPlayer.playSFX("correct")
            showSuccess = true
            delay(SUCCESS_DISPLAY_DELAY_MS)
            onComplete(true)
        } else {
            // Ошибка — сбрасываем после задержки
            AudioPlayer.playSFX("wrong")
            delay(WRONG_ANSWER_RESET_DELAY_MS)
            usedLetterIndices.clear()
            slots.fill(null)
            selectedLetterIndex = null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Составь слог «$targetSyllable»",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TitleToSlotsSpacer))

        // Слоты для сборки
        SyllableSlots(
            slots = slots,
            letters = letters,
            selectedLetterIndex = selectedLetterIndex,
            showSuccess = showSuccess,
            onSlotClick = { slotIndex ->
                if (isCompleted) return@SyllableSlots

                val existingLetter = slots[slotIndex]

                if (existingLetter != null) {
                    // Убираем букву из слота обратно в доступные
                    usedLetterIndices.remove(existingLetter)
                    slots[slotIndex] = null
                } else if (selectedLetterIndex != null) {
                    // Вставляем выбранную букву в пустой слот
                    slots[slotIndex] = selectedLetterIndex
                    usedLetterIndices.add(selectedLetterIndex!!)
                    selectedLetterIndex = null
                }
            }
        )

        Spacer(modifier = Modifier.height(SlotToLettersSpacer))

        // Доступные буквы
        LetterButtonsRow(
            letters = letters,
            usedLetterIndices = usedLetterIndices,
            selectedLetterIndex = selectedLetterIndex,
            onLetterClick = { letterIndex ->
                if (isCompleted) return@LetterButtonsRow

                if (selectedLetterIndex == letterIndex) {
                    // Отмена выбора
                    selectedLetterIndex = null
                } else if (!usedLetterIndices.contains(letterIndex)) {
                    // Выбор буквы
                    selectedLetterIndex = letterIndex

                    // Автоматически вставляем в первый свободный слот
                    val firstEmptySlot = slots.indexOf(null)
                    if (firstEmptySlot != -1) {
                        slots[firstEmptySlot] = letterIndex
                        usedLetterIndices.add(letterIndex)
                        selectedLetterIndex = null
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(ElementSpacing))

        // Кнопка сброса
        TextButton(
            onClick = {
                if (isCompleted) return@TextButton
                AudioPlayer.playSFX("click")
                usedLetterIndices.clear()
                slots.fill(null)
                selectedLetterIndex = null
            },
            enabled = !isCompleted && (usedLetterIndices.isNotEmpty() || selectedLetterIndex != null)
        ) {
            Text(
                text = "Сбросить",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) Color.Gray else FairyPink
            )
        }
    }
}

// -------------------------------------------------------------------------
// Слоты для сборки слога
// -------------------------------------------------------------------------

/**
 * Ряд слотов, куда ребёнок вставляет буквы.
 *
 * @param slots               Состояние слотов (индексы букв или null).
 * @param letters             Список доступных букв.
 * @param selectedLetterIndex Индекс выбранной буквы (для подсветки активного слота).
 * @param showSuccess         Флаг успешного завершения.
 * @param onSlotClick         Колбэк при нажатии на слот.
 */
@Composable
private fun SyllableSlots(
    slots: List<Int?>,
    letters: List<IndexedLetter>,
    selectedLetterIndex: Int?,
    showSuccess: Boolean,
    onSlotClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SlotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, letterIndex ->
            val isTargeted = selectedLetterIndex != null && slots[index] == null

            // Анимация цвета обводки при успехе
            val borderColor by animateColorAsState(
                targetValue = when {
                    showSuccess -> FairyGreen
                    isTargeted -> FairyGold
                    else -> FairyPurple
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "SlotBorderColor"
            )

            SlotView(
                letter = letterIndex?.let { letters[it].char.toString() },
                borderColor = borderColor,
                isHighlighted = isTargeted,
                onClick = { onSlotClick(index) }
            )
        }
    }
}

/**
 * Один слот для буквы.
 *
 * @param letter       Буква в слоте или null (пустой).
 * @param borderColor  Цвет обводки.
 * @param isHighlighted Подсвечен ли слот (выбрана буква).
 * @param onClick      Колбэк при нажатии.
 */
@Composable
private fun SlotView(
    letter: String?,
    borderColor: Color,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            letter != null -> WhiteBackground
            isHighlighted -> FairyGold.copy(alpha = 0.2f)
            else -> Color.White
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "SlotBackground"
    )

    Box(
        modifier = Modifier
            .size(SlotSize)
            .shadow(SlotElevation, RoundedCornerShape(CornerRadius))
            .clip(RoundedCornerShape(CornerRadius))
            .background(bgColor)
            .border(
                width = SlotBorderWidth,
                color = borderColor,
                shape = RoundedCornerShape(CornerRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = SlotFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = FairyPurple,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Кнопки с буквами
// -------------------------------------------------------------------------

/**
 * Ряд кнопок с доступными буквами.
 *
 * @param letters             Список букв.
 * @param usedLetterIndices   Индексы уже использованных букв.
 * @param selectedLetterIndex Индекс выбранной буквы.
 * @param onLetterClick       Колбэк при нажатии на букву.
 */
@Composable
private fun LetterButtonsRow(
    letters: List<IndexedLetter>,
    usedLetterIndices: List<Int>,
    selectedLetterIndex: Int?,
    onLetterClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LetterButtonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        letters.forEachIndexed { index, indexedLetter ->
            val isUsed = usedLetterIndices.contains(index)
            val isSelected = selectedLetterIndex == index

            val bgColor by animateColorAsState(
                targetValue = when {
                    isUsed -> Color.LightGray
                    isSelected -> FairyGold
                    else -> FairyGreen
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "LetterButtonBg"
            )

            LetterButton(
                char = indexedLetter.char,
                enabled = !isUsed,
                backgroundColor = bgColor,
                onClick = { onLetterClick(index) }
            )
        }
    }
}

/**
 * Одна кнопка с буквой.
 *
 * @param char            Буква.
 * @param enabled         Доступна ли кнопка.
 * @param backgroundColor Цвет фона.
 * @param onClick         Колбэк при нажатии.
 */
@Composable
private fun LetterButton(
    char: Char,
    enabled: Boolean,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(LetterButtonSize)
            .shadow(
                elevation = if (enabled) LetterButtonElevation else 0.dp,
                shape = RoundedCornerShape(CornerRadius)
            )
            .clip(RoundedCornerShape(CornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = LetterButtonFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Вспомогательная модель
// -------------------------------------------------------------------------

/**
 * Буква с индексом для уникальной идентификации.
 *
 * Необходима, чтобы различать одинаковые буквы в слоге
 * (например, две «А» в слоге «МАМА»).
 *
 * @property index Уникальный индекс в исходном слове.
 * @property char  Символ буквы.
 */
private data class IndexedLetter(
    val index: Int,
    val char: Char
) {
    override fun toString(): String = char.toString()
}
