// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/MathExampleGame.kt

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
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch
import kotlin.random.Random

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Минимальное значение первого операнда для сложения */
private const val ADD_MIN_A = 1

/** Максимальное значение первого операнда для сложения */
private const val ADD_MAX_A = 5

/** Минимальное значение второго операнда для сложения */
private const val ADD_MIN_B = 1

/** Максимальный результат сложения */
private const val ADD_MAX_RESULT = 10

/** Минимальное значение первого операнда для вычитания */
private const val SUB_MIN_A = 2

/** Максимальное значение первого операнда для вычитания */
private const val SUB_MAX_A = 10

/** Минимальное значение второго операнда для вычитания */
private const val SUB_MIN_B = 1

/** Количество вариантов ответа */
private const val OPTIONS_COUNT = 3

/** Минимальное значение варианта ответа */
private const val OPTION_MIN = 0

/** Максимальное значение варианта ответа */
private const val OPTION_MAX = 10

/** Размер кнопки с вариантом */
private val OptionButtonSize = 85.dp

/** Горизонтальный интервал между кнопками */
private val OptionButtonSpacing = 16.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед кнопками */
private val OptionsTopSpacer = 32.dp

/** Отступ между примером и кнопками */
private val ExampleToOptionsSpacer = 32.dp

/** Отступ между знаком и операндами */
private val OperatorSpacing = 8.dp

/** Радиус скругления кнопок */
private val ButtonCornerRadius = 20.dp

/** Размер шрифта примера */
private val ExampleFontSize = 56.sp

/** Размер шрифта знака операции */
private val OperatorFontSize = 48.sp

/** Размер шрифта цифры на кнопке */
private val OptionButtonFontSize = 28.sp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 1200L

/** Задержка появления примера (мс) */
private const val EXAMPLE_APPEAR_DELAY_MS = 300L

/** Длительность анимации появления примера (мс) */
private const val EXAMPLE_APPEAR_DURATION_MS = 500

/** Тень кнопки */
private val ButtonElevation = 6.dp

/** Тень кнопки при нажатии */
private val ButtonPressedElevation = 10.dp

/** Толщина обводки выбранной кнопки */
private val SelectedBorderWidth = 3.dp

/** Размер контейнера с примером */
private val ExampleContainerSize = 120.dp

// -------------------------------------------------------------------------
// Игра «Реши пример»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Реши пример».
 *
 * Ребёнку показывается простой арифметический пример на сложение или вычитание
 * (результат до 10). Нужно выбрать правильный ответ из трёх вариантов.
 *
 * Особенности:
 * - Случайная генерация примеров (сложение / вычитание)
 * - Анимированное появление примера
 * - 3 варианта ответа: правильный + 2 случайных
 * - Визуальная обратная связь: зелёный / красный
 * - Подсветка правильного ответа после выбора
 * - Задержка 1.2 сек для осознания результата
 * - Звуковые эффекты correct / wrong
 *
 * @param onResult Колбэк: `true` если выбран правильный ответ.
 */
