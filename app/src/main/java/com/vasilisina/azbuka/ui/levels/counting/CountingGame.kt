// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/CountingGame.kt

package com.vasilisina.azbuka.ui.levels.counting

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Минимальное количество предметов */
private const val MIN_ITEM_COUNT = 1

/** Максимальное количество предметов */
private const val MAX_ITEM_COUNT = 10

/** Количество вариантов ответа */
private const val OPTIONS_COUNT = 3

/** Размер кружочка-предмета */
private val ItemCircleSize = 40.dp

/** Отступ между кружочками */
private val ItemCirclePadding = 4.dp

/** Размер кнопки с вариантом ответа */
private val OptionButtonSize = 80.dp

/** Горизонтальный интервал между кнопками */
private val OptionButtonSpacing = 16.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед предметами */
private val ItemsTopSpacer = 24.dp

/** Отступ перед кнопками */
private val OptionsTopSpacer = 32.dp

/** Минимальная высота контейнера предметов */
private val ItemsMinHeight = 100.dp

/** Радиус скругления кнопок */
private val ButtonCornerRadius = 16.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 1200L

/** Задержка появления предметов (stagger, мс) */
private const val ITEM_STAGGER_DELAY_MS = 60L

/** Длительность анимации появления предмета (мс) */
private const val ITEM_APPEAR_DURATION_MS = 300

/** Тень кнопки */
private val ButtonElevation = 6.dp

/** Тень кнопки при нажатии */
private val ButtonPressedElevation = 10.dp

/** Размер шрифта цифры на кнопке */
private val OptionButtonFontSize = 28.sp

/** Толщина обводки выбранной кнопки */
private val SelectedBorderWidth = 3.dp

// -------------------------------------------------------------------------
// Игра «Счёт предметов»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Сколько предметов?»
 *
 * На экране отображается случайное количество (1–10) золотых кружочков.
 * Ребёнок должен пересчитать их и выбрать правильную цифру из трёх вариантов.
 *
 * Особенности:
 * - Каскадная анимация появления предметов
 * - 3 варианта ответа: правильный + 2 случайных
 * - Визуальная обратная связь: зелёный (правильно) / красный (ошибка)
 * - Задержка 1.2 сек перед переходом к следующему этапу
 * - Звуковые эффекты correct / wrong
 * - Кнопки блокируются после выбора
 *
 * @param onResult Колбэк: `true` если выбран правильный ответ.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountingGame(onResult: (correct: Boolean) -> Unit) {
    // Генерируем случайное количество предметов
    val itemCount = remember { Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1) }

    // Генерируем варианты ответа: правильный + 2 неправильных
    val options = remember(itemCount) {
        val wrongAnswers = mutableSetOf<Int>()
        while (wrongAnswers.size < OPTIONS_COUNT - 1) {
            val candidate = Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1)
            if (candidate != itemCount) {
                wrongAnswers.add(candidate)
            }
        }
        (wrongAnswers + itemCount).shuffled()
    }

    // Выбранный ответ (null = ещё не выбран)
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    // Флаг блокировки после выбора
    var isLocked by remember { mutableStateOf(false) }

    // Флаг для анимации появления предметов
    var showItems by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Запускаем анимацию появления предметов
    LaunchedEffect(Unit) {
        showItems = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Сколько здесь предметов?",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        // Предметы (кружочки) с каскадной анимацией
        ItemCircles(
            itemCount = itemCount,
            showItems = showItems
        )

        Spacer(modifier = Modifier.height(OptionsTopSpacer))

        // Кнопки с вариантами ответа
        OptionButtons(
            options = options,
            selectedAnswer = selectedAnswer,
            correctAnswer = itemCount,
            isLocked = isLocked,
            onOptionClick = { num ->
                if (!isLocked) {
                    selectedAnswer = num
                    isLocked = true
                    val isCorrect = num == itemCount

                    AudioPlayer.playSFX(if (isCorrect) "correct" else "wrong")

                    coroutineScope.launch {
                        delay(RESULT_DELAY_MS)
                        onResult(isCorrect)
                    }
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Предметы (золотые кружочки)
// -------------------------------------------------------------------------

/**
 * Отображает золотые кружочки с каскадной анимацией появления.
 *
 * @param itemCount Количество предметов.
 * @param showItems Флаг запуска анимации.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCircles(
    itemCount: Int,
    showItems: Boolean
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ItemsMinHeight),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        repeat(itemCount) { index ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(showItems) {
                if (showItems) {
                    delay(ITEM_STAGGER_DELAY_MS * index)
                    isVisible = true
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = tween(ITEM_APPEAR_DURATION_MS)
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(ItemCirclePadding)
                        .size(ItemCircleSize)
                        .shadow(4.dp, CircleShape)
                        .background(FairyGold, CircleShape)
                        .border(1.dp, FairyGold.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Кнопки с вариантами ответа
// -------------------------------------------------------------------------

/**
 * Ряд кнопок с вариантами ответа.
 *
 * @param options        Список вариантов (3 цифры).
 * @param selectedAnswer Выбранный ответ (null = ничего не выбрано).
 * @param correctAnswer  Правильный ответ.
 * @param isLocked       Заблокированы ли кнопки.
 * @param onOptionClick  Колбэк при выборе варианта.
 */
@Composable
private fun OptionButtons(
    options: List<Int>,
    selectedAnswer: Int?,
    correctAnswer: Int,
    isLocked: Boolean,
    onOptionClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OptionButtonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { num ->
            val isSelected = selectedAnswer == num
            val isCorrectAnswer = num == correctAnswer

            // Определяем цвет кнопки
            val buttonColor by animateColorAsState(
                targetValue = when {
                    // До выбора — голубой
                    !isLocked -> FairyBlue
                    // Выбрана правильно — зелёный
                    isSelected && isCorrectAnswer -> FairyGreen
                    // Выбрана неправильно — красный
                    isSelected && !isCorrectAnswer -> Color.Red.copy(alpha = 0.7f)
                    // После блокировки подсвечиваем правильный ответ зелёным
                    isLocked && isCorrectAnswer -> FairyGreen.copy(alpha = 0.5f)
                    // Остальные невыбранные — серый
                    else -> Color.Gray.copy(alpha = 0.3f)
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "OptionButtonColor"
            )

            // Цвет обводки для выделения
            val borderColor by animateColorAsState(
                targetValue = when {
                    isSelected -> FairyPurple
                    isLocked && isCorrectAnswer && !isSelected -> FairyGreen.copy(alpha = 0.6f)
                    else -> Color.Transparent
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "OptionBorderColor"
            )

            // Цвет текста
            val textColor = when {
                isSelected -> Color.White
                isLocked && isCorrectAnswer -> DarkText
                isLocked && !isSelected -> Color.White.copy(alpha = 0.5f)
                else -> DarkText
            }

            Button(
                onClick = { onOptionClick(num) },
                enabled = !isLocked,
                modifier = Modifier
                    .size(OptionButtonSize)
                    .shadow(
                        elevation = if (isSelected) ButtonPressedElevation else ButtonElevation,
                        shape = RoundedCornerShape(ButtonCornerRadius)
                    )
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(
                                width = SelectedBorderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(ButtonCornerRadius)
                            )
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = textColor,
                    disabledContainerColor = buttonColor,
                    disabledContentColor = textColor
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = ButtonElevation,
                    pressedElevation = ButtonPressedElevation,
                    focusedElevation = ButtonElevation,
                    hoveredElevation = ButtonElevation,
                    disabledElevation = if (isSelected) ButtonPressedElevation else 0.dp
                )
            ) {
                Text(
                    text = "$num",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = OptionButtonFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
