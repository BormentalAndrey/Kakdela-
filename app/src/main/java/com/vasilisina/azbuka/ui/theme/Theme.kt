// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/theme/Theme.kt

package com.vasilisina.azbuka.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// -------------------------------------------------------------------------
// Цветовая палитра сказочной темы «Василисина азбука»
// -------------------------------------------------------------------------

/** Нежно-розовый (акценты, кнопки) */
val FairyPink = Color(0xFFFFB7C5)

/** Небесно-голубой (фоны, второстепенные элементы) */
val FairyBlue = Color(0xFF87CEEB)

/** Мягкий зелёный (положительные действия, успех) */
val FairyGreen = Color(0xFF98FB98)

/** Сказочное золото (звёзды, награды, выделение) */
val FairyGold = Color(0xFFFFD700)

/** Лавандовый / сказочный фиолетовый (основной акцент) */
val FairyPurple = Color(0xFFDDA0DD)

/** Тёмно-серый текст (основной) */
val DarkText = Color(0xFF2E2E2E)

/** Тепло-белый фон (основной фон экранов) */
val WhiteBackground = Color(0xFFFFF8F0)

/** Белая поверхность карточек */
private val FairySurface = Color(0xFFFFFFFF)

/** Светло-серый контур */
private val FairyOutline = Color(0xFFBDBDBD)

/** Цвет ошибки (красный) */
private val FairyError = Color(0xFFD32F2F)

/** Затемнённый оверлей для модальных окон */
private val FairyScrim = Color(0x80000000)

/** Цвет для заблокированных элементов */
private val FairyDisabled = Color(0xFF9E9E9E)

// -------------------------------------------------------------------------
// Тёмная палитра (для ночного режима, задел на будущее)
// -------------------------------------------------------------------------

private val DarkFairyBackground = Color(0xFF1A1A2E)
private val DarkFairySurface = Color(0xFF252540)
private val DarkFairyOnBackground = Color(0xFFE0E0E0)
private val DarkFairyOnSurface = Color(0xFFE0E0E0)

// -------------------------------------------------------------------------
// Material3 Color Schemes
// -------------------------------------------------------------------------

/**
 * Светлая цветовая схема.
 *
 * Используется по умолчанию для всей игры.
 * Построена на сказочной палитре: фиолетовый + золото + зелёный.
 */
private val LightColorScheme = lightColorScheme(
    // Primary — основной цвет (кнопки, акценты)
    primary = FairyPurple,
    onPrimary = Color.White,
    primaryContainer = FairyPurple.copy(alpha = 0.18f),
    onPrimaryContainer = DarkText,

    // Secondary — второстепенный цвет (золото)
    secondary = FairyGold,
    onSecondary = DarkText,
    secondaryContainer = FairyGold.copy(alpha = 0.28f),
    onSecondaryContainer = DarkText,

    // Tertiary — третичный цвет (зелёный)
    tertiary = FairyGreen,
    onTertiary = DarkText,
    tertiaryContainer = FairyGreen.copy(alpha = 0.25f),
    onTertiaryContainer = DarkText,

    // Фон и поверхность
    background = WhiteBackground,
    onBackground = DarkText,
    surface = FairySurface,
    onSurface = DarkText,
    surfaceVariant = FairyPink.copy(alpha = 0.18f),
    onSurfaceVariant = DarkText,

    // Ошибка
    error = FairyError,
    onError = Color.White,
    errorContainer = FairyError.copy(alpha = 0.12f),
    onErrorContainer = FairyError,

    // Контуры
    outline = FairyOutline,
    outlineVariant = FairyOutline.copy(alpha = 0.5f),

    // Затемнение
    scrim = FairyScrim,

    // Инверсные цвета
    inverseSurface = DarkText,
    inverseOnSurface = Color.White,
    inversePrimary = FairyPink
)

/**
 * Тёмная цветовая схема (задел на будущее).
 *
 * Сейчас не используется, но готова для внедрения
 * переключателя «День / Ночь» в настройках.
 */
@Suppress("unused")
private val DarkColorScheme = darkColorScheme(
    primary = FairyPurple.copy(alpha = 0.8f),
    onPrimary = DarkText,
    primaryContainer = FairyPurple.copy(alpha = 0.25f),
    onPrimaryContainer = Color.White,

    secondary = FairyGold.copy(alpha = 0.8f),
    onSecondary = DarkText,
    secondaryContainer = FairyGold.copy(alpha = 0.25f),
    onSecondaryContainer = Color.White,

    tertiary = FairyGreen.copy(alpha = 0.8f),
    onTertiary = DarkText,
    tertiaryContainer = FairyGreen.copy(alpha = 0.25f),
    onTertiaryContainer = Color.White,

    background = DarkFairyBackground,
    onBackground = DarkFairyOnBackground,
    surface = DarkFairySurface,
    onSurface = DarkFairyOnSurface,
    surfaceVariant = DarkFairySurface,
    onSurfaceVariant = DarkFairyOnSurface.copy(alpha = 0.7f),

    error = FairyError.copy(alpha = 0.8f),
    onError = Color.White,
    errorContainer = FairyError.copy(alpha = 0.25f),
    onErrorContainer = FairyError,

    outline = Color.White.copy(alpha = 0.2f),
    outlineVariant = Color.White.copy(alpha = 0.1f),

    scrim = Color.Black.copy(alpha = 0.6f),

    inverseSurface = Color.White,
    inverseOnSurface = DarkText,
    inversePrimary = FairyPurple
)

