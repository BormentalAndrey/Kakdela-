// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/alphabet/SyllableBuilderGame.kt

package com.vasilisina.azbuka.ui.levels.alphabet

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
import androidx.compose.runtime.mutableStateListOf
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
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground
import kotlinx.coroutines.delay

private val SlotSize = 64.dp
private val LetterButtonSize = 72.dp
private val CornerRadius = 16.dp
private val SlotBorderWidth = 3.dp
private val SlotSpacing = 10.dp
private val LetterButtonSpacing = 10.dp
private val SlotToLettersSpacer = 20.dp
private val TitleToSlotsSpacer = 12.dp
private val GamePadding = 10.dp
private val ElementSpacing = 10.dp
private const val WRONG_ANSWER_RESET_DELAY_MS = 600L
private const val SUCCESS_DISPLAY_DELAY_MS = 400L
private const val COLOR_ANIMATION_DURATION_MS = 300
private val SlotElevation = 4.dp
private val LetterButtonElevation = 4.dp
private val SlotFontSize = 30.sp
private val LetterButtonFontSize = 32.sp

private data class IndexedLetter(val index: Int, val char: Char) {
    override fun toString(): String = char.toString()
}

@Composable
fun SyllableBuilderGame(targetSyllable: String, onComplete: (correct: Boolean) -> Unit) {
    val letters = remember(targetSyllable) { targetSyllable.toList().mapIndexed { index, char -> IndexedLetter(index, char) }.shuffled() }
    val usedLetterIndices = remember { mutableStateListOf<Int>() }
    val slots = remember(targetSyllable) { mutableStateListOf(*Array<Int?>(targetSyllable.length) { null }) }
    var selectedLetterIndex by remember { mutableStateOf<Int?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val currentSyllable = slots.mapNotNull { index -> index?.let { letters[it].char.toString() } }.joinToString("")

    LaunchedEffect(currentSyllable) {
        if (isCompleted) return@LaunchedEffect
        if (currentSyllable.length != targetSyllable.length) return@LaunchedEffect
        if (currentSyllable == targetSyllable) { isCompleted = true; AudioPlayer.playSFX("correct"); showSuccess = true; delay(SUCCESS_DISPLAY_DELAY_MS); onComplete(true) }
        else { AudioPlayer.playSFX("wrong"); delay(WRONG_ANSWER_RESET_DELAY_MS); usedLetterIndices.clear(); slots.fill(null); selectedLetterIndex = null }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Составь слог «$targetSyllable»", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(TitleToSlotsSpacer))

        // Слоты
        Row(horizontalArrangement = Arrangement.spacedBy(SlotSpacing), verticalAlignment = Alignment.CenterVertically) {
            slots.forEachIndexed { index, letterIndex ->
                val isTargeted = selectedLetterIndex != null && slots[index] == null
                val borderColor by animateColorAsState(targetValue = when { showSuccess -> FairyGreen; isTargeted -> FairyGold; else -> FairyPurple }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "SlotBorder")
                SlotView(letter = letterIndex?.let { letters[it].char.toString() }, borderColor = borderColor, isHighlighted = isTargeted, onClick = {
                    if (isCompleted) return@SlotView
                    val existing = slots[index]
                    if (existing != null) { usedLetterIndices.remove(existing); slots[index] = null }
                    else if (selectedLetterIndex != null) { slots[index] = selectedLetterIndex; usedLetterIndices.add(selectedLetterIndex!!); selectedLetterIndex = null }
                })
            }
        }

        Spacer(modifier = Modifier.height(SlotToLettersSpacer))

        // Кнопки с буквами — зелёные, квадратные
        Row(horizontalArrangement = Arrangement.spacedBy(LetterButtonSpacing), verticalAlignment = Alignment.CenterVertically) {
            letters.forEach { indexedLetter ->
                val isUsed = usedLetterIndices.contains(indexedLetter.index)
                val isSelected = selectedLetterIndex == indexedLetter.index
                val bgColor by animateColorAsState(targetValue = when { isUsed -> Color.LightGray; isSelected -> FairyGold; else -> FairyGreen }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "LetterBg")
                LetterButton(char = indexedLetter.char, enabled = !isUsed, backgroundColor = bgColor, onClick = {
                    if (isCompleted) return@LetterButton
                    if (selectedLetterIndex == indexedLetter.index) selectedLetterIndex = null
                    else if (!isUsed) {
                        selectedLetterIndex = indexedLetter.index
                        val firstEmpty = slots.indexOf(null)
                        if (firstEmpty != -1) { slots[firstEmpty] = indexedLetter.index; usedLetterIndices.add(indexedLetter.index); selectedLetterIndex = null }
                    }
                })
            }
        }

        Spacer(modifier = Modifier.height(ElementSpacing))

        Button(onClick = { usedLetterIndices.clear(); slots.fill(null); selectedLetterIndex = null }, enabled = !isCompleted && (usedLetterIndices.isNotEmpty() || selectedLetterIndex != null), modifier = Modifier.height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPink)) { Text("Сбросить", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SlotView(letter: String?, borderColor: Color, isHighlighted: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = when { letter != null -> WhiteBackground; isHighlighted -> FairyGold.copy(alpha = 0.2f); else -> Color.White }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "SlotBg")
    Box(modifier = Modifier.size(SlotSize).shadow(SlotElevation, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(bgColor).border(SlotBorderWidth, borderColor, RoundedCornerShape(CornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        if (letter != null) Text(text = letter, style = MaterialTheme.typography.headlineLarge.copy(fontSize = SlotFontSize, fontWeight = FontWeight.Bold), color = FairyPurple, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LetterButton(char: Char, enabled: Boolean, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(LetterButtonSize).shadow(if (enabled) LetterButtonElevation else 0.dp, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(backgroundColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = char.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = LetterButtonFontSize, fontWeight = FontWeight.Bold), color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
    }
}
