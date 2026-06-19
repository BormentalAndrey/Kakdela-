// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/FindOddOneGame.kt

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

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Размер ячейки с предметом */
private val ItemSize = 80.dp

/** Радиус скругления ячеек */
private val ItemCornerRadius = 16.dp

/** Горизонтальный интервал между предметами */
private val ItemSpacing = 12.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед предметами */
private val ItemsTopSpacer = 32.dp

/** Отступ между заголовком и подсказкой */
private val HintSpacer = 8.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед вызовом onResult (мс) */
private const val RESULT_DELAY_MS = 1200L

/** Задержка появления предметов (stagger, мс) */
private const val ITEM_STAGGER_DELAY_MS = 100L

/** Длительность анимации появления предмета (мс) */
private const val ITEM_APPEAR_DURATION_MS = 350

/** Тень ячейки */
private val ItemElevation = 6.dp

/** Тень ячейки при выборе */
private val ItemSelectedElevation = 10.dp

/** Толщина обводки выбранной ячейки */
private val SelectedBorderWidth = 3.dp

/** Толщина обводки правильного ответа */
private val CorrectBorderWidth = 3.dp

/** Размер шрифта emoji */
private val EmojiFontSize = 40.sp

/** Размер шрифта подсказки */
private val HintFontSize = 16.sp

/** Размер шрифта названия категории */
private val CategoryFontSize = 14.sp

// -------------------------------------------------------------------------
// Модель данных
// -------------------------------------------------------------------------

/**
 * Предмет для игры «Найди лишнее».
 *
 * @property emoji    Emoji-представление предмета.
 * @property category Категория, к которой относится предмет.
 */
private data class GameItem(
    val emoji: String,
    val category: String
)

// -------------------------------------------------------------------------
// Наборы предметов
// -------------------------------------------------------------------------

/** Наборы из 4 предметов: 3 одной категории + 1 лишний */
private val ItemSets = listOf(
    listOf(
        GameItem("🍎", "Фрукты"),
        GameItem("🍌", "Фрукты"),
        GameItem("🍊", "Фрукты"),
        GameItem("🚗", "Транспорт")
    ),
    listOf(
        GameItem("🐶", "Животные"),
        GameItem("🐱", "Животные"),
        GameItem("🐰", "Животные"),
        GameItem("✈️", "Транспорт")
    ),
    listOf(
        GameItem("👟", "Обувь"),
        GameItem("👠", "Обувь"),
        GameItem("👢", "Обувь"),
        GameItem("🍕", "Еда")
    ),
    listOf(
        GameItem("🌹", "Цветы"),
        GameItem("🌻", "Цветы"),
        GameItem("🌷", "Цветы"),
        GameItem("📱", "Техника")
    ),
    listOf(
        GameItem("⚽", "Спорт"),
        GameItem("🏀", "Спорт"),
        GameItem("🎾", "Спорт"),
        GameItem("🎸", "Музыка")
    )
)

// -------------------------------------------------------------------------
// Игра «Найди лишнее»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Найди лишнее».
 *
 * На экране 4 предмета, 3 из которых принадлежат одной категории,
 * а один — лишний. Ребёнок должен найти и нажать на лишний предмет.
 *
 * Особенности:
 * - 5 различных наборов предметов (фрукты, животные, обувь, цветы, спорт)
 * - Каскадная анимация появления предметов
 * - Визуальная обратная связь: зелёный (правильно) / красный (ошибка)
 * - Подсветка правильного ответа зелёной обводкой после выбора
 * - Отображение категории под каждым предметом
 * - Задержка 1.2 сек для осознания результата
 * - Звуковые эффекты correct / wrong
 *
 * @param onResult Колбэк: `true` если выбран правильный (лишний) предмет.
 */
