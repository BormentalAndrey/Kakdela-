// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/final/FinalSceneScreen.kt

package com.vasilisina.azbuka.ui.levels.final

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

private val TableItems = listOf("🍽️", "🥛", "🍴", "🧃")
private const val TABLE_SLOTS_COUNT = 4
private val TableSlotSize = 80.dp
private val TableItemSize = 70.dp
private val TableCornerRadius = 12.dp
private val TableSlotSpacing = 16.dp
private val TableItemSpacing = 12.dp
private val ItemsTopSpacer = 40.dp
private val TitleTopSpacer = 32.dp
private const val TABLE_COMPLETE_DELAY_MS = 800L
private const val FIREWORKS_DURATION_MS = 4000L
private const val FIREWORKS_PARTICLE_COUNT = 30
private const val FINAL_MAX_STARS = 3
private const val APPEAR_DURATION_MS = 400
private const val STAGGER_DELAY_MS = 80L
private val FinalTitleFontSize = 48.sp
private val TableItemFontSize = 32.sp
private val AlbumStarFontSize = 24.sp
private val SlotElevation = 4.dp
private val ItemElevation = 4.dp
private val ItemSelectedElevation = 8.dp

// -------------------------------------------------------------------------
// Модель
// -------------------------------------------------------------------------

private data class FireworkParticle(
    val color: Color, val x: Float, val y: Float,
    val targetX: Float, val targetY: Float, val delay: Long, val size: Float
)

// -------------------------------------------------------------------------
// Главный экран
// -------------------------------------------------------------------------

@Composable
fun FinalSceneScreen(level: Int = 5, onComplete: (stars: Int) -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    val earnedStars = FINAL_MAX_STARS

    var vasilisaState by remember { mutableStateOf(CharacterState("Василиса", CharacterEmotion.HAPPY)) }
    var kuzyaState by remember { mutableStateOf(CharacterState("Кузя", CharacterEmotion.HAPPY)) }

    // ИСПРАВЛЕНО: try-catch на случай отсутствия файла
    DisposableEffect(Unit) {
        try {
            AudioPlayer.playMusic(context, R.raw.music_final, loop = true)
        } catch (_: Exception) { }
        onDispose { AudioPlayer.stopMusic() }
    }

    Box(modifier = Modifier.fillMaxSize().background(WhiteBackground).statusBarsPadding().navigationBarsPadding()) {
        when (stage) {
            0 -> TableSettingGame(onComplete = { vasilisaState = vasilisaState.copy(emotion = CharacterEmotion.CLAP); kuzyaState = kuzyaState.copy(emotion = CharacterEmotion.CLAP); stage = 1 })
            1 -> FireworksAnimation(onDone = { stage = 2 })
            2 -> FinalAlbum(vasilisaState = vasilisaState, kuzyaState = kuzyaState, onDone = { GameState.completeLevel(level, earnedStars); onComplete(earnedStars) })
        }
    }
}

// -------------------------------------------------------------------------
// Сервировка
// -------------------------------------------------------------------------

@Composable
private fun TableSettingGame(onComplete: () -> Unit) {
    val items = remember { TableItems }
    val slots = remember { mutableStateListOf<String?>(null, null, null, null) }
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val allPlaced = slots.all { it != null }

    LaunchedEffect(allPlaced) { if (allPlaced) { AudioPlayer.playSFX("correct"); delay(TABLE_COMPLETE_DELAY_MS); onComplete() } }

    var showElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showElements = true }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Накрой на стол!", style = MaterialTheme.typography.headlineLarge.copy(fontSize = FinalTitleFontSize, fontWeight = FontWeight.Bold), color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(TitleTopSpacer))
        Text(text = if (allPlaced) "Отлично! Стол накрыт." else if (selectedItemIndex >= 0) "Выбери место для предмета" else "Выбери предмет и помести его на стол", style = MaterialTheme.typography.bodyMedium, color = DarkText.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(TableSlotSpacing)) {
            slots.forEachIndexed { index, value ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(STAGGER_DELAY_MS * index); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(APPEAR_DURATION_MS))) {
                    TableSlot(item = value, isHighlighted = selectedItemIndex >= 0 && value == null, onClick = { if (selectedItemIndex >= 0 && value == null) { slots[index] = items[selectedItemIndex]; selectedItemIndex = -1 } })
                }
            }
        }

        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(TableItemSpacing)) {
            items.forEachIndexed { index, item ->
                val isUsed = slots.contains(item)
                val isSelected = selectedItemIndex == index
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(STAGGER_DELAY_MS * (TABLE_SLOTS_COUNT + index)); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(APPEAR_DURATION_MS))) {
                    TableItem(item = item, isUsed = isUsed, isSelected = isSelected, onClick = { if (!isUsed) selectedItemIndex = if (isSelected) -1 else index })
                }
            }
        }
    }
}

