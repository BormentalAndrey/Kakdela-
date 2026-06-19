// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/PatternGameScreen.kt

package com.vasilisina.azbuka.ui.levels.logic

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Доступные цвета для паттернов */
private val PatternColors = listOf(FairyPink, FairyBlue, FairyGreen, FairyGold)

/** Количество элементов в показываемом ряду */
private const val PATTERN_LENGTH = 3

/** Количество вариантов для выбора */
private const val OPTIONS_COUNT = 3

/** Размер кружочка в ряду */
private val PatternCircleSize = 50.dp

/** Размер кружочка-варианта */
private val OptionCircleSize = 60.dp

/** Размер ячейки со знаком вопроса */
private val QuestionSlotSize = 50.dp

/** Радиус скругления знака вопроса */
private val QuestionSlotCornerRadius = 8.dp

/** Горизонтальный интервал в ряду */
private val RowSpacing = 12.dp

/** Горизонтальный интервал между вариантами */
private val OptionsSpacing = 16.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед рядом */
private val PatternTopSpacer = 24.dp

/** Отступ перед вариантами */
private val OptionsTopSpacer = 32.dp

/** Отступ перед подсказкой */
private val HintTopSpacer = 16.dp

/** Толщина обводки кружочка в ряду */
private val PatternBorderWidth = 2.dp

/** Толщина обводки знака вопроса */
private val QuestionBorderWidth = 2.dp

/** Толщина обводки выбранного варианта */
private val SelectedBorderWidth = 3.dp

/** Толщина обводки правильного варианта */
private val CorrectBorderWidth = 3.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 1200L

/** Задержка появления элементов (stagger, мс) */
private const val ELEMENT_STAGGER_DELAY_MS = 120L

/** Длительность анимации появления элемента (мс) */
private const val ELEMENT_APPEAR_DURATION_MS = 350

/** Тень элемента в ряду */
private val PatternCircleElevation = 4.dp

/** Тень варианта */
private val OptionCircleElevation = 6.dp

/** Тень варианта при выборе */
private val OptionSelectedElevation = 10.dp

/** Размер шрифта знака вопроса */
private val QuestionFontSize = 28.sp

/** Размер шрифта подсказки */
private val HintFontSize = 16.sp

/** Цвет обводки кружочков в ряду */
private val PatternBorderColor = Color.Gray.copy(alpha = 0.5f)

// -------------------------------------------------------------------------
// Игра «Продолжи ряд»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Продолжи ряд».
 *
 * На экране показан ряд из 3 цветных кружочков, образующих паттерн.
 * Последовательность: A, B, C, ?. Правильное продолжение — снова A.
 * Ребёнок должен выбрать правильный цвет из трёх вариантов.
 *
 * Особенности:
 * - Случайная генерация паттерна из 4 возможных цветов
 * - Каскадная анимация появления элементов
 * - Визуальная обратная связь: зелёный (правильно) / красный (ошибка)
 * - Подсветка правильного варианта после ответа
 * - Задержка 1.2 сек для осознания результата
 * - Звуковые эффекты correct / wrong
 *
 * @param onResult Колбэк: `true` если выбран правильный вариант.
 */
