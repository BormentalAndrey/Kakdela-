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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

private val SlotSize = 80.dp
private val LetterButtonSize = 70.dp
private val CornerRadius = 16.dp
private val SlotBorderWidth = 3.dp
private val SlotSpacing = 16.dp
private val LetterButtonSpacing = 12.dp
private val SlotToLettersSpacer = 40.dp
private val TitleToSlotsSpacer = 24.dp
private val GamePadding = 16.dp
private val ElementSpacing = 8.dp
private const val WRONG_ANSWER_RESET_DELAY_MS = 600L
private const val SUCCESS_DISPLAY_DELAY_MS = 400L
private const val COLOR_ANIMATION_DURATION_MS = 300
private val SlotElevation = 6.dp
private val LetterButtonElevation = 4.dp
private val SlotFontSize = 36.sp
private val LetterButtonFontSize = 32.sp

// -------------------------------------------------------------------------
// Модель
// -------------------------------------------------------------------------

private data class IndexedLetter(val index: Int, val char: Char) {
    override fun toString(): String = char.toString()
}

// -------------------------------------------------------------------------
// Игра
// -------------------------------------------------------------------------

@Composable
fun SyllableBuilderGame(
    targetSyllable: String,
    onComplete: (correct: Boolean) -> Unit
) {
    val letters = remember(targetSyllable) {
        targetSyllable.toList().mapIndexed { index, char ->
            IndexedLetter(index, char)
        }.shuffled()
    }

    val usedLetterIndices = remember { mutableStateListOf<Int>() }

    // ИСПРАВЛЕНО: явно указан тип Array<Int?>
    val slots = remember(targetSyllable) {
        mutableStateListOf(*Array<Int?>(targetSyllable.length) { null })
    }

    var selectedLetterIndex by remember { mutableStateOf<Int?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val currentSyllable = slots.mapNotNull { index ->
        index?.let { letters[it].char.toString() }
    }.joinToString("")

    LaunchedEffect(currentSyllable) {
        if (isCompleted) return@LaunchedEffect
        if (currentSyllable.length != targetSyllable.length) return@LaunchedEffect

        if (currentSyllable == targetSyllable) {
            isCompleted = true
            AudioPlayer.playSFX("correct")
            showSuccess = true
            delay(SUCCESS_DISPLAY_DELAY_MS)
            onComplete(true)
        } else {
            AudioPlayer.playSFX("wrong")
            delay(WRONG_ANSWER_RESET_DELAY_MS)
            usedLetterIndices.clear()
            slots.fill(null)
            selectedLetterIndex = null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(GamePadding)
    ) {
        Text(
            text = "Составь слог «$targetSyllable»",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(TitleToSlotsSpacer))

        SyllableSlots(
            slots = slots,
            letters = letters,
            selectedLetterIndex = selectedLetterIndex,
            showSuccess = showSuccess,
            onSlotClick = { slotIndex ->
                if (isCompleted) return@SyllableSlots
                val existing = slots[slotIndex]
                if (existing != null) {
                    usedLetterIndices.remove(existing)
                    slots[slotIndex] = null
                } else if (selectedLetterIndex != null) {
                    slots[slotIndex] = selectedLetterIndex
                    usedLetterIndices.add(selectedLetterIndex!!)
                    selectedLetterIndex = null
                }
            }
        )

        Spacer(modifier = Modifier.height(SlotToLettersSpacer))

        LetterButtonsRow(
            letters = letters,
            usedLetterIndices = usedLetterIndices,
            selectedLetterIndex = selectedLetterIndex,
            onLetterClick = { letterIndex ->
                if (isCompleted) return@LetterButtonsRow
                if (selectedLetterIndex == letterIndex) {
                    selectedLetterIndex = null
                } else if (!usedLetterIndices.contains(letterIndex)) {
                    selectedLetterIndex = letterIndex
                    val firstEmpty = slots.indexOf(null)
                    if (firstEmpty != -1) {
                        slots[firstEmpty] = letterIndex
                        usedLetterIndices.add(letterIndex)
                        selectedLetterIndex = null
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(ElementSpacing))

        TextButton(
            onClick = {
                if (isCompleted) return@TextButton
                AudioPlayer.playSFX("click")
                usedLetterIndices.clear()
                slots.fill(null)
                selectedLetterIndex = null
            },
            enabled = !isCompleted && (usedLetterIndices.isNotEmpty() || selectedLetterIndex != null)
        ) {
            Text(
                text = "Сбросить",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) Color.Gray else FairyPink
            )
        }
    }
}

// -------------------------------------------------------------------------
// Слоты
// -------------------------------------------------------------------------

@Composable
private fun SyllableSlots(
    slots: List<Int?>,
    letters: List<IndexedLetter>,
    selectedLetterIndex: Int?,
    showSuccess: Boolean,
    onSlotClick: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(SlotSpacing), verticalAlignment = Alignment.CenterVertically) {
        slots.forEachIndexed { index, letterIndex ->
            val isTargeted = selectedLetterIndex != null && slots[index] == null
            val borderColor by animateColorAsState(
                targetValue = when {
                    showSuccess -> FairyGreen
                    isTargeted -> FairyGold
                    else -> FairyPurple
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "SlotBorder"
            )
            SlotView(
                letter = letterIndex?.let { letters[it].char.toString() },
                borderColor = borderColor,
                isHighlighted = isTargeted,
                onClick = { onSlotClick(index) }
            )
        }
    }
}

@Composable
private fun SlotView(letter: String?, borderColor: Color, isHighlighted: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = when {
            letter != null -> WhiteBackground
            isHighlighted -> FairyGold.copy(alpha = 0.2f)
            else -> Color.White
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "SlotBg"
    )
    Box(
        modifier = Modifier
            .size(SlotSize)
            .shadow(SlotElevation, RoundedCornerShape(CornerRadius))
            .clip(RoundedCornerShape(CornerRadius))
            .background(bgColor)
            .border(SlotBorderWidth, borderColor, RoundedCornerShape(CornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (letter != null) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = SlotFontSize, fontWeight = FontWeight.Bold),
                color = FairyPurple,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Кнопки букв
// -------------------------------------------------------------------------

@Composable
private fun LetterButtonsRow(
    letters: List<IndexedLetter>,
    usedLetterIndices: List<Int>,
    selectedLetterIndex: Int?,
    onLetterClick: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(LetterButtonSpacing), verticalAlignment = Alignment.CenterVertically) {
        letters.forEachIndexed { index, indexedLetter ->
            val isUsed = usedLetterIndices.contains(index)
            val isSelected = selectedLetterIndex == index
            val bgColor by animateColorAsState(
                targetValue = when {
                    isUsed -> Color.LightGray
                    isSelected -> FairyGold
                    else -> FairyGreen
                },
                animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
                label = "LetterBg"
            )
            LetterButton(
                char = indexedLetter.char,
                enabled = !isUsed,
                backgroundColor = bgColor,
                onClick = { onLetterClick(index) }
            )
        }
    }
}

@Composable
private fun LetterButton(char: Char, enabled: Boolean, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(LetterButtonSize)
            .shadow(if (enabled) LetterButtonElevation else 0.dp, RoundedCornerShape(CornerRadius))
            .clip(RoundedCornerShape(CornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.toString(),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = LetterButtonFontSize, fontWeight = FontWeight.Bold),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
