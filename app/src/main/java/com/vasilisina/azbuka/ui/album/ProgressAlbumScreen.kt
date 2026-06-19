// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/album/ProgressAlbumScreen.kt

package com.vasilisina.azbuka.ui.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Отступ экрана */
private val ScreenPadding = 16.dp

/** Вертикальный отступ заголовка */
private val TitleVerticalPadding = 16.dp

/** Интервал между карточками уровней */
private val CardSpacing = 12.dp

/** Внутренний отступ LazyColumn */
private val LazyColumnVerticalPadding = 8.dp

/** Внутренний отступ карточки */
private val CardInnerPadding = 16.dp

/** Высота кнопки «Назад» */
private val BackButtonHeight = 60.dp

/** Ширина кнопки «Назад» (доля от экрана) */
private const val BACK_BUTTON_WIDTH_FRACTION = 0.6f

/** Отступ перед кнопкой «Назад» */
private val BackButtonTopSpacer = 16.dp

/** Радиус скругления карточек */
private val CardCornerRadius = 16.dp

/** Тень карточки */
private val CardElevation = 4.dp

/** Тень карточки при нажатии (не используется, но константа) */
private val CardPressedElevation = 8.dp

/** Длительность анимации появления карточки (мс) */
private const val CARD_ANIMATION_DURATION_MS = 400

/** Задержка между появлением карточек (stagger, мс) */
private const val CARD_STAGGER_DELAY_MS = 100L

/** Длительность анимации прогресс-бара (мс) */
private const val PROGRESS_ANIMATION_DURATION_MS = 1000

/** Высота прогресс-бара */
private val ProgressBarHeight = 12.dp

/** Радиус скругления прогресс-бара */
private val ProgressBarCornerRadius = 6.dp

/** Размер шрифта для общего количества звёзд */
private val TotalStarsFontSize = 18.sp

// -------------------------------------------------------------------------
// Модель данных
// -------------------------------------------------------------------------

/**
 * Данные об уровне для альбома.
 *
 * @property level Номер уровня (1–5).
 * @property title Название уровня (например, «Алфавит»).
 */
private data class AlbumLevel(
    val level: Int,
    val title: String
)

// -------------------------------------------------------------------------
// Главный экран альбома
// -------------------------------------------------------------------------

/**
 * Альбом успехов — показывает прогресс по всем уровням.
 *
 * Особенности:
 * - Заголовок «Альбом успехов»
 * - Общий прогресс-бар (сколько звёзд собрано)
 * - Список карточек уровней с анимацией появления
 * - Каждая карточка: название, номер, звёзды (★/☆) или 🔒
 * - Кнопка «Назад» внизу
 *
 * @param onBack Колбэк для возврата в главное меню / на карту.
 */
@Composable
fun ProgressAlbumScreen(
    onBack: () -> Unit
) {
    val levels = remember {
        listOf(
            AlbumLevel(1, "Алфавит"),
            AlbumLevel(2, "Счёт"),
            AlbumLevel(3, "Печать"),
            AlbumLevel(4, "Логика"),
            AlbumLevel(5, "Финал")
        )
    }

    val totalStars = GameState.getTotalStars()
    val maxStars = GameState.TOTAL_MAX_STARS
    val completionPercent = GameState.getCompletionPercent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        AlbumTitle()

        // Общий прогресс
        OverallProgress(
            totalStars = totalStars,
            maxStars = maxStars,
            completionPercent = completionPercent
        )

        Spacer(modifier = Modifier.height(TitleVerticalPadding))

        // Список уровней
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CardSpacing),
            contentPadding = PaddingValues(vertical = LazyColumnVerticalPadding)
        ) {
            itemsIndexed(
                items = levels,
                key = { _, item -> item.level }
            ) { index, item ->
                AnimatedLevelCard(
                    level = item,
                    index = index,
                    stars = GameState.getStars(item.level),
                    isUnlocked = GameState.isLevelUnlocked(item.level)
                )
            }
        }

        // Кнопка «Назад»
        Spacer(modifier = Modifier.height(BackButtonTopSpacer))

        BackButton(onClick = {
            AudioPlayer.playSFX("click")
            onBack()
        })
    }
}

// -------------------------------------------------------------------------
// Заголовок альбома
// -------------------------------------------------------------------------

/**
 * Заголовок «Альбом успехов» с декоративным звёздным градиентом.
 */
@Composable
private fun AlbumTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TitleVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Альбом успехов",
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(FairyPurple, FairyGold, FairyPink)
                )
            ),
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Общий прогресс
// -------------------------------------------------------------------------

/**
 * Блок с общим прогрессом: звёзды X/Y + прогресс-бар.
 *
 * @param totalStars       Собрано звёзд.
 * @param maxStars         Максимально возможное количество звёзд.
 * @param completionPercent Процент завершения (0–100).
 */
