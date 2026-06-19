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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
private val ItemCellSize = 80.dp
private val ItemCornerRadius = 16.dp
private const val RESULT_DELAY_MS = 1200L
private val RiddleTextSize = 20.sp
private val ProgressTextSize = 16.sp

private data class RiddleData(
    val text: String,
    val images: List<Int>,
    val correctIndex: Int,
    val answerName: String
)

private val RiddlesBase = listOf(
    RiddleData("Зимой белый,\nлетом серый", listOf(R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_rabbit, R.drawable.item_ball), 2, "Заяц"),
    RiddleData("Кто мурлычет\nу окошка?", listOf(R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_rabbit, R.drawable.item_ball), 0, "Кошка"),
    RiddleData("Верный друг,\nвиляет хвостом", listOf(R.drawable.item_dog, R.drawable.item_cat, R.drawable.item_rabbit, R.drawable.item_car), 0, "Собака"),
    RiddleData("Длинные уши,\nлюбит морковку", listOf(R.drawable.item_rabbit, R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_apple), 0, "Кролик"),
    RiddleData("Кто говорит\n«му-у»?", listOf(R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_rabbit, R.drawable.item_ball), 1, "Корова"),
    RiddleData("Кто говорит\n«хрю-хрю»?", listOf(R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_rabbit, R.drawable.item_ball), 1, "Свинья"),
    RiddleData("Любит бананы,\nпрыгает ловко", listOf(R.drawable.item_banana, R.drawable.item_cat, R.drawable.item_dog, R.drawable.item_rabbit), 1, "Обезьяна"),
    RiddleData("Круглое, румяное,\nс дерева упало", listOf(R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_banana, R.drawable.item_ball), 0, "Яблоко"),
    RiddleData("Оранжевая, сладкая,\nлюбит зайчик", listOf(R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_banana, R.drawable.item_ball), 1, "Морковь"),
    RiddleData("Жёлтый, кислый,\nк чаю нужен", listOf(R.drawable.item_banana, R.drawable.item_orange, R.drawable.item_apple, R.drawable.item_ball), 1, "Лимон"),
    RiddleData("Сто одежек\nи все без застёжек", listOf(R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_banana, R.drawable.item_ball), 0, "Капуста"),
    RiddleData("Круглая, зелёная,\nполосатая", listOf(R.drawable.item_ball, R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_banana), 0, "Арбуз"),
    RiddleData("Четыре колеса,\nвезёт людей", listOf(R.drawable.item_car, R.drawable.item_plane, R.drawable.item_ball, R.drawable.item_phone), 0, "Машина"),
    RiddleData("Летает в небе\nс крыльями", listOf(R.drawable.item_plane, R.drawable.item_car, R.drawable.item_ball, R.drawable.item_phone), 0, "Самолёт"),
    RiddleData("Круглый, резиновый,\nскачет", listOf(R.drawable.item_ball, R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_car), 0, "Мяч"),
    RiddleData("По нему говорят\nс друзьями", listOf(R.drawable.item_phone, R.drawable.item_ball, R.drawable.item_car, R.drawable.item_plane), 0, "Телефон"),
    RiddleData("Не лает, не кусает,\nа в дом не пускает", listOf(R.drawable.icon_lock, R.drawable.item_dog, R.drawable.item_cat, R.drawable.item_ball), 0, "Замок"),
    RiddleData("Висит груша —\nнельзя скушать", listOf(R.drawable.item_phone, R.drawable.item_apple, R.drawable.item_banana, R.drawable.item_orange), 0, "Лампочка"),
    RiddleData("Всегда идёт,\nа не уходит", listOf(R.drawable.item_ball, R.drawable.item_car, R.drawable.item_phone, R.drawable.item_apple), 2, "Часы"),
    RiddleData("Зимой и летом\nодним цветом", listOf(R.drawable.item_flower_rose, R.drawable.item_flower_tulip, R.drawable.item_flower_sunflower, R.drawable.item_apple), 0, "Роза"),
    RiddleData("Красная, красивая,\nв саду растёт", listOf(R.drawable.item_flower_rose, R.drawable.item_flower_tulip, R.drawable.item_flower_sunflower, R.drawable.item_car), 0, "Роза"),
    RiddleData("Жёлтый цветок\nповорачивается к солнцу", listOf(R.drawable.item_flower_sunflower, R.drawable.item_flower_rose, R.drawable.item_flower_tulip, R.drawable.item_banana), 0, "Подсолнух"),
    RiddleData("На ногах,\nзащищают от грязи", listOf(R.drawable.item_boot, R.drawable.item_shoe, R.drawable.item_heels, R.drawable.item_ball), 0, "Сапоги"),
    RiddleData("Красивые,\nна каблучке", listOf(R.drawable.item_heels, R.drawable.item_boot, R.drawable.item_shoe, R.drawable.item_ball), 0, "Туфельки"),
    RiddleData("Носят на ногах\nв спортзале", listOf(R.drawable.item_shoe, R.drawable.item_boot, R.drawable.item_heels, R.drawable.item_ball), 0, "Кроссовки"),
    RiddleData("Бросают в кольцо\nна площадке", listOf(R.drawable.item_basketball, R.drawable.item_ball, R.drawable.item_tennis, R.drawable.item_apple), 0, "Баскетбол"),
    RiddleData("Бьют ракеткой\nчерез сетку", listOf(R.drawable.item_tennis, R.drawable.item_ball, R.drawable.item_basketball, R.drawable.item_car), 0, "Теннис"),
    RiddleData("Играют ногами,\nкруглый мяч", listOf(R.drawable.item_ball, R.drawable.item_basketball, R.drawable.item_tennis, R.drawable.item_apple), 0, "Футбол"),
    RiddleData("Струнный инструмент,\nиграют пальцами", listOf(R.drawable.item_guitar, R.drawable.item_ball, R.drawable.item_phone, R.drawable.item_car), 0, "Гитара"),
    RiddleData("Из неё едят суп,\nглубокая", listOf(R.drawable.item_plate, R.drawable.item_glass, R.drawable.item_fork, R.drawable.item_juice), 0, "Тарелка"),
    RiddleData("Из неё пьют\nводу и сок", listOf(R.drawable.item_glass, R.drawable.item_plate, R.drawable.item_fork, R.drawable.item_juice), 0, "Стакан"),
    RiddleData("Ею едят\nкотлету", listOf(R.drawable.item_fork, R.drawable.item_plate, R.drawable.item_glass, R.drawable.item_juice), 0, "Вилка"),
    RiddleData("Вкусный,\nс сыром и томатом", listOf(R.drawable.item_pizza, R.drawable.item_apple, R.drawable.item_orange, R.drawable.item_banana), 0, "Пицца"),
)