@Composable
fun MathExampleGame(onResult: (correct: Boolean) -> Unit) {
    // Генерируем пример
    val example = remember {
        generateMathExample()
    }
    val (a, b, op) = example
    val correctAnswer = if (op == "+") a + b else a - b

    // Генерируем варианты ответа
    val options = remember(correctAnswer) {
        generateOptions(correctAnswer)
    }

    // Выбранный ответ (null = ещё не выбран)
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    // Флаг блокировки после выбора
    var isLocked by remember { mutableStateOf(false) }

    // Флаг анимации появления
    var showExample by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Запускаем анимацию появления
    LaunchedEffect(Unit) {
        delay(EXAMPLE_APPEAR_DELAY_MS)
        showExample = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Реши пример:",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ExampleToOptionsSpacer))

        // Анимированное отображение примера
        AnimatedVisibility(
            visible = showExample,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(
                animationSpec = tween(EXAMPLE_APPEAR_DURATION_MS)
            )
        ) {
            MathExampleDisplay(a = a, b = b, operator = op)
        }

        Spacer(modifier = Modifier.height(OptionsTopSpacer))

        // Кнопки с вариантами ответа
        OptionButtons(
            options = options,
            selectedAnswer = selectedAnswer,
            correctAnswer = correctAnswer,
            isLocked = isLocked,
            onOptionClick = { num ->
                if (!isLocked) {
                    selectedAnswer = num
                    isLocked = true
                    val isCorrect = num == correctAnswer

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
// Отображение примера
// -------------------------------------------------------------------------

/**
 * Отображает арифметический пример в декоративном контейнере.
 *
 * @param a        Первый операнд.
 * @param b        Второй операнд.
 * @param operator Знак операции («+» или «−»).
 */
@Composable
private fun MathExampleDisplay(
    a: Int,
    b: Int,
    operator: String
) {
    Box(
        modifier = Modifier
            .size(ExampleContainerSize)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        FairyPurple.copy(alpha = 0.15f),
                        FairyPink.copy(alpha = 0.1f),
                        WhiteBackground
                    )
                )
            )
            .border(2.dp, FairyPurple.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Первый операнд
            Text(
                text = "$a",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = ExampleFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = FairyPurple,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(horizontal = OperatorSpacing))

            // Знак операции
            Text(
                text = operator,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = OperatorFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = FairyGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(horizontal = OperatorSpacing))

            // Второй операнд
            Text(
                text = "$b",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = ExampleFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = FairyPurple,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.padding(horizontal = OperatorSpacing))

            // Знак равенства и вопрос
            Text(
                text = "= ?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = ExampleFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = FairyGold,
                textAlign = TextAlign.Center
            )
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

            // Анимированный цвет кнопки
            val buttonColor by animateColorAsState(
                targetValue = when {
                    // До выбора — голубой
                    !isLocked -> FairyBlue
                    // Выбрана правильно — зелёный
                    isSelected && isCorrectAnswer -> FairyGreen
                    // Выбрана неправильно — красный
                    isSelected && !isCorrectAnswer -> Color.Red.copy(alpha = 0.7f)
                    // После блокировки подсвечиваем правильный ответ
                    isLocked && isCorrectAnswer -> FairyGreen.copy(alpha = 0.5f)
                    // Остальные невыбранные — серый
                    else -> Color.Gray.copy(alpha = 0.3f)
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "MathOptionButtonColor"
            )

            // Анимированный цвет обводки
            val borderColor by animateColorAsState(
                targetValue = when {
                    isSelected -> FairyPurple
                    isLocked && isCorrectAnswer && !isSelected -> FairyGreen.copy(alpha = 0.6f)
                    else -> Color.Transparent
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "MathOptionBorderColor"
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

// -------------------------------------------------------------------------
// Генерация примера
// -------------------------------------------------------------------------

/**
 * Генерирует случайный арифметический пример.
 *
 * Правила:
 * - Сложение: a + b ≤ [ADD_MAX_RESULT]
 * - Вычитание: a ≥ b, результат ≥ 0
 *
 * @return Triple(a, b, operator) где operator = "+" или "−".
 */
private fun generateMathExample(): Triple<Int, Int, String> {
    val isAddition = Random.nextBoolean()

    return if (isAddition) {
        val a = Random.nextInt(ADD_MIN_A, ADD_MAX_A + 1)
        // b подбираем так, чтобы сумма не превышала ADD_MAX_RESULT
        val maxB = (ADD_MAX_RESULT - a).coerceAtLeast(ADD_MIN_B)
        val b = if (maxB >= ADD_MIN_B) {
            Random.nextInt(ADD_MIN_B, maxB + 1)
        } else {
            ADD_MIN_B
        }
        Triple(a, b, "+")
    } else {
        val a = Random.nextInt(SUB_MIN_A, SUB_MAX_A + 1)
        // b должно быть от 1 до a
        val b = Random.nextInt(SUB_MIN_B, a + 1)
        Triple(a, b, "−")
    }
}

/**
 * Генерирует варианты ответа: правильный + 2 случайных неправильных.
 *
 * @param correctAnswer Правильный ответ.
 * @return Список из [OPTIONS_COUNT] перемешанных вариантов.
 */
private fun generateOptions(correctAnswer: Int): List<Int> {
    val wrongAnswers = mutableSetOf<Int>()

    while (wrongAnswers.size < OPTIONS_COUNT - 1) {
        val candidate = Random.nextInt(OPTION_MIN, OPTION_MAX + 1)
        if (candidate != correctAnswer) {
            wrongAnswers.add(candidate)
        }
    }

    return (wrongAnswers + correctAnswer).shuffled()
}
