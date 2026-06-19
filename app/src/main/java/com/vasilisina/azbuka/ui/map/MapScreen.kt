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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.vasilisina.azbuka.ui.theme.*
import kotlinx.coroutines.delay

private val MapPadding = 10.dp
private val CityPointSize = 50.dp
private val CityPointPadding = 3.dp
private val BackButtonCornerRadius = 8.dp
private val LevelNumberFontSize = 18.sp
private val CityNameFontSize = 12.sp
private const val CITY_ANIMATION_DURATION_MS = 400
private const val CITY_STAGGER_DELAY_MS = 80L
private val CityPointElevation = 5.dp
private val BackButtonElevation = 2.dp
private val CompletedBorderWidth = 2.dp
private val CompletedBorderColor = FairyGreen

private data class City(val name: String, val level: Int)

@Composable
fun MapScreen(onLevelSelected: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try { AudioPlayer.playMusic(context = context, resId = R.raw.music_map, loop = true) } catch (_: Exception) { }
    }

    val cities = remember {
        listOf(
            City("Москва", 1), City("Тула", 2), City("Вологда", 3),
            City("Казань", 4), City("Ярославль", 5), City("Владивосток", 6)
        )
    }
    val cityRows = remember(cities) { listOf(cities.subList(0, 2), cities.subList(2, 4), cities.subList(4, 6)) }

    Box(modifier = Modifier.fillMaxSize().background(WhiteBackground).systemBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing).navigationBarsPadding()) {
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
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, WhiteBackground.copy(alpha = 0.2f)))))
    }
}

@Composable
private fun CityRow(cities: List<City>, rowIndex: Int, onLevelSelected: (Int) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(CITY_STAGGER_DELAY_MS * (rowIndex + 1)); isVisible = true }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        cities.forEach { city ->
            AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(CITY_ANIMATION_DURATION_MS)) + scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))) {
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

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = isUnlocked) { AudioPlayer.playSFX("click"); onLevelSelected(level) }.padding(CityPointPadding)) {
        Box(modifier = Modifier.size(CityPointSize).shadow(if (isUnlocked) CityPointElevation else 0.dp, CircleShape).background(pointColor, CircleShape).border(if (isCompleted) CompletedBorderWidth else 0.dp, borderColor, CircleShape), contentAlignment = Alignment.Center) {
            if (!isUnlocked) Image(painter = painterResource(id = R.drawable.icon_lock), contentDescription = "Закрыто", modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit)
            else Text(text = level.toString(), style = MaterialTheme.typography.headlineMedium.copy(fontSize = LevelNumberFontSize, fontWeight = FontWeight.Bold), color = DarkText)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = cityName, style = MaterialTheme.typography.bodyLarge.copy(fontSize = CityNameFontSize, fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal), color = if (isUnlocked) DarkText else Color.Gray, textAlign = TextAlign.Center)
        if (isUnlocked && isCompleted) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(stars) { Image(painter = painterResource(id = R.drawable.star_filled), contentDescription = null, modifier = Modifier.size(10.dp)) }
                repeat(GameState.MAX_STARS_PER_LEVEL - stars) { Image(painter = painterResource(id = R.drawable.star_empty), contentDescription = null, modifier = Modifier.size(10.dp)) }
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
        elevation = ButtonDefaults.buttonElevation(defaultElevation = BackButtonElevation, pressedElevation = BackButtonElevation * 2f)
    ) {
        Image(painter = painterResource(id = R.drawable.icon_back_arrow), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Назад", style = MaterialTheme.typography.labelLarge)
    }
}
