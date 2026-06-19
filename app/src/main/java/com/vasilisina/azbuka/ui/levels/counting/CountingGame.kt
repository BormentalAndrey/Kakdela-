// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/CountingGame.kt

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val MIN_ITEM_COUNT = 1
private const val MAX_ITEM_COUNT = 10
private const val OPTIONS_COUNT = 3
private val ItemCircleSize = 40.dp
private val ItemCirclePadding = 4.dp
private val OptionButtonSize = 88.dp
private val OptionButtonSpacing = 14.dp
private val GamePadding = 10.dp
private val ItemsTopSpacer = 16.dp
private val OptionsTopSpacer = 24.dp
private val ItemsMinHeight = 100.dp
private val ButtonCornerRadius = 16.dp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val RESULT_DELAY_MS = 1200L
private const val ITEM_STAGGER_DELAY_MS = 50L
private const val ITEM_APPEAR_DURATION_MS = 250
private val ButtonElevation = 4.dp
private val ButtonPressedElevation = 8.dp
private val OptionButtonFontSize = 36.sp
private val SelectedBorderWidth = 3.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountingGame(onResult: (correct: Boolean) -> Unit) {
    val itemCount = remember { Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1) }
    val options = remember(itemCount) {
        val wrongAnswers = mutableSetOf<Int>()
        while (wrongAnswers.size < OPTIONS_COUNT - 1) { wrongAnswers.add(Random.nextInt(MIN_ITEM_COUNT, MAX_ITEM_COUNT + 1)) }
        (wrongAnswers - itemCount).take(OPTIONS_COUNT - 1).let { (it + itemCount).shuffled() }
    }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { showItems = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Сколько здесь предметов?", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        ItemCircles(itemCount = itemCount, showItems = showItems)
        Spacer(modifier = Modifier.height(OptionsTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(OptionButtonSpacing), verticalAlignment = Alignment.CenterVertically) {
            options.forEach { num ->
                val isSelected = selectedAnswer == num
                val isCorrectAnswer = num == itemCount
                val buttonColor by animateColorAsState(targetValue = when { !isLocked -> FairyBlue; isSelected && isCorrectAnswer -> FairyGreen; isSelected && !isCorrectAnswer -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrectAnswer -> FairyGreen.copy(alpha = 0.5f); else -> Color.Gray.copy(alpha = 0.3f) }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "BtnColor")
                val borderColor by animateColorAsState(targetValue = when { isSelected -> FairyPurple; isLocked && isCorrectAnswer && !isSelected -> FairyGreen.copy(alpha = 0.6f); else -> Color.Transparent }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "BorderColor")
                val textColor = when { isSelected -> Color.White; isLocked && isCorrectAnswer -> DarkText; isLocked && !isSelected -> Color.White.copy(alpha = 0.5f); else -> DarkText }

                Button(
                    onClick = { if (!isLocked) { selectedAnswer = num; isLocked = true; AudioPlayer.playSFX(if (isCorrectAnswer) "correct" else "wrong"); coroutineScope.launch { delay(RESULT_DELAY_MS); onResult(isCorrectAnswer) } } },
                    enabled = !isLocked,
                    modifier = Modifier.size(OptionButtonSize).shadow(if (isSelected) ButtonPressedElevation else ButtonElevation, RoundedCornerShape(ButtonCornerRadius)).then(if (borderColor != Color.Transparent) Modifier.border(SelectedBorderWidth, borderColor, RoundedCornerShape(ButtonCornerRadius)) else Modifier),
                    shape = RoundedCornerShape(ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = textColor, disabledContainerColor = buttonColor, disabledContentColor = textColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = ButtonElevation, pressedElevation = ButtonPressedElevation, disabledElevation = if (isSelected) ButtonPressedElevation else 0.dp)
                ) {
                    Text(text = "$num", style = MaterialTheme.typography.headlineMedium.copy(fontSize = OptionButtonFontSize, fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCircles(itemCount: Int, showItems: Boolean) {
    FlowRow(modifier = Modifier.fillMaxWidth().heightIn(min = ItemsMinHeight), horizontalArrangement = Arrangement.Center, verticalArrangement = Arrangement.Center) {
        repeat(itemCount) { index ->
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(showItems) { if (showItems) { delay(ITEM_STAGGER_DELAY_MS * index); isVisible = true } }
            AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(ITEM_APPEAR_DURATION_MS))) {
                Box(modifier = Modifier.padding(ItemCirclePadding).size(ItemCircleSize).shadow(4.dp, CircleShape).background(FairyGold, CircleShape).border(1.dp, FairyGold.copy(alpha = 0.5f), CircleShape))
            }
        }
    }
}
