// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/KeyboardGame.kt

package com.vasilisina.azbuka.ui.levels.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyboardLetters = listOf("А", "О", "У", "М", "П", "Р", "С", "Т", "К", "В", "Л", "Н")
private const val GRID_COLUMNS = 4
private val KeySize = 64.dp
private val KeyCornerRadius = 14.dp
private val KeyBorderWidth = 2.dp
private val GridSpacing = 8.dp
private val GamePadding = 10.dp
private val KeyboardTopSpacer = 16.dp
private const val COLOR_ANIMATION_DURATION_MS = 250
private const val SUCCESS_DELAY_MS = 800L
private const val WRONG_FLASH_DURATION_MS = 500L
private const val KEY_STAGGER_DELAY_MS = 30L
private const val KEY_APPEAR_DURATION_MS = 300
private val KeyElevation = 3.dp
private val KeyPressedElevation = 6.dp
private val KeyFontSize = 26.sp
private val HintFontSize = 14.sp
private val TargetLetterSize = 48.sp

@Composable
fun KeyboardGame(onDone: () -> Unit) {
    val letters = remember { KeyboardLetters }
    val targetLetter = remember { letters.random() }
    var isFound by remember { mutableStateOf(false) }
    var wrongKeyIndex by remember { mutableStateOf<Int?>(null) }
    var showKeyboard by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { showKeyboard = true }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)
    ) {
        Text(text = "Нажми на букву", style = MaterialTheme.typography.headlineSmall, color = DarkText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "«$targetLetter»", style = MaterialTheme.typography.headlineLarge.copy(fontSize = TargetLetterSize, fontWeight = FontWeight.Bold), color = FairyPurple, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(KeyboardTopSpacer))

        // Сетка клавиатуры — по 4 в ряд
        val rows = letters.chunked(GRID_COLUMNS)
        rows.forEach { row ->
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(GridSpacing),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                row.forEach { letter ->
                    val index = letters.indexOf(letter)
                    val isTarget = letter == targetLetter
                    val isWrong = wrongKeyIndex == index
                    val appearDelay = index * KEY_STAGGER_DELAY_MS

                    AnimatedKeyButton(
                        letter = letter, isTarget = isTarget, isFound = isFound, isWrong = isWrong,
                        appearDelay = appearDelay, showKeyboard = showKeyboard,
                        onClick = {
                            if (!isFound) {
                                if (isTarget) { isFound = true; AudioPlayer.playSFX("correct"); coroutineScope.launch { delay(SUCCESS_DELAY_MS); onDone() } }
                                else { wrongKeyIndex = index; AudioPlayer.playSFX("wrong"); coroutineScope.launch { delay(WRONG_FLASH_DURATION_MS); wrongKeyIndex = null } }
                            }
                        }
                    )
                }
            }
        }

        if (!isFound) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Найди и нажми нужную букву", style = MaterialTheme.typography.bodySmall.copy(fontSize = HintFontSize), color = DarkText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AnimatedKeyButton(letter: String, isTarget: Boolean, isFound: Boolean, isWrong: Boolean, appearDelay: Long, showKeyboard: Boolean, onClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showKeyboard) { if (showKeyboard) { delay(appearDelay); isVisible = true } }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(KEY_APPEAR_DURATION_MS))) {
        KeyButton(letter = letter, isTarget = isTarget, isFound = isFound, isWrong = isWrong, onClick = onClick)
    }
}

@Composable
fun KeyButton(letter: String, isTarget: Boolean, isFound: Boolean, isWrong: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(targetValue = when { isFound && isTarget -> FairyGold; isWrong -> Color.Red.copy(alpha = 0.7f); else -> FairyBlue }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "KeyBg")
    val borderColor by animateColorAsState(targetValue = when { isFound && isTarget -> FairyGold; isWrong -> Color.Red; else -> FairyPurple }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "KeyBorder")
    val textColor by animateColorAsState(targetValue = when { isFound && isTarget -> DarkText; else -> Color.White }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "KeyText")
    val elevation = if (isFound && isTarget) KeyPressedElevation else KeyElevation

    Box(
        modifier = Modifier.size(KeySize).shadow(elevation, RoundedCornerShape(KeyCornerRadius)).clip(RoundedCornerShape(KeyCornerRadius)).background(backgroundColor).border(KeyBorderWidth, borderColor, RoundedCornerShape(KeyCornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isFound) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = letter, style = MaterialTheme.typography.headlineMedium.copy(fontSize = KeyFontSize, fontWeight = FontWeight.Bold), color = textColor, textAlign = TextAlign.Center)
    }
}
