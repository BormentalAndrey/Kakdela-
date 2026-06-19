// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/menu/MainMenuScreen.kt

package com.vasilisina.azbuka.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.R
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

/** Цвета для анимированного градиентного фона */
private val BackgroundColors = listOf(
    FairyBlue,
    FairyPink,
    FairyGold,
    FairyPurple
)

/** Длительность анимации смены цвета фона (мс) */
private const val BACKGROUND_COLOR_DURATION_MS = 1500

/** Интервал смены цвета фона (мс) */
private const val BACKGROUND_COLOR_INTERVAL_MS = 3000L

/** Длительность анимации появления меню (мс) */
private const val MENU_ENTRANCE_DURATION_MS = 600

/** Длительность анимации пульсации (мс) */
private const val PULSE_DURATION_MS = 2500

/** Минимальная альфа для пульсации */
private const val PULSE_MIN_ALPHA = 0.95f

/** Максимальная альфа для пульсации */
private const val PULSE_MAX_ALPHA = 1f

/** Ширина кнопок относительно экрана */
private const val BUTTON_WIDTH_FRACTION = 0.8f

/** Высота кнопок */
private val ButtonHeight = 72.dp

/** Отступ по горизонтали для колонки */
private val HorizontalPadding = 32.dp

/** Вертикальный интервал между элементами */
private val VerticalSpacing = 24.dp

/** Отступ перед кнопками */
private val TopSpacerHeight = 40.dp

/** Высота кнопок (минимальная для accessibility) */
private val MinButtonHeight = 60.dp

/** Радиус скругления кнопок */
private val ButtonCornerRadius = 16.dp

/** Тень кнопки по умолчанию */
private val ButtonDefaultElevation = 6.dp

/** Тень кнопки при нажатии */
private val ButtonPressedElevation = 10.dp

/** Тень кнопки в фокусе */
private val ButtonFocusedElevation = 8.dp

// -------------------------------------------------------------------------
// Главный экран
// -------------------------------------------------------------------------

/**
 * Главное меню игры «Василисина азбука».
 *
 * Особенности:
 * - Анимированный градиентный фон (плавная смена 4 цветов)
 * - Пульсирующая анимация всего меню
 * - Анимированное появление кнопок при входе
 * - Три кнопки: «Играть», «Альбом успехов», «Выход»
 * - Автоматический запуск фоновой музыки
 * - Звук при нажатии на любую кнопку
 *
 * @param onPlay   Колбэк для кнопки «Играть» (переход на карту).
 * @param onAlbum  Колбэк для кнопки «Альбом успехов».
 * @param onQuit   Колбэк для кнопки «Выход» (завершение приложения).
 */
@Composable
fun MainMenuScreen(
    onPlay: () -> Unit,
    onAlbum: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current

    // Запуск фоновой музыки главного меню
    LaunchedEffect(Unit) {
        AudioPlayer.playMusic(
            context = context,
            resId = R.raw.music_main,
            loop = true
        )
    }

    // Индекс текущего цвета фона
    var colorIndex by remember { mutableIntStateOf(0) }

    // Флаг видимости меню (для анимации появления)
    var isMenuVisible by remember { mutableStateOf(false) }

    // Запускаем анимацию появления после первого кадра
    LaunchedEffect(Unit) {
        // Небольшая задержка для плавного старта
        delay(100)
        isMenuVisible = true
    }

    // Циклическая смена цвета фона каждые BACKGROUND_COLOR_INTERVAL_MS
    LaunchedEffect(Unit) {
        while (true) {
            delay(BACKGROUND_COLOR_INTERVAL_MS)
            colorIndex = (colorIndex + 1) % BackgroundColors.size
        }
    }

    // Анимированный переход цвета фона
    val backgroundColor by animateColorAsState(
        targetValue = BackgroundColors[colorIndex],
        animationSpec = tween(
            durationMillis = BACKGROUND_COLOR_DURATION_MS,
            easing = LinearEasing
        ),
        label = "MainMenuBackgroundColor"
    )

    // Бесконечная пульсация (лёгкое изменение прозрачности)
    val infiniteTransition = rememberInfiniteTransition(label = "MainMenuPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = PULSE_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MainMenuPulseAlpha"
    )

    // Вертикальный градиент от текущего цвета к тёплому белому
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor,
            WhiteBackground
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Анимированное появление меню
        AnimatedVisibility(
            visible = isMenuVisible,
            enter = fadeIn(
                animationSpec = tween(MENU_ENTRANCE_DURATION_MS)
            ) + slideInVertically(
                initialOffsetY = { it / 4 }, // Въезжает снизу на 25% высоты
                animationSpec = tween(MENU_ENTRANCE_DURATION_MS)
            ) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(MENU_ENTRANCE_DURATION_MS)
            ),
            exit = fadeOut(tween(300))
        ) {
            MenuContent(
                alpha = pulseAlpha,
                onPlay = onPlay,
                onAlbum = onAlbum,
                onQuit = onQuit
            )
        }
    }
}

