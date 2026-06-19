// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/menu/MainMenuScreen.kt

package com.vasilisina.azbuka.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

private val BackgroundColors = listOf(FairyBlue, FairyPink, FairyGold, FairyPurple)
private const val BACKGROUND_COLOR_DURATION_MS = 1500
private const val BACKGROUND_COLOR_INTERVAL_MS = 3000L
private const val MENU_ENTRANCE_DURATION_MS = 600
private const val PULSE_DURATION_MS = 2500
private const val PULSE_MIN_ALPHA = 0.95f
private const val PULSE_MAX_ALPHA = 1f
private const val BUTTON_WIDTH_FRACTION = 0.8f
private val ButtonHeight = 72.dp
private val HorizontalPadding = 32.dp
private val VerticalSpacing = 24.dp
private val TopSpacerHeight = 40.dp
private val ButtonCornerRadius = 16.dp
private val ButtonDefaultElevation = 6.dp
private val ButtonPressedElevation = 10.dp
private val ButtonFocusedElevation = 8.dp

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onAlbum: () -> Unit, onQuit: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        try { AudioPlayer.playMusic(context = context, resId = R.raw.music_main, loop = true) } catch (_: Exception) { }
    }
    var colorIndex by remember { mutableIntStateOf(0) }
    var isMenuVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); isMenuVisible = true }
    LaunchedEffect(Unit) { while (true) { delay(BACKGROUND_COLOR_INTERVAL_MS); colorIndex = (colorIndex + 1) % BackgroundColors.size } }
    val backgroundColor by animateColorAsState(targetValue = BackgroundColors[colorIndex], animationSpec = tween(BACKGROUND_COLOR_DURATION_MS, easing = LinearEasing), label = "Bg")
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = PULSE_MIN_ALPHA, targetValue = PULSE_MAX_ALPHA, animationSpec = infiniteRepeatable(animation = tween(PULSE_DURATION_MS), repeatMode = RepeatMode.Reverse), label = "Alpha")
    val backgroundBrush = Brush.verticalGradient(colors = listOf(backgroundColor, WhiteBackground))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = isMenuVisible, enter = fadeIn(tween(MENU_ENTRANCE_DURATION_MS)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(MENU_ENTRANCE_DURATION_MS)) + scaleIn(initialScale = 0.9f, animationSpec = tween(MENU_ENTRANCE_DURATION_MS)), exit = fadeOut(tween(300))) {
            MenuContent(alpha = pulseAlpha, onPlay = onPlay, onAlbum = onAlbum, onQuit = onQuit)
        }
    }
}

@Composable
private fun MenuContent(alpha: Float, onPlay: () -> Unit, onAlbum: () -> Unit, onQuit: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding).alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
        Text(text = "Василисина азбука", style = MaterialTheme.typography.headlineLarge, color = DarkText, textAlign = TextAlign.Center)
        Text(text = "Путешествие по России", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(TopSpacerHeight))
        MainMenuButton(text = "Играть", color = FairyPurple, onClick = { AudioPlayer.playSFX("click"); onPlay() })
        MainMenuButton(text = "Альбом успехов", color = FairyGreen, onClick = { AudioPlayer.playSFX("click"); onAlbum() })
        MainMenuButton(text = "Выход", color = FairyPink, onClick = { AudioPlayer.playSFX("click"); onQuit() })
    }
}

@Composable
private fun MainMenuButton(text: String, color: Color, onClick: () -> Unit) {
    // ИСПРАВЛЕНО: убран .then(Modifier.size(...)) — size принимает Dp, не Modifier
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION).height(ButtonHeight),
        shape = RoundedCornerShape(ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = DarkText, disabledContainerColor = color.copy(alpha = 0.4f), disabledContentColor = DarkText.copy(alpha = 0.4f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = ButtonDefaultElevation, pressedElevation = ButtonPressedElevation, focusedElevation = ButtonFocusedElevation, hoveredElevation = ButtonFocusedElevation, disabledElevation = 0.dp)
    ) { Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}
