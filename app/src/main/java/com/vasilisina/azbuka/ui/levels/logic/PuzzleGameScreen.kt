// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/levels/logic/PuzzleGameScreen.kt

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val PuzzlePieces = listOf(
    R.drawable.puzzle_piece_1,
    R.drawable.puzzle_piece_2,
    R.drawable.puzzle_piece_3,
    R.drawable.puzzle_piece_4
)
private const val PUZZLE_SIZE = 4
private val GridCellSize = 90.dp
private val PieceButtonSize = 70.dp
private val CornerRadius = 12.dp
private val GridBorderWidth = 2.dp
private val ErrorBorderWidth = 3.dp
private val SuccessBorderWidth = 3.dp
private val GridSpacing = 8.dp
private val PieceSpacing = 12.dp
private val GamePadding = 16.dp
private val GridTopSpacer = 24.dp
private val PiecesTopSpacer = 32.dp
private val HintSpacer = 8.dp
private const val COLOR_ANIMATION_DURATION_MS = 300
private const val ERROR_RESET_DELAY_MS = 1000L
private const val SUCCESS_DELAY_MS = 1000L
private const val ELEMENT_STAGGER_DELAY_MS = 100L
private const val ELEMENT_APPEAR_DURATION_MS = 350
private val GridCellElevation = 4.dp
private val PieceButtonElevation = 4.dp
private val PieceSelectedElevation = 8.dp
private val HintFontSize = 16.sp
private val ErrorBackgroundColor = Color(0xFFFFEBEE)

@Composable
fun PuzzleGameScreen(onResult: (correct: Boolean) -> Unit) {
    val shuffledPieces = remember { PuzzlePieces.shuffled() }
    val grid = remember { mutableStateListOf<Int?>(null, null, null, null) }

    var selectedPieceIndex by remember { mutableIntStateOf(-1) }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showElements by remember { mutableStateOf(false) }

    val isComplete = grid.all { it != null }
    val isCorrect = grid.toList() == PuzzlePieces

    LaunchedEffect(isComplete) {
        if (!isComplete || isLocked) return@LaunchedEffect
        isLocked = true
        if (isCorrect) {
            isSuccess = true
            AudioPlayer.playSFX("correct")
            delay(SUCCESS_DELAY_MS)
            onResult(true)
        } else {
            isError = true
            AudioPlayer.playSFX("wrong")
            delay(ERROR_RESET_DELAY_MS)
            grid.fill(null)
            selectedPieceIndex = -1
            isError = false
            isLocked = false
        }
    }

    LaunchedEffect(Unit) { showElements = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(GamePadding)) {
        Text(text = "Собери пазл", style = MaterialTheme.typography.headlineMedium, color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(HintSpacer))
        Text(
            text = if (isSuccess) "Правильно! Пазл собран." else if (selectedPieceIndex >= 0) "Выбери ячейку для кусочка" else "Выбери кусочек и помести его в ячейку",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = HintFontSize),
            color = when { isSuccess -> FairyGreen; isError -> FairyPink; else -> DarkText.copy(alpha = 0.6f) },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(GridTopSpacer))

        PuzzleGrid(grid = grid, isError = isError, isSuccess = isSuccess, showElements = showElements, onCellClick = { cellIndex ->
            if (isLocked) return@PuzzleGrid
            if (selectedPieceIndex >= 0 && grid[cellIndex] == null) {
                grid[cellIndex] = shuffledPieces[selectedPieceIndex]
                selectedPieceIndex = -1
            }
        })

        Spacer(modifier = Modifier.height(PiecesTopSpacer))

        PuzzlePiecesRow(pieces = shuffledPieces, usedPieces = grid.filterNotNull(), selectedIndex = selectedPieceIndex, showElements = showElements, onPieceClick = { pieceIndex ->
            if (isLocked) return@PuzzlePiecesRow
            val isUsed = grid.contains(shuffledPieces[pieceIndex])
            if (!isUsed) selectedPieceIndex = if (selectedPieceIndex == pieceIndex) -1 else pieceIndex
        })
    }
}

