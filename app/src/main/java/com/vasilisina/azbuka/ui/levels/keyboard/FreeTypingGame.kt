// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/FreeTypingGame.kt

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Буквы на клавиатуре */
private val KeyboardLetters = listOf("А", "О", "У", "М", "П", "Р", "С", "Т", "К", "В", "Л", "Н")

/** Максимальная длина вводимой строки */
private const val MAX_TYPED_LENGTH = 10

/** Количество колонок в сетке */
private const val GRID_COLUMNS = 4

/** Размер клавиши */
private val KeySize = 70.dp

/** Радиус скругления клавиш */
private val KeyCornerRadius = 16.dp

/** Радиус скругления поля ввода */
private val InputFieldCornerRadius = 16.dp

/** Высота поля ввода */
private val InputFieldHeight = 70.dp

/** Ширина поля ввода (доля экрана) */
private const val INPUT_FIELD_WIDTH_FRACTION = 0.9f

/** Горизонтальный интервал между клавишами */
private val GridHorizontalSpacing = 12.dp

/** Вертикальный интервал между клавишами */
private val GridVerticalSpacing = 12.dp

/** Отступ контейнера */
private val ScreenPadding = 16.dp

/** Отступ перед полем ввода */
private val InputFieldTopSpacer = 24.dp

/** Отступ перед клавиатурой */
private val KeyboardTopSpacer = 24.dp

/** Отступ перед кнопками управления */
private val ControlsTopSpacer = 16.dp

/** Внутренний отступ поля ввода */
private val InputFieldInnerPadding = 16.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 200

/** Задержка появления клавиш (stagger, мс) */
private const val KEY_STAGGER_DELAY_MS = 30L

/** Длительность анимации появления клавиши (мс) */
private const val KEY_APPEAR_DURATION_MS = 250

/** Тень клавиши */
private val KeyElevation = 4.dp

/** Тень клавиши при нажатии */
private val KeyPressedElevation = 2.dp

/** Тень поля ввода */
private val InputFieldElevation = 2.dp

/** Размер шрифта на клавише */
private val KeyFontSize = 28.sp

/** Размер шрифта в поле ввода */
private val InputFieldFontSize = 32.sp

/** Размер шрифта кнопок управления */
private val ControlButtonFontSize = 18.sp

/** Цвет плейсхолдера */
private val PlaceholderColor = Color.LightGray.copy(alpha = 0.5f)

// -------------------------------------------------------------------------
// Игра «Свободный ввод»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Свободный ввод».
 *
 * Ребёнок может свободно набирать буквы на экранной клавиатуре.
 * Развивает навык печати и знакомство с расположением букв.
 *
 * Особенности:
 * - Клавиатура из 12 букв (сетка 3×4)
 * - Поле ввода до 10 символов
 * - Каскадная анимация появления клавиш
 * - Кнопка «Стереть» для удаления последней буквы
 * - Кнопка «Готово» для завершения этапа
 * - Визуальная обратная связь при нажатии
 * - Звуковой эффект click при нажатии
 *
 * @param onDone Колбэк при нажатии кнопки «Готово».
 */