@Composable
private fun TableSlot(item: String?, isHighlighted: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = when { item != null -> FairyGold.copy(alpha = 0.2f); isHighlighted -> FairyGold.copy(alpha = 0.3f); else -> FairyBlue.copy(alpha = 0.3f) }, animationSpec = tween(300), label = "SlotBg")
    val borderColor by animateColorAsState(targetValue = when { item != null -> FairyGold; isHighlighted -> FairyPurple; else -> FairyBlue }, animationSpec = tween(300), label = "SlotBorder")
    Box(modifier = Modifier.size(TableSlotSize).shadow(SlotElevation, RoundedCornerShape(TableCornerRadius)).clip(RoundedCornerShape(TableCornerRadius)).background(bgColor).border(2.dp, borderColor, RoundedCornerShape(TableCornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = item ?: "", fontSize = TableItemFontSize, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TableItem(item: String, isUsed: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = when { isUsed -> Color.LightGray.copy(alpha = 0.4f); isSelected -> FairyGold; else -> FairyGreen }, animationSpec = tween(300), label = "ItemBg")
    val textColor by animateColorAsState(targetValue = when { isUsed -> Color.White.copy(alpha = 0.3f); isSelected -> DarkText; else -> Color.White }, animationSpec = tween(300), label = "ItemText")
    val elevation = when { isUsed -> 0.dp; isSelected -> ItemSelectedElevation; else -> ItemElevation }
    Box(modifier = Modifier.size(TableItemSize).shadow(elevation, RoundedCornerShape(TableCornerRadius)).clip(RoundedCornerShape(TableCornerRadius)).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isUsed, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = item, fontSize = TableItemFontSize, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.Center)
    }
}

// -------------------------------------------------------------------------
// Салют
// -------------------------------------------------------------------------

@Composable
private fun FireworksAnimation(onDone: () -> Unit) {
    var showFireworks by remember { mutableStateOf(false) }

    val particles = remember {
        val colors = listOf(FairyGold, FairyPink, FairyBlue, FairyGreen, FairyPurple)
        List(FIREWORKS_PARTICLE_COUNT) {
            val angle = Random.nextFloat() * 360f
            val distance = Random.nextFloat() * 150f + 100f
            val rad = Math.toRadians(angle.toDouble())
            FireworkParticle(color = colors.random(), x = 0f, y = 0f, targetX = (cos(rad) * distance).toFloat(), targetY = (sin(rad) * distance).toFloat(), delay = Random.nextLong(500), size = Random.nextFloat() * 12f + 6f)
        }
    }

    val titleScale by animateFloatAsState(targetValue = if (showFireworks) 1f else 0f, animationSpec = tween(1000), label = "TitleScale")
    // ИСПРАВЛЕНО: добавлен импорт RepeatMode
    val pulseScale by animateFloatAsState(targetValue = if (showFireworks) 1.05f else 0f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "TitlePulse")

    LaunchedEffect(Unit) { showFireworks = true; delay(FIREWORKS_DURATION_MS); onDone() }

    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(FairyPurple.copy(alpha = 0.3f), FairyBlue.copy(alpha = 0.15f), WhiteBackground))), contentAlignment = Alignment.Center) {
        particles.forEach { particle ->
            var particleVisible by remember { mutableStateOf(false) }
            var currentX by remember { mutableStateOf(0f) }
            var currentY by remember { mutableStateOf(0f) }
            LaunchedEffect(showFireworks) {
                if (showFireworks) {
                    delay(particle.delay); particleVisible = true
                    val steps = 20; for (i in 1..steps) { currentX = particle.targetX * (i.toFloat() / steps); currentY = particle.targetY * (i.toFloat() / steps); delay(16) }
                    delay(500); particleVisible = false
                }
            }
            AnimatedVisibility(visible = particleVisible, enter = fadeIn(tween(200)), exit = fadeOut(tween(500))) {
                Box(modifier = Modifier.offset { IntOffset(currentX.toInt(), currentY.toInt()) }.size(particle.size.dp).background(particle.color, CircleShape))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = titleScale * pulseScale; scaleY = titleScale * pulseScale }) {
            Text(text = "🎉", fontSize = 64.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "ПОЗДРАВЛЯЕМ!", style = MaterialTheme.typography.headlineLarge.copy(fontSize = FinalTitleFontSize, fontWeight = FontWeight.Bold), color = FairyGold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Ты прошёл все уровни!", style = MaterialTheme.typography.bodyLarge, color = DarkText, textAlign = TextAlign.Center)
        }
    }
}

// -------------------------------------------------------------------------
// Альбом
// -------------------------------------------------------------------------

@Composable
private fun FinalAlbum(vasilisaState: CharacterState, kuzyaState: CharacterState, onDone: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedVisibility(visible = showContent, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(600))) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { CharacterView(state = vasilisaState, sizeDp = 120); CharacterView(state = kuzyaState, sizeDp = 120) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(visible = showContent, enter = fadeIn(tween(800, delayMillis = 400))) {
            Text(text = "Твой альбом успехов", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(24.dp))

        (1..GameState.MAX_LEVELS).forEach { level ->
            val stars = GameState.getStars(level)
            val isUnlocked = GameState.isLevelUnlocked(level)
            var rowVisible by remember { mutableStateOf(false) }
            LaunchedEffect(showContent) { if (showContent) { delay(STAGGER_DELAY_MS * level + 600); rowVisible = true } }
            AnimatedVisibility(visible = rowVisible, enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = spring())) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Уровень $level", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isUnlocked) FontWeight.Medium else FontWeight.Normal), color = if (isUnlocked) DarkText else Color.Gray)
                    Text(text = if (isUnlocked) buildString { repeat(stars) { append('★') }; repeat(GameState.MAX_STARS_PER_LEVEL - stars) { append('☆') } } else "🔒", fontSize = AlbumStarFontSize, color = if (isUnlocked) FairyGold else Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        AnimatedVisibility(visible = showContent, enter = fadeIn(tween(600, delayMillis = 1200))) {
            Button(onClick = { AudioPlayer.playSFX("click"); onDone() }, modifier = Modifier.fillMaxWidth(0.6f).height(60.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPurple, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)) {
                Text("Завершить игру", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
