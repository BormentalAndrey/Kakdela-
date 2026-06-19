// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/final/FinalSceneScreen.kt

package com.vasilisina.azbuka.ui.levels.final

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.vasilisina.azbuka.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val TableItems = listOf(R.drawable.item_plate, R.drawable.item_glass, R.drawable.item_fork, R.drawable.item_juice)
private const val TABLE_SLOTS_COUNT = 4
private val TableSlotSize = 64.dp
private val TableItemSize = 56.dp
private val TableCornerRadius = 10.dp
private val TableSlotSpacing = 10.dp
private val TableItemSpacing = 8.dp
private val ItemsTopSpacer = 20.dp
private val TitleTopSpacer = 16.dp
private const val TABLE_COMPLETE_DELAY_MS = 800L
private const val FIREWORKS_DURATION_MS = 3500L
private const val FIREWORKS_PARTICLE_COUNT = 20
private const val FINAL_MAX_STARS = 3
private const val APPEAR_DURATION_MS = 400
private const val STAGGER_DELAY_MS = 50L
private val FinalTitleFontSize = 32.sp
private val AlbumStarFontSize = 18.sp
private val SlotElevation = 3.dp
private val ItemElevation = 3.dp
private val ItemSelectedElevation = 5.dp
private val ItemImageSize = 36.dp

private data class FireworkParticle(
    val color: Color, val x: Float, val y: Float,
    val targetX: Float, val targetY: Float, val delay: Long, val size: Float
)

@Composable
fun FinalSceneScreen(level: Int = 6, onComplete: (stars: Int) -> Unit) {
    val context = LocalContext.current
    var stage by remember { mutableIntStateOf(0) }
    val earnedStars = FINAL_MAX_STARS
    var vasilisaState by remember { mutableStateOf(CharacterState("Василиса", CharacterEmotion.HAPPY)) }
    var kuzyaState by remember { mutableStateOf(CharacterState("Кузя", CharacterEmotion.HAPPY)) }

    DisposableEffect(Unit) {
        try { AudioPlayer.playMusic(context, R.raw.music_final, loop = true) } catch (_: Exception) { }
        onDispose { AudioPlayer.stopMusic() }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Image(painter = painterResource(id = R.drawable.bg_level_5_final), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))

        when (stage) {
            0 -> TableSettingGame(onComplete = { vasilisaState = vasilisaState.clap(); kuzyaState = kuzyaState.clap(); stage = 1 })
            1 -> FireworksAnimation(onDone = { stage = 2 })
            2 -> FinalAlbum(vasilisaState = vasilisaState, kuzyaState = kuzyaState, onDone = { GameState.completeLevel(level, earnedStars); onComplete(earnedStars) })
        }
    }
}

@Composable
private fun TableSettingGame(onComplete: () -> Unit) {
    val items = remember { TableItems }
    val slots = remember { mutableStateListOf<Int?>(null, null, null, null) }
    var selectedItemIndex by remember { mutableIntStateOf(-1) }
    val allPlaced = slots.all { it != null }
    LaunchedEffect(allPlaced) { if (allPlaced) { AudioPlayer.playSFX("correct"); delay(TABLE_COMPLETE_DELAY_MS); onComplete() } }
    var showElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showElements = true }

    Column(modifier = Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = "Накрой на стол!", style = MaterialTheme.typography.headlineLarge.copy(fontSize = FinalTitleFontSize, fontWeight = FontWeight.Bold), color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(TitleTopSpacer))
        Text(text = if (allPlaced) "Отлично! Стол накрыт." else if (selectedItemIndex >= 0) "Выбери место для предмета" else "Выбери предмет и помести его на стол", style = MaterialTheme.typography.bodyMedium, color = DarkText.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(TableSlotSpacing)) {
            slots.forEachIndexed { index, itemRes ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(STAGGER_DELAY_MS * index); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(APPEAR_DURATION_MS))) {
                    TableSlot(itemRes = itemRes, isHighlighted = selectedItemIndex >= 0 && itemRes == null, onClick = { if (selectedItemIndex >= 0 && itemRes == null) { slots[index] = items[selectedItemIndex]; selectedItemIndex = -1 } })
                }
            }
        }

        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(TableItemSpacing)) {
            items.forEachIndexed { index, itemRes ->
                val isUsed = slots.contains(itemRes); val isSelected = selectedItemIndex == index
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(STAGGER_DELAY_MS * (TABLE_SLOTS_COUNT + index)); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(APPEAR_DURATION_MS))) {
                    TableItem(itemRes = itemRes, isUsed = isUsed, isSelected = isSelected, onClick = { if (!isUsed) selectedItemIndex = if (isSelected) -1 else index })
                }
            }
        }
    }
}

