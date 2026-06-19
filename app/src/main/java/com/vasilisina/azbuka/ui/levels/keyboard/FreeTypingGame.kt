// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/FreeTypingGame.kt

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay

private val KeyboardLetters = listOf("А", "О", "У", "М", "П", "Р", "С", "Т", "К", "В", "Л", "Н")
private const val MAX_TYPED_LENGTH = 10
private const val GRID_COLUMNS = 4
private val KeySize = 60.dp
private val KeyCornerRadius = 14.dp
private val InputFieldCornerRadius = 14.dp
private val InputFieldHeight = 56.dp
private const val INPUT_FIELD_WIDTH_FRACTION = 0.85f
private val GridSpacing = 8.dp
private val ScreenPadding = 10.dp
private val InputFieldTopSpacer = 16.dp
private val KeyboardTopSpacer = 16.dp
private val ControlsTopSpacer = 12.dp
private val InputFieldInnerPadding = 12.dp
private const val COLOR_ANIMATION_DURATION_MS = 200
private const val KEY_STAGGER_DELAY_MS = 25L
private const val KEY_APPEAR_DURATION_MS = 250
private val KeyElevation = 3.dp
private val KeyPressedElevation = 2.dp
private val InputFieldElevation = 2.dp
private val KeyFontSize = 24.sp
private val InputFieldFontSize = 28.sp
private val ControlButtonFontSize = 16.sp
private val PlaceholderColor = Color.LightGray.copy(alpha = 0.5f)

@Composable
fun FreeTypingGame(onDone: () -> Unit) {
    val letters = remember { KeyboardLetters }
    var typedText by remember { mutableStateOf("") }
    var showKeyboard by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val inputFieldBorderColor by animateColorAsState(targetValue = when { typedText.isEmpty() -> FairyBlue.copy(alpha = 0.5f); typedText.length >= MAX_TYPED_LENGTH -> FairyPink; else -> FairyPurple }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Border")
    val inputTextColor by animateColorAsState(targetValue = if (typedText.isEmpty()) PlaceholderColor else FairyPurple, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Text")

    LaunchedEffect(Unit) { showKeyboard = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(ScreenPadding)) {
        Text(text = "Напечатай что хочешь!", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(InputFieldTopSpacer))

        // Поле ввода
        Box(modifier = Modifier.fillMaxWidth(INPUT_FIELD_WIDTH_FRACTION).height(InputFieldHeight).shadow(InputFieldElevation, RoundedCornerShape(InputFieldCornerRadius)).background(Color.White, RoundedCornerShape(InputFieldCornerRadius)).border(2.dp, inputFieldBorderColor, RoundedCornerShape(InputFieldCornerRadius)).padding(InputFieldInnerPadding), contentAlignment = Alignment.Center) {
            if (typedText.isEmpty()) Text(text = "Нажми на букву...", style = MaterialTheme.typography.bodyLarge.copy(fontSize = InputFieldFontSize, fontWeight = FontWeight.Light), color = PlaceholderColor, textAlign = TextAlign.Center)
            else Text(text = typedText, style = MaterialTheme.typography.headlineLarge.copy(fontSize = InputFieldFontSize, fontWeight = FontWeight.Bold), color = inputTextColor, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${typedText.length} / $MAX_TYPED_LENGTH", style = MaterialTheme.typography.bodySmall, color = if (typedText.length >= MAX_TYPED_LENGTH) FairyPink else DarkText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(KeyboardTopSpacer))

        // Клавиатура — по 4 в ряд
        val rows = letters.chunked(GRID_COLUMNS)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing), modifier = Modifier.padding(vertical = 4.dp)) {
                row.forEach { letter ->
                    val index = letters.indexOf(letter)
                    AnimatedKeyBox(letter = letter, appearDelay = index * KEY_STAGGER_DELAY_MS, showKeyboard = showKeyboard, onClick = { if (typedText.length < MAX_TYPED_LENGTH) { typedText += letter; AudioPlayer.playSFX("click") } })
                }
            }
        }

        Spacer(modifier = Modifier.height(ControlsTopSpacer))

        // Кнопки управления
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { if (typedText.isNotEmpty()) { typedText = typedText.dropLast(1); AudioPlayer.playSFX("click") } }, enabled = typedText.isNotEmpty(), modifier = Modifier.height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPink)) { Text("Стереть", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Button(onClick = { AudioPlayer.playSFX("click"); onDone() }, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyGreen)) { Text("Готово", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkText) }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AnimatedKeyBox(letter: String, appearDelay: Long, showKeyboard: Boolean, onClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showKeyboard) { if (showKeyboard) { delay(appearDelay); isVisible = true } }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(KEY_APPEAR_DURATION_MS))) {
        KeyBox(letter = letter, onClick = onClick)
    }
}

@Composable
fun KeyBox(letter: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(KeySize).shadow(KeyElevation, RoundedCornerShape(KeyCornerRadius)).clip(RoundedCornerShape(KeyCornerRadius)).background(FairyBlue).border(1.dp, FairyBlue.copy(alpha = 0.5f), RoundedCornerShape(KeyCornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }, contentAlignment = Alignment.Center) {
        Text(text = letter, style = MaterialTheme.typography.headlineMedium.copy(fontSize = KeyFontSize, fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center)
    }
}
