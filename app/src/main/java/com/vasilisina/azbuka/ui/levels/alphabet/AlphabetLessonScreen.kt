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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

private val AlphabetData = listOf(
    "А" to "МАМА", "Б" to "БАБА", "В" to "ВОДА", "Г" to "ГОРА", "Д" to "ДОМА",
    "Е" to "ЛЕТО", "Ё" to "ЁЛКА", "Ж" to "ЖУКИ", "З" to "ЗИМА", "И" to "ИГРА",
    "Й" to "МАЙ",  "К" to "КОТЫ", "Л" to "ЛУНА", "М" to "МОРЕ", "Н" to "НОГА",
    "О" to "ОКНО", "П" to "ПАПА", "Р" to "РУКА", "С" to "САДЫ", "Т" to "ТАЗЫ",
    "У" to "УТРО", "Ф" to "ФЛАГ", "Х" to "ХЛЕБ", "Ц" to "ЦАРИ", "Ч" to "ЧАСЫ",
    "Ш" to "ШАРЫ", "Щ" to "ЩЕКА", "Ъ" to "Ъ",    "Ы" to "ТЫ",   "Ь" to "Ь",
    "Э" to "ЭТО",  "Ю" to "ЮЛА",  "Я" to "ЯМА"
)

private const val TOTAL_LETTERS = 33

private val ScreenPadding = 16.dp
private val CharacterSpacer = 12.dp
private val StageSpacer = 12.dp
private val CompleteButtonSpacer = 24.dp
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.6f
private val LetterFontSize = 96.sp
private const val LETTER_APPEAR_DURATION_MS = 500
private const val LETTER_SHOW_DELAY_MS = 500L
private const val LETTER_DISPLAY_DURATION_MS = 2000L
private val LetterContainerCornerRadius = 24.dp
private val LetterContainerSize = 160.dp
private const val STAGE_TRANSITION_DURATION_MS = 300
private val ProgressBarHeight = 8.dp
private val StarFontSize = 48.sp

@Composable
fun AlphabetLessonScreen(level: Int = 1, onComplete: (Int) -> Unit) {
    val context = LocalContext.current
    var currentLetterIndex by remember { mutableIntStateOf(0) }
    var stage by remember { mutableIntStateOf(0) }
    var earnedStars by remember { mutableIntStateOf(0) }

    val targetLetter = AlphabetData[currentLetterIndex].first
    val targetWord = AlphabetData[currentLetterIndex].second
    val skipWordStage = targetLetter in listOf("Ъ", "Ы", "Ь")

    var vasilisaState by remember { mutableStateOf(CharacterState("Василиса", CharacterEmotion.HAPPY)) }
    val letterProgress = (currentLetterIndex).toFloat() / TOTAL_LETTERS

    DisposableEffect(Unit) {
        AudioPlayer.playMusic(context, R.raw.music_level1, loop = true)
        onDispose { AudioPlayer.stopMusic() }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Фон уровня
        Image(
            painter = painterResource(id = R.drawable.bg_level_1_alphabet),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Полупрозрачный слой для читаемости
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CharacterView(state = vasilisaState, sizeDp = 120)
            Spacer(modifier = Modifier.height(CharacterSpacer))
            LetterProgressBar(progress = letterProgress, currentLetter = currentLetterIndex + 1, total = TOTAL_LETTERS)
            Spacer(modifier = Modifier.height(StageSpacer))

            when (stage) {
                0 -> ShowLetterStage(letter = targetLetter, onDone = { stage = 1; vasilisaState = vasilisaState.idle() })
                1 -> LetterFinderGame(targetLetter = targetLetter, onComplete = { foundAll ->
                    if (foundAll) earnedStars++
                    if (skipWordStage) advanceOrComplete(currentLetterIndex, earnedStars, level, onComplete) { newIndex, _ -> currentLetterIndex = newIndex; stage = 0 }
                    else stage = 2
                    vasilisaState = vasilisaState.clap()
                })
                2 -> SyllableBuilderGame(targetSyllable = targetWord, onComplete = { correct ->
                    if (correct) earnedStars++
                    advanceOrComplete(currentLetterIndex, earnedStars, level, onComplete) { newIndex, _ -> currentLetterIndex = newIndex; stage = 0 }
                    vasilisaState = vasilisaState.clap()
                })
                3 -> LevelComplete(earnedStars = earnedStars, onComplete = {
                    GameState.completeLevel(level, earnedStars.coerceAtMost(GameState.MAX_STARS_PER_LEVEL))
                    onComplete(earnedStars.coerceAtMost(GameState.MAX_STARS_PER_LEVEL))
                })
            }
        }
    }
}

private fun advanceOrComplete(currentIndex: Int, stars: Int, level: Int, onComplete: (Int) -> Unit, onAdvance: (Int, Int) -> Unit) {
    if (currentIndex >= TOTAL_LETTERS - 1) {
        GameState.completeLevel(level, stars.coerceAtMost(GameState.MAX_STARS_PER_LEVEL))
        onComplete(stars.coerceAtMost(GameState.MAX_STARS_PER_LEVEL))
    } else onAdvance(currentIndex + 1, stars)
}

@Composable
private fun LetterProgressBar(progress: Float, currentLetter: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.8f)) {
        Text(text = "Буква $currentLetter из $total", style = MaterialTheme.typography.bodySmall, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        androidx.compose.material3.LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(ProgressBarHeight), color = FairyGold, trackColor = FairyBlue.copy(alpha = 0.3f))
    }
}

@Composable
fun ShowLetterStage(letter: String, onDone: () -> Unit) {
    var isLetterVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(LETTER_SHOW_DELAY_MS); isLetterVisible = true; delay(LETTER_DISPLAY_DURATION_MS); onDone() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Знакомимся с буквой!", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(StageSpacer))
        AnimatedVisibility(visible = isLetterVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(LETTER_APPEAR_DURATION_MS)), exit = fadeOut(tween(200))) {
            Box(modifier = Modifier.size(LetterContainerSize).clip(RoundedCornerShape(LetterContainerCornerRadius)).background(Brush.radialGradient(colors = listOf(FairyPurple.copy(alpha = 0.2f), FairyBlue.copy(alpha = 0.1f), WhiteBackground))), contentAlignment = Alignment.Center) {
                Text(text = letter, fontSize = LetterFontSize, fontWeight = FontWeight.Bold, color = FairyPurple, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(StageSpacer))
        Text(text = "Послушай, как она звучит!", style = MaterialTheme.typography.bodyLarge, color = DarkText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LevelComplete(earnedStars: Int, onComplete: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); isVisible = true }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(400))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            StarDisplay(earnedStars = earnedStars.coerceAtMost(GameState.MAX_STARS_PER_LEVEL))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Все буквы пройдены!", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Уровень завершён!", style = MaterialTheme.typography.bodyLarge, color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(CompleteButtonSpacer))
            Button(onClick = { AudioPlayer.playSFX("click"); onComplete() }, modifier = Modifier.fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyGreen, contentColor = DarkText), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) { Text("Завершить", style = MaterialTheme.typography.labelLarge) }
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
            LaunchedEffect(Unit) { delay(300L * (index + 1)); starVisible = true }
            val starColor by animateColorAsState(targetValue = if (isEarned) FairyGold else Color.LightGray.copy(alpha = 0.4f), animationSpec = tween(400), label = "Star")
            AnimatedVisibility(visible = starVisible, enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))) {
                Text(text = if (isEarned) "★" else "☆", fontSize = StarFontSize, color = starColor, textAlign = TextAlign.Center)
            }
        }
    }
}
