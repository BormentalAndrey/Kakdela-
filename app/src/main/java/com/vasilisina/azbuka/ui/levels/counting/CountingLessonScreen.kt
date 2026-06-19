// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/CountingLessonScreen.kt

package com.vasilisina.azbuka.ui.levels.counting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.characters.CharacterEmotion
import com.vasilisina.azbuka.characters.CharacterState
import com.vasilisina.azbuka.characters.CharacterView
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.common.AdaptiveBox
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay

private val ScreenPadding = 12.dp
private val CharacterSpacer = 10.dp
private val ElementSpacer = 8.dp
private val CompleteButtonSpacer = 24.dp
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.5f
private val CompleteButtonHeight = 50.dp
private val ButtonCornerRadius = 14.dp
private val CharacterSize = 110
private const val TOTAL_STAGES = 3
private const val STAGE_TRANSITION_DURATION_MS = 400
private const val STAR_DISPLAY_DURATION_MS = 500
private const val STAR_STAGGER_DELAY_MS = 200L
private val StageProgressHeight = 6.dp
private val StarFontSize = 40.sp

@Composable
fun CountingLessonScreen(level: Int = 2, onComplete: (stars: Int) -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    var earnedStars by remember { mutableIntStateOf(0) }
    var kuzyaState by remember { mutableStateOf(CharacterState("Кузя", CharacterEmotion.HAPPY)) }

    DisposableEffect(Unit) {
        try { AudioPlayer.playMusic(context, R.raw.music_level2, loop = true) } catch (_: Exception) { }
        onDispose { AudioPlayer.stopMusic() }
    }

    val stageProgress = (stage.coerceIn(0, TOTAL_STAGES)).toFloat() / TOTAL_STAGES

    AdaptiveBox {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Image(painter = painterResource(id = R.drawable.bg_level_2_counting), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))

            Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CharacterView(state = kuzyaState, sizeDp = CharacterSize)
                Spacer(modifier = Modifier.height(CharacterSpacer))
                StageProgressIndicator(progress = stageProgress, currentStage = stage)
                Spacer(modifier = Modifier.height(ElementSpacer))

                when (stage) {
                    0 -> CountingGame(onResult = { correct -> if (correct) earnedStars++; stage = 1 })
                    1 -> MathExampleGame(onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> ComparisonGame(onResult = { correct -> if (correct) earnedStars++; if (earnedStars == 0) earnedStars = 1; kuzyaState = kuzyaState.copy(emotion = CharacterEmotion.CLAP); stage = 3 })
                    3 -> LevelComplete(earnedStars = earnedStars, onComplete = { GameState.completeLevel(level, earnedStars); onComplete(earnedStars) })
                }
            }
        }
    }
}

@Composable
private fun StageProgressIndicator(progress: Float, currentStage: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.8f)) {
        Text(text = if (currentStage < TOTAL_STAGES) "Этап ${currentStage + 1} из $TOTAL_STAGES" else "Завершено!", style = MaterialTheme.typography.bodySmall, color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(StageProgressHeight), color = FairyGold, trackColor = FairyBlue.copy(alpha = 0.3f))
    }
}

@Composable
private fun LevelComplete(earnedStars: Int, onComplete: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); isVisible = true }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(STAGE_TRANSITION_DURATION_MS))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            StarDisplay(earnedStars = earnedStars)
            Spacer(modifier = Modifier.height(ElementSpacer))
            Text(text = when (earnedStars) { 3 -> "Отлично!"; 2 -> "Хорошо!"; else -> "Молодец!" }, style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Уровень пройден!", style = MaterialTheme.typography.bodyLarge, color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(CompleteButtonSpacer))
            Button(onClick = { AudioPlayer.playSFX("click"); onComplete() }, modifier = Modifier.fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION).height(CompleteButtonHeight), shape = RoundedCornerShape(ButtonCornerRadius), colors = ButtonDefaults.buttonColors(containerColor = FairyGreen, contentColor = DarkText), elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)) { Text("Далее →", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun StarDisplay(earnedStars: Int) {
    val maxStars = GameState.MAX_STARS_PER_LEVEL
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(maxStars) { index ->
            val isEarned = index < earnedStars
            var starVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(STAR_STAGGER_DELAY_MS * (index + 1)); starVisible = true }
            val starColor by animateColorAsState(targetValue = if (isEarned) FairyGold else Color.LightGray.copy(alpha = 0.3f), animationSpec = tween(STAR_DISPLAY_DURATION_MS), label = "Star")
            AnimatedVisibility(visible = starVisible, enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))) {
                Text(text = if (isEarned) "★" else "☆", fontSize = StarFontSize, color = starColor, textAlign = TextAlign.Center)
            }
        }
    }
}
