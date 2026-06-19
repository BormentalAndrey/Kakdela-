// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/map/MapScreen.kt

package com.vasilisina.azbuka.ui.map

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vasilisina.azbuka.R
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.data.GameState
import com.vasilisina.azbuka.ui.theme.DarkText
import com.vasilisina.azbuka.ui.theme.FairyBlue
import com.vasilisina.azbuka.ui.theme.FairyGold
import com.vasilisina.azbuka.ui.theme.FairyGreen
import com.vasilisina.azbuka.ui.theme.FairyPink
import com.vasilisina.azbuka.ui.theme.FairyPurple
import com.vasilisina.azbuka.ui.theme.WhiteBackground

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Отступ от краёв экрана для карты */
private val MapPadding = 16.dp

/** Размер точки города */
private val CityPointSize = 70.dp

/** Отступ вокруг точки города */
private val CityPointPadding = 8.dp

/** Радиус скругления кнопки «Назад» */
private val BackButtonCornerRadius = 12.dp

/** Размер шрифта номера уровня на точке */
private val LevelNumberFontSize = 28.sp

/** Размер шрифта названия города */
private val CityNameFontSize = 18.sp

/** Размер шрифта звёзд */
private val StarsFontSize = 16.sp

/** Длительность анимации появления точки */
private const val CITY_ANIMATION_DURATION_MS = 400

/** Задержка между появлением точек */
private const val CITY_STAGGER_DELAY_MS = 150L

/** Тень для точки города */
private val CityPointElevation = 8.dp

/** Тень для кнопки «Назад» */
private val BackButtonElevation = 4.dp

/** Толщина обводки для пройденного уровня */
private val CompletedBorderWidth = 3.dp

/** Цвет обводки для пройденного уровня */
private val CompletedBorderColor = FairyGreen

// -------------------------------------------------------------------------
// Модель данных
// -------------------------------------------------------------------------

/**
 * Данные о городе на карте.
 *
 * @property name  Название города.
 * @property level Номер уровня (1–5).
 */
private data class City(
    val name: String,
    val level: Int
)

// -------------------------------------------------------------------------
// Главный экран карты
// -------------------------------------------------------------------------

/**
 * Экран карты России с городами-уровнями.
 *
 * Особенности:
 * - Карта России как фон ([R.drawable.map_russia])
 * - 5 городов-точек (Москва, Тула, Вологда, Казань, Владивосток)
 * - Каскадная анимация появления точек
 * - Открытые уровни — золотые, закрытые — серые
 * - Пройденные уровни — зелёная обводка + звёзды
 * - Кнопка «Назад» в левом верхнем углу
 * - Автоматический запуск музыки карты
 *
 * @param onLevelSelected Колбэк при выборе уровня (получает номер уровня 1–5).
 * @param onBack          Колбэк для кнопки «Назад».
 */
@Composable
fun MapScreen(
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Запуск музыки карты
    LaunchedEffect(Unit) {
        AudioPlayer.playMusic(
            context = context,
            resId = R.raw.music_map,
            loop = true
        )
    }

    // Список городов
    val cities = remember {
        listOf(
            City("Москва", 1),
            City("Тула", 2),
            City("Вологда", 3),
            City("Казань", 4),
            City("Владивосток", 5)
        )
    }

    // Разбиваем на строки: [1, 2], [3, 4], [5]
    val cityRows = remember(cities) {
        listOf(
            cities.subList(0, 2),   // Москва, Тула
            cities.subList(2, 4),   // Вологда, Казань
            cities.subList(4, 5)    // Владивосток
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
    ) {
        // Карта России (фон)
        MapBackground()

        // Точки городов
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MapPadding),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            cityRows.forEachIndexed { rowIndex, row ->
                CityRow(
                    cities = row,
                    rowIndex = rowIndex,
                    onLevelSelected = onLevelSelected
                )
            }
        }

        // Кнопка «Назад»
        BackButton(
            onClick = {
                AudioPlayer.playSFX("click")
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(MapPadding)
        )
    }
}

// -------------------------------------------------------------------------
// Фон карты
// -------------------------------------------------------------------------

/**
 * Отображает карту России как фоновое изображение.
 *
 * Поверх карты — лёгкий градиент для улучшения читаемости точек.
 */
