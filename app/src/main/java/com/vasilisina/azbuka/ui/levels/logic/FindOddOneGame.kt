// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/FindOddOneGame.kt

package com.vasilisina.azbuka.ui.levels.logic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ItemSize = 80.dp
private val ItemCornerRadius = 16.dp
private val ItemSpacing = 12.dp
private val GamePadding = 16.dp
private val ItemsTopSpacer = 32.dp
private val HintSpacer = 8.dp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val RESULT_DELAY_MS = 1200L
private const val ITEM_STAGGER_DELAY_MS = 100L
private const val ITEM_APPEAR_DURATION_MS = 350
private val ItemElevation = 6.dp
private val ItemSelectedElevation = 10.dp
private val SelectedBorderWidth = 3.dp
private val CorrectBorderWidth = 3.dp
private val HintFontSize = 16.sp
private val CategoryFontSize = 14.sp
private val ImageInsideSize = 56.dp

private data class GameItem(val imageRes: Int, val category: String, val name: String)

private val AllFruits = listOf(
    GameItem(R.drawable.item_apple, "Фрукты", "Яблоко"),
    GameItem(R.drawable.item_banana, "Фрукты", "Банан"),
    GameItem(R.drawable.item_orange, "Фрукты", "Апельсин")
)
private val AllAnimals = listOf(
    GameItem(R.drawable.item_dog, "Животные", "Собака"),
    GameItem(R.drawable.item_cat, "Животные", "Кошка"),
    GameItem(R.drawable.item_rabbit, "Животные", "Кролик")
)
private val AllShoes = listOf(
    GameItem(R.drawable.item_shoe, "Обувь", "Ботинок"),
    GameItem(R.drawable.item_heels, "Обувь", "Туфелька"),
    GameItem(R.drawable.item_boot, "Обувь", "Сапог")
)
private val AllFlowers = listOf(
    GameItem(R.drawable.item_flower_rose, "Цветы", "Роза"),
    GameItem(R.drawable.item_flower_sunflower, "Цветы", "Подсолнух"),
    GameItem(R.drawable.item_flower_tulip, "Цветы", "Тюльпан")
)
private val AllSports = listOf(
    GameItem(R.drawable.item_ball, "Спорт", "Мяч"),
    GameItem(R.drawable.item_basketball, "Спорт", "Баскетбол"),
    GameItem(R.drawable.item_tennis, "Спорт", "Теннис")
)

private val OddTransport = listOf(
    GameItem(R.drawable.item_car, "Транспорт", "Машина"),
    GameItem(R.drawable.item_plane, "Транспорт", "Самолёт")
)
private val OddFood = listOf(GameItem(R.drawable.item_pizza, "Еда", "Пицца"))
private val OddTech = listOf(GameItem(R.drawable.item_phone, "Техника", "Телефон"))
private val OddMusic = listOf(GameItem(R.drawable.item_guitar, "Музыка", "Гитара"))

private fun generateRandomSet(): List<GameItem> {
    val mainCategory = listOf(AllFruits, AllAnimals, AllShoes, AllFlowers, AllSports).random()
    val mainItems = if (mainCategory.size >= 3) mainCategory.shuffled().take(3) else mainCategory.toList()
    val oddItem = (OddTransport + OddFood + OddTech + OddMusic).random()
    val set = (mainItems + oddItem).shuffled()
    val oddCount = set.count { it.category == oddItem.category }
    return if (oddCount == 1) set else generateRandomSet()
}

@Composable
fun FindOddOneGame(onResult: (correct: Boolean) -> Unit) {
    val currentSet = remember { generateRandomSet() }
    val oddIndex = remember(currentSet) { currentSet.indexOfFirst { item -> currentSet.count { it.category == item.category } == 1 } }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var isLocked by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { showItems = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(GamePadding)) {
        Text(text = "Найди лишнее!", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(HintSpacer))
        Text(text = "Один предмет не подходит к остальным", style = MaterialTheme.typography.bodySmall.copy(fontSize = HintFontSize), color = DarkText.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        Row(horizontalArrangement = Arrangement.spacedBy(ItemSpacing), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            currentSet.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val isCorrectItem = index == oddIndex
                AnimatedGameItem(item = item, isSelected = isSelected, isCorrectItem = isCorrectItem, isLocked = isLocked, appearDelay = ITEM_STAGGER_DELAY_MS * index, showItems = showItems, onClick = {
                    if (!isLocked) { selectedIndex = index; isLocked = true; AudioPlayer.playSFX(if (isCorrectItem) "correct" else "wrong"); coroutineScope.launch { delay(RESULT_DELAY_MS); onResult(isCorrectItem) } }
                })
            }
        }

        if (isLocked) {
            Spacer(modifier = Modifier.height(16.dp))
            val correctItem = currentSet[oddIndex]
            Text(text = if (selectedIndex == oddIndex) "Правильно! Лишний предмет — ${correctItem.name} (${correctItem.category})" else "Лишний предмет — ${correctItem.name} (${correctItem.category})", style = MaterialTheme.typography.bodyMedium, color = if (selectedIndex == oddIndex) FairyGreen else FairyPink, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AnimatedGameItem(item: GameItem, isSelected: Boolean, isCorrectItem: Boolean, isLocked: Boolean, appearDelay: Long, showItems: Boolean, onClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showItems) { if (showItems) { delay(appearDelay); isVisible = true } }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(ITEM_APPEAR_DURATION_MS))) {
        GameItemCell(item = item, isSelected = isSelected, isCorrectItem = isCorrectItem, isLocked = isLocked, onClick = onClick)
    }
}

@Composable
private fun GameItemCell(item: GameItem, isSelected: Boolean, isCorrectItem: Boolean, isLocked: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(targetValue = when { isSelected && isCorrectItem -> FairyGreen; isSelected && !isCorrectItem -> Color.Red.copy(alpha = 0.7f); isLocked && isCorrectItem -> FairyGreen.copy(alpha = 0.3f); else -> FairyBlue }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Bg")
    val borderColor by animateColorAsState(targetValue = when { isSelected -> FairyGold; isLocked && isCorrectItem && !isSelected -> FairyGreen; else -> Color.Transparent }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "Border")
    val elevation = if (isSelected) ItemSelectedElevation else ItemElevation

    // ИСПРАВЛЕНО: weight внутри RowScope — работает
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Box(modifier = Modifier.size(ItemSize).shadow(elevation, RoundedCornerShape(ItemCornerRadius)).background(backgroundColor, RoundedCornerShape(ItemCornerRadius)).then(if (borderColor != Color.Transparent) Modifier.border(if (isSelected) SelectedBorderWidth else CorrectBorderWidth, borderColor, RoundedCornerShape(ItemCornerRadius)) else Modifier).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isLocked) { onClick() }, contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = item.imageRes), contentDescription = item.name, modifier = Modifier.size(ImageInsideSize), contentScale = ContentScale.Fit)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.name, style = MaterialTheme.typography.bodySmall.copy(fontSize = CategoryFontSize, fontWeight = if (isCorrectItem && isLocked) FontWeight.Bold else FontWeight.Normal), color = if (isCorrectItem && isLocked) FairyGreen else DarkText.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
