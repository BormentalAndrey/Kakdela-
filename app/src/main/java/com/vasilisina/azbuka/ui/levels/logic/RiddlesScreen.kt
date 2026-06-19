// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/RiddlesScreen.kt

package com.vasilisina.azbuka.ui.levels.logic

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

private val ScreenPadding = 16.dp
private val CharacterSpacer = 24.dp
private val ElementSpacer = 16.dp
private val CompleteButtonSpacer = 32.dp
private const val COMPLETE_BUTTON_WIDTH_FRACTION = 0.5f
private val CompleteButtonHeight = 60.dp
private val ButtonCornerRadius = 16.dp
private val CharacterSize = 150
private const val TOTAL_STAGES = 3
private const val CORRECT_NEEDED = 3
private const val STAGE_TRANSITION_DURATION_MS = 400
private const val STAR_DISPLAY_DURATION_MS = 500
private const val STAR_STAGGER_DELAY_MS = 200L
private val StageProgressHeight = 8.dp
private val StarFontSize = 48.sp
private val RiddleTextSize = 22.sp
private val ProgressTextSize = 16.sp
private const val RESULT_DELAY_MS = 1500L

private data class Riddle(val text: String, val answer: String)

private val AllRiddles = listOf(
    Riddle("Без окон, без дверей,\nполна горница людей", "огурец"),
    Riddle("Зимой белый,\nлетом серый", "заяц"),
    Riddle("Кто зимой холодной\nходит злой, голодный?", "волк"),
    Riddle("Рыжая плутовка,\nпушистый хвост", "лиса"),
    Riddle("Косолапый\nлюбит мёд", "медведь"),
    Riddle("Маленький, колючий,\nяблоки носит", "ёж"),
    Riddle("Кто мурлычет\nу окошка?", "кошка"),
    Riddle("Кто громко лает\nво дворе?", "собака"),
    Riddle("Кто несёт\nяйца?", "курица"),
    Riddle("Кто кукарекает\nпо утрам?", "петух"),
    Riddle("Кто говорит «му-у»?", "корова"),
    Riddle("Кто говорит «бе-е»?", "овца"),
    Riddle("Кто говорит «хрю-хрю»?", "свинья"),
    Riddle("Кто говорит «и-го-го»?", "лошадь"),
    Riddle("Кто плавает и крякает?", "утка"),
    Riddle("Кто прыгает по деревьям?", "белка"),
    Riddle("Кто самый высокий зверь?", "жираф"),
    Riddle("Зимой падает, весной тает", "снег"),
    Riddle("После дождя появляется\nцветная дуга", "радуга"),
    Riddle("Светит днём и греет", "солнце"),
    Riddle("Светит ночью", "луна"),
    Riddle("Маленькие огоньки на небе", "звёзды"),
    Riddle("Идёт, а ног нет", "дождь"),
    Riddle("Гремит и сверкает летом", "гроза"),
    Riddle("Дует, а не видно", "ветер"),
    Riddle("Белые кораблики по небу", "облака"),
    Riddle("Зелёная красавица в лесу", "ёлка"),
    Riddle("Белый ствол, чёрные полоски", "берёза"),
    Riddle("Растёт на грядке, красный", "помидор"),
    Riddle("Оранжевая, сладкая", "морковь"),
    Riddle("Круглая, зелёная, полосатая", "арбуз"),
    Riddle("Висит груша — нельзя скушать", "лампочка"),
    Riddle("Сто одежек без застёжек", "капуста"),
    Riddle("Сидит дед во сто шуб одет", "лук"),
    Riddle("Жёлтый, кислый, к чаю", "лимон"),
    Riddle("Не лает, не кусает,\nв дом не пускает", "замок"),
    Riddle("Всегда идёт, не уходит", "часы"),
    Riddle("У него четыре ножки", "стол"),
    Riddle("На четырёх ножках отдыхает", "стул"),
    Riddle("Мягкая, на ней спят", "кровать"),
    Riddle("В ней варят суп", "кастрюля"),
    Riddle("Из неё пьют чай", "чашка"),
    Riddle("Ею едят суп", "ложка"),
    Riddle("Им режут хлеб", "нож"),
    Riddle("В неё смотрятся", "зеркало"),
    Riddle("Светит дома вечером", "лампа"),
    Riddle("Четыре колеса, везёт людей", "машина"),
    Riddle("Летает в небе с крыльями", "самолёт"),
    Riddle("Плывёт по морю", "корабль"),
    Riddle("Едет по рельсам", "поезд"),
    Riddle("На двух колёсах с педалями", "велосипед"),
    Riddle("Под землёй быстро едет", "метро"),
    Riddle("Возит людей по городу", "автобус"),
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

    Box(modifier = Modifier.fillMaxSize().background(WhiteBackground).statusBarsPadding().navigationBarsPadding().padding(ScreenPadding), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(state = kuzyaState, sizeDp = CharacterSize)
            Spacer(modifier = Modifier.height(CharacterSpacer))
            StageProgressIndicator(progress = stageProgress, currentStage = stage)
            Spacer(modifier = Modifier.height(ElementSpacer))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (stage) {
                    0 -> TypeRiddleGame(title = "Отгадай загадку!", onResult = { correct -> if (correct) earnedStars++; stage = 1 })
                    1 -> TypeRiddleGame(title = "Угадай животное!", onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> TypeRiddleGame(title = "Угадай предмет!", onResult = { correct -> if (correct) earnedStars++; if (earnedStars == 0) earnedStars = 1; kuzyaState = kuzyaState.copy(emotion = CharacterEmotion.CLAP); stage = 3 })
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
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(StageProgressHeight), color = FairyGold, trackColor = FairyBlue.copy(alpha = 0.3f))
    }
}

@Composable
private fun TypeRiddleGame(title: String, onResult: (Boolean) -> Unit) {
    var correctCount by remember { mutableIntStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    var currentRiddle by remember { mutableStateOf(AllRiddles.random()) }

    LaunchedEffect(correctCount) {
        if (correctCount >= CORRECT_NEEDED) {
            delay(500)
            onResult(true)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Правильно: $correctCount из $CORRECT_NEEDED", style = MaterialTheme.typography.bodyMedium.copy(fontSize = ProgressTextSize, fontWeight = FontWeight.Medium), color = FairyGold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))

        // Загадка
        Box(modifier = Modifier.fillMaxWidth().background(FairyPurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
            Text(text = currentRiddle.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = RiddleTextSize, fontWeight = FontWeight.Medium, lineHeight = 32.sp), color = FairyPurple, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Поле ввода
        OutlinedTextField(
            value = typedAnswer,
            onValueChange = { if (!isLocked) typedAnswer = it },
            modifier = Modifier.fillMaxWidth(0.85f),
            placeholder = { Text("Напиши ответ...", color = Color.Gray) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
            singleLine = true,
            enabled = !isLocked,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FairyPurple,
                unfocusedBorderColor = FairyBlue,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка "Проверить"
        Button(
            onClick = {
                val cleanAnswer = typedAnswer.trim().lowercase()
                val cleanCorrect = currentRiddle.answer.trim().lowercase()
                isCorrect = cleanAnswer == cleanCorrect
                isLocked = true
                showResult = true
                if (isCorrect) {
                    correctCount++
                    AudioPlayer.playSFX("correct")
                } else {
                    AudioPlayer.playSFX("wrong")
                }
                kotlinx.coroutines.MainScope().launch {
                    delay(RESULT_DELAY_MS)
                    if (correctCount < CORRECT_NEEDED) {
                        currentRiddle = AllRiddles.random()
                        typedAnswer = ""
                        isLocked = false
                        showResult = false
                    }
                }
            },
            enabled = typedAnswer.isNotBlank() && !isLocked,
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FairyGold)
        ) {
            Text("Проверить", style = MaterialTheme.typography.labelLarge, color = DarkText)
        }

        // Результат
        if (showResult) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isCorrect) "✓ Правильно!" else "✗ Ответ: ${currentRiddle.answer}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCorrect) FairyGreen else FairyPink,
                textAlign = TextAlign.Center
            )
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
            Text(text = when (earnedStars) { 3 -> "Отлично!"; 2 -> "Хорошо!"; else -> "Молодец!" }, style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Уровень пройден!", style = MaterialTheme.typography.bodyLarge, color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(CompleteButtonSpacer))
            Button(onClick = { AudioPlayer.playSFX("click"); onComplete() }, modifier = Modifier.fillMaxWidth(COMPLETE_BUTTON_WIDTH_FRACTION).height(CompleteButtonHeight), shape = RoundedCornerShape(ButtonCornerRadius), colors = ButtonDefaults.buttonColors(containerColor = FairyGreen, contentColor = DarkText), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) { Text("Далее →", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun StarDisplay(earnedStars: Int) {
    val maxStars = GameState.MAX_STARS_PER_LEVEL
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
