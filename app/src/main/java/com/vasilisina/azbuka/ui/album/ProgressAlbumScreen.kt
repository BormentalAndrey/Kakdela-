// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/album/ProgressAlbumScreen.kt

package com.vasilisina.azbuka.ui.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground

private val ScreenPadding = 16.dp
private val TitleVerticalPadding = 16.dp
private val CardSpacing = 12.dp
private val LazyColumnVerticalPadding = 8.dp
private val CardInnerPadding = 16.dp
private val BackButtonHeight = 60.dp
private const val BACK_BUTTON_WIDTH_FRACTION = 0.6f
private val BackButtonTopSpacer = 16.dp
private val CardCornerRadius = 16.dp
private val CardElevation = 4.dp
private val CardPressedElevation = 8.dp
private const val CARD_ANIMATION_DURATION_MS = 400
private const val CARD_STAGGER_DELAY_MS = 100L
private val ProgressBarHeight = 12.dp
private val TotalStarsFontSize = 18.sp

private data class AlbumLevel(val level: Int, val title: String)

@Composable
fun ProgressAlbumScreen(onBack: () -> Unit) {
    val levels = remember { listOf(AlbumLevel(1, "Алфавит"), AlbumLevel(2, "Счёт"), AlbumLevel(3, "Печать"), AlbumLevel(4, "Логика"), AlbumLevel(5, "Финал")) }
    val totalStars = GameState.getTotalStars()
    val maxStars = GameState.TOTAL_MAX_STARS
    val completionPercent = GameState.getCompletionPercent()

    Column(modifier = Modifier.fillMaxSize().background(WhiteBackground).statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing).navigationBarsPadding().padding(ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        AlbumTitle()
        OverallProgress(totalStars = totalStars, maxStars = maxStars, completionPercent = completionPercent)
        Spacer(modifier = Modifier.height(TitleVerticalPadding))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CardSpacing), contentPadding = PaddingValues(vertical = LazyColumnVerticalPadding)) {
            itemsIndexed(items = levels, key = { _, item -> item.level }) { index, item ->
                AnimatedLevelCard(level = item, index = index, stars = GameState.getStars(item.level), isUnlocked = GameState.isLevelUnlocked(item.level))
            }
        }
        Spacer(modifier = Modifier.height(BackButtonTopSpacer))
        BackButton(onClick = { AudioPlayer.playSFX("click"); onBack() })
    }
}

@Composable
private fun AlbumTitle() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = TitleVerticalPadding), contentAlignment = Alignment.Center) {
        Text(text = "Альбом успехов", style = MaterialTheme.typography.headlineLarge.copy(brush = Brush.linearGradient(colors = listOf(FairyPurple, FairyGold, FairyPink))), textAlign = TextAlign.Center)
    }
}

@Composable
private fun OverallProgress(totalStars: Int, maxStars: Int, completionPercent: Int) {
    var showProgress by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showProgress = true }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = "Собрано $totalStars из $maxStars звёзд", style = MaterialTheme.typography.bodyLarge.copy(fontSize = TotalStarsFontSize, fontWeight = FontWeight.Medium), color = DarkText)
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(visible = showProgress) {
            // ИСПРАВЛЕНО: progress = Float, не лямбда
            LinearProgressIndicator(
                progress = completionPercent / 100f,
                modifier = Modifier.fillMaxWidth(0.8f).height(ProgressBarHeight),
                color = FairyGold,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$completionPercent%", style = MaterialTheme.typography.bodySmall, color = DarkText.copy(alpha = 0.6f))
    }
}

@Composable
private fun AnimatedLevelCard(level: AlbumLevel, index: Int, stars: Int, isUnlocked: Boolean) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(CARD_STAGGER_DELAY_MS * (index + 1)); isVisible = true }
    AnimatedVisibility(visible = isVisible, enter = fadeIn(animationSpec = tween(CARD_ANIMATION_DURATION_MS)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) {
        LevelProgressCard(name = level.title, level = level.level, stars = stars, isUnlocked = isUnlocked)
    }
}

@Composable
private fun LevelProgressCard(name: String, level: Int, stars: Int, isUnlocked: Boolean) {
    val safeStars = stars.coerceIn(0, GameState.MAX_STARS_PER_LEVEL)
    val cardColor by animateColorAsState(targetValue = if (isUnlocked) Color.White else Color.LightGray.copy(alpha = 0.30f), animationSpec = tween(300), label = "Card")
    val starsColor by animateColorAsState(targetValue = if (isUnlocked) FairyGold else Color.Gray, animationSpec = tween(300), label = "Stars")
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardCornerRadius), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)) {
        Row(modifier = Modifier.fillMaxWidth().padding(CardInnerPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "$name (Ур. $level)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (safeStars > 0) FontWeight.Bold else FontWeight.Normal), color = if (isUnlocked) DarkText else Color.Gray, modifier = Modifier.weight(1f))
            Text(text = if (isUnlocked) buildStarString(safeStars, GameState.MAX_STARS_PER_LEVEL) else "🔒", style = MaterialTheme.typography.headlineMedium, color = starsColor)
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(BACK_BUTTON_WIDTH_FRACTION).height(BackButtonHeight), shape = RoundedCornerShape(CardCornerRadius), colors = ButtonDefaults.buttonColors(containerColor = FairyPurple, contentColor = Color.White), elevation = ButtonDefaults.buttonElevation(defaultElevation = CardElevation, pressedElevation = CardPressedElevation, focusedElevation = CardElevation, hoveredElevation = CardElevation, disabledElevation = 0.dp)) {
        Text(text = "Назад", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

private fun buildStarString(filled: Int, total: Int): String {
    return buildString { repeat(filled) { append('★') }; repeat(total - filled) { append('☆') } }
}
