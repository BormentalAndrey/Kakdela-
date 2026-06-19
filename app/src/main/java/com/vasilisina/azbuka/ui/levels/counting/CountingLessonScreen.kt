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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.delay

private val ScreenPadding = 12.dp
private val CharacterSpacer = 8.dp
private val ElementSpacer = 10.dp
private val CompleteButtonSpacer = 20.dp
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.5f
private val CompleteButtonHeight = 50.dp
private val ButtonCornerRadius = 14.dp
private val CharacterSize = 100
private const val TOTAL_STAGES = 3
private const val STAGE_TRANSITION_DURATION_MS = 400
private const val STAR_DISPLAY_DURATION_MS = 500
private const val STAR_STAGGER_DELAY_MS = 200L
private val StageProgressHeight = 6.dp
private val StarFontSize = 36.sp

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

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Image(painter = painterResource(id = R.drawable.bg_level_2_counting), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))

        Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(state = kuzyaState, sizeDp = CharacterSize)
            Spacer(modifier = Modifier.height(CharacterSpacer))
            StageProgressIndicator(progress = stageProgress, currentStage = stage)
            Spacer(modifier = Modifier.height(ElementSpacer))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (stage) {
                    0 -> CountingGame(onResult = { correct -> if (correct) earnedStars++; stage = 1 })
                    1 -> MathExampleGame(onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> ComparisonGame(onResult = { correct -> if (correct) earnedStars++; if (earnedStars == 0) earnedStars = 1; kuzyaState = kuzyaState.clap(); stage = 3 })
                    3 -> LevelComplete(earnedStars = earnedStars, onComplete = { GameState.completeLevel(level, earnedStars); onComplete(earnedStars) })
                }
            }
        }
    }
}

@Composable
private fun StageProgressIndicator(progress: Float, currentStage: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.9f)) {
        Text(text = if (currentStage < TOTAL_STAGES) "Этап ${currentStage + 1} из $TOTAL_STAGES" else "Завершено!", style = MaterialTheme.typography.bodyMedium, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(StageProgressHeight), color = FairyGold, trackColor = FairyBlue.copy(alpha = 0.3f))
    }
}

@Composable
fun CountingGame(onResult: (Boolean) -> Unit) {
    val targetCount = remember { (3..9).random() }
    val options = remember(targetCount) {
        val wrongs = mutableSetOf<Int>()
        while (wrongs.size < 2) { wrongs.add((targetCount - 3..targetCount + 3).filter { it > 0 && it != targetCount }.random()) }
        (wrongs + targetCount).shuffled()
    }
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedOption) {
        if (selectedOption != null) {
            val isCorrect = selectedOption == targetCount
            if (isCorrect) AudioPlayer.playSFX("correct") else AudioPlayer.playSFX("wrong")
            delay(1000)
            onResult(isCorrect)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Сколько здесь звёздочек?", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.size(160.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(targetCount) { Text(text = "⭐", fontSize = 40.sp, textAlign = TextAlign.Center) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val isCorrect = option == targetCount
                val bg = when { selectedOption == null -> FairyBlue.copy(alpha = 0.2f); isSelected && isCorrect -> FairyGreen; isSelected && !isCorrect -> Color.Red.copy(alpha = 0.6f); !isSelected && isCorrect -> FairyGreen; else -> FairyBlue.copy(alpha = 0.2f) }
                Card(modifier = Modifier.size(64.dp).clickable(enabled = selectedOption == null) { selectedOption = option }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = option.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DarkText) }
                }
            }
        }
    }
}

@Composable
fun MathExampleGame(onResult: (Boolean) -> Unit) {
    val a = remember { (1..5).random() }
    val b = remember { (1..5).random() }
    val correctAnswer = a + b
    val options = remember(correctAnswer) {
        val wrongs = mutableSetOf<Int>()
        while (wrongs.size < 2) { wrongs.add((correctAnswer - 3..correctAnswer + 3).filter { it > 0 && it != correctAnswer }.random()) }
        (wrongs + correctAnswer).shuffled()
    }
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedOption) {
        if (selectedOption != null) {
            val isCorrect = selectedOption == correctAnswer
            if (isCorrect) AudioPlayer.playSFX("correct") else AudioPlayer.playSFX("wrong")
            delay(1000)
            onResult(isCorrect)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Реши пример", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "$a + $b = ?", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = FairyPurple)
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val isCorrect = option == correctAnswer
                val bg = when { selectedOption == null -> FairyGold; isSelected && isCorrect -> FairyGreen; isSelected && !isCorrect -> Color.Red.copy(alpha = 0.6f); !isSelected && isCorrect -> FairyGreen; else -> Color.LightGray }
                Card(modifier = Modifier.size(70.dp).clickable(enabled = selectedOption == null) { selectedOption = option }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = option.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun ComparisonGame(onResult: (Boolean) -> Unit) {
    val a = remember { (1..10).random() }
    val b = remember { (1..10).random() }
    val correctSign = if (a > b) ">" else if (a < b) "<" else "="
    val options = listOf("<", "=", ">")
    var selectedOption by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedOption) {
        if (selectedOption != null) {
            val isCorrect = selectedOption == correctSign
            if (isCorrect) AudioPlayer.playSFX("correct") else AudioPlayer.playSFX("wrong")
            delay(1200)
            onResult(isCorrect)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Сравни числа", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = a.toString(), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = FairyPurple)
            Box(modifier = Modifier.size(60.dp).background(FairyBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(text = selectedOption ?: "?", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = DarkText) }
            Text(text = b.toString(), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = FairyPurple)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val isCorrect = option == correctSign
                val bg = when { selectedOption == null -> FairyGold; isSelected && isCorrect -> FairyGreen; isSelected && !isCorrect -> Color.Red.copy(alpha = 0.6f); !isSelected && isCorrect && selectedOption != null -> FairyGreen; else -> if (selectedOption != null) Color.LightGray else FairyGold }
                Card(modifier = Modifier.size(64.dp).clickable(enabled = selectedOption == null) { selectedOption = option }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = option, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }
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
            Text(text = when (earnedStars) { 3 -> "Отлично!"; 2 -> "Хорошо!"; else -> "Молодец!" }, style = MaterialTheme.typography.headlineMedium, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Уровень пройден!", style = MaterialTheme.typography.bodyLarge, color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(CompleteButtonSpacer))
            Button(onClick = { AudioPlayer.playSFX("click"); onComplete() }, modifier = Modifier.fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION).height(CompleteButtonHeight), shape = RoundedCornerShape(ButtonCornerRadius), colors = ButtonDefaults.buttonColors(containerColor = FairyGreen, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)) { Text("Далее →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StarDisplay(earnedStars: Int) {
    val maxStars = GameState.MAX_STARS_PER_LEVEL
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(maxStars) { index ->
            val isEarned = index < earnedStars
            var starVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(STAR_STAGGER_DELAY_MS * (index + 1)); starVisible = true }
            val starColor by animateColorAsState(targetValue = if (isEarned) FairyGold else Color.LightGray.copy(alpha = 0.4f), animationSpec = tween(STAR_DISPLAY_DURATION_MS), label = "Star")
            AnimatedVisibility(visible = starVisible, enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))) {
                Text(text = if (isEarned) "★" else "☆", fontSize = StarFontSize, color = starColor, textAlign = TextAlign.Center)
            }
        }
    }
}
