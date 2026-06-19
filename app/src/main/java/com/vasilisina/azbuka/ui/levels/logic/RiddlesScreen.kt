// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/RiddlesScreen.kt

package com.vasilisina.azbuka.ui.levels.logic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenPadding = 10.dp
private val CharacterSpacer = 4.dp
private val ElementSpacer = 4.dp
private val CompleteButtonSpacer = 16.dp
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.5f
private val CompleteButtonHeight = 48.dp
private val ButtonCornerRadius = 12.dp
private val CharacterSize = 80
private const val TOTAL_STAGES = 3
private const val CORRECT_NEEDED = 3
private const val STAGE_TRANSITION_DURATION_MS = 400
private const val STAR_DISPLAY_DURATION_MS = 500
private const val STAR_STAGGER_DELAY_MS = 200L
private val StageProgressHeight = 5.dp
private val StarFontSize = 32.sp
private val RiddleTextSize = 15.sp
private val ProgressTextSize = 13.sp
private const val RESULT_DELAY_MS = 1500L
private val LetterButtonSize = 46.dp
private val SlotSize = 40.dp
private val LetterFontSize = 24.sp
private const val MAX_LETTERS_PER_ROW = 7
private const val MAX_SLOTS_PER_ROW = 8

private data class Riddle(val text: String, val answer: String)

private val AllRiddles = listOf(
    Riddle("Без окон, без дверей,\nполна горница людей", "огурец"),
    Riddle("Зимой белый,\nлетом серый", "заяц"),
    Riddle("Кто зимой холодной\nходит злой, голодный?", "волк"),
    Riddle("Рыжая плутовка,\nпушистый хвост", "лиса"),
    Riddle("Косолапый\nлюбит мёд", "медведь"),
    Riddle("Маленький, колючий,\nяблоки носит", "еж"),
    Riddle("Кто мурлычет\nу окошка?", "кошка"),
    Riddle("Кто громко лает\nво дворе?", "собака"),
    Riddle("Кто несёт\nяйца?", "курица"),
    Riddle("Кто кукарекает\nпо утрам?", "петух"),
    Riddle("Кто говорит му?", "корова"),
    Riddle("Кто говорит хрю?", "свинья"),
    Riddle("Кто плавает и крякает?", "утка"),
    Riddle("Кто прыгает по деревьям?", "белка"),
    Riddle("Кто самый высокий зверь?", "жираф"),
    Riddle("Зимой падает, весной тает", "снег"),
    Riddle("Светит днём и греет", "солнце"),
    Riddle("Светит ночью", "луна"),
    Riddle("Идёт, а ног нет", "дождь"),
    Riddle("Дует, а не видно", "ветер"),
    Riddle("Зелёная красавица в лесу", "елка"),
    Riddle("Белый ствол, чёрные полоски", "береза"),
    Riddle("Растёт на грядке, красный", "помидор"),
    Riddle("Оранжевая, сладкая", "морковь"),
    Riddle("Круглая, зелёная, полосатая", "арбуз"),
    Riddle("Сто одежек без застёжек", "капуста"),
    Riddle("Сидит дед во сто шуб одет", "лук"),
    Riddle("Жёлтый, кислый, к чаю", "лимон"),
    Riddle("Не лает, не кусает,\nв дом не пускает", "замок"),
    Riddle("Всегда идёт, не уходит", "часы"),
    Riddle("У него четыре ножки", "стол"),
    Riddle("Мягкая, на ней спят", "кровать"),
    Riddle("В ней варят суп", "кастрюля"),
    Riddle("Из неё пьют чай", "чашка"),
    Riddle("Ею едят суп", "ложка"),
    Riddle("Им режут хлеб", "нож"),
    Riddle("В неё смотрятся", "зеркало"),
    Riddle("Четыре колеса, везёт людей", "машина"),
    Riddle("Летает в небе с крыльями", "самолет"),
    Riddle("Плывёт по морю", "корабль"),
    Riddle("Едет по рельсам", "поезд"),
    Riddle("На двух колёсах с педалями", "велосипед"),
    Riddle("Любит сыр", "мышь"),
    Riddle("Полосатый и рычит", "тигр"),
    Riddle("Царь зверей", "лев"),
    Riddle("У него длинный хобот", "слон"),
    Riddle("Любит бананы", "обезьяна"),
    Riddle("Носит домик на спине", "черепаха"),
    Riddle("Прыгает по болоту", "лягушка"),
    Riddle("Ползёт без ног", "змея"),
    Riddle("Летает и жужжит", "пчела"),
    Riddle("Круглый, резиновый, скачет", "мяч"),
    Riddle("Что открывает дверь?", "ключ"),
)

private val AllLetters = "абвгдежзийклмнопрстуфхцчшщъыьэюя"

private fun generateLetterOptions(answer: String): List<Char> {
    val answerLetters = answer.lowercase().toList()
    val otherLetters = AllLetters.toList().filter { it !in answerLetters }
    val extraCount = minOf(2, otherLetters.size)
    val extraLetters = otherLetters.shuffled().take(extraCount)
    return (answerLetters + extraLetters).shuffled()
}