private val AnimalBase = RiddlesBase.filter { it.answerName in listOf("Заяц", "Кошка", "Собака", "Кролик", "Корова", "Свинья", "Обезьяна") }
private val ItemBase = RiddlesBase.filter { it.answerName !in listOf("Заяц", "Кошка", "Собака", "Кролик", "Корова", "Свинья", "Обезьяна", "Яблоко", "Морковь", "Лимон", "Капуста", "Арбуз") }

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
                    0 -> RiddleGame(pool = RiddlesBase, title = "Отгадай загадку!", onResult = { correct -> if (correct) earnedStars++; stage = 1 })
                    1 -> RiddleGame(pool = AnimalBase, title = "Угадай животное!", onResult = { correct -> if (correct) earnedStars++; stage = 2 })
                    2 -> RiddleGame(pool = ItemBase, title = "Угадай предмет!", onResult = { correct -> if (correct) earnedStars++; if (earnedStars == 0) earnedStars = 1; kuzyaState = kuzyaState.copy(emotion = CharacterEmotion.CLAP); stage = 3 })
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
private fun RiddleGame(pool: List<RiddleData>, title: String, onResult: (Boolean) -> Unit) {
    var currentRiddle by remember { mutableStateOf(pool.random()) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var isCorrectAnswer by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val options = remember(currentRiddle) { currentRiddle.images }

    LaunchedEffect(correctCount) { if (correctCount >= CORRECT_NEEDED) { delay(500); onResult(true) } }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Правильно: $correctCount из $CORRECT_NEEDED", style = MaterialTheme.typography.bodyMedium.copy(fontSize = ProgressTextSize, fontWeight = FontWeight.Medium), color = FairyGold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).background(FairyPurple.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = currentRiddle.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = RiddleTextSize, fontWeight = FontWeight.Medium, lineHeight = 28.sp), color = FairyPurple, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(20.dp))

        // ИСПРАВЛЕНО: убран weight, используется fillMaxWidth внутри Row через weight в GameImageCell
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, resId ->
                val isSelected = selectedIndex == index
                val isCorrectOption = index == currentRiddle.correctIndex
                // ИСПРАВЛЕНО: передаём modifier с weight внутрь RowScope
                GameImageCell(
                    modifier = Modifier.weight(1f),
                    resId = resId, isSelected = isSelected, isCorrect = isCorrectOption, isLocked = isLocked,
                    onClick = {
                        if (!isLocked) {
                            selectedIndex = index; isLocked = true; isCorrectAnswer = isCorrectOption; showResult = true
                            if (isCorrectOption) { correctCount++; AudioPlayer.playSFX("correct") } else { AudioPlayer.playSFX("wrong") }
                            coroutineScope.launch {
                                delay(RESULT_DELAY_MS)
                                if (correctCount < CORRECT_NEEDED) {
                                    currentRiddle = pool.filter { it != currentRiddle }.random()
                                    selectedIndex = -1; isLocked = false; showResult = false
                                }
                            }
                        }
                    }
                )
            }
        }

        if (showResult && !isCorrectAnswer) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Правильный ответ: ${currentRiddle.answerName}", style = MaterialTheme.typography.bodyMedium, color = FairyGreen, textAlign = TextAlign.Center)
        }
    }
}

// ИСПРАВЛЕНО: modifier передаётся из RowScope, weight работает
@Composable
private fun GameImageCell(
    modifier: Modifier = Modifier,
    resId: Int, isSelected: Boolean, isCorrect: Boolean, isLocked: Boolean, onClick: () -> Unit
) {
    val bgColor by animateColorAsState(targetValue = when { isSelected && isCorrect -> FairyGreen; isSelected && !isCorrect -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrect -> FairyGreen.copy(alpha = 0.3f); else -> FairyBlue }, animationSpec = tween(300), label = "CellBg")
    val borderColor by animateColorAsState(targetValue = when { isSelected -> FairyGold; isLocked && isCorrect && !isSelected -> FairyGreen; else -> Color.Transparent }, animationSpec = tween(300), label = "CellBorder")
    val elevation = if (isSelected) 10.dp else 6.dp

    Box(
        modifier = modifier.size(ItemCellSize).shadow(elevation, RoundedCornerShape(ItemCornerRadius)).background(bgColor, RoundedCornerShape(ItemCornerRadius))
            .then(if (borderColor != Color.Transparent) Modifier.border(3.dp, borderColor, RoundedCornerShape(ItemCornerRadius)) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.size(52.dp), contentScale = ContentScale.Fit)
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
