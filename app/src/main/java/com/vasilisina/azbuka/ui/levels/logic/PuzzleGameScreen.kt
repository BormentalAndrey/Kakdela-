// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/PuzzleGameScreen.kt

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
import androidx.compose.runtime.mutableStateListOf
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
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import kotlinx.coroutines.delay

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Части пазла (правильный порядок) */
private val PuzzlePieces = listOf("🧩1", "🧩2", "🧩3", "🧩4")

/** Количество частей пазла */
private const val PUZZLE_SIZE = 4

/** Размер ячейки сетки */
private val GridCellSize = 90.dp

/** Размер кнопки с кусочком */
private val PieceButtonSize = 70.dp

/** Радиус скругления ячеек и кнопок */
private val CornerRadius = 12.dp

/** Толщина обводки ячейки */
private val GridBorderWidth = 2.dp

/** Толщина обводки при ошибке */
private val ErrorBorderWidth = 3.dp

/** Толщина обводки при успехе */
private val SuccessBorderWidth = 3.dp

/** Горизонтальный/вертикальный интервал между ячейками */
private val GridSpacing = 8.dp

/** Горизонтальный интервал между кусочками */
private val PieceSpacing = 12.dp

/** Отступ контейнера */
private val GamePadding = 16.dp

/** Отступ перед сеткой */
private val GridTopSpacer = 24.dp

/** Отступ перед кусочками */
private val PiecesTopSpacer = 32.dp

/** Отступ перед подсказкой */
private val HintSpacer = 8.dp

/** Длительность анимации смены цвета (мс) */
private const val COLOR_ANIMATION_DURATION_MS = 300

/** Задержка перед сбросом при ошибке (мс) */
private const val ERROR_RESET_DELAY_MS = 1000L

/** Задержка перед вызовом onResult при успехе (мс) */
private const val SUCCESS_DELAY_MS = 1000L

/** Задержка появления элементов (stagger, мс) */
private const val ELEMENT_STAGGER_DELAY_MS = 100L

/** Длительность анимации появления (мс) */
private const val ELEMENT_APPEAR_DURATION_MS = 350

/** Тень ячейки сетки */
private val GridCellElevation = 4.dp

/** Тень кнопки с кусочком */
private val PieceButtonElevation = 4.dp

/** Тень кнопки при выборе */
private val PieceSelectedElevation = 8.dp

/** Размер шрифта в ячейке */
private val GridCellFontSize = 36.sp

/** Размер шрифта на кусочке */
private val PieceFontSize = 28.sp

/** Размер шрифта подсказки */
private val HintFontSize = 16.sp

/** Цвет фона ячейки при ошибке */
private val ErrorBackgroundColor = Color(0xFFFFEBEE)

// -------------------------------------------------------------------------
// Игра «Собери пазл»
// -------------------------------------------------------------------------

/**
 * Мини-игра «Собери пазл».
 *
 * Ребёнок собирает картинку из 4 частей на сетке 2×2.
 * Кусочки перемешаны, нужно расставить их в правильном порядке.
 *
 * Особенности:
 * - 4 части пазла: 🧩1, 🧩2, 🧩3, 🧩4
 * - Сетка 2×2 для размещения
 * - Каскадная анимация появления ячеек и кусочков
 * - Выбор кусочка → нажатие на ячейку для размещения
 * - Повторное нажатие на кусочек отменяет выбор
 * - Визуальная обратная связь при успехе / ошибке
 * - Красная подсветка сетки при неправильной сборке
 * - Автосброс через 1 сек при ошибке
 * - Звуковые эффекты correct / wrong
 *
 * @param onResult Колбэк: `true` если пазл собран правильно.
 */