@Composable
private fun TableSlot(itemRes: Int?, isHighlighted: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = when { itemRes != null -> FairyGold.copy(alpha = 0.2f); isHighlighted -> FairyGold.copy(alpha = 0.3f); else -> FairyBlue.copy(alpha = 0.3f) }, animationSpec = tween(300), label = "SlotBg")
    val borderColor by animateColorAsState(targetValue = when { itemRes != null -> FairyGold; isHighlighted -> FairyPurple; else -> FairyBlue }, animationSpec = tween(300), label = "SlotBorder")
    Box(modifier = Modifier.size(TableSlotSize).shadow(SlotElevation, RoundedCornerShape(TableCornerRadius)).clip(RoundedCornerShape(TableCornerRadius)).background(bgColor).border(2.dp, borderColor, RoundedCornerShape(TableCornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        if (itemRes != null) Image(painter = painterResource(id = itemRes), contentDescription = "Предмет", modifier = Modifier.size(ItemImageSize), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun TableItem(itemRes: Int, isUsed: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = when { isUsed -> Color.LightGray.copy(alpha = 0.4f); isSelected -> FairyGold; else -> FairyGreen }, animationSpec = tween(300), label = "ItemBg")
    val elevation = when { isUsed -> 0.dp; isSelected -> ItemSelectedElevation; else -> ItemElevation }
    Box(modifier = Modifier.size(TableItemSize).shadow(elevation, RoundedCornerShape(TableCornerRadius)).clip(RoundedCornerShape(TableCornerRadius)).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isUsed, onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = itemRes), contentDescription = "Предмет", modifier = Modifier.size(ItemImageSize), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun FireworksAnimation(onDone: () -> Unit) {
    var showFireworks by remember { mutableStateOf(false) }
    val particles = remember {
        val colors = listOf(FairyGold, FairyPink, FairyBlue, FairyGreen, FairyPurple)
        List(FIREWORKS_PARTICLE_COUNT) { val angle = Random.nextFloat() * 360f; val distance = Random.nextFloat() * 100f + 60f; val rad = Math.toRadians(angle.toDouble()); FireworkParticle(colors.random(), 0f, 0f, (cos(rad) * distance).toFloat(), (sin(rad) * distance).toFloat(), Random.nextLong(300), Random.nextFloat() * 8f + 3f) }
    }
    val titleScale by animateFloatAsState(targetValue = if (showFireworks) 1f else 0f, animationSpec = tween(800), label = "TitleScale")
    val pulseScale by animateFloatAsState(targetValue = if (showFireworks) 1.05f else 0f, animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse), label = "TitlePulse")
    LaunchedEffect(Unit) { showFireworks = true; delay(FIREWORKS_DURATION_MS); onDone() }

    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(FairyPurple.copy(alpha = 0.3f), Color.Transparent))), contentAlignment = Alignment.Center) {
        particles.forEach { particle ->
            var particleVisible by remember { mutableStateOf(false) }; var currentX by remember { mutableStateOf(0f) }; var currentY by remember { mutableStateOf(0f) }
            LaunchedEffect(showFireworks) { if (showFireworks) { delay(particle.delay); particleVisible = true; val steps = 15; for (i in 1..steps) { currentX = particle.targetX * (i.toFloat() / steps); currentY = particle.targetY * (i.toFloat() / steps); delay(16) }; delay(400); particleVisible = false } }
            AnimatedVisibility(visible = particleVisible, enter = fadeIn(tween(150)), exit = fadeOut(tween(400))) { Box(modifier = Modifier.offset { IntOffset(currentX.toInt(), currentY.toInt()) }.size(particle.size.dp).background(particle.color, CircleShape)) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = titleScale * pulseScale; scaleY = titleScale * pulseScale }) {
            Text(text = "🎉", fontSize = 40.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "ПОЗДРАВЛЯЕМ!", style = MaterialTheme.typography.headlineLarge.copy(fontSize = FinalTitleFontSize, fontWeight = FontWeight.Bold), color = FairyGold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Ты прошёл все уровни!", style = MaterialTheme.typography.bodyLarge, color = DarkText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FinalAlbum(vasilisaState: CharacterState, kuzyaState: CharacterState, onDone: () -> Unit) {
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedVisibility(visible = showContent, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(500))) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { CharacterView(state = vasilisaState, sizeDp = 90); CharacterView(state = kuzyaState, sizeDp = 90) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = showContent, enter = fadeIn(tween(600, delayMillis = 300))) {
            Text(text = "Твой альбом успехов", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(12.dp))

        (1..GameState.MAX_LEVELS).forEach { level ->
            val stars = GameState.getStars(level); val isUnlocked = GameState.isLevelUnlocked(level)
            var rowVisible by remember { mutableStateOf(false) }
            LaunchedEffect(showContent) { if (showContent) { delay(STAGGER_DELAY_MS * level + 400); rowVisible = true } }
            AnimatedVisibility(visible = rowVisible, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = spring())) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Уровень $level", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isUnlocked) FontWeight.Medium else FontWeight.Normal), color = if (isUnlocked) DarkText else Color.Gray)
                    Text(text = if (isUnlocked) buildString { repeat(stars) { append('★') }; repeat(GameState.MAX_STARS_PER_LEVEL - stars) { append('☆') } } else "🔒", fontSize = AlbumStarFontSize, color = if (isUnlocked) FairyGold else Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        AnimatedVisibility(visible = showContent, enter = fadeIn(tween(400, delayMillis = 800))) {
            Button(onClick = { AudioPlayer.playSFX("click"); onDone() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPurple, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)) { Text("Завершить", style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        }
    }
}
