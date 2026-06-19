// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/ComparisonGame.kt

package com.vasilisina.azbuka.ui.levels.counting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MIN_ITEM_COUNT = 2
private const val MAX_ITEM_COUNT = 8
private val ItemCircleSize = 28.dp
private val ItemVerticalPadding = 2.dp
private val ComparisonButtonSize = 100.dp
private val ItemsToButtonSpacer = 12.dp
private val GamePadding = 10.dp
private val GroupsTopSpacer = 16.dp
private val GroupsHorizontalSpacer = 20.dp
private val ButtonCornerRadius = 14.dp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val RESULT_DELAY_MS = 1200L
private const val ITEM_STAGGER_DELAY_MS = 40L
private const val ITEM_APPEAR_DURATION_MS = 250
private val ButtonElevation = 4.dp
private val ButtonPressedElevation = 8.dp
private val CircleElevation = 3.dp
private val SelectedBorderWidth = 3.dp
private val ButtonFontSize = 22.sp
private val GroupLabelFontSize = 13.sp

@Composable
fun ComparisonGame(onResult: (correct: Boolean) -> Unit) {
    val countLeft = remember { Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1) }
    val countRight = remember(countLeft) { var c: Int; do { c = Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1) } while (c == countLeft); c }
    val biggerSide = if (countLeft > countRight) "left" else "right"
    var selectedSide by remember { mutableStateOf<String?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Где больше предметов?", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(GroupsTopSpacer))

        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            ComparisonSide(count = countLeft, side = "left", label = "Слева", circleColor = FairyPink, selectedSide = selectedSide, biggerSide = biggerSide, isLocked = isLocked, onSelect = { side ->
                if (!isLocked) { selectedSide = side; isLocked = true; AudioPlayer.playSFX(if (side == biggerSide) "correct" else "wrong"); coroutineScope.launch { delay(RESULT_DELAY_MS); onResult(side == biggerSide) } }
            })
            Spacer(modifier = Modifier.padding(horizontal = GroupsHorizontalSpacer))
            ComparisonSide(count = countRight, side = "right", label = "Справа", circleColor = FairyGold, selectedSide = selectedSide, biggerSide = biggerSide, isLocked = isLocked, onSelect = { side ->
                if (!isLocked) { selectedSide = side; isLocked = true; AudioPlayer.playSFX(if (side == biggerSide) "correct" else "wrong"); coroutineScope.launch { delay(RESULT_DELAY_MS); onResult(side == biggerSide) } }
            })
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ComparisonSide(count: Int, side: String, label: String, circleColor: Color, selectedSide: String?, biggerSide: String, isLocked: Boolean, onSelect: (String) -> Unit) {
    val isSelected = selectedSide == side
    val isCorrectSide = side == biggerSide
    val buttonColor by animateColorAsState(targetValue = when { !isLocked -> FairyBlue; isSelected && isCorrectSide -> FairyGreen; isSelected && !isCorrectSide -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrectSide -> FairyGreen.copy(alpha = 0.5f); else -> Color.Gray.copy(alpha = 0.3f) }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "BtnColor$side")
    val borderColor by animateColorAsState(targetValue = when { isSelected -> FairyPurple; isLocked && isCorrectSide && !isSelected -> FairyGreen.copy(alpha = 0.6f); else -> Color.Transparent }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Border$side")
    val textColor = when { isSelected -> Color.White; isLocked && isCorrectSide -> DarkText; isLocked && !isSelected -> Color.White.copy(alpha = 0.5f); else -> DarkText }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = GroupLabelFontSize, fontWeight = FontWeight.Medium), color = DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            repeat(count) { index ->
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(ITEM_STAGGER_DELAY_MS * index); isVisible = true }
                AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(ITEM_APPEAR_DURATION_MS))) {
                    Box(modifier = Modifier.padding(vertical = ItemVerticalPadding).size(ItemCircleSize).shadow(CircleElevation, CircleShape).background(circleColor, CircleShape).border(1.dp, circleColor.copy(alpha = 0.5f), CircleShape))
                }
            }
        }

        Spacer(modifier = Modifier.height(ItemsToButtonSpacer))

        Button(
            onClick = { onSelect(side) },
            enabled = !isLocked,
            modifier = Modifier.size(ComparisonButtonSize).shadow(if (isSelected) ButtonPressedElevation else ButtonElevation, RoundedCornerShape(ButtonCornerRadius)).then(if (borderColor != Color.Transparent) Modifier.border(SelectedBorderWidth, borderColor, RoundedCornerShape(ButtonCornerRadius)) else Modifier),
            shape = RoundedCornerShape(ButtonCornerRadius),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = textColor, disabledContainerColor = buttonColor, disabledContentColor = textColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = ButtonElevation, pressedElevation = ButtonPressedElevation, disabledElevation = if (isSelected) ButtonPressedElevation else 0.dp)
        ) {
            Text(text = "Тут больше", style = MaterialTheme.typography.labelLarge.copy(fontSize = ButtonFontSize, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        }
    }
}
