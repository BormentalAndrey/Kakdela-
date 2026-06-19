// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/WordBuilderGame.kt

package com.vasilisina.azbuka.ui.levels.keyboard

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
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Слова для составления */
private val WordPool = listOf("МАМА", "РУСЬ", "МИР")

/** Размер ячейки слота */
private val SlotSize = 60.dp

/** Размер кнопки с буквой */
private val LetterButtonSize = 60.dp

/** Радиус скругления слотов и кнопок */
private val CornerRadius = 12.dp

/** Толщина обводки слота */
private val SlotBorderWidth = 2.dp

/** Горизонтальный интервал между слотами */
private val SlotSpacing = 8.dp

/** Горизонтальный интервал между буквами */
private val LetterButtonSpacing = 8.dp

/** Отступ между слотами и буквами */
private val SlotsToLettersSpacer = 32.dp

/** Отступ между заголовком и слотами */
private val TitleToSlotsSpacer = 24.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед кнопкой «Сбросить» */
private val ResetButtonSpacer = 16.dp

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 800L

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка появления элементов (мс) */
private const val ELEMENT_STAGGER_DELAY_MS = 80L

/** Длительность анимации появления (мс) */
private const val ELEMENT_APPEAR_DURATION_MS = 300

/** Тень слота */
private val SlotElevation = 4.dp

/** Тень кнопки с буквой */
private val LetterButtonElevation = 4.dp

/** Размер шрифта в слоте */
private val SlotFontSize = 24.sp

/** Размер шрифта на кнопке */
private val LetterButtonFontSize = 24.sp

// -------------------------------------------------------------------------
// Модель данных
// -------------------------------------------------------------------------

/**
 * Буква с уникальным идентификатором.
 *
 * Необходима, чтобы различать одинаковые буквы в слове
 * (например, две «М» и две «А» в слове «МАМА»).
 *
 * @property id Уникальный идентификатор (индекс в исходном слове).
 * @property char Символ буквы.
 */
private data class IndexedLetter(
    val id: Int,
    val char: String
) {
    override fun toString(): String = char
}

// -------------------------------------------------------------------------
// Игра «Собери слово»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Собери слово».
 *
 * Ребёнок составляет слово из перемешанных букв.
 * Буквы нажимаются в нижнем ряду и автоматически вставляются
 * в первый свободный слот в верхнем ряду.
 *
 * Особенности:
 * - 3 возможных слова: МАМА, РУСЬ, МИР (выбирается случайно)
 * - Буквы генерируются вперемешку
 * - Каскадная анимация появления слотов и букв
 * - Авто-вставка в первый свободный слот
 * - Нажатие на заполненный слот возвращает букву обратно
 * - Визуальная обратная связь при успехе / ошибке
 * - Автосброс через 800 мс при неправильном ответе
 * - Кнопка «Сбросить» для ручной очистки
 *
 * @param onResult Колбэк: `true` если слово составлено правильно.
 */
