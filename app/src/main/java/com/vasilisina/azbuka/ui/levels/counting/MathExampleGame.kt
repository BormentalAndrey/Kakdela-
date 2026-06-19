// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/counting/MathExampleGame.kt

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

private const val ADD_MIN_A = 1
private const val ADD_MAX_A = 5
private const val ADD_MIN_B = 1
private const val ADD_MAX_RESULT = 10
private const val SUB_MIN_A = 2
private const val SUB_MAX_A = 10
private const val SUB_MIN_B = 1
private const val OPTIONS_COUNT = 3
private const val OPTION_MIN = 0
private const val OPTION_MAX = 10
private val OptionButtonSize = 88.dp
private val OptionButtonSpacing = 14.dp
private val GamePadding = 10.dp
private val OptionsTopSpacer = 20.dp
private val ExampleToOptionsSpacer = 20.dp
private val ButtonCornerRadius = 16.dp
private val ExampleFontSize = 48.sp
private val OperatorFontSize = 40.sp
private val OptionButtonFontSize = 36.sp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val RESULT_DELAY_MS = 1200L
private const val EXAMPLE_APPEAR_DELAY_MS = 200L
private const val EXAMPLE_APPEAR_DURATION_MS = 400
private val ButtonElevation = 4.dp
private val ButtonPressedElevation = 8.dp
private val SelectedBorderWidth = 3.dp

@Composable
fun MathExampleGame(onResult: (correct: Boolean) -> Unit) {
    val example = remember { generateMathExample() }
    val (a, b, op) = example
    val correctAnswer = if (op == "+") a + b else a - b
    val options = remember(correctAnswer) { generateOptions(correctAnswer) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var showExample by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { delay(EXAMPLE_APPEAR_DELAY_MS); showExample = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(GamePadding)) {
        Text(text = "Реши пример:", style = MaterialTheme.typography.headlineSmall, color = DarkText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(ExampleToOptionsSpacer))

        AnimatedVisibility(visible = showExample, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(animationSpec = tween(EXAMPLE_APPEAR_DURATION_MS))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text = "$a", style = MaterialTheme.typography.headlineLarge.copy(fontSize = ExampleFontSize, fontWeight = FontWeight.Bold), color = FairyPurple)
                Text(text = " $op ", style = MaterialTheme.typography.headlineLarge.copy(fontSize = OperatorFontSize, fontWeight = FontWeight.Bold), color = FairyGold)
                Text(text = "$b", style = MaterialTheme.typography.headlineLarge.copy(fontSize = ExampleFontSize, fontWeight = FontWeight.Bold), color = FairyPurple)
                Text(text = " = ?", style = MaterialTheme.typography.headlineLarge.copy(fontSize = ExampleFontSize, fontWeight = FontWeight.Bold), color = FairyGold)
            }
        }

        Spacer(modifier = Modifier.height(OptionsTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(OptionButtonSpacing), verticalAlignment = Alignment.CenterVertically) {
            options.forEach { num ->
                val isSelected = selectedAnswer == num
                val isCorrectAnswer = num == correctAnswer
                val buttonColor by animateColorAsState(targetValue = when { !isLocked -> FairyBlue; isSelected && isCorrectAnswer -> FairyGreen; isSelected && !isCorrectAnswer -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrectAnswer -> FairyGreen.copy(alpha = 0.5f); else -> Color.Gray.copy(alpha = 0.3f) }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "BtnColor")
                val borderColor by animateColorAsState(targetValue = when { isSelected -> FairyPurple; isLocked && isCorrectAnswer && !isSelected -> FairyGreen.copy(alpha = 0.6f); else -> Color.Transparent }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Border")
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

private fun generateMathExample(): Triple<Int, Int, String> {
    return if (Random.nextBoolean()) {
        val a = Random.nextInt(ADD_MIN_A, ADD_MAX_A + 1)
        val maxB = (ADD_MAX_RESULT - a).coerceAtLeast(ADD_MIN_B)
        val b = if (maxB >= ADD_MIN_B) Random.nextInt(ADD_MIN_B, maxB + 1) else ADD_MIN_B
        Triple(a, b, "+")
    } else {
        val a = Random.nextInt(SUB_MIN_A, SUB_MAX_A + 1)
        val b = Random.nextInt(SUB_MIN_B, a + 1)
        Triple(a, b, "−")
    }
}

private fun generateOptions(correctAnswer: Int): List<Int> {
    val wrongAnswers = mutableSetOf<Int>()
    while (wrongAnswers.size < OPTIONS_COUNT - 1) { wrongAnswers.add(Random.nextInt(OPTION_MIN, OPTION_MAX + 1)) }
    return ((wrongAnswers - correctAnswer).take(OPTIONS_COUNT - 1) + correctAnswer).shuffled()
}