@Composable
fun RiddlesScreen(level: Int = 5, onComplete: (stars: Int) -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    var earnedStars by remember { mutableIntStateOf(0) }
    var kuzyaState by remember { mutableStateOf(CharacterState("Кузя", CharacterEmotion.HAPPY)) }

    DisposableEffect(Unit) {
        try { AudioPlayer.playMusic(context, R.raw.music_level4, loop = true) } catch (_: Exception) { }
        onDispose { AudioPlayer.stopMusic() }
    }

    val stageProgress = (stage.coerceIn(0, TOTAL_STAGES)).toFloat() / TOTAL_STAGES

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Image(painter = painterResource(id = R.drawable.bg_level_4_logic), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))

        Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(state = kuzyaState, sizeDp = CharacterSize)
            Spacer(modifier = Modifier.height(CharacterSpacer))
            StageProgressIndicator(progress = stageProgress, currentStage = stage)
            Spacer(modifier = Modifier.height(ElementSpacer))
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (stage) {
                    0 -> BuildWordGame(title = "Отгадай загадку!", onResult = { correct -> if (correct) earnedStars++; stage = 1 })
                    1 -> BuildWordGame(title = "Угадай животное!", onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> BuildWordGame(title = "Угадай предмет!", onResult = { correct -> if (correct) earnedStars++; if (earnedStars == 0) earnedStars = 1; kuzyaState = kuzyaState.clap(); stage = 3 })
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
private fun BuildWordGame(title: String, onResult: (Boolean) -> Unit) {
    var correctCount by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    var currentRiddle by remember { mutableStateOf(AllRiddles.random()) }
    val letterOptions = remember(currentRiddle) { generateLetterOptions(currentRiddle.answer) }
    // ✅ Уникальные индексы для каждой буквы
    val letterOptionsWithIndex = remember(letterOptions) { letterOptions.mapIndexed { i, ch -> i to ch } }
    val answerLength = currentRiddle.answer.length
    val slots = remember(currentRiddle) { mutableStateListOf(*Array(answerLength) { '_' }) }
    val usedLetterIndices = remember(currentRiddle) { mutableStateListOf<Int>() }
    val builtWord = slots.joinToString("").replace("_", "").trim()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(correctCount) { if (correctCount >= CORRECT_NEEDED) { delay(500); onResult(true) } }

    val scrollState = rememberScrollState()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Правильно: $correctCount из $CORRECT_NEEDED", style = MaterialTheme.typography.bodyMedium.copy(fontSize = ProgressTextSize, fontWeight = FontWeight.Bold), color = FairyPurple, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().background(FairyBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
            Text(text = currentRiddle.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = RiddleTextSize, fontWeight = FontWeight.Bold, lineHeight = 22.sp), color = DarkText, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Слоты
        val slotRows = slots.chunked(MAX_SLOTS_PER_ROW)
        slotRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { letter ->
                    val idx = slots.indexOf(letter)
                    Box(modifier = Modifier.size(SlotSize).background(if (letter != '_') FairyGold.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).border(2.dp, if (letter != '_') FairyGold else FairyBlue, RoundedCornerShape(8.dp)).clickable(enabled = letter != '_' && !isLocked) {
                        if (idx >= 0 && letter != '_') {
                            val lastUsed = usedLetterIndices.lastOrNull { letterOptionsWithIndex[it].second == letter }
                            if (lastUsed != null) { usedLetterIndices.remove(lastUsed); slots[idx] = '_' }
                        }
                    }, contentAlignment = Alignment.Center) { Text(text = if (letter != '_') letter.uppercase() else "", fontSize = LetterFontSize, fontWeight = FontWeight.Bold, color = FairyPurple) }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Кнопки с буквами — ✅ уникальные индексы
        val letterRows = letterOptionsWithIndex.chunked(MAX_LETTERS_PER_ROW)
        letterRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { (uniqueIndex, letter) ->
                    val isUsed = usedLetterIndices.contains(uniqueIndex)
                    Box(modifier = Modifier.size(LetterButtonSize).shadow(if (!isUsed) 3.dp else 0.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(if (isUsed) Color.LightGray else FairyBlue).clickable(enabled = !isUsed && !isLocked) {
                        val firstEmpty = slots.indexOf('_')
                        if (firstEmpty != -1) { slots[firstEmpty] = letter; usedLetterIndices.add(uniqueIndex) }
                    }, contentAlignment = Alignment.Center) { Text(text = letter.uppercase(), fontSize = LetterFontSize, fontWeight = FontWeight.Bold, color = if (isUsed) Color.White.copy(alpha = 0.3f) else Color.White) }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { usedLetterIndices.clear(); slots.fill('_') }, enabled = usedLetterIndices.isNotEmpty() && !isLocked, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPink)) { Text("Сброс", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Button(onClick = {
                isCorrect = builtWord.equals(currentRiddle.answer, ignoreCase = true)
                isLocked = true; showResult = true
                if (isCorrect) { correctCount++; AudioPlayer.playSFX("correct") } else { AudioPlayer.playSFX("wrong") }
                coroutineScope.launch { delay(RESULT_DELAY_MS); if (correctCount < CORRECT_NEEDED) { currentRiddle = AllRiddles.filter { it != currentRiddle }.random(); isLocked = false; showResult = false } }
            }, enabled = builtWord.length == answerLength && !isLocked, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyGold)) { Text("Проверить", fontSize = 14.sp, color = DarkText, fontWeight = FontWeight.Bold) }
        }

        if (showResult) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = if (isCorrect) "✓ Правильно!" else "✗ Ответ: ${currentRiddle.answer}", style = MaterialTheme.typography.titleMedium, color = if (isCorrect) FairyGreen else FairyPink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))
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
