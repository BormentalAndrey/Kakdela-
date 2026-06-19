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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.*
import kotlinx.coroutines.delay

private val BackgroundColors = listOf(FairyBlue, FairyPink, FairyGold, FairyPurple)
private const val BACKGROUND_COLOR_DURATION_MS = 1500
private const val BACKGROUND_COLOR_INTERVAL_MS = 3000L
private const val MENU_ENTRANCE_DURATION_MS = 600
private const val PULSE_DURATION_MS = 2500
private const val PULSE_MIN_ALPHA = 0.95f
private const val PULSE_MAX_ALPHA = 1f
private const val BUTTON_WIDTH_FRACTION = 0.7f
private val ButtonHeight = 50.dp
private val HorizontalPadding = 20.dp
private val VerticalSpacing = 14.dp
private val TopSpacerHeight = 16.dp
private val ButtonCornerRadius = 12.dp
private val ButtonDefaultElevation = 3.dp
private val ButtonPressedElevation = 6.dp
private val ButtonIconSize = 22.dp
private val TitleFontSize = 26.sp
private val SubtitleFontSize = 16.sp

@Composable
fun MainMenuScreen(onPlay: () -> Unit, onAlbum: () -> Unit, onFairyTale: () -> Unit, onQuit: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try { AudioPlayer.playMusic(context = context, resId = R.raw.music_main, loop = true) } catch (_: Exception) { }
    }

    var colorIndex by remember { mutableIntStateOf(0) }
    var isMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100); isMenuVisible = true
        while (true) { delay(BACKGROUND_COLOR_INTERVAL_MS); colorIndex = (colorIndex + 1) % BackgroundColors.size }
    }

    val backgroundColor by animateColorAsState(targetValue = BackgroundColors[colorIndex], animationSpec = tween(BACKGROUND_COLOR_DURATION_MS, easing = LinearEasing), label = "Bg")
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(initialValue = PULSE_MIN_ALPHA, targetValue = PULSE_MAX_ALPHA, animationSpec = infiniteRepeatable(animation = tween(PULSE_DURATION_MS), repeatMode = RepeatMode.Reverse), label = "Alpha")
    val backgroundBrush = Brush.verticalGradient(colors = listOf(backgroundColor, WhiteBackground))

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).systemBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = isMenuVisible, enter = fadeIn(tween(MENU_ENTRANCE_DURATION_MS)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(MENU_ENTRANCE_DURATION_MS)) + scaleIn(initialScale = 0.9f, animationSpec = tween(MENU_ENTRANCE_DURATION_MS)), exit = fadeOut(tween(300))) {
            MenuContent(alpha = pulseAlpha, onPlay = onPlay, onAlbum = onAlbum, onFairyTale = onFairyTale, onQuit = onQuit)
        }
    }
}

@Composable
private fun MenuContent(alpha: Float, onPlay: () -> Unit, onAlbum: () -> Unit, onFairyTale: () -> Unit, onQuit: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = HorizontalPadding).alpha(alpha), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(VerticalSpacing)) {
        Image(painter = painterResource(id = R.drawable.decoration_cloud), contentDescription = null, modifier = Modifier.size(80.dp, 40.dp), contentScale = ContentScale.Fit)

        Text(text = "Василисина азбука", style = MaterialTheme.typography.headlineLarge.copy(fontSize = TitleFontSize, fontWeight = FontWeight.Bold), color = DarkText, textAlign = TextAlign.Center)
        Text(text = "Путешествие по России", style = MaterialTheme.typography.headlineMedium.copy(fontSize = SubtitleFontSize), color = DarkText, textAlign = TextAlign.Center)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { Image(painter = painterResource(id = R.drawable.decoration_star_small), contentDescription = null, modifier = Modifier.size(12.dp), contentScale = ContentScale.Fit) }
        }

        Spacer(modifier = Modifier.height(TopSpacerHeight))

        MainMenuButton(text = "Играть", iconRes = R.drawable.btn_play, color = FairyPurple, onClick = { AudioPlayer.playSFX("click"); onPlay() })
        MainMenuButton(text = "Сказка", iconRes = R.drawable.btn_play, color = FairyBlue, onClick = { AudioPlayer.playSFX("click"); onFairyTale() })
        MainMenuButton(text = "Альбом успехов", iconRes = R.drawable.btn_album, color = FairyGreen, onClick = { AudioPlayer.playSFX("click"); onAlbum() })
        MainMenuButton(text = "Выход", iconRes = R.drawable.btn_exit, color = FairyPink, onClick = { AudioPlayer.playSFX("click"); onQuit() })
    }
}

@Composable
private fun MainMenuButton(text: String, iconRes: Int, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION).height(ButtonHeight),
        shape = RoundedCornerShape(ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = ButtonDefaultElevation, pressedElevation = ButtonPressedElevation)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(ButtonIconSize), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, color = Color.White)
        }
    }
}
