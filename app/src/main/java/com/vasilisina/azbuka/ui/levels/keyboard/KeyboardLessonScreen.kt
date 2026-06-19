// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/KeyboardLessonScreen.kt

package com.vasilisina.azbuka.ui.levels.keyboard

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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clip
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

private val RussianAlphabet = listOf(
    "Й", "Ц", "У", "К", "Е", "Н", "Г", "Ш", "Щ", "З", "Х", "Ъ",
    "Ф", "Ы", "В", "А", "П", "Р", "О", "Л", "Д", "Ж", "Э",
    "Я", "Ч", "С", "М", "И", "Т", "Ь", "Б", "Ю", "Ё"
)

@Composable
fun KeyboardLessonScreen(level: Int = 3, onComplete: (stars: Int) -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    var earnedStars by remember { mutableIntStateOf(0) }
    var vasilisaState by remember { mutableStateOf(CharacterState("Василиса", CharacterEmotion.HAPPY)) }

    DisposableEffect(Unit) {
        try { AudioPlayer.playMusic(context, R.raw.music_level3, loop = true) } catch (_: Exception) { }
        onDispose { AudioPlayer.stopMusic() }
    }

    val stageProgress = (stage.coerceIn(0, TOTAL_STAGES)).toFloat() / TOTAL_STAGES

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Image(painter = painterResource(id = R.drawable.bg_level_3_keyboard), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))

        Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(state = vasilisaState, sizeDp = CharacterSize)
            Spacer(modifier = Modifier.height(CharacterSpacer))
            StageProgressIndicator(progress = stageProgress, currentStage = stage)
            Spacer(modifier = Modifier.height(ElementSpacer))

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (stage) {
                    0 -> KeyboardGame(onDone = { earnedStars++; stage = 1 })
                    1 -> WordBuilderGame(onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> FreeTypingGame(onDone = { if (earnedStars == 0) earnedStars = 1; vasilisaState = vasilisaState.clap(); stage = 3 })
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
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(StageProgressHeight).clip(RoundedCornerShape(50)), color = FairyGold, trackColor = FairyBlue.copy(alpha = 0.3f))
    }
}

@Composable
fun VirtualKeyboard(onKeyPress: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 40.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(RussianAlphabet) { letter ->
            Card(
                modifier = Modifier.height(48.dp).clickable { AudioPlayer.playSFX("click"); onKeyPress(letter) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = FairyBlue.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = letter, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkText)
                }
            }
        }
    }
}

@Composable
fun KeyboardGame(onDone: () -> Unit) {
    val targetLetter = remember { RussianAlphabet.random() }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(showSuccess) { if (showSuccess) { AudioPlayer.playSFX("correct"); delay(1000); onDone() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Найди букву", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.size(80.dp).background(if (showSuccess) FairyGreen else FairyGold, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text(text = targetLetter, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(20.dp))
        VirtualKeyboard(onKeyPress = { key -> if (key == targetLetter) showSuccess = true else AudioPlayer.playSFX("wrong") })
    }
}

@Composable
fun WordBuilderGame(onResult: (Boolean) -> Unit) {
    val words = listOf("КОТ", "ДОМ", "ЛЕС", "МАК", "СОК")
    val targetWord = remember { words.random() }
    var typedWord by remember { mutableStateOf("") }

    LaunchedEffect(typedWord) { if (typedWord == targetWord) { AudioPlayer.playSFX("correct"); delay(1000); onResult(true) } }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Напечатай слово", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = targetWord, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = FairyPurple.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            targetWord.forEachIndexed { index, _ ->
                val char = typedWord.getOrNull(index)?.toString() ?: ""
                Box(modifier = Modifier.size(50.dp).background(if (char.isNotEmpty()) FairyGreen else FairyBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text(text = char, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (char.isNotEmpty()) Color.White else DarkText)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        VirtualKeyboard(onKeyPress = { key ->
            if (typedWord.length < targetWord.length) {
                if (key == targetWord[typedWord.length].toString()) typedWord += key else AudioPlayer.playSFX("wrong")
            }
        })
    }
}

@Composable
fun FreeTypingGame(onDone: () -> Unit) {
    var typedLetters by remember { mutableStateOf("") }
    val maxLetters = 5

    LaunchedEffect(typedLetters) { if (typedLetters.length == maxLetters) { AudioPlayer.playSFX("correct"); delay(1500); onDone() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Напечатай любые $maxLetters букв!", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(maxLetters) { index ->
                val char = typedLetters.getOrNull(index)?.toString() ?: ""
                Box(modifier = Modifier.size(44.dp).background(if (char.isNotEmpty()) FairyGold else FairyBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    AnimatedVisibility(visible = char.isNotEmpty(), enter = scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy))) {
                        Text(text = char, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        VirtualKeyboard(onKeyPress = { key -> if (typedLetters.length < maxLetters) typedLetters += key })
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