@Composable
fun PatternGameScreen(onResult: (correct: Boolean) -> Unit) {
    // Генерируем паттерн: 3 случайных цвета
    val pattern = remember { PatternColors.shuffled().take(PATTERN_LENGTH) }

    // Правильное продолжение — первый элемент паттерна (A)
    val correctColor = remember(pattern) { pattern[0] }

    // Варианты ответа: правильный + 2 неправильных
    val options = remember(correctColor) {
        val wrongColors = PatternColors
            .filter { it != correctColor }
            .shuffled()
            .take(OPTIONS_COUNT - 1)
        (wrongColors + correctColor).shuffled()
    }

    // Выбранный индекс (-1 = не выбрано)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Флаг блокировки после выбора
    var isLocked by remember { mutableStateOf(false) }

    // Флаг для анимации появления
    var showElements by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Запускаем анимацию
    LaunchedEffect(Unit) {
        showElements = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Продолжи ряд:",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PatternTopSpacer))

        // Ряд с паттерном
        PatternRow(
            pattern = pattern,
            showElements = showElements
        )

        Spacer(modifier = Modifier.height(OptionsTopSpacer))

        // Подсказка
        Text(
            text = "Выбери следующий цвет:",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(HintTopSpacer))

        // Варианты ответа
        OptionsRow(
            options = options,
            correctColor = correctColor,
            selectedIndex = selectedIndex,
            isLocked = isLocked,
            showElements = showElements,
            onOptionClick = { index, color ->
                if (!isLocked) {
                    selectedIndex = index
                    isLocked = true
                    val isCorrect = color == correctColor

                    AudioPlayer.playSFX(if (isCorrect) "correct" else "wrong")

                    coroutineScope.launch {
                        delay(RESULT_DELAY_MS)
                        onResult(isCorrect)
                    }
                }
            }
        )

        // Текст с результатом
        if (isLocked) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (selectedIndex == options.indexOf(correctColor)) {
                    "Правильно! Узор повторяется."
                } else {
                    "Правильный ответ выделен зелёным."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedIndex == options.indexOf(correctColor)) {
                    FairyGreen
                } else {
                    FairyPink
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Ряд с паттерном
// -------------------------------------------------------------------------

/**
 * Отображает ряд из цветных кружочков + знак вопроса.
 *
 * @param pattern      Список из 3 цветов.
 * @param showElements Флаг запуска анимации.
 */
@Composable
private fun PatternRow(
    pattern: List<Color>,
    showElements: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RowSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Элементы паттерна
        pattern.forEachIndexed { index, color ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(showElements) {
                if (showElements) {
                    delay(ELEMENT_STAGGER_DELAY_MS * index)
                    isVisible = true
                }
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
                Box(
                    modifier = Modifier
                        .size(PatternCircleSize)
                        .shadow(PatternCircleElevation, CircleShape)
                        .background(color, CircleShape)
                        .border(PatternBorderWidth, PatternBorderColor, CircleShape)
                )
            }
        }

        // Знак вопроса
        var isQuestionVisible by remember { mutableStateOf(false) }

        LaunchedEffect(showElements) {
            if (showElements) {
                delay(ELEMENT_STAGGER_DELAY_MS * PATTERN_LENGTH)
                isQuestionVisible = true
            }
        }

        AnimatedVisibility(
            visible = isQuestionVisible,
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
            Box(
                modifier = Modifier
                    .size(QuestionSlotSize)
                    .border(
                        width = QuestionBorderWidth,
                        color = FairyPurple,
                        shape = RoundedCornerShape(QuestionSlotCornerRadius)
                    )
                    .clip(RoundedCornerShape(QuestionSlotCornerRadius))
                    .background(FairyPurple.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = QuestionFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FairyPurple,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Варианты ответа
// -------------------------------------------------------------------------

/**
 * Ряд из трёх вариантов для выбора следующего цвета.
 *
 * @param options        Список цветов-вариантов.
 * @param correctColor   Правильный цвет.
 * @param selectedIndex  Индекс выбранного варианта (-1 = не выбрано).
 * @param isLocked       Заблокирован ли выбор.
 * @param showElements   Флаг запуска анимации.
 * @param onOptionClick  Колбэк при выборе варианта.
 */
@Composable
private fun OptionsRow(
    options: List<Color>,
    correctColor: Color,
    selectedIndex: Int,
    isLocked: Boolean,
    showElements: Boolean,
    onOptionClick: (Int, Color) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OptionsSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, color ->
            val isSelected = selectedIndex == index
            val isCorrectOption = color == correctColor

            // Stagger-задержка
            val appearDelay = ELEMENT_STAGGER_DELAY_MS * (PATTERN_LENGTH + 1 + index)

            AnimatedOptionCircle(
                color = color,
                isSelected = isSelected,
                isCorrectOption = isCorrectOption,
                isLocked = isLocked,
                appearDelay = appearDelay,
                showElements = showElements,
                onClick = { onOptionClick(index, color) }
            )
        }
    }
}

// -------------------------------------------------------------------------
// Анимированный вариант
// -------------------------------------------------------------------------

/**
 * Кружочек-вариант с анимацией появления.
 */
@Composable
private fun AnimatedOptionCircle(
    color: Color,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isLocked: Boolean,
    appearDelay: Long,
    showElements: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showElements) {
        if (showElements) {
            delay(appearDelay)
            isVisible = true
        }
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
        OptionCircle(
            originalColor = color,
            isSelected = isSelected,
            isCorrectOption = isCorrectOption,
            isLocked = isLocked,
            onClick = onClick
        )
    }
}

// -------------------------------------------------------------------------
// Кружочек-вариант
// -------------------------------------------------------------------------

/**
 * Один кружочек-вариант ответа.
 *
 * @param originalColor  Исходный цвет варианта.
 * @param isSelected     Выбран ли этот вариант.
 * @param isCorrectOption Правильный ли это вариант.
 * @param isLocked       Заблокирован ли выбор.
 * @param onClick        Колбэк при нажатии.
 */
@Composable
private fun OptionCircle(
    originalColor: Color,
    isSelected: Boolean,
    isCorrectOption: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    // Анимированный цвет кружочка
    val displayColor by animateColorAsState(
        targetValue = when {
            // Выбран правильно — зелёный
            isSelected && isCorrectOption -> FairyGreen
            // Выбран неправильно — красный
            isSelected && !isCorrectOption -> Color.Red.copy(alpha = 0.7f)
            // После блокировки правильный — полупрозрачный зелёный
            isLocked && isCorrectOption && !isSelected -> FairyGreen.copy(alpha = 0.5f)
            // Обычное состояние — исходный цвет
            else -> originalColor
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "OptionDisplayColor"
    )

    // Анимированный цвет обводки
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isLocked && isCorrectOption && !isSelected -> FairyGreen
            else -> Color.Transparent
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "OptionBorderColor"
    )

    // Тень
    val elevation = if (isSelected) OptionSelectedElevation else OptionCircleElevation

    Box(
        modifier = Modifier
            .size(OptionCircleSize)
            .shadow(elevation, CircleShape)
            .background(displayColor, CircleShape)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(
                        width = if (isSelected) SelectedBorderWidth else CorrectBorderWidth,
                        color = borderColor,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                enabled = !isLocked
            ) { onClick() }
    )
}