@Composable
private fun OverallProgress(
    totalStars: Int,
    maxStars: Int,
    completionPercent: Int
) {
    var showProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showProgress = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Текст: «Собрано 7 из 15 звёзд»
        Text(
            text = "Собрано $totalStars из $maxStars звёзд",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = TotalStarsFontSize,
                fontWeight = FontWeight.Medium
            ),
            color = DarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Прогресс-бар
        AnimatedVisibility(visible = showProgress) {
            LinearProgressIndicator(
                progress = { completionPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(ProgressBarHeight)
                    .background(
                        Color.LightGray.copy(alpha = 0.3f),
                        RoundedCornerShape(ProgressBarCornerRadius)
                    ),
                color = FairyGold,
                trackColor = Color.LightGray.copy(alpha = 0.3f),
            )
        }

        // Процент
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$completionPercent%",
            style = MaterialTheme.typography.bodySmall,
            color = DarkText.copy(alpha = 0.6f)
        )
    }
}

// -------------------------------------------------------------------------
// Анимированная карточка уровня
// -------------------------------------------------------------------------

/**
 * Карточка уровня с анимацией появления (stagger + scale).
 *
 * @param level      Данные уровня.
 * @param index      Индекс в списке (для задержки анимации).
 * @param stars      Количество звёзд (0–3).
 * @param isUnlocked Открыт ли уровень.
 */
@Composable
private fun AnimatedLevelCard(
    level: AlbumLevel,
    index: Int,
    stars: Int,
    isUnlocked: Boolean
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(CARD_STAGGER_DELAY_MS * (index + 1))
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(CARD_ANIMATION_DURATION_MS)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        LevelProgressCard(
            name = level.title,
            level = level.level,
            stars = stars,
            isUnlocked = isUnlocked
        )
    }
}

// -------------------------------------------------------------------------
// Карточка прогресса уровня
// -------------------------------------------------------------------------

/**
 * Карточка одного уровня в альбоме.
 *
 * Отображает:
 * - Название уровня и его номер
 * - Звёзды (★/☆) или 🔒 (если уровень закрыт)
 * - Фон: белый (открыт) / светло-серый (закрыт)
 *
 * @param name       Название уровня (например, «Алфавит»).
 * @param level      Номер уровня (1–5).
 * @param stars      Количество полученных звёзд (0–3).
 * @param isUnlocked Открыт ли уровень.
 */
@Composable
private fun LevelProgressCard(
    name: String,
    level: Int,
    stars: Int,
    isUnlocked: Boolean
) {
    val safeStars = stars.coerceIn(0, GameState.MAX_STARS_PER_LEVEL)

    // Анимация цвета фона карточки
    val cardColor by animateColorAsState(
        targetValue = if (isUnlocked) {
            Color.White
        } else {
            Color.LightGray.copy(alpha = 0.30f)
        },
        animationSpec = tween(300),
        label = "CardColor"
    )

    // Анимация цвета текста звёзд
    val starsColor by animateColorAsState(
        targetValue = if (isUnlocked) {
            FairyGold
        } else {
            Color.Gray
        },
        animationSpec = tween(300),
        label = "StarsColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CardElevation
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Левая часть: название + номер
            Text(
                text = "$name (Ур. $level)",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (safeStars > 0) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isUnlocked) {
                    DarkText
                } else {
                    Color.Gray
                },
                modifier = Modifier.weight(1f)
            )

            // Правая часть: звёзды или замок
            Text(
                text = if (isUnlocked) {
                    buildStarString(safeStars, GameState.MAX_STARS_PER_LEVEL)
                } else {
                    "🔒"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = starsColor
            )
        }
    }
}

// -------------------------------------------------------------------------
// Кнопка «Назад»
// -------------------------------------------------------------------------

/**
 * Кнопка возврата в главное меню / на карту.
 *
 * @param onClick Действие при нажатии.
 */
@Composable
private fun BackButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(BACK_BUTTON_WIDTH_FRACTION)
            .height(BackButtonHeight),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = FairyPurple,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = CardElevation,
            pressedElevation = CardPressedElevation,
            focusedElevation = CardElevation,
            hoveredElevation = CardElevation,
            disabledElevation = 0.dp
        )
    ) {
        Text(
            text = "Назад",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

// -------------------------------------------------------------------------
// Утилиты
// -------------------------------------------------------------------------

/**
 * Формирует строку со звёздами: ★★★ (если 3), ★★☆ (если 2) и т.д.
 *
 * @param filled  Количество заполненных звёзд.
 * @param total   Общее количество звёзд.
 * @return Строка вида "★★★", "★★☆", "★☆☆".
 */
private fun buildStarString(filled: Int, total: Int): String {
    return buildString {
        repeat(filled) { append('★') }
        repeat(total - filled) { append('☆') }
    }
}