@Composable
fun PuzzleGameScreen(onResult: (correct: Boolean) -> Unit) {
    // Кусочки в перемешанном порядке
    val shuffledPieces = remember { PuzzlePieces.shuffled() }

    // Сетка 2×2: хранит кусочки или null
    val grid = remember { mutableStateListOf<String?>(null, null, null, null) }

    // Индекс выбранного кусочка (-1 = не выбран)
    var selectedPieceIndex by remember { mutableIntStateOf(-1) }

    // Флаг ошибки (красная подсветка)
    var isError by remember { mutableStateOf(false) }

    // Флаг успеха
    var isSuccess by remember { mutableStateOf(false) }

    // Флаг блокировки
    var isLocked by remember { mutableStateOf(false) }

    // Флаг анимации появления
    var showElements by remember { mutableStateOf(false) }

    // Проверка завершения
    val isComplete = grid.all { it != null }
    val isCorrect = grid.toList() == PuzzlePieces

    // Обработка завершения
    LaunchedEffect(isComplete) {
        if (!isComplete || isLocked) return@LaunchedEffect

        isLocked = true

        if (isCorrect) {
            // Успех!
            isSuccess = true
            AudioPlayer.playSFX("correct")
            delay(SUCCESS_DELAY_MS)
            onResult(true)
        } else {
            // Ошибка — красная подсветка и сброс
            isError = true
            AudioPlayer.playSFX("wrong")
            delay(ERROR_RESET_DELAY_MS)
            grid.fill(null)
            selectedPieceIndex = -1
            isError = false
            isLocked = false
        }
    }

    // Запускаем анимацию появления
    LaunchedEffect(Unit) {
        showElements = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GamePadding)
    ) {
        // Заголовок
        Text(
            text = "Собери пазл",
            style = MaterialTheme.typography.headlineMedium,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(HintSpacer))

        // Подсказка
        Text(
            text = if (isSuccess) {
                "Правильно! Пазл собран."
            } else if (selectedPieceIndex >= 0) {
                "Выбери ячейку для кусочка"
            } else {
                "Выбери кусочек и помести его в ячейку"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = HintFontSize
            ),
            color = when {
                isSuccess -> FairyGreen
                isError -> FairyPink
                else -> DarkText.copy(alpha = 0.6f)
            },
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(GridTopSpacer))

        // Сетка 2×2
        PuzzleGrid(
            grid = grid,
            isError = isError,
            isSuccess = isSuccess,
            showElements = showElements,
            onCellClick = { cellIndex ->
                if (isLocked) return@PuzzleGrid

                if (selectedPieceIndex >= 0 && grid[cellIndex] == null) {
                    // Размещаем кусочек в ячейку
                    grid[cellIndex] = shuffledPieces[selectedPieceIndex]
                    selectedPieceIndex = -1
                }
            }
        )

        Spacer(modifier = Modifier.height(PiecesTopSpacer))

        // Кусочки для выбора
        PuzzlePiecesRow(
            pieces = shuffledPieces,
            usedPieces = grid.filterNotNull(),
            selectedIndex = selectedPieceIndex,
            showElements = showElements,
            onPieceClick = { pieceIndex ->
                if (isLocked) return@PuzzlePiecesRow

                val piece = shuffledPieces[pieceIndex]
                val isUsed = grid.contains(piece)

                if (!isUsed) {
                    // Переключаем выбор
                    selectedPieceIndex = if (selectedPieceIndex == pieceIndex) -1 else pieceIndex
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Сетка пазла
// -------------------------------------------------------------------------

/**
 * Сетка 2×2 для размещения кусочков пазла.
 *
 * @param grid          Состояние сетки.
 * @param isError       Флаг ошибки (красная подсветка).
 * @param isSuccess     Флаг успеха (зелёная подсветка).
 * @param showElements  Флаг анимации появления.
 * @param onCellClick   Колбэк при нажатии на ячейку.
 */
@Composable
private fun PuzzleGrid(
    grid: List<String?>,
    isError: Boolean,
    isSuccess: Boolean,
    showElements: Boolean,
    onCellClick: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(GridSpacing)
    ) {
        for (row in 0..1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(GridSpacing)
            ) {
                for (col in 0..1) {
                    val cellIndex = row * 2 + col
                    val piece = grid[cellIndex]

                    // Stagger-задержка
                    val appearDelay = ELEMENT_STAGGER_DELAY_MS * cellIndex

                    AnimatedGridCell(
                        piece = piece,
                        isError = isError,
                        isSuccess = isSuccess,
                        appearDelay = appearDelay,
                        showElements = showElements,
                        onClick = { onCellClick(cellIndex) }
                    )
                }
            }
        }
    }
}

/**
 * Анимированная ячейка сетки.
 */
@Composable
private fun AnimatedGridCell(
    piece: String?,
    isError: Boolean,
    isSuccess: Boolean,
    appearDelay: Long,
    showElements: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showElements) {
        if (showElements) {
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
            animationSpec = tween(ELEMENT_APPEAR_DURATION_MS)
        )
    ) {
        GridCell(
            piece = piece,
            isError = isError,
            isSuccess = isSuccess,
            onClick = onClick
        )
    }
}

/**
 * Одна ячейка сетки.
 */
@Composable
private fun GridCell(
    piece: String?,
    isError: Boolean,
    isSuccess: Boolean,
    onClick: () -> Unit
) {
    // Анимированный цвет обводки
    val borderColor by animateColorAsState(
        targetValue = when {
            isSuccess && piece != null -> FairyGreen
            isError -> Color.Red
            piece != null -> FairyGold
            else -> FairyPurple
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "GridBorderColor"
    )

    // Анимированный цвет фона
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isError -> ErrorBackgroundColor
            isSuccess && piece != null -> FairyGreen.copy(alpha = 0.15f)
            piece != null -> FairyGold.copy(alpha = 0.15f)
            else -> Color.White
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "GridBackgroundColor"
    )

    // Толщина обводки
    val borderWidth = when {
        isError -> ErrorBorderWidth
        isSuccess && piece != null -> SuccessBorderWidth
        else -> GridBorderWidth
    }

    Box(
        modifier = Modifier
            .size(GridCellSize)
            .shadow(GridCellElevation, RoundedCornerShape(CornerRadius))
            .clip(RoundedCornerShape(CornerRadius))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(CornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = piece ?: "",
            fontSize = GridCellFontSize,
            fontWeight = if (piece != null) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// -------------------------------------------------------------------------
// Кусочки пазла
// -------------------------------------------------------------------------

/**
 * Ряд с кусочками пазла для выбора.
 *
 * @param pieces         Перемешанные кусочки.
 * @param usedPieces     Уже размещённые кусочки.
 * @param selectedIndex  Индекс выбранного кусочка (-1 = не выбран).
 * @param showElements   Флаг анимации появления.
 * @param onPieceClick   Колбэк при нажатии на кусочек.
 */
@Composable
private fun PuzzlePiecesRow(
    pieces: List<String>,
    usedPieces: List<String>,
    selectedIndex: Int,
    showElements: Boolean,
    onPieceClick: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PieceSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pieces.forEachIndexed { index, piece ->
            val isUsed = usedPieces.contains(piece)
            val isSelected = selectedIndex == index

            // Stagger-задержка (после ячеек)
            val appearDelay = ELEMENT_STAGGER_DELAY_MS * (PUZZLE_SIZE + index)

            AnimatedPuzzlePiece(
                piece = piece,
                isUsed = isUsed,
                isSelected = isSelected,
                appearDelay = appearDelay,
                showElements = showElements,
                onClick = { onPieceClick(index) }
            )
        }
    }
}

/**
 * Анимированный кусочек пазла.
 */
@Composable
private fun AnimatedPuzzlePiece(
    piece: String,
    isUsed: Boolean,
    isSelected: Boolean,
    appearDelay: Long,
    showElements: Boolean,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showElements) {
        if (showElements) {
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
            animationSpec = tween(ELEMENT_APPEAR_DURATION_MS)
        )
    ) {
        PuzzlePiece(
            piece = piece,
            isUsed = isUsed,
            isSelected = isSelected,
            onClick = onClick
        )
    }
}

/**
 * Один кусочек пазла.
 */
@Composable
private fun PuzzlePiece(
    piece: String,
    isUsed: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Анимированный цвет фона
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isUsed -> Color.LightGray.copy(alpha = 0.5f)
            isSelected -> FairyGold
            else -> FairyBlue
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "PieceBackgroundColor"
    )

    // Анимированный цвет текста
    val textColor by animateColorAsState(
        targetValue = when {
            isUsed -> Color.White.copy(alpha = 0.3f)
            isSelected -> DarkText
            else -> Color.White
        },
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "PieceTextColor"
    )

    // Тень
    val elevation = when {
        isUsed -> 0.dp
        isSelected -> PieceSelectedElevation
        else -> PieceButtonElevation
    }

    Box(
        modifier = Modifier
            .size(PieceButtonSize)
            .shadow(elevation, RoundedCornerShape(CornerRadius))
            .clip(RoundedCornerShape(CornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                enabled = !isUsed
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = piece,
            fontSize = PieceFontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
