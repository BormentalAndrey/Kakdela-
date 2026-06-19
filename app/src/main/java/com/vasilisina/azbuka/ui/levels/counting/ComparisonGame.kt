// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/ComparisonGame.kt

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
import kotlin.random.Random

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Минимальное количество предметов в группе */
private const val MIN_ITEM_COUNT = 2

/** Максимальное количество предметов в группе */
private const val MAX_ITEM_COUNT = 8

/** Размер кружочка-предмета */
private val ItemCircleSize = 24.dp

/** Вертикальный отступ между кружочками */
private val ItemVerticalPadding = 2.dp

/** Размер кнопки «Тут больше» */
private val ComparisonButtonWidth = 120.dp

/** Высота кнопки «Тут больше» */
private val ComparisonButtonHeight = 50.dp

/** Отступ между кружочками и кнопкой */
private val ItemsToButtonSpacer = 16.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед группами */
private val GroupsTopSpacer = 24.dp

/** Отступ между левой и правой группой */
private val GroupsHorizontalSpacer = 32.dp

/** Радиус скругления кнопок */
private val ButtonCornerRadius = 12.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 1200L

/** Задержка появления предметов (stagger, мс) */
private const val ITEM_STAGGER_DELAY_MS = 50L

/** Длительность анимации появления предмета (мс) */
private const val ITEM_APPEAR_DURATION_MS = 250

/** Тень кнопки */
private val ButtonElevation = 6.dp

/** Тень кнопки при нажатии */
private val ButtonPressedElevation = 10.dp

/** Тень кружочка */
private val CircleElevation = 4.dp

/** Толщина обводки выбранной кнопки */
private val SelectedBorderWidth = 3.dp

/** Размер шрифта текста на кнопке */
private val ButtonFontSize = 16.sp

/** Размер шрифта заголовка группы */
private val GroupLabelFontSize = 14.sp

// -------------------------------------------------------------------------
// Игра «Чего больше?»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Чего больше?»
 *
 * На экране две группы предметов: левая (розовые кружочки) и правая (золотые).
 * Ребёнок должен определить, в какой группе предметов больше,
 * и нажать соответствующую кнопку «Тут больше».
 *
 * Особенности:
 * - Случайная генерация количества предметов (2–8)
 * - Гарантированно разное количество в группах
 * - Каскадная анимация появления предметов
 * - Визуальная обратная связь: зелёный / красный
 * - Подсветка правильной стороны после ответа
 * - Задержка 1.2 сек для осознания результата
 * - Звуковые эффекты correct / wrong
 *
 * @param onResult Колбэк: `true` если выбрана правильная сторона.
 */
@Composable
fun ComparisonGame(onResult: (correct: Boolean) -> Unit) {
    // Генерируем разное количество предметов
    val countLeft = remember { Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1) }
    val countRight = remember(countLeft) {
        var c: Int
        do {
            c = Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1)
        } while (c == countLeft)
        c
    }

    // Определяем, где больше
    val biggerSide = if (countLeft > countRight) "left" else "right"

    // Выбранная сторона (null = ещё не выбрана)
    var selectedSide by remember { mutableStateOf<String?>(null) }

    // Флаг блокировки после выбора
    var isLocked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Где больше предметов?",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(GroupsTopSpacer))

        // Две группы предметов
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Левая группа
            ComparisonSide(
                count = countLeft,
                side = "left",
                label = "Слева",
                circleColor = FairyPink,
                selectedSide = selectedSide,
                biggerSide = biggerSide,
                isLocked = isLocked,
                onSelect = { side ->
                    if (!isLocked) {
                        selectedSide = side
                        isLocked = true
                        val isCorrect = side == biggerSide

                        AudioPlayer.playSFX(if (isCorrect) "correct" else "wrong")

                        coroutineScope.launch {
                            delay(RESULT_DELAY_MS)
                            onResult(isCorrect)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.padding(horizontal = GroupsHorizontalSpacer))

            // Правая группа
            ComparisonSide(
                count = countRight,
                side = "right",
                label = "Справа",
                circleColor = FairyGold,
                selectedSide = selectedSide,
                biggerSide = biggerSide,
                isLocked = isLocked,
                onSelect = { side ->
                    if (!isLocked) {
                        selectedSide = side
                        isLocked = true
                        val isCorrect = side == biggerSide

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
}

// -------------------------------------------------------------------------
// Одна сторона сравнения
// -------------------------------------------------------------------------

/**
 * Отображает группу предметов и кнопку «Тут больше».
 *
 * @param count         Количество предметов.
 * @param side          Идентификатор стороны ("left" или "right").
 * @param label         Человекочитаемая подпись («Слева», «Справа»).
 * @param circleColor   Цвет кружочков.
 * @param selectedSide  Выбранная сторона (null = ничего не выбрано).
 * @param biggerSide    Сторона, где предметов больше.
 * @param isLocked      Заблокированы ли кнопки.
 * @param onSelect      Колбэк при выборе стороны.
 */
@Composable
private fun ComparisonSide(
    count: Int,
    side: String,
    label: String,
    circleColor: Color,
    selectedSide: String?,
    biggerSide: String,
    isLocked: Boolean,
    onSelect: (String) -> Unit
) {
    val isSelected = selectedSide == side
    val isCorrectSide = side == biggerSide

    // Анимированный цвет кнопки
    val buttonColor by animateColorAsState(
        targetValue = when {
            // До выбора — голубой
            !isLocked -> FairyBlue
            // Выбрана правильно — зелёный
            isSelected && isCorrectSide -> FairyGreen
            // Выбрана неправильно — красный
            isSelected && !isCorrectSide -> Color.Red.copy(alpha = 0.7f)
            // После блокировки подсвечиваем правильную сторону
            isLocked && isCorrectSide -> FairyGreen.copy(alpha = 0.5f)
            // Остальные — серый
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "ComparisonButtonColor$side"
    )

    // Анимированный цвет обводки
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> FairyPurple
            isLocked && isCorrectSide && !isSelected -> FairyGreen.copy(alpha = 0.6f)
            else -> Color.Transparent
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "ComparisonBorderColor$side"
    )

    // Цвет текста
    val textColor = when {
        isSelected -> Color.White
        isLocked && isCorrectSide -> DarkText
        isLocked && !isSelected -> Color.White.copy(alpha = 0.5f)
        else -> DarkText
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Подпись группы
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = GroupLabelFontSize,
                fontWeight = FontWeight.Medium
            ),
            color = DarkText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Кружочки с каскадной анимацией
        AnimatedItemCircles(count = count, circleColor = circleColor)

        Spacer(modifier = Modifier.height(ItemsToButtonSpacer))

        // Кнопка «Тут больше»
        Button(
            onClick = { onSelect(side) },
            enabled = !isLocked,
            modifier = Modifier
                .size(ComparisonButtonWidth, ComparisonButtonHeight)
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
                text = "Тут больше",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ButtonFontSize,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Анимированные кружочки
// -------------------------------------------------------------------------

/**
 * Отображает столбик кружочков с каскадной анимацией появления.
 *
 * @param count       Количество кружочков.
 * @param circleColor Цвет кружочков.
 */
@Composable
private fun AnimatedItemCircles(
    count: Int,
    circleColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(count) { index ->
            var isVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(ITEM_STAGGER_DELAY_MS * index)
                isVisible = true
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
                        .padding(vertical = ItemVerticalPadding)
                        .size(ItemCircleSize)
                        .shadow(CircleElevation, CircleShape)
                        .background(circleColor, CircleShape)
                        .border(1.dp, circleColor.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}