@Composable
fun FreeTypingGame(onDone: () -> Unit) {
    val letters = remember { KeyboardLetters }

    // Набранный текст
    var typedText by remember { mutableStateOf("") }

    // Флаг анимации появления
    var showKeyboard by remember { mutableStateOf(false) }

    // Анимация цвета поля ввода
    val inputFieldBorderColor by animateColorAsState(
        targetValue = when {
            typedText.isEmpty() -> FairyBlue.copy(alpha = 0.5f)
            typedText.length >= MAX_TYPED_LENGTH -> FairyPink
            else -> FairyPurple
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "InputFieldBorder"
    )

    // Анимация цвета текста
    val inputTextColor by animateColorAsState(
        targetValue = if (typedText.isEmpty()) PlaceholderColor else FairyPurple,
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "InputTextColor"
    )

    // Запускаем анимацию появления
    LaunchedEffect(Unit) {
        showKeyboard = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(ScreenPadding)
    ) {
        // Заголовок
        Text(
            text = "Напечатай что хочешь!",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(InputFieldTopSpacer))

        // Поле ввода
        Box(
            modifier = Modifier
                .fillMaxWidth(INPUT_FIELD_WIDTH_FRACTION)
                .height(InputFieldHeight)
                .shadow(InputFieldElevation, RoundedCornerShape(InputFieldCornerRadius))
                .background(Color.White, RoundedCornerShape(InputFieldCornerRadius))
                .border(
                    width = 2.dp,
                    color = inputFieldBorderColor,
                    shape = RoundedCornerShape(InputFieldCornerRadius)
                )
                .padding(InputFieldInnerPadding),
            contentAlignment = Alignment.Center
        ) {
            // Текст или плейсхолдер
            if (typedText.isEmpty()) {
                Text(
                    text = "Нажми на букву...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = InputFieldFontSize,
                        fontWeight = FontWeight.Light
                    ),
                    color = PlaceholderColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = typedText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = InputFieldFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = inputTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Счётчик символов
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${typedText.length} / $MAX_TYPED_LENGTH",
            style = MaterialTheme.typography.bodySmall,
            color = if (typedText.length >= MAX_TYPED_LENGTH) {
                FairyPink
            } else {
                DarkText.copy(alpha = 0.5f)
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(KeyboardTopSpacer))

        // Клавиатура
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(GridHorizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(GridVerticalSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(letters) { index, letter ->
                val appearDelay = index * KEY_STAGGER_DELAY_MS

                AnimatedKeyBox(
                    letter = letter,
                    appearDelay = appearDelay,
                    showKeyboard = showKeyboard,
                    onClick = {
                        if (typedText.length < MAX_TYPED_LENGTH) {
                            typedText += letter
                            AudioPlayer.playSFX("click")
                        }
                    }
                )
            }
        }

        // Кнопки управления
        ControlButtons(
            typedText = typedText,
            onErase = {
                if (typedText.isNotEmpty()) {
                    typedText = typedText.dropLast(1)
                    AudioPlayer.playSFX("click")
                }
            },
            onDone = {
                AudioPlayer.playSFX("click")
                onDone()
            }
        )
    }
}

// -------------------------------------------------------------------------
// Анимированная клавиша
// -------------------------------------------------------------------------

/**
 * Клавиша с анимацией появления.
 *
 * @param letter       Буква на клавише.
 * @param appearDelay  Задержка перед появлением (мс).
 * @param showKeyboard Флаг запуска анимации.
 * @param onClick      Колбэк при нажатии.
 */
@Composable
private fun AnimatedKeyBox(
    letter: String,
    appearDelay: Long,
    showKeyboard: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            kotlinx.coroutines.delay(appearDelay)
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
        KeyBox(letter = letter, onClick = onClick)
    }
}

// -------------------------------------------------------------------------
// Клавиша
// -------------------------------------------------------------------------

/**
 * Одна клавиша клавиатуры.
 *
 * @param letter  Буква.
 * @param onClick Колбэк при нажатии.
 */
@Composable
fun KeyBox(letter: String, onClick: () -> Unit) {
    // Состояние нажатия для анимации тени
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(KeySize)
            .shadow(
                elevation = if (isPressed) KeyPressedElevation else KeyElevation,
                shape = RoundedCornerShape(KeyCornerRadius)
            )
            .clip(RoundedCornerShape(KeyCornerRadius))
            .background(FairyBlue)
            .border(
                width = 1.dp,
                color = FairyBlue.copy(alpha = 0.5f),
                shape = RoundedCornerShape(KeyCornerRadius)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Без ripple (сказочный стиль)
            ) {
                isPressed = true
                onClick()
                // Сбрасываем нажатие (анимация тени)
                // В реальном проекте лучше использовать InteractionSource
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = KeyFontSize,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }

    // Сбрасываем состояние нажатия
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

// -------------------------------------------------------------------------
// Кнопки управления
// -------------------------------------------------------------------------

/**
 * Кнопки «Стереть» и «Готово».
 *
 * @param typedText Текущий набранный текст.
 * @param onErase   Колбэк при нажатии «Стереть».
 * @param onDone    Колбэк при нажатии «Готово».
 */
@Composable
private fun ControlButtons(
    typedText: String,
    onErase: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ControlsTopSpacer),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка «Стереть»
        TextButton(
            onClick = onErase,
            enabled = typedText.isNotEmpty()
        ) {
            Text(
                text = "⌫ Стереть",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = ControlButtonFontSize,
                    fontWeight = FontWeight.Medium
                ),
                color = if (typedText.isNotEmpty()) {
                    FairyPink
                } else {
                    Color.Gray.copy(alpha = 0.4f)
                }
            )
        }

        // Кнопка «Готово»
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(KeyCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = FairyGreen,
                contentColor = DarkText
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.height(50.dp)
        ) {
            Text(
                text = "Готово ✓",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = ControlButtonFontSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
