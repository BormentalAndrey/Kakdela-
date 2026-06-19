// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/map/MapScreen.kt

package com.vasilisina.azbuka.ui.map

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground

private val MapPadding = 16.dp
private val CityPointSize = 70.dp
private val CityPointPadding = 8.dp
private val BackButtonCornerRadius = 12.dp
private val LevelNumberFontSize = 28.sp
private val CityNameFontSize = 18.sp
private val StarsFontSize = 16.sp
private const val CITY_ANIMATION_DURATION_MS = 400
private const val CITY_STAGGER_DELAY_MS = 150L
private val CityPointElevation = 8.dp
private val BackButtonElevation = 4.dp
private val CompletedBorderWidth = 3.dp
private val CompletedBorderColor = FairyGreen

private data class City(val name: String, val level: Int)

@Composable
fun MapScreen(onLevelSelected: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try { AudioPlayer.playMusic(context = context, resId = R.raw.music_map, loop = true) } catch (_: Exception) { }
    }

    val cities = remember { listOf(City("Москва", 1), City("Тула", 2), City("Вологда", 3), City("Казань", 4), City("Владивосток", 5)) }
    val cityRows = remember(cities) { listOf(cities.subList(0, 2), cities.subList(2, 4), cities.subList(4, 5)) }

    Box(modifier = Modifier.fillMaxSize().background(WhiteBackground).statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing).navigationBarsPadding()) {
        MapBackground()
        Column(modifier = Modifier.fillMaxSize().padding(MapPadding), verticalArrangement = Arrangement.SpaceEvenly) {
            cityRows.forEachIndexed { rowIndex, row -> CityRow(cities = row, rowIndex = rowIndex, onLevelSelected = onLevelSelected) }
        }
        BackButton(onClick = { AudioPlayer.playSFX("click"); onBack() }, modifier = Modifier.align(Alignment.TopStart).padding(MapPadding))
    }
}

@Composable
private fun MapBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.map_russia), contentDescription = "Карта России", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, WhiteBackground.copy(alpha = 0.15f), WhiteBackground.copy(alpha = 0.3f)))))
    }
}

@Composable
private fun CityRow(cities: List<City>, rowIndex: Int, onLevelSelected: (Int) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(CITY_STAGGER_DELAY_MS * (rowIndex + 1)); isVisible = true }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (cities.size == 1) Arrangement.Center else Arrangement.SpaceEvenly) {
        cities.forEach { city ->
            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(CITY_ANIMATION_DURATION_MS)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) {
                CityPoint(cityName = city.name, level = city.level, onLevelSelected = onLevelSelected)
            }
        }
    }
}

@Composable
private fun CityPoint(cityName: String, level: Int, onLevelSelected: (Int) -> Unit) {
    val isUnlocked = GameState.isLevelUnlocked(level)
    val stars = GameState.getStars(level)
    val isCompleted = stars > 0

    val pointColor by animateColorAsState(targetValue = if (isUnlocked) FairyGold else Color.Gray.copy(alpha = 0.45f), animationSpec = tween(300), label = "Point")
    val borderColor by animateColorAsState(targetValue = if (isCompleted) CompletedBorderColor else Color.Transparent, animationSpec = tween(300), label = "Border")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = isUnlocked) { AudioPlayer.playSFX("click"); onLevelSelected(level) }.padding(CityPointPadding)) {
        Box(
            modifier = Modifier.size(CityPointSize).shadow(if (isUnlocked) CityPointElevation else 0.dp, CircleShape).background(pointColor, CircleShape).then(if (isCompleted) Modifier.border(CompletedBorderWidth, borderColor, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                // Замок — картинка
                Image(
                    painter = painterResource(id = R.drawable.icon_lock),
                    contentDescription = "Закрыто",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(text = level.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontSize = LevelNumberFontSize, fontWeight = FontWeight.Bold), color = DarkText, textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = cityName, style = MaterialTheme.typography.bodyLarge.copy(fontSize = CityNameFontSize, fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal), color = if (isUnlocked) DarkText else Color.Gray, textAlign = TextAlign.Center)
        if (isUnlocked && isCompleted) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(stars) { Image(painter = painterResource(id = R.drawable.star_filled), contentDescription = "★", modifier = Modifier.size(16.dp), contentScale = ContentScale.Fit) }
                repeat(GameState.MAX_STARS_PER_LEVEL - stars) { Image(painter = painterResource(id = R.drawable.star_empty), contentDescription = "☆", modifier = Modifier.size(16.dp), contentScale = ContentScale.Fit) }
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(BackButtonElevation, RoundedCornerShape(BackButtonCornerRadius)),
        shape = RoundedCornerShape(BackButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(containerColor = FairyPink, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = BackButtonElevation, pressedElevation = BackButtonElevation * 1.5f, focusedElevation = BackButtonElevation, hoveredElevation = BackButtonElevation, disabledElevation = 0.dp)
    ) {
        Image(painter = painterResource(id = R.drawable.icon_back_arrow), contentDescription = "Назад", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.size(4.dp))
        Text(text = "Назад", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}
