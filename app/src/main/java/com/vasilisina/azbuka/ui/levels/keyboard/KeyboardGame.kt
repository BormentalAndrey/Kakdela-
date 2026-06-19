// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/KeyboardGame.kt

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/** Буквы на клавиатуре */
private val KeyboardLetters = listOf("А", "О", "У", "М", "П", "Р", "С", "Т", "К", "В", "Л", "Н")

/** Количество колонок в сетке */
private const val GRID_COLUMNS = 4

/** Размер кнопки клавиши */
private val KeySize = 70.dp

/** Радиус скругления клавиш */
private val KeyCornerRadius = 16.dp

/** Толщина обводки клавиши */
private val KeyBorderWidth = 2.dp

/** Горизонтальный интервал между клавишами */
private val GridHorizontalSpacing = 12.dp

/** Вертикальный интервал между клавишами */
private val GridVerticalSpacing = 12.dp

/** Высота сетки клавиатуры */
private val GridHeight = 350.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед клавиатурой */
private val KeyboardTopSpacer = 24.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 250

/** Задержка перед вызовом onDone (мс) */
private const val SUCCESS_DELAY_MS = 800L

/** Длительность показа ошибки (мс) */
private const val WRONG_FLASH_DURATION_MS = 500L

/** Задержка появления клавиш (stagger, мс) */
private const val KEY_STAGGER_DELAY_MS = 40L

/** Длительность анимации появления клавиши (мс) */
private const val KEY_APPEAR_DURATION_MS = 300

/** Тень клавиши */
private val KeyElevation = 4.dp

/** Тень клавиши при нажатии (правильный ответ) */
private val KeyPressedElevation = 8.dp

/** Размер шрифта буквы на клавише */
private val KeyFontSize = 28.sp

/** Размер шрифта подсказки */
private val HintFontSize = 16.sp

// -------------------------------------------------------------------------
// Игра «Клавиатура»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Найди букву на клавиатуре».
 *
 * На экране отображается клавиатура из 12 букв (сетка 3×4).
 * Ребёнок должен найти и нажать указанную букву.
 *
 * Особенности:
 * - Каскадная анимация появления клавиш
 * - Визуальная обратная связь: золотой (правильно) / красный (ошибка)
 * - Подсветка правильной клавиши после нахождения
 * - Мигание красным при ошибке на 500 мс
 * - Звуковые эффекты correct / wrong
 * - Задержка 800 мс перед завершением
 *
 * @param onDone Колбэк при успешном нахождении буквы.
 */
