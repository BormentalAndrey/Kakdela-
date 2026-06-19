// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/alphabet/LetterFinderGame.kt

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Размер ячейки с буквой */
private val CellSize = 60.dp

/** Радиус скругления ячейки */
private val CellCornerRadius = 12.dp

/** Горизонтальный интервал между ячейками */
private val CellHorizontalSpacing = 12.dp

/** Вертикальный интервал между рядами */
private val CellVerticalSpacing = 6.dp

/** Отступ контейнера игры */
private val GamePadding = 16.dp

/** Отступ перед сеткой */
private val GridTopSpacer = 16.dp

/** Количество целевых букв на поле */
private const val TARGET_LETTER_COUNT = 5

/** Количество отвлекающих букв на поле */
private const val DISTRACTOR_LETTER_COUNT = 5

/** Общее количество ячеек */
private const val TOTAL_CELLS = TARGET_LETTER_COUNT + DISTRACTOR_LETTER_COUNT

/** Количество ячеек в ряду */
private const val CELLS_PER_ROW = 5

/** Количество рядов */
private const val ROW_COUNT = TOTAL_CELLS / CELLS_PER_ROW

/** Задержка между появлением ячеек (stagger, мс) */
private const val CELL_STAGGER_DELAY_MS = 50L

/** Длительность анимации появления ячейки (мс) */
private const val CELL_APPEAR_DURATION_MS = 300

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Тень ячейки */
private val CellElevation = 4.dp

/** Толщина обводки найденной ячейки */
private val FoundBorderWidth = 2.dp

/** Размер шрифта в ячейке */
private val CellFontSize = 28.sp

/** Размер шрифта счётчика */
private val CounterFontSize = 18.sp

// -------------------------------------------------------------------------
// Игра «Найди буквы»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Найди буквы».
 *
 * На экране 10 ячеек: 5 с целевой буквой, 5 с отвлекающими.
 * Ребёнок должен найти и нажать все 5 целевых букв.
 *
 * Особенности:
 * - Каскадная анимация появления ячеек
 * - Визуальная обратная связь (✓ зелёный / ✕ серый)
 * - Счётчик найденных букв
 * - Звуковые эффекты correct / wrong
 *
 * @param targetLetter Целевая буква для поиска (например, «А»).
 * @param onComplete   Колбэк: `true` если найдены все 5 букв.
 */
@Composable
fun LetterFinderGame(
    targetLetter: String,
    onComplete: (Boolean) -> Unit
) {
    // Алфавит для генерации отвлекающих букв
    val allLetters = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"

    // Генерируем поле: 5 целевых + 5 случайных, перемешиваем
    val symbols = remember(targetLetter) {
        val others = allLetters
            .filter { it.toString() != targetLetter }
            .shuffled()
            .take(DISTRACTOR_LETTER_COUNT)
            .map { it.toString() }

        val result = mutableListOf<String>()
        repeat(TARGET_LETTER_COUNT) { result.add(targetLetter) }
        result.addAll(others)

        result.shuffled()
    }

    // Состояние ячеек: индекс → правильная ли буква была нажата?
    val clickedStates = remember { mutableStateMapOf<Int, Boolean>() }

    // Счётчик найденных букв
    var foundCount by remember { mutableIntStateOf(0) }

    // Флаг завершения (блокирует повторные вызовы onComplete)
    var isCompleted by remember { mutableStateOf(false) }

    // Прогресс: сколько осталось найти
    val remainingCount = TARGET_LETTER_COUNT - foundCount

    // Разбиваем на строки по 5
    val rows = remember(symbols) {
        symbols.chunked(CELLS_PER_ROW)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Text(
            text = "Найди все буквы «$targetLetter»!",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Счётчик с прогрессом
        FoundCounter(
            found = foundCount,
            total = TARGET_LETTER_COUNT,
            remaining = remainingCount
        )

        Spacer(modifier = Modifier.height(GridTopSpacer))

        // Сетка ячеек
        rows.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(CellHorizontalSpacing),
                modifier = Modifier.padding(vertical = CellVerticalSpacing)
            ) {
                row.forEachIndexed { colIndex, letter ->
                    val cellIndex = rowIndex * CELLS_PER_ROW + colIndex

                    // Stagger-задержка для анимации появления
                    val appearDelay = (rowIndex * CELLS_PER_ROW + colIndex) * CELL_STAGGER_DELAY_MS

                    AnimatedLetterCell(
                        letter = letter,
                        targetLetter = targetLetter,
                        isClicked = clickedStates.containsKey(cellIndex),
                        isCorrect = clickedStates[cellIndex] ?: false,
                        appearDelay = appearDelay,
                        onTap = { correct ->
                            if (!isCompleted && !clickedStates.containsKey(cellIndex)) {
                                clickedStates[cellIndex] = correct

                                if (correct) {
                                    foundCount++
                                    AudioPlayer.playSFX("correct")

                                    if (foundCount >= TARGET_LETTER_COUNT) {
                                        isCompleted = true
                                        onComplete(true)
                                    }
                                } else {
                                    AudioPlayer.playSFX("wrong")
                                }
                            }
                        }
                    )
                }
            }
        }

        // Подсказка, если ещё не завершено
        if (!isCompleted) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Нажимай на буквы, чтобы найти все «$targetLetter»",
                style = MaterialTheme.typography.bodySmall,
                color = DarkText.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Счётчик найденных букв
// -------------------------------------------------------------------------

/**
 * Отображает прогресс поиска букв.
 *
 * @param found     Найдено букв.
 * @param total     Всего нужно найти.
 * @param remaining Осталось найти.
 */
@Composable
private fun FoundCounter(
    found: Int,
    total: Int,
    remaining: Int
) {
    val counterColor by animateColorAsState(
        targetValue = when {
            found == total -> FairyGreen
            found > 0 -> FairyGold
            else -> DarkText
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "CounterColor"
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Найдено: ",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = CounterFontSize
            ),
            color = DarkText
        )

        Text(
            text = "$found из $total",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = CounterFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = counterColor
        )

        if (found < total) {
            Text(
                text = "  (осталось $remaining)",
                style = MaterialTheme.typography.bodySmall,
                color = DarkText.copy(alpha = 0.6f)
            )
        } else {
            Text(
                text = "  ✓",
                style = MaterialTheme.typography.bodyLarge,
                color = FairyGreen
            )
        }
    }
}

