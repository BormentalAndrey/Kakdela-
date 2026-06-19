// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/keyboard/WordBuilderGame.kt

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
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay

private val WordPool = listOf("МАМА", "РУСЬ", "МИР")
private val SlotSize = 56.dp
private val LetterButtonSize = 58.dp
private val CornerRadius = 14.dp
private val SlotBorderWidth = 2.dp
private val SlotSpacing = 6.dp
private val LetterButtonSpacing = 6.dp
private val SlotsToLettersSpacer = 20.dp
private val TitleToSlotsSpacer = 16.dp
private val GamePadding = 10.dp
private val ResetButtonSpacer = 12.dp
private const val RESULT_DELAY_MS = 800L
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val ELEMENT_STAGGER_DELAY_MS = 60L
private const val ELEMENT_APPEAR_DURATION_MS = 250
private val SlotElevation = 3.dp
private val LetterButtonElevation = 3.dp
private val SlotFontSize = 26.sp
private val LetterButtonFontSize = 26.sp
private const val MAX_LETTERS_PER_ROW = 6

private data class IndexedLetter(val id: Int, val char: String) {
    override fun toString(): String = char
}

@Composable
fun WordBuilderGame(onResult: (correct: Boolean) -> Unit) {
    val targetWord = remember { WordPool.random() }
    val letterData: List<IndexedLetter> = remember(targetWord) { targetWord.mapIndexed { index, char -> IndexedLetter(id = index, char = char.toString()) }.shuffled() }
    val usedIndices = remember { mutableStateListOf<Int>() }
    val slots = remember(targetWord) { mutableStateListOf(*Array<IndexedLetter?>(targetWord.length) { null }) }
    var isCompleted by remember { mutableStateOf(false) }
    val filledWord = slots.map { it?.char ?: "" }.joinToString("")
    val scrollState = rememberScrollState()

    LaunchedEffect(filledWord) {
        if (isCompleted) return@LaunchedEffect
        if (filledWord.length != targetWord.length) return@LaunchedEffect
        if (filledWord == targetWord) { isCompleted = true; AudioPlayer.playSFX("correct"); delay(RESULT_DELAY_MS); onResult(true) }
        else { AudioPlayer.playSFX("wrong"); delay(RESULT_DELAY_MS); usedIndices.clear(); slots.fill(null) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Собери слово", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Цель: $targetWord", style = MaterialTheme.typography.bodyLarge, color = FairyPurple, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(TitleToSlotsSpacer))

        // Слоты
        Row(horizontalArrangement = Arrangement.spacedBy(SlotSpacing), verticalAlignment = Alignment.CenterVertically) {
            slots.forEachIndexed { index, letter ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(ELEMENT_STAGGER_DELAY_MS * index); isVisible = true }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
                    WordSlot(letter = letter?.char, isEmpty = letter == null, onClick = { if (letter != null) { usedIndices.remove(letter.id); slots[index] = null } })
                }
            }
        }

        Spacer(modifier = Modifier.height(SlotsToLettersSpacer))

        // Кнопки с буквами
        val letterRows = letterData.chunked(MAX_LETTERS_PER_ROW)
        letterRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(LetterButtonSpacing)) {
                row.forEach { indexedLetter ->
                    val isUsed = usedIndices.contains(indexedLetter.id)
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(ELEMENT_STAGGER_DELAY_MS * letterData.indexOf(indexedLetter)); isVisible = true }
                    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
                        WordLetterButton(letter = indexedLetter.char, isUsed = isUsed, onClick = { if (!isUsed) { val firstEmpty = slots.indexOf(null); if (firstEmpty != -1) { slots[firstEmpty] = indexedLetter; usedIndices.add(indexedLetter.id) } } })
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(ResetButtonSpacer))

        Button(onClick = { usedIndices.clear(); slots.fill(null) }, enabled = !isCompleted && usedIndices.isNotEmpty(), modifier = Modifier.height(44.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPink)) { Text("Сброс", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WordSlot(letter: String?, isEmpty: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = if (isEmpty) Color.White.copy(alpha = 0.9f) else FairyGold.copy(alpha = 0.2f), animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "SlotBg")
    val borderColor by animateColorAsState(targetValue = if (letter != null) FairyGold else FairyBlue, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "SlotBorder")
    Box(modifier = Modifier.size(SlotSize).shadow(SlotElevation, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(bgColor).border(SlotBorderWidth, borderColor, RoundedCornerShape(CornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = letter != null, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = letter ?: "", style = MaterialTheme.typography.headlineMedium.copy(fontSize = SlotFontSize, fontWeight = FontWeight.Bold), color = if (letter != null) FairyPurple else Color.Transparent, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WordLetterButton(letter: String, isUsed: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(targetValue = if (isUsed) Color.LightGray.copy(alpha = 0.5f) else FairyBlue, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "LetterBg")
    Box(modifier = Modifier.size(LetterButtonSize).shadow(if (!isUsed) LetterButtonElevation else 0.dp, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isUsed, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = letter, style = MaterialTheme.typography.headlineMedium.copy(fontSize = LetterButtonFontSize, fontWeight = FontWeight.Bold), color = if (isUsed) Color.White.copy(alpha = 0.3f) else Color.White, textAlign = TextAlign.Center)
    }
}
