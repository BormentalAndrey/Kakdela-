// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/PatternGameScreen.kt

package com.vasilisina.azbuka.ui.levels.logic

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ✅ 3 цвета для паттерна
private val PatternColors = listOf(FairyPink, FairyBlue, FairyGreen)
private const val PATTERN_LENGTH = 3
private const val OPTIONS_COUNT = 3
private val PatternCircleSize = 64.dp
private val OptionCircleSize = 80.dp
private val QuestionSlotSize = 64.dp
private val QuestionSlotCornerRadius = 12.dp
private val RowSpacing = 10.dp
private val OptionsSpacing = 14.dp
private val GamePadding = 10.dp
private val PatternTopSpacer = 16.dp
private val OptionsTopSpacer = 24.dp
private val HintTopSpacer = 12.dp
private val PatternBorderWidth = 2.dp
private val QuestionBorderWidth = 2.dp
private val SelectedBorderWidth = 3.dp
private val CorrectBorderWidth = 3.dp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val RESULT_DELAY_MS = 1200L
private const val ELEMENT_STAGGER_DELAY_MS = 80L
private const val ELEMENT_APPEAR_DURATION_MS = 300
private val PatternCircleElevation = 4.dp
private val OptionCircleElevation = 4.dp
private val OptionSelectedElevation = 8.dp
private val QuestionFontSize = 28.sp
private val HintFontSize = 14.sp
private val PatternBorderColor = Color.Gray.copy(alpha = 0.5f)

@Composable
fun PatternGameScreen(onResult: (correct: Boolean) -> Unit) {
    val pattern = remember { PatternColors.shuffled().take(PATTERN_LENGTH) }
    val correctColor = remember(pattern) { pattern[0] }
    val options = remember(correctColor) { val wrong = PatternColors.filter { it != correctColor }.shuffled().take(OPTIONS_COUNT - 1); (wrong + correctColor).shuffled() }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var isLocked by remember { mutableStateOf(false) }
    var showElements by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { showElements = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Продолжи ряд:", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(PatternTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(RowSpacing), verticalAlignment = Alignment.CenterVertically) {
            pattern.forEachIndexed { index, color ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(ELEMENT_STAGGER_DELAY_MS * index); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
                    Box(modifier = Modifier.size(PatternCircleSize).shadow(PatternCircleElevation, CircleShape).background(color, CircleShape).border(PatternBorderWidth, PatternBorderColor, CircleShape))
                }
            }
            var isQuestionVisible by remember { mutableStateOf(false) }
            LaunchedEffect(showElements) { if (showElements) { delay(ELEMENT_STAGGER_DELAY_MS * PATTERN_LENGTH); isQuestionVisible = true } }
            AnimatedVisibility(visible = isQuestionVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
                Box(modifier = Modifier.size(QuestionSlotSize).border(QuestionBorderWidth, FairyPurple, RoundedCornerShape(QuestionSlotCornerRadius)).clip(RoundedCornerShape(QuestionSlotCornerRadius)).background(FairyPurple.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Text(text = "?", style = MaterialTheme.typography.headlineMedium.copy(fontSize = QuestionFontSize, fontWeight = FontWeight.Bold), color = FairyPurple, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(OptionsTopSpacer))
        Text(text = "Выбери следующий цвет:", style = MaterialTheme.typography.bodyLarge, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(HintTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(OptionsSpacing), verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { index, color ->
                val isSelected = selectedIndex == index
                val isCorrectOption = color == correctColor
                val appearDelay = ELEMENT_STAGGER_DELAY_MS * (PATTERN_LENGTH + 1 + index)
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(showElements) { if (showElements) { delay(appearDelay); isVisible = true } }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
                    val displayColor by animateColorAsState(targetValue = when { isSelected && isCorrectOption -> FairyGreen; isSelected && !isCorrectOption -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrectOption && !isSelected -> FairyGreen.copy(alpha = 0.5f); else -> color }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Display$index")
                    val borderColor by animateColorAsState(targetValue = when { isSelected -> Color.White; isLocked && isCorrectOption && !isSelected -> FairyGreen; else -> Color.Transparent }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Border$index")
                    val elevation = if (isSelected) OptionSelectedElevation else OptionCircleElevation

                    Box(modifier = Modifier.size(OptionCircleSize).shadow(elevation, CircleShape).background(displayColor, CircleShape).then(if (borderColor != Color.Transparent) Modifier.border(if (isSelected) SelectedBorderWidth else CorrectBorderWidth, borderColor, CircleShape) else Modifier).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isLocked) {
                        if (!isLocked) { selectedIndex = index; isLocked = true; AudioPlayer.playSFX(if (isCorrectOption) "correct" else "wrong"); coroutineScope.launch { delay(RESULT_DELAY_MS); onResult(isCorrectOption) } }
                    })
                }
            }
        }

        if (isLocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = if (selectedIndex == options.indexOf(correctColor)) "Правильно!" else "Правильный ответ выделен зелёным.", style = MaterialTheme.typography.bodyMedium, color = if (selectedIndex == options.indexOf(correctColor)) FairyGreen else FairyPink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