// -------------------------------------------------------------------------
// Анимированная ячейка с буквой
// -------------------------------------------------------------------------

/**
 * Ячейка с буквой, появляющаяся с задержкой и анимацией.
 *
 * @param letter        Буква в ячейке.
 * @param targetLetter  Целевая буква.
 * @param isClicked     Была ли ячейка уже нажата.
 * @param isCorrect     Правильная ли буква была нажата (если isClicked = true).
 * @param appearDelay   Задержка перед появлением (мс).
 * @param onTap         Колбэк при нажатии: `true` если буква правильная.
 */
@Composable
private fun AnimatedLetterCell(
    letter: String,
    targetLetter: String,
    isClicked: Boolean,
    isCorrect: Boolean,
    appearDelay: Long,
    onTap: (Boolean) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(appearDelay)
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
            animationSpec = tween(CELL_APPEAR_DURATION_MS)
        )
    ) {
        LetterCell(
            letter = letter,
            targetLetter = targetLetter,
            alreadyClicked = isClicked,
            isCorrect = isCorrect,
            onTap = onTap
        )
    }
}

// -------------------------------------------------------------------------
// Ячейка с буквой
// -------------------------------------------------------------------------

/**
 * Отдельная ячейка с буквой.
 *
 * Состояния:
 * - **Не нажата**: голубой фон, буква
 * - **Нажата правильно**: зелёный фон + обводка, ✓
 * - **Нажата неправильно**: серый фон, ✕
 *
 * @param letter         Буква в ячейке.
 * @param targetLetter   Целевая буква для сравнения.
 * @param alreadyClicked Была ли нажата.
 * @param isCorrect      Правильный ли был ответ.
 * @param onTap          Колбэк: `true` если буква совпадает с целевой.
 */
@Composable
fun LetterCell(
    letter: String,
    targetLetter: String,
    alreadyClicked: Boolean,
    isCorrect: Boolean,
    onTap: (Boolean) -> Unit
) {
    val isTarget = letter == targetLetter

    // Анимация цвета фона
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !alreadyClicked -> FairyBlue
            isCorrect -> FairyGreen
            else -> Color.Gray.copy(alpha = 0.6f)
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "CellBackground"
    )

    // Анимация цвета обводки (только для правильных)
    val borderColor by animateColorAsState(
        targetValue = if (alreadyClicked && isCorrect) {
            FairyPurple
        } else {
            Color.Transparent
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "CellBorder"
    )

    // Текст внутри ячейки
    val cellText = when {
        !alreadyClicked -> letter
        isCorrect -> "✓"
        else -> "✕"
    }

    // Цвет текста
    val textColor = when {
        !alreadyClicked -> DarkText
        isCorrect -> Color.White
        else -> Color.White.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .size(CellSize)
            .shadow(
                elevation = if (!alreadyClicked) CellElevation else 0.dp,
                shape = RoundedCornerShape(CellCornerRadius)
            )
            .clip(RoundedCornerShape(CellCornerRadius))
            .background(backgroundColor)
            .then(
                if (alreadyClicked && isCorrect) {
                    Modifier.border(
                        width = FoundBorderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(CellCornerRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                enabled = !alreadyClicked
            ) {
                onTap(isTarget)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cellText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = CellFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