@Composable
fun FindOddOneGame(onResult: (correct: Boolean) -> Unit) {
    // Выбираем случайный набор
    val currentSet = remember { ItemSets.random() }

    // Находим индекс лишнего предмета (тот, чья категория встречается 1 раз)
    val oddIndex = remember(currentSet) {
        currentSet.indexOfFirst { item ->
            currentSet.count { it.category == item.category } == 1
        }
    }

    // Выбранный индекс (null = ещё не выбрано)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Флаг блокировки после выбора
    var isLocked by remember { mutableStateOf(false) }

    // Флаг для анимации появления
    var showItems by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Запускаем анимацию появления
    LaunchedEffect(Unit) {
        showItems = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Найди лишнее!",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(HintSpacer))

        // Подсказка
        Text(
            text = "Один предмет не подходит к остальным",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = HintFontSize
            ),
            color = DarkText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ItemsTopSpacer))

        // Предметы
        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            currentSet.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val isCorrectItem = index == oddIndex

                // Stagger-задержка для анимации
                val appearDelay = ITEM_STAGGER_DELAY_MS * index

                AnimatedGameItem(
                    item = item,
                    index = index,
                    isSelected = isSelected,
                    isCorrectItem = isCorrectItem,
                    isLocked = isLocked,
                    appearDelay = appearDelay,
                    showItems = showItems,
                    onClick = {
                        if (!isLocked) {
                            selectedIndex = index
                            isLocked = true
                            val isCorrect = isCorrectItem

                            AudioPlayer.playSFX(if (isCorrect) "correct" else "wrong")

                            coroutineScope.launch {
                                delay(RESULT_DELAY_MS)
                                onResult(isCorrect)
                            }
                        }
                    }
                )
            }
        }

        // Подсказка с результатом
        if (isLocked) {
            Spacer(modifier = Modifier.height(16.dp))
            val correctItem = currentSet[oddIndex]
            Text(
                text = if (selectedIndex == oddIndex) {
                    "Правильно! Лишний предмет — ${correctItem.emoji} (${correctItem.category})"
                } else {
                    "Лишний предмет — ${correctItem.emoji} (${correctItem.category})"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selectedIndex == oddIndex) FairyGreen else FairyPink,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Анимированный предмет
// -------------------------------------------------------------------------

/**
 * Ячейка с предметом, появляющаяся с задержкой и анимацией.
 *
 * @param item          Предмет для отображения.
 * @param index         Индекс в ряду.
 * @param isSelected    Выбран ли этот предмет.
 * @param isCorrectItem Является ли этот предмет правильным ответом.
 * @param isLocked      Заблокирован ли выбор.
 * @param appearDelay   Задержка перед появлением (мс).
 * @param showItems     Флаг запуска анимации.
 * @param onClick       Колбэк при нажатии.
 */
@Composable
private fun AnimatedGameItem(
    item: GameItem,
    index: Int,
    isSelected: Boolean,
    isCorrectItem: Boolean,
    isLocked: Boolean,
    appearDelay: Long,
    showItems: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showItems) {
        if (showItems) {
            delay(appearDelay)
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(
            animationSpec = tween(ITEM_APPEAR_DURATION_MS)
        )
    ) {
        GameItemCell(
            item = item,
            isSelected = isSelected,
            isCorrectItem = isCorrectItem,
            isLocked = isLocked,
            onClick = onClick
        )
    }
}

// -------------------------------------------------------------------------
// Ячейка с предметом
// -------------------------------------------------------------------------

/**
 * Одна ячейка с emoji-предметом и названием категории.
 *
 * Состояния:
 * - **Не выбрана**: голубой фон
 * - **Выбрана правильно**: зелёный фон + золотая обводка
 * - **Выбрана неправильно**: красный фон
 * - **После блокировки**: правильный ответ подсвечивается зелёной обводкой
 *
 * @param item          Предмет.
 * @param isSelected    Выбрана ли эта ячейка.
 * @param isCorrectItem Правильный ли это ответ.
 * @param isLocked      Заблокирован ли выбор.
 * @param onClick       Колбэк при нажатии.
 */
@Composable
private fun GameItemCell(
    item: GameItem,
    isSelected: Boolean,
    isCorrectItem: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    // Анимированный цвет фона
    val backgroundColor by animateColorAsState(
        targetValue = when {
            // Выбрана правильно — зелёный
            isSelected && isCorrectItem -> FairyGreen
            // Выбрана неправильно — красный
            isSelected && !isCorrectItem -> Color.Red.copy(alpha = 0.7f)
            // После блокировки правильный ответ — светло-зелёный
            isLocked && isCorrectItem -> FairyGreen.copy(alpha = 0.3f)
            // Обычное состояние — голубой
            else -> FairyBlue
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "OddItemBackground"
    )

    // Анимированный цвет обводки
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> FairyGold
            isLocked && isCorrectItem && !isSelected -> FairyGreen
            else -> Color.Transparent
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "OddItemBorder"
    )

    // Цвет текста (emoji)
    val emojiColor = when {
        isSelected -> Color.White
        else -> DarkText
    }

    // Тень
    val elevation = if (isSelected) ItemSelectedElevation else ItemElevation

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        // Кнопка с emoji
        Box(
            modifier = Modifier
                .size(ItemSize)
                .shadow(elevation, RoundedCornerShape(ItemCornerRadius))
                .background(backgroundColor, RoundedCornerShape(ItemCornerRadius))
                .then(
                    if (borderColor != Color.Transparent) {
                        Modifier.border(
                            width = if (isSelected) SelectedBorderWidth else CorrectBorderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(ItemCornerRadius)
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Без ripple (сказочный стиль)
                    enabled = !isLocked
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.emoji,
                fontSize = EmojiFontSize,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Название категории
        Text(
            text = item.category,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = CategoryFontSize,
                fontWeight = if (isCorrectItem && isLocked) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isCorrectItem && isLocked) FairyGreen else DarkText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}
