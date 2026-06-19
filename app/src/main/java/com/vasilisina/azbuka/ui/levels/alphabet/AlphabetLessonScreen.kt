// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/alphabet/AlphabetLessonScreen.kt

package com.vasilisina.azbuka.ui.levels.alphabet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
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
private val CharacterSpacer = 16.dp

/** Отступ между элементами внутри этапа */
private val StageSpacer = 16.dp

/** Отступ перед кнопкой «Далее» */
private val CompleteButtonSpacer = 24.dp

/** Ширина кнопки «Далее» (доля экрана) */
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.6f

/** Размер буквы на этапе показа */
private val LetterFontSize = 96.sp

/** Длительность анимации появления буквы (мс) */
private const val LETTER_APPEAR_DURATION_MS = 500

/** Задержка перед показом буквы (мс) */
private const val LETTER_SHOW_DELAY_MS = 500L

/** Длительность показа буквы (мс) */
private const val LETTER_DISPLAY_DURATION_MS = 2000L

/** Радиус скругления контейнера буквы */
private val LetterContainerCornerRadius = 24.dp

/** Размер контейнера буквы */
private val LetterContainerSize = 160.dp

/** Длительность кроссфейда между этапами (мс) */
private const val STAGE_TRANSITION_DURATION_MS = 300

// -------------------------------------------------------------------------
// Главный экран урока
// -------------------------------------------------------------------------

/**
 * Уровень 1 — «Алфавит».
 *
 * Три этапа:
 * 1. **Знакомство с буквой** — Василиса показывает и называет букву
 * 2. **Найди буквы** — игра на поиск целевой буквы среди других
 * 3. **Составь слог** — drag-and-drop букв для составления слога
 *
 * Персонаж Василиса реагирует на успехи: HAPPY → IDLE → CLAP.
 *
 * @param level      Номер уровня (по умолчанию 1).
 * @param onComplete Колбэк при завершении уровня (получает количество звёзд 1–3).
 */
@Composable
fun AlphabetLessonScreen(
    level: Int = 1,
    onComplete: (Int) -> Unit
) {
    val context = LocalContext.current

    // Текущий этап (0 = буква, 1 = поиск, 2 = слог, 3 = завершено)
    var stage by remember { mutableIntStateOf(0) }

    // Заработанные звёзды
    var earnedStars by remember { mutableIntStateOf(0) }

    // Целевая буква для изучения
    val targetLetter = remember { "А" }

    // Состояние Василисы
    var vasilisaState by remember {
        mutableStateOf(
            CharacterState(
                name = "Василиса",
                emotion = CharacterEmotion.HAPPY
            )
        )
    }

    // Запуск / остановка музыки уровня
    DisposableEffect(Unit) {
        AudioPlayer.playMusic(context, R.raw.music_level1, loop = true)
        onDispose {
            AudioPlayer.stopMusic()
        }
    }

    // Основной контейнер
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .padding(ScreenPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Персонаж Василиса
            CharacterView(
                state = vasilisaState,
                sizeDp = 160
            )

            Spacer(modifier = Modifier.height(CharacterSpacer))

            // Этапы урока
            when (stage) {
                0 -> ShowLetterStage(
                    letter = targetLetter,
                    onDone = {
                        stage = 1
                        vasilisaState = vasilisaState.idle()
                    }
                )

                1 -> LetterFinderGame(
                    targetLetter = targetLetter,
                    onComplete = { foundAll ->
                        if (foundAll) earnedStars++
                        stage = 2
                        vasilisaState = vasilisaState.clap()
                    }
                )

                2 -> SyllableBuilderGame(
                    targetSyllable = "МА",
                    onComplete = { correct ->
                        if (correct) earnedStars++
                        // Минимум 1 звезда за прохождение
                        if (earnedStars == 0) earnedStars = 1
                        stage = 3
                        vasilisaState = vasilisaState.clap()
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
// Этап 0: Знакомство с буквой
// -------------------------------------------------------------------------

/**
 * Показывает крупную букву с анимацией появления.
 *
 * Василиса «называет» букву, ребёнок слушает.
 *
 * @param letter Буква для показа (например, «А»).
 * @param onDone Колбэк по завершении этапа.
 */
@Composable
fun ShowLetterStage(
    letter: String,
    onDone: () -> Unit
) {
    var isLetterVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(LETTER_SHOW_DELAY_MS)
        isLetterVisible = true
        delay(LETTER_DISPLAY_DURATION_MS)
        onDone()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Подсказка
        Text(
            text = "Знакомимся с буквой!",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(StageSpacer))

        // Анимированное появление буквы в декоративном контейнере
        AnimatedVisibility(
            visible = isLetterVisible,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(
                animationSpec = tween(LETTER_APPEAR_DURATION_MS)
            ),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .size(LetterContainerSize)
                    .clip(RoundedCornerShape(LetterContainerCornerRadius))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                FairyPurple.copy(alpha = 0.2f),
                                FairyBlue.copy(alpha = 0.1f),
                                WhiteBackground
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter,
                    fontSize = LetterFontSize,
                    fontWeight = FontWeight.Bold,
                    color = FairyPurple,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(StageSpacer))

        // Инструкция
        Text(
            text = "Послушай, как она звучит!",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkText,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Этап 3: Завершение уровня
// -------------------------------------------------------------------------

/**
 * Экран завершения уровня.
 *
 * Показывает количество заработанных звёзд и кнопку «Далее».
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
        ) + fadeIn(tween(400))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Звёзды
            StarDisplay(earnedStars = earnedStars)

            Spacer(modifier = Modifier.height(8.dp))

            // Текст поздравления
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
                modifier = Modifier.fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION),
                shape = RoundedCornerShape(16.dp),
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
 * Отображает 3 звезды (заполненные и пустые) с анимацией появления.
 *
 * @param earnedStars Количество заработанных звёзд (0–3).
 */
@Composable
private fun StarDisplay(earnedStars: Int) {
    val maxStars = GameState.MAX_STARS_PER_LEVEL

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxStars) { index ->
            val isEarned = index < earnedStars

            var starVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(300L * (index + 1))
                starVisible = true
            }

            val starColor by animateColorAsState(
                targetValue = if (isEarned) FairyGold else Color.LightGray.copy(alpha = 0.4f),
                animationSpec = tween(400),
                label = "StarColor"
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
                    fontSize = 48.sp,
                    color = starColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