@Composable
fun WordBuilderGame(onResult: (correct: Boolean) -> Unit) {
    // Целевое слово
    val targetWord = remember { WordPool.random() }

    // Буквы целевого слова с уникальными индексами, перемешаны
    val letterData: List<IndexedLetter> = remember(targetWord) {
        targetWord.mapIndexed { index, char ->
            IndexedLetter(id = index, char = char.toString())
        }.shuffled()
    }

    // Индексы использованных букв
    val usedIndices = remember { mutableStateListOf<Int>() }

    // Слоты: каждый хранит IndexedLetter или null
    val slots = remember(targetWord) {
        mutableStateListOf<IndexedLetter?>(
            *Array(targetWord.length) { null }
        )
    }

    // Флаг завершения (блокирует повторные вызовы onResult)
    var isCompleted by remember { mutableStateOf(false) }

    // Текущее собранное слово
    val filledWord = slots.map { it?.char ?: "" }.joinToString("")

    // Проверка завершения
    LaunchedEffect(filledWord) {
        if (isCompleted) return@LaunchedEffect
        if (filledWord.length != targetWord.length) return@LaunchedEffect

        if (filledWord == targetWord) {
            // Успех!
            isCompleted = true
            AudioPlayer.playSFX("correct")
            delay(RESULT_DELAY_MS)
            onResult(true)
        } else {
            // Ошибка — сбрасываем
            AudioPlayer.playSFX("wrong")
            delay(RESULT_DELAY_MS)
            usedIndices.clear()
            slots.fill(null)
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
            text = "Собери слово",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Подсказка с целевым словом
        Text(
            text = "Цель: $targetWord",
            style = MaterialTheme.typography.bodyLarge,
            color = FairyPurple,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TitleToSlotsSpacer))

        // Слоты для сборки
        WordSlots(
            slots = slots,
            onSlotClick = { slotIndex ->
                if (isCompleted) return@WordSlots

                val existingLetter = slots[slotIndex]
                if (existingLetter != null) {
                    // Возвращаем букву в доступные
                    usedIndices.remove(existingLetter.id)
                    slots[slotIndex] = null
                }
            }
        )

        Spacer(modifier = Modifier.height(SlotsToLettersSpacer))

        // Буквы на выбор
        LetterButtonsRow(
            letterData = letterData,
            usedIndices = usedIndices,
            onLetterClick = { indexedLetter ->
                if (isCompleted) return@LetterButtonsRow

                // Находим первый свободный слот
                val firstEmpty = slots.indexOf(null)
                if (firstEmpty != -1) {
                    slots[firstEmpty] = indexedLetter
                    usedIndices.add(indexedLetter.id)
                }
            }
        )

        Spacer(modifier = Modifier.height(ResetButtonSpacer))

        // Кнопка сброса
        TextButton(
            onClick = {
                if (isCompleted) return@TextButton
                AudioPlayer.playSFX("click")
                usedIndices.clear()
                slots.fill(null)
            },
            enabled = !isCompleted && usedIndices.isNotEmpty()
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
// Слоты для сборки слова
// -------------------------------------------------------------------------

/**
 * Ряд слотов для составления слова.
 *
 * @param slots       Состояние слотов.
 * @param onSlotClick Колбэк при нажатии на слот.
 */
@Composable
private fun WordSlots(
    slots: List<IndexedLetter?>,
    onSlotClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SlotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, letter ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(ELEMENT_STAGGER_DELAY_MS * index)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = tween(ELEMENT_APPEAR_DURATION_MS)
                )
            ) {
                WordSlot(
                    letter = letter?.char,
                    isEmpty = letter == null,
                    onClick = { onSlotClick(index) }
                )
            }
        }
    }
}

/**
 * Один слот для буквы.
 *
 * @param letter  Буква в слоте или null.
 * @param isEmpty Пуст ли слот.
 * @param onClick Колбэк при нажатии.
 */
@Composable
private fun WordSlot(
    letter: String?,
    isEmpty: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isEmpty) WhiteBackground else FairyPurple.copy(alpha = 0.1f),
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "SlotBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (letter != null) FairyGreen else FairyPurple,
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "SlotBorder"
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
                indication = null,
                enabled = letter != null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter ?: "",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = SlotFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = if (letter != null) FairyPurple else Color.Transparent,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Кнопки с буквами
// -------------------------------------------------------------------------

/**
 * Ряд кнопок с доступными буквами.
 *
 * @param letterData  Список букв с идентификаторами.
 * @param usedIndices Индексы использованных букв.
 * @param onLetterClick Колбэк при нажатии на букву.
 */
@Composable
private fun LetterButtonsRow(
    letterData: List<IndexedLetter>,
    usedIndices: List<Int>,
    onLetterClick: (IndexedLetter) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LetterButtonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        letterData.forEachIndexed { index, indexedLetter ->
            val isUsed = usedIndices.contains(indexedLetter.id)

            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(ELEMENT_STAGGER_DELAY_MS * index)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = tween(ELEMENT_APPEAR_DURATION_MS)
                )
            ) {
                WordLetterButton(
                    letter = indexedLetter.char,
                    isUsed = isUsed,
                    onClick = {
                        if (!isUsed) {
                            onLetterClick(indexedLetter)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Одна кнопка с буквой.
 *
 * @param letter Буква.
 * @param isUsed Использована ли уже.
 * @param onClick Колбэк при нажатии.
 */
@Composable
private fun WordLetterButton(
    letter: String,
    isUsed: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isUsed) Color.LightGray.copy(alpha = 0.5f) else FairyGreen,
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "LetterBg"
    )

    Box(
        modifier = Modifier
            .size(LetterButtonSize)
            .shadow(
                elevation = if (!isUsed) LetterButtonElevation else 0.dp,
                shape = RoundedCornerShape(CornerRadius)
            )
            .clip(RoundedCornerShape(CornerRadius))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isUsed,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = LetterButtonFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = if (isUsed) Color.White.copy(alpha = 0.4f) else Color.White,
            textAlign = TextAlign.Center
        )
    }
}
