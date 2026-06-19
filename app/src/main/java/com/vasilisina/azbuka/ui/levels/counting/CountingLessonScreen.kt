// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/CountingLessonScreen.kt

package com.vasilisina.azbuka.ui.levels.counting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.characters.CharacterEmotion
import com.vasilisina.azbuka.characters.CharacterState
import com.vasilisina.azbuka.characters.CharacterView
import com.vasilisina.azbuka.data.GameState
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

/** Отступ экрана */
private val ScreenPadding = 16.dp

/** Отступ между персонажем и контентом */
private val CharacterSpacer = 24.dp

/** Отступ между элементами */
private val ElementSpacer = 16.dp

/** Отступ перед кнопкой «Далее» */
private val CompleteButtonSpacer = 32.dp

/** Ширина кнопки «Далее» (доля экрана) */
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.5f

/** Высота кнопки «Далее» */
private val CompleteButtonHeight = 60.dp

/** Радиус скругления кнопок */
private val ButtonCornerRadius = 16.dp

/** Размер персонажа */
private val CharacterSize = 150

/** Количество этапов */
private const val TOTAL_STAGES = 3

/** Длительность анимации перехода (мс) */
private const val STAGE_TRANSITION_DURATION_MS = 400

/** Длительность анимации появления звёзд (мс) */
private const val STAR_DISPLAY_DURATION_MS = 500

/** Задержка между появлением звёзд (мс) */
private const val STAR_STAGGER_DELAY_MS = 200L

/** Высота прогресс-бара этапов */
private val StageProgressHeight = 8.dp

/** Радиус скругления прогресс-бара */
private val StageProgressCornerRadius = 4.dp

/** Размер шрифта звёзд */
private val StarFontSize = 48.sp

// -------------------------------------------------------------------------
// Главный экран урока
// -------------------------------------------------------------------------

/**
 * Уровень 2 — «Счёт».
 *
 * Три этапа:
 * 1. **Счёт предметов** — посчитать количество и выбрать цифру
 * 2. **Реши пример** — простые примеры на + и − (до 10)
 * 3. **Чего больше?** — сравнить две группы предметов
 *
 * Персонаж Кузя помогает ребёнку и реагирует на успехи.
 *
 * @param level      Номер уровня (по умолчанию 2).
 * @param onComplete Колбэк при завершении уровня (получает количество звёзд 1–3).
 */
@Composable
fun CountingLessonScreen(
    level: Int = 2,
    onComplete: (stars: Int) -> Unit
) {
    val context = LocalContext.current

    // Текущий этап: 0 = счёт, 1 = примеры, 2 = сравнение, 3 = завершено
    var stage by remember { mutableIntStateOf(0) }

    // Заработанные звёзды
    var earnedStars by remember { mutableIntStateOf(0) }

    // Состояние Кузи
    var kuzyaState by remember {
        mutableStateOf(CharacterState("Кузя", CharacterEmotion.HAPPY))
    }

    // Управление музыкой уровня
    DisposableEffect(Unit) {
        AudioPlayer.playMusic(context, R.raw.music_level2, loop = true)
        onDispose {
            AudioPlayer.stopMusic()
        }
    }

    // Прогресс по этапам (для индикатора)
    val stageProgress = (stage.coerceIn(0, TOTAL_STAGES)).toFloat() / TOTAL_STAGES

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Персонаж Кузя
            CharacterView(
                state = kuzyaState,
                sizeDp = CharacterSize
            )

            Spacer(modifier = Modifier.height(CharacterSpacer))

            // Индикатор прогресса по этапам
            StageProgressIndicator(progress = stageProgress, currentStage = stage)

            Spacer(modifier = Modifier.height(ElementSpacer))

            // Этапы урока
            when (stage) {
                0 -> CountingGame(
                    onResult = { correct ->
                        if (correct) earnedStars++
                        stage = 1
                    }
                )

                1 -> MathExampleGame(
                    onResult = { correct ->
                        if (correct) earnedStars++
                        stage = 2
                    }
                )

                2 -> ComparisonGame(
                    onResult = { correct ->
                        if (correct) earnedStars++
                        // Минимум 1 звезда за прохождение
                        if (earnedStars == 0) earnedStars = 1
                        kuzyaState = kuzyaState.copy(emotion = CharacterEmotion.CLAP)
                        stage = 3
                    }
                )

                3 -> LevelComplete(
                    earnedStars = earnedStars,
                    onComplete = {
                        GameState.completeLevel(level, earnedStars)
                        onComplete(earnedStars)
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Индикатор прогресса по этапам
// -------------------------------------------------------------------------

/**
 * Прогресс-бар, показывающий продвижение по этапам уровня.
 *
 * @param progress     Значение от 0.0 до 1.0.
 * @param currentStage Текущий этап (для подписи).
 */
@Composable
private fun StageProgressIndicator(
    progress: Float,
    currentStage: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        // Текст: «Этап 1 из 3»
        Text(
            text = if (currentStage < TOTAL_STAGES) {
                "Этап ${currentStage + 1} из $TOTAL_STAGES"
            } else {
                "Завершено!"
            },
            style = MaterialTheme.typography.bodySmall,
            color = DarkText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Прогресс-бар
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(StageProgressHeight),
            color = FairyGold,
            trackColor = FairyBlue.copy(alpha = 0.3f),
        )
    }
}

// -------------------------------------------------------------------------
// Экран завершения уровня
// -------------------------------------------------------------------------

/**
 * Экран завершения уровня с анимированными звёздами.
 *
 * @param earnedStars Количество звёзд (1–3).
 * @param onComplete  Колбэк при нажатии «Далее».
 */
@Composable
private fun LevelComplete(
    earnedStars: Int,
    onComplete: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(tween(STAGE_TRANSITION_DURATION_MS))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Анимированные звёзды
            StarDisplay(earnedStars = earnedStars)

            Spacer(modifier = Modifier.height(ElementSpacer))

            // Текст результата
            Text(
                text = when (earnedStars) {
                    3 -> "Отлично!"
                    2 -> "Хорошо!"
                    else -> "Молодец!"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Уровень пройден!",
                style = MaterialTheme.typography.bodyLarge,
                color = DarkText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(CompleteButtonSpacer))

            // Кнопка «Далее»
            Button(
                onClick = {
                    AudioPlayer.playSFX("click")
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION)
                    .height(CompleteButtonHeight),
                shape = RoundedCornerShape(ButtonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FairyGreen,
                    contentColor = DarkText
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Далее →",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Отображение звёзд
// -------------------------------------------------------------------------

/**
 * Отображает 3 звезды с каскадной анимацией появления.
 *
 * @param earnedStars Количество заработанных звёзд (0–3).
 */
@Composable
private fun StarDisplay(earnedStars: Int) {
    val maxStars = GameState.MAX_STARS_PER_LEVEL

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxStars) { index ->
            val isEarned = index < earnedStars

            var starVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(STAR_STAGGER_DELAY_MS * (index + 1))
                starVisible = true
            }

            val starColor by animateColorAsState(
                targetValue = if (isEarned) FairyGold else Color.LightGray.copy(alpha = 0.3f),
                animationSpec = tween(STAR_DISPLAY_DURATION_MS),
                label = "CompleteStarColor"
            )

            AnimatedVisibility(
                visible = starVisible,
                enter = scaleIn(
                    initialScale = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
            ) {
                Text(
                    text = if (isEarned) "★" else "☆",
                    fontSize = StarFontSize,
                    color = starColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