// -------------------------------------------------------------------------
// Формы (Shapes)
// -------------------------------------------------------------------------

/**
 * Формы компонентов в сказочном стиле.
 *
 * Все углы скруглённые, что соответствует
 * мягкому визуальному стилю игры.
 */
private val AppShapes = Shapes(
    // Маленькие компоненты: чипсы, маленькие кнопки
    small = RoundedCornerShape(8.dp),

    // Средние компоненты: карточки, диалоги
    medium = RoundedCornerShape(16.dp),

    // Крупные компоненты: bottom sheet, полноэкранные диалоги
    large = RoundedCornerShape(24.dp)
)

// -------------------------------------------------------------------------
// Типографика
// -------------------------------------------------------------------------

/**
 * Типографика игры.
 *
 * Использует системный шрифт с увеличенными размерами
 * для удобства чтения детьми 5–7 лет.
 */
private val AppTypography = Typography(
    // Заголовок экрана (32 sp, Bold)
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // Подзаголовок (24 sp, SemiBold)
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Маленький заголовок (20 sp, Medium)
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),

    // Основной текст (20 sp)
    bodyLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 28.sp,
        letterSpacing = 0.25.sp
    ),

    // Второстепенный текст (16 sp)
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),

    // Мелкий текст (14 sp)
    bodySmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    ),

    // Текст на кнопках (22 sp, Medium)
    labelLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),

    // Текст на маленьких кнопках / чипсах (16 sp)
    labelMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Текст на очень маленьких элементах (12 sp)
    labelSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),

    // Заголовок (18 sp, SemiBold)
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),

    // Подзаголовок в списке (16 sp, Medium)
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Мелкий заголовок (14 sp, Medium)
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

// -------------------------------------------------------------------------
// Главная тема
// -------------------------------------------------------------------------

/**
 * Главная тема приложения «Василисина азбука».
 *
 * Оборачивает весь контент в [MaterialTheme] со сказочной палитрой.
 * Настраивает цвет строки состояния и навигационной панели.
 *
 * Использование:
 * ```kotlin
 * VasilisaTheme {
 *     // Весь контент приложения
 *     MainScreen()
 * }
 * ```
 *
 * @param darkTheme Использовать ли тёмную тему (по умолчанию — системная настройка).
 * @param content   Контент, который будет обёрнут в тему.
 */
@Composable
fun VasilisaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Пока игра всегда использует светлую тему,
    // так как сказочный стиль рассчитан на светлый фон.
    // Когда будут готовы тёмные спрайты — можно включить переключение:
    // val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = LightColorScheme

    // Настройка системных баров (status bar, navigation bar)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()

            if (activity != null) {
                val window = activity.window

                // Цвет статус-бара и навигационной панели
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()

                // Тёмные иконки на светлом фоне
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// -------------------------------------------------------------------------
// Утилиты
// -------------------------------------------------------------------------

/**
 * Рекурсивно ищет Activity в иерархии контекстов.
 *
 * Необходим, так как [LocalView.current.context] может быть
 * обёрткой ([ContextWrapper]), а не непосредственно Activity.
 *
 * @return [Activity] или null, если Activity не найдена.
 */
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

// -------------------------------------------------------------------------
// Вспомогательные функции для UI (могут использоваться в других файлах)
// -------------------------------------------------------------------------

/**
 * Возвращает цвет для отображения звёзд прогресса.
 *
 * @param stars Текущее количество звёзд.
 * @param maxStars Максимальное количество звёзд.
 * @return [FairyGold] если есть хотя бы одна звезда, иначе [FairyDisabled].
 */
fun starColor(stars: Int, maxStars: Int = 3): Color {
    return if (stars > 0) FairyGold else FairyDisabled
}

/**
 * Возвращает цвет для кнопки уровня на карте.
 *
 * @param isUnlocked Открыт ли уровень.
 * @param isCompleted Пройден ли уровень.
 * @return Цвет кнопки.
 */
fun levelButtonColor(isUnlocked: Boolean, isCompleted: Boolean): Color {
    return when {
        isCompleted -> FairyGreen
        isUnlocked -> FairyGold
        else -> FairyDisabled
    }
}

/**
 * Возвращает цвет для ячейки с буквой в зависимости от состояния.
 *
 * @param isCorrect Буква правильная.
 * @param isSelected Буква выбрана.
 * @return Цвет фона ячейки.
 */
fun letterCellColor(isCorrect: Boolean, isSelected: Boolean): Color {
    return when {
        isCorrect && isSelected -> FairyGreen
        !isCorrect && isSelected -> FairyError.copy(alpha = 0.3f)
        else -> FairyBlue
    }
}

/**
 * Проверяет, поддерживает ли устройство динамические цвета (Material You).
 *
 * Доступно начиная с Android 12 (API 31).
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicColors(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
