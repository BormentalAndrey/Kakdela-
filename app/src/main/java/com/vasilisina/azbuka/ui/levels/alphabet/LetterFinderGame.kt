// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/alphabet/LetterFinderGame.kt

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay

private val CellSize = 64.dp
private val CellCornerRadius = 14.dp
private val CellHorizontalSpacing = 10.dp
private val CellVerticalSpacing = 6.dp
private val GamePadding = 10.dp
private val GridTopSpacer = 12.dp
private const val TARGET_LETTER_COUNT = 5
private const val DISTRACTOR_LETTER_COUNT = 5
private const val CELLS_PER_ROW = 5
private const val CELL_STAGGER_DELAY_MS = 40L
private const val CELL_APPEAR_DURATION_MS = 250
private const val COLOR_ANIMATION_DURATION_MS = 300
private val CellElevation = 4.dp
private val FoundBorderWidth = 2.dp
private val CellFontSize = 28.sp
private val CounterFontSize = 17.sp

@Composable
fun LetterFinderGame(targetLetter: String, onComplete: (Boolean) -> Unit) {
    val allLetters = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    val scrollState = rememberScrollState()

    val symbols = remember(targetLetter) {
        val others = allLetters.toList().filter { it.toString() != targetLetter }.shuffled().take(DISTRACTOR_LETTER_COUNT).map { it.toString() }
        val result = mutableListOf<String>()
        repeat(TARGET_LETTER_COUNT) { result.add(targetLetter) }
        result.addAll(others)
        result.shuffled()
    }

    // ✅ Каждой букве — уникальный индекс
    val symbolsWithIndex = remember(symbols) { symbols.mapIndexed { i, ch -> i to ch } }

    val clickedStates = remember { mutableStateMapOf<Int, Boolean>() }
    var foundCount by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    val remainingCount = TARGET_LETTER_COUNT - foundCount

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Найди все буквы «$targetLetter»!", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(6.dp))
        FoundCounter(found = foundCount, total = TARGET_LETTER_COUNT, remaining = remainingCount)
        Spacer(modifier = Modifier.height(GridTopSpacer))

        val rows = symbolsWithIndex.chunked(CELLS_PER_ROW)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CellHorizontalSpacing), modifier = Modifier.padding(vertical = CellVerticalSpacing)) {
                row.forEach { (cellIndex, letter) ->
                    val appearDelay = cellIndex * CELL_STAGGER_DELAY_MS
                    AnimatedLetterCell(
                        letter = letter, targetLetter = targetLetter,
                        isClicked = clickedStates.containsKey(cellIndex),
                        isCorrect = clickedStates[cellIndex] ?: false,
                        appearDelay = appearDelay,
                        onTap = { correct ->
                            if (!isCompleted && !clickedStates.containsKey(cellIndex)) {
                                clickedStates[cellIndex] = correct
                                if (correct) { foundCount++; AudioPlayer.playSFX("correct"); if (foundCount >= TARGET_LETTER_COUNT) { isCompleted = true; onComplete(true) } }
                                else { AudioPlayer.playSFX("wrong") }
                            }
                        }
                    )
                }
            }
        }

        if (!isCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Нажимай на буквы, чтобы найти все «$targetLetter»", style = MaterialTheme.typography.bodySmall, color = DarkText.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FoundCounter(found: Int, total: Int, remaining: Int) {
    val counterColor by animateColorAsState(targetValue = when { found == total -> FairyGreen; found > 0 -> FairyGold; else -> DarkText }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Counter")
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Найдено: ", style = MaterialTheme.typography.bodyLarge.copy(fontSize = CounterFontSize), color = DarkText)
        Text(text = "$found из $total", style = MaterialTheme.typography.bodyLarge.copy(fontSize = CounterFontSize, fontWeight = FontWeight.Bold), color = counterColor)
        if (found < total) Text(text = "  (осталось $remaining)", style = MaterialTheme.typography.bodySmall, color = DarkText.copy(alpha = 0.6f))
        else Text(text = "  ✓", style = MaterialTheme.typography.bodyLarge, color = FairyGreen)
    }
}

@Composable
private fun AnimatedLetterCell(letter: String, targetLetter: String, isClicked: Boolean, isCorrect: Boolean, appearDelay: Long, onTap: (Boolean) -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(appearDelay); isVisible = true }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(CELL_APPEAR_DURATION_MS))) {
        LetterCell(letter = letter, targetLetter = targetLetter, alreadyClicked = isClicked, isCorrect = isCorrect, onTap = onTap)
    }
}

@Composable
fun LetterCell(letter: String, targetLetter: String, alreadyClicked: Boolean, isCorrect: Boolean, onTap: (Boolean) -> Unit) {
    val isTarget = letter == targetLetter
    val backgroundColor by animateColorAsState(targetValue = when { !alreadyClicked -> FairyBlue; isCorrect -> FairyGreen; else -> Color.Gray.copy(alpha = 0.6f) }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "CellBg")
    val borderColor by animateColorAsState(targetValue = if (alreadyClicked && isCorrect) FairyPurple else Color.Transparent, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "CellBorder")
    val cellText = when { !alreadyClicked -> letter; isCorrect -> "✓"; else -> "✕" }
    val textColor = when { !alreadyClicked -> DarkText; isCorrect -> Color.White; else -> Color.White.copy(alpha = 0.8f) }

    Box(modifier = Modifier.size(CellSize).shadow(if (!alreadyClicked) CellElevation else 0.dp, RoundedCornerShape(CellCornerRadius)).clip(RoundedCornerShape(CellCornerRadius)).background(backgroundColor).then(if (alreadyClicked && isCorrect) Modifier.border(FoundBorderWidth, borderColor, RoundedCornerShape(CellCornerRadius)) else Modifier).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !alreadyClicked) { onTap(isTarget) }, contentAlignment = Alignment.Center) {
        Text(text = cellText, style = MaterialTheme.typography.headlineMedium.copy(fontSize = CellFontSize, fontWeight = FontWeight.Bold), color = textColor, textAlign = TextAlign.Center)
    }
}