@Composable
fun KeyboardGame(onDone: () -> Unit) {
    // Перемешиваем буквы для разнообразия (опционально)
    val letters = remember { KeyboardLetters }

    // Целевая буква
    val targetLetter = remember { letters.random() }

    // Флаг: буква найдена
    var isFound by remember { mutableStateOf(false) }

    // Индекс клавиши с ошибкой (для подсветки красным)
    var wrongKeyIndex by remember { mutableStateOf<Int?>(null) }

    // Флаг для анимации появления
    var showKeyboard by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Запускаем анимацию появления клавиатуры
    LaunchedEffect(Unit) {
        showKeyboard = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок с подсказкой
        Text(
            text = "Нажми на букву",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Целевая буква (крупно)
        Text(
            text = "«$targetLetter»",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            ),
            color = FairyPurple,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(KeyboardTopSpacer))

        // Сетка клавиатуры 3×4
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(GridHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(GridVerticalSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .height(GridHeight)
        ) {
            itemsIndexed(letters) { index, letter ->
                val isTarget = letter == targetLetter
                val isWrong = wrongKeyIndex == index

                // Stagger-задержка для анимации появления
                val appearDelay = index * KEY_STAGGER_DELAY_MS

                AnimatedKeyButton(
                    letter = letter,
                    isTarget = isTarget,
                    isFound = isFound,
                    isWrong = isWrong,
                    appearDelay = appearDelay,
                    showKeyboard = showKeyboard,
                    onClick = {
                        if (!isFound) {
                            if (isTarget) {
                                // Правильно!
                                isFound = true
                                AudioPlayer.playSFX("correct")

                                coroutineScope.launch {
                                    delay(SUCCESS_DELAY_MS)
                                    onDone()
                                }
                            } else {
                                // Ошибка — подсвечиваем красным
                                wrongKeyIndex = index
                                AudioPlayer.playSFX("wrong")

                                coroutineScope.launch {
                                    delay(WRONG_FLASH_DURATION_MS)
                                    wrongKeyIndex = null
                                }
                            }
                        }
                    }
                )
            }
        }

        // Подсказка
        if (!isFound) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Найди и нажми нужную букву",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = HintFontSize
                ),
                color = DarkText.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Анимированная клавиша
// -------------------------------------------------------------------------

/**
 * Клавиша с анимацией появления.
 *
 * @param letter       Буква на клавише.
 * @param isTarget     Является ли эта буква целевой.
 * @param isFound      Найдена ли уже целевая буква.
 * @param isWrong      Была ли эта клавиша нажата ошибочно.
 * @param appearDelay  Задержка перед появлением (мс).
 * @param showKeyboard Флаг запуска анимации.
 * @param onClick      Колбэк при нажатии.
 */
@Composable
private fun AnimatedKeyButton(
    letter: String,
    isTarget: Boolean,
    isFound: Boolean,
    isWrong: Boolean,
    appearDelay: Long,
    showKeyboard: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
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
            animationSpec = tween(KEY_APPEAR_DURATION_MS)
        )
    ) {
        KeyButton(
            letter = letter,
            isTarget = isTarget,
            isFound = isFound,
            isWrong = isWrong,
            onClick = onClick
        )
    }
}

// -------------------------------------------------------------------------
// Кнопка клавиши
// -------------------------------------------------------------------------

/**
 * Одна клавиша клавиатуры.
 *
 * Состояния:
 * - **Обычная**: голубой фон, фиолетовая обводка
 * - **Найдена (правильная)**: золотой фон, золотая обводка
 * - **Ошибка**: красный фон
 * - **Неактивна**: после нахождения все клавиши блокируются
 *
 * @param letter    Буква.
 * @param isTarget  Целевая ли буква.
 * @param isFound   Найдена ли уже целевая буква.
 * @param isWrong   Подсветка ошибки.
 * @param onClick   Колбэк при нажатии.
 */
@Composable
fun KeyButton(
    letter: String,
    isTarget: Boolean,
    isFound: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    // Анимированный цвет фона
    val backgroundColor by animateColorAsState(
        targetValue = when {
            // Найдена правильная — золотой
            isFound && isTarget -> FairyGold
            // Ошибка — красный
            isWrong -> Color.Red.copy(alpha = 0.7f)
            // Обычное состояние — голубой
            else -> FairyBlue
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "KeyBackgroundColor"
    )

    // Анимированный цвет обводки
    val borderColor by animateColorAsState(
        targetValue = when {
            isFound && isTarget -> FairyGold
            isWrong -> Color.Red
            else -> FairyPurple
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "KeyBorderColor"
    )

    // Анимированный цвет текста
    val textColor by animateColorAsState(
        targetValue = when {
            isFound && isTarget -> DarkText
            else -> Color.White
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "KeyTextColor"
    )

    // Тень меняется при успехе
    val elevation = if (isFound && isTarget) KeyPressedElevation else KeyElevation

    Box(
        modifier = Modifier
            .size(KeySize)
            .shadow(elevation, RoundedCornerShape(KeyCornerRadius))
            .clip(RoundedCornerShape(KeyCornerRadius))
            .background(backgroundColor)
            .border(
                width = KeyBorderWidth,
                color = borderColor,
                shape = RoundedCornerShape(KeyCornerRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                enabled = !isFound
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = KeyFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