@Composable
private fun MapBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Карта
        Image(
            painter = painterResource(id = R.drawable.map_russia),
            contentDescription = "Карта России",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Лёгкий градиент поверх карты для контрастности
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            WhiteBackground.copy(alpha = 0.15f),
                            WhiteBackground.copy(alpha = 0.3f)
                        )
                    )
                )
        )
    }
}

// -------------------------------------------------------------------------
// Строка городов
// -------------------------------------------------------------------------

/**
 * Отображает строку из 1–2 городов с анимацией появления.
 *
 * @param cities          Список городов в строке.
 * @param rowIndex        Индекс строки для stagger-задержки.
 * @param onLevelSelected Колбэк выбора уровня.
 */
@Composable
private fun CityRow(
    cities: List<City>,
    rowIndex: Int,
    onLevelSelected: (Int) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Stagger-задержка: каждая следующая строка появляется позже
        kotlinx.coroutines.delay(CITY_STAGGER_DELAY_MS * (rowIndex + 1))
        isVisible = true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (cities.size == 1) {
            Arrangement.Center
        } else {
            Arrangement.SpaceEvenly
        }
    ) {
        cities.forEach { city ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(
                    animationSpec = tween(CITY_ANIMATION_DURATION_MS)
                ) + scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            ) {
                CityPoint(
                    cityName = city.name,
                    level = city.level,
                    onLevelSelected = onLevelSelected
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Точка города
// -------------------------------------------------------------------------

/**
 * Кликабельная точка города на карте.
 *
 * Состояния:
 * - **Заблокирован**: серый цвет, иконка 🔒
 * - **Открыт, не пройден**: золотой цвет, номер уровня
 * - **Пройден**: золотой цвет + зелёная обводка, номер уровня + звёзды
 *
 * @param cityName        Название города.
 * @param level           Номер уровня (1–5).
 * @param onLevelSelected Колбэк при нажатии на открытый город.
 */
@Composable
private fun CityPoint(
    cityName: String,
    level: Int,
    onLevelSelected: (Int) -> Unit
) {
    // Реактивные проверки состояния уровня
    val isUnlocked = GameState.isLevelUnlocked(level)
    val stars = GameState.getStars(level)
    val isCompleted = stars > 0

    // Анимация цвета точки
    val pointColor by animateColorAsState(
        targetValue = when {
            isUnlocked -> FairyGold
            else -> Color.Gray.copy(alpha = 0.45f)
        },
        animationSpec = tween(300),
        label = "CityPointColor"
    )

    // Анимация цвета обводки
    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) CompletedBorderColor else Color.Transparent,
        animationSpec = tween(300),
        label = "CityPointBorder"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Без ripple (сказочный стиль)
                enabled = isUnlocked
            ) {
                AudioPlayer.playSFX("click")
                onLevelSelected(level)
            }
            .padding(CityPointPadding)
    ) {
        // Круглая точка
        Box(
            modifier = Modifier
                .size(CityPointSize)
                .shadow(
                    elevation = if (isUnlocked) CityPointElevation else 0.dp,
                    shape = CircleShape
                )
                .background(
                    color = pointColor,
                    shape = CircleShape
                )
                .then(
                    if (isCompleted) {
                        Modifier.border(
                            width = CompletedBorderWidth,
                            color = borderColor,
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUnlocked) level.toString() else "🔒",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = LevelNumberFontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isUnlocked) DarkText else Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Название города
        Text(
            text = cityName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = CityNameFontSize,
                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isUnlocked) DarkText else Color.Gray,
            textAlign = TextAlign.Center
        )

        // Звёзды за пройденный уровень
        if (isUnlocked && isCompleted) {
            Text(
                text = buildString {
                    repeat(stars) { append("★") }
                    repeat(GameState.MAX_STARS_PER_LEVEL - stars) { append("☆") }
                },
                color = FairyGold,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = StarsFontSize
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------------------
// Кнопка «Назад»
// -------------------------------------------------------------------------

/**
 * Кнопка возврата в главное меню.
 *
 * Располагается в левом верхнем углу экрана.
 *
 * @param onClick Действие при нажатии.
 * @param modifier Модификатор позиционирования.
 */
@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(BackButtonElevation, RoundedCornerShape(BackButtonCornerRadius)),
        shape = RoundedCornerShape(BackButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = FairyPink,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = BackButtonElevation,
            pressedElevation = BackButtonElevation * 1.5f,
            focusedElevation = BackButtonElevation,
            hoveredElevation = BackButtonElevation,
            disabledElevation = 0.dp
        )
    ) {
        Text(
            text = "← Назад",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