@Composable
private fun PuzzleGrid(grid: List<Int?>, isError: Boolean, isSuccess: Boolean, showElements: Boolean, onCellClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GridSpacing)) {
        for (row in 0..1) {
            Row(horizontalArrangement = Arrangement.spacedBy(GridSpacing)) {
                for (col in 0..1) {
                    val cellIndex = row * 2 + col
                    val appearDelay = ELEMENT_STAGGER_DELAY_MS * cellIndex
                    AnimatedGridCell(pieceRes = grid[cellIndex], isError = isError, isSuccess = isSuccess, appearDelay = appearDelay, showElements = showElements, onClick = { onCellClick(cellIndex) })
                }
            }
        }
    }
}

@Composable
private fun AnimatedGridCell(pieceRes: Int?, isError: Boolean, isSuccess: Boolean, appearDelay: Long, showElements: Boolean, onClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showElements) { if (showElements) { delay(appearDelay); isVisible = true } }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
        GridCell(pieceRes = pieceRes, isError = isError, isSuccess = isSuccess, onClick = onClick)
    }
}

@Composable
private fun GridCell(pieceRes: Int?, isError: Boolean, isSuccess: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(targetValue = when { isSuccess && pieceRes != null -> FairyGreen; isError -> Color.Red; pieceRes != null -> FairyGold; else -> FairyPurple }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "GridBorder")
    val backgroundColor by animateColorAsState(targetValue = when { isError -> ErrorBackgroundColor; isSuccess && pieceRes != null -> FairyGreen.copy(alpha = 0.15f); pieceRes != null -> FairyGold.copy(alpha = 0.15f); else -> Color.White }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "GridBg")
    val borderWidth = when { isError -> ErrorBorderWidth; isSuccess && pieceRes != null -> SuccessBorderWidth; else -> GridBorderWidth }

    Box(modifier = Modifier.size(GridCellSize).shadow(GridCellElevation, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(backgroundColor).border(borderWidth, borderColor, RoundedCornerShape(CornerRadius)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
        if (pieceRes != null) {
            Image(painter = painterResource(id = pieceRes), contentDescription = "Кусочек пазла", modifier = Modifier.size(80.dp), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
private fun PuzzlePiecesRow(pieces: List<Int>, usedPieces: List<Int>, selectedIndex: Int, showElements: Boolean, onPieceClick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(PieceSpacing), verticalAlignment = Alignment.CenterVertically) {
        pieces.forEachIndexed { index, pieceRes ->
            val appearDelay = ELEMENT_STAGGER_DELAY_MS * (PUZZLE_SIZE + index)
            AnimatedPuzzlePiece(pieceRes = pieceRes, isUsed = usedPieces.contains(pieceRes), isSelected = selectedIndex == index, appearDelay = appearDelay, showElements = showElements, onClick = { onPieceClick(index) })
        }
    }
}

@Composable
private fun AnimatedPuzzlePiece(pieceRes: Int, isUsed: Boolean, isSelected: Boolean, appearDelay: Long, showElements: Boolean, onClick: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showElements) { if (showElements) { delay(appearDelay); isVisible = true } }
    AnimatedVisibility(visible = isVisible, enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(ELEMENT_APPEAR_DURATION_MS))) {
        PuzzlePiece(pieceRes = pieceRes, isUsed = isUsed, isSelected = isSelected, onClick = onClick)
    }
}

@Composable
private fun PuzzlePiece(pieceRes: Int, isUsed: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(targetValue = when { isUsed -> Color.LightGray.copy(alpha = 0.5f); isSelected -> FairyGold; else -> FairyBlue }, animationSpec = tween(COLOR_ANIMATION_DURATION_MS), label = "PieceBg")
    val elevation = when { isUsed -> 0.dp; isSelected -> PieceSelectedElevation; else -> PieceButtonElevation }

    Box(modifier = Modifier.size(PieceButtonSize).shadow(elevation, RoundedCornerShape(CornerRadius)).clip(RoundedCornerShape(CornerRadius)).background(backgroundColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !isUsed, onClick = onClick), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = pieceRes), contentDescription = "Кусочек пазла", modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
    }
}