// -------------------------------------------------------------------------
// Содержимое меню
// -------------------------------------------------------------------------

/**
 * Колонка с заголовками и кнопками меню.
 *
 * @param alpha   Текущая прозрачность (для пульсации).
 * @param onPlay  Колбэк кнопки «Играть».
 * @param onAlbum Колбэк кнопки «Альбом успехов».
 * @param onQuit  Колбэк кнопки «Выход».
 */
@Composable
private fun MenuContent(
    alpha: Float,
    onPlay: () -> Unit,
    onAlbum: () -> Unit,
    onQuit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HorizontalPadding)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        // Заголовок игры
        Text(
            text = "Василисина азбука",
            style = MaterialTheme.typography.headlineLarge,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        // Подзаголовок
        Text(
            text = "Путешествие по России",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        // Отступ перед кнопками
        Spacer(modifier = Modifier.height(TopSpacerHeight))

        // Кнопка «Играть»
        MainMenuButton(
            text = "Играть",
            color = FairyPurple,
            onClick = {
                AudioPlayer.playSFX("click")
                onPlay()
            }
        )

        // Кнопка «Альбом успехов»
        MainMenuButton(
            text = "Альбом успехов",
            color = FairyGreen,
            onClick = {
                AudioPlayer.playSFX("click")
                onAlbum()
            }
        )

        // Кнопка «Выход»
        MainMenuButton(
            text = "Выход",
            color = FairyPink,
            onClick = {
                AudioPlayer.playSFX("click")
                onQuit()
            }
        )
    }
}

// -------------------------------------------------------------------------
// Кнопка меню
// -------------------------------------------------------------------------

/**
 * Кнопка главного меню в сказочном стиле.
 *
 * Особенности:
 * - Ширина 80% экрана, высота 72 dp (минимум 60 dp)
 * - Скруглённые углы 16 dp
 * - Тень с анимацией при нажатии
 * - Цвет фона и текста настраиваются
 * - Текст крупный (22 sp, Medium)
 *
 * @param text  Текст кнопки.
 * @param color Цвет фона кнопки.
 * @param onClick Действие при нажатии.
 */
@Composable
private fun MainMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(BUTTON_WIDTH_FRACTION)
            .height(ButtonHeight)
            .then(
                // Гарантируем минимальную высоту для accessibility
                Modifier.size(
                    width = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION),
                    height = maxOf(ButtonHeight, MinButtonHeight)
                )
            ),
        shape = RoundedCornerShape(ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = DarkText,
            disabledContainerColor = color.copy(alpha = 0.4f),
            disabledContentColor = DarkText.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = ButtonDefaultElevation,
            pressedElevation = ButtonPressedElevation,
            focusedElevation = ButtonFocusedElevation,
            hoveredElevation = ButtonFocusedElevation,
            disabledElevation = 0.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Альтернативные варианты (задел на будущее)
// -------------------------------------------------------------------------

/**
 * Версия главного меню с дополнительной кнопкой «Настройки».
 *
 * Может использоваться в будущих версиях игры.
 *
 * @param onSettings Колбэк для кнопки «Настройки».
 */
@Suppress("unused")
@Composable
fun MainMenuScreenWithSettings(
    onPlay: () -> Unit,
    onAlbum: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        AudioPlayer.playMusic(context, R.raw.music_main)
    }

    var colorIndex by remember { mutableIntStateOf(0) }
    var isMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isMenuVisible = true
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(BACKGROUND_COLOR_INTERVAL_MS)
            colorIndex = (colorIndex + 1) % BackgroundColors.size
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = BackgroundColors[colorIndex],
        animationSpec = tween(BACKGROUND_COLOR_DURATION_MS, easing = LinearEasing),
        label = "BackgroundWithSettings"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "MenuWithSettingsPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = PULSE_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MS),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseWithSettings"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(backgroundColor, WhiteBackground))
            )
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isMenuVisible,
            enter = fadeIn(tween(MENU_ENTRANCE_DURATION_MS)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(MENU_ENTRANCE_DURATION_MS)
                    ) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(MENU_ENTRANCE_DURATION_MS)),
            exit = fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HorizontalPadding)
                    .alpha(pulseAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
            ) {
                Text(
                    text = "Василисина азбука",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Путешествие по России",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(TopSpacerHeight))

                MainMenuButton("Играть", FairyPurple) {
                    AudioPlayer.playSFX("click")
                    onPlay()
                }

                MainMenuButton("Альбом успехов", FairyGreen) {
                    AudioPlayer.playSFX("click")
                    onAlbum()
                }

                MainMenuButton("Настройки", FairyBlue) {
                    AudioPlayer.playSFX("click")
                    onSettings()
                }

                MainMenuButton("Выход", FairyPink) {
                    AudioPlayer.playSFX("click")
                    onQuit()
                }
            }
        }
    }
}
