// Сохранить в app/src/main/java/com/vasilisina/azbuka/characters/CharacterView.kt

package com.vasilisina.azbuka.characters

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vasilisina.azbuka.R

// -------------------------------------------------------------------------
// Константы
// -------------------------------------------------------------------------

/** Размер персонажа по умолчанию в dp */
private const val DEFAULT_CHARACTER_SIZE_DP = 180

/** Отступ вокруг персонажа в dp */
private const val CHARACTER_PADDING_DP = 8

/** Длительность кроссфейда между эмоциями в мс */
private const val CROSSFADE_DURATION_MS = 400

/** Длительность анимации появления/скрытия в мс */
private const val VISIBILITY_ANIMATION_DURATION_MS = 300

/** Минимально допустимый размер персонажа в dp */
private const val MIN_CHARACTER_SIZE_DP = 80

/** Максимально допустимый размер персонажа в dp */
private const val MAX_CHARACTER_SIZE_DP = 400

// -------------------------------------------------------------------------
// Основная Composable-функция
// -------------------------------------------------------------------------

/**
 * Отображает персонажа (Василису или Кузю) с анимациями.
 *
 * Функциональность:
 * - Плавная смена эмоций через [Crossfade]
 * - Анимированное появление/скрытие через [AnimatedVisibility]
 * - Поддержка разных размеров через параметр [sizeDp]
 * - Семантика для accessibility services
 * - Обработка нажатий (опционально)
 *
 * @param state      Текущее состояние персонажа (имя, эмоция, видимость).
 * @param modifier   Модификатор, применяемый к корневому контейнеру.
 * @param sizeDp     Размер персонажа в dp (по умолчанию 180, минимум 80).
 * @param onClick    Опциональный обработчик нажатия на персонажа.
 */
@Composable
fun CharacterView(
    state: CharacterState,
    modifier: Modifier = Modifier,
    sizeDp: Int = DEFAULT_CHARACTER_SIZE_DP,
    onClick: (() -> Unit)? = null
) {
    // Валидация размера
    val validatedSize = sizeDp.coerceIn(MIN_CHARACTER_SIZE_DP, MAX_CHARACTER_SIZE_DP)

    // Анимация появления/скрытия
    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(
            animationSpec = tween(VISIBILITY_ANIMATION_DURATION_MS)
        ) + scaleIn(
            initialScale = 0.6f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(
            animationSpec = tween(VISIBILITY_ANIMATION_DURATION_MS)
        ) + scaleOut(
            targetScale = 0.6f,
            animationSpec = tween(VISIBILITY_ANIMATION_DURATION_MS)
        )
    ) {
        CharacterContent(
            state = state,
            modifier = modifier,
            sizeDp = validatedSize,
            onClick = onClick
        )
    }
}

// -------------------------------------------------------------------------
// Внутренний контент персонажа
// -------------------------------------------------------------------------

/**
 * Внутреннее содержимое: изображение с анимацией смены эмоций.
 */
@Composable
private fun CharacterContent(
    state: CharacterState,
    modifier: Modifier,
    sizeDp: Int,
    onClick: (() -> Unit)?
) {
    // Получаем ресурс изображения
    val imageRes = remember(state.name, state.emotion) {
        getImageRes(state)
    }

    // Если ресурс не найден — не рисуем
    if (imageRes == 0) {
        // Заглушка на случай отсутствия спрайта
        Box(
            modifier = modifier
                .size(sizeDp.dp)
                .padding(CHARACTER_PADDING_DP.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = state.emotion.emoji,
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
            )
        }
        return
    }

    // Собираем модификаторы
    val combinedModifier = modifier
        .size(sizeDp.dp)
        .padding(CHARACTER_PADDING_DP.dp)
        .semantics {
            contentDescription = "${state.normalizedName}: ${state.emotion.displayName}"
            role = if (onClick != null) Role.Button else Role.Image
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Без ripple-эффекта (сказочный стиль)
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    // Анимация смены эмоции
    Crossfade(
        targetState = imageRes,
        animationSpec = tween(
            durationMillis = CROSSFADE_DURATION_MS
        ),
        label = "CharacterEmotionCrossfade"
    ) { res ->
        Image(
            painter = painterResource(id = res),
            contentDescription = null, // Уже задано в semantics
            modifier = combinedModifier,
            contentScale = ContentScale.Fit
        )
    }
}

// -------------------------------------------------------------------------
// Альтернативные Composable-функции
// -------------------------------------------------------------------------

/**
 * Отображает персонажа с анимацией масштабирования при смене эмоции.
 *
 * Более «живая» альтернатива [CharacterView].
 * Использует [AnimatedContent] вместо [Crossfade].
 */
@Composable
fun CharacterViewAnimated(
    state: CharacterState,
    modifier: Modifier = Modifier,
    sizeDp: Int = DEFAULT_CHARACTER_SIZE_DP,
    onClick: (() -> Unit)? = null
) {
    val validatedSize = sizeDp.coerceIn(MIN_CHARACTER_SIZE_DP, MAX_CHARACTER_SIZE_DP)

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(tween(VISIBILITY_ANIMATION_DURATION_MS)) +
                scaleIn(initialScale = 0.5f, animationSpec = spring()),
        exit = fadeOut(tween(VISIBILITY_ANIMATION_DURATION_MS)) +
                scaleOut(targetScale = 0.5f, animationSpec = tween(VISIBILITY_ANIMATION_DURATION_MS))
    ) {
        val imageRes = remember(state.name, state.emotion) {
            getImageRes(state)
        }

        if (imageRes == 0) return@AnimatedVisibility

        val combinedModifier = modifier
            .size(validatedSize.dp)
            .padding(CHARACTER_PADDING_DP.dp)
            .semantics {
                contentDescription = "${state.normalizedName}: ${state.emotion.displayName}"
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )

        AnimatedContent(
            targetState = imageRes,
            transitionSpec = {
                (fadeIn(tween(CROSSFADE_DURATION_MS)) +
                        scaleIn(initialScale = 0.8f, animationSpec = spring()))
                    .togetherWith(
                        fadeOut(tween(CROSSFADE_DURATION_MS / 2)) +
                                scaleOut(targetScale = 0.8f, animationSpec = tween(CROSSFADE_DURATION_MS / 2))
                    )
            },
            label = "CharacterEmotionAnimatedContent"
        ) { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                modifier = combinedModifier,
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Отображает двух персонажей рядом (Василиса + Кузя).
 *
 * Удобно для сцен диалога.
 *
 * @param vasilisaState Состояние Василисы.
 * @param kuzyaState    Состояние Кузи.
 * @param modifier      Модификатор для контейнера.
 * @param sizeDp        Размер каждого персонажа.
 */
@Composable
fun CharacterPairView(
    vasilisaState: CharacterState,
    kuzyaState: CharacterState,
    modifier: Modifier = Modifier,
    sizeDp: Int = DEFAULT_CHARACTER_SIZE_DP
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        CharacterView(
            state = vasilisaState,
            sizeDp = sizeDp
        )
        CharacterView(
            state = kuzyaState,
            sizeDp = sizeDp
        )
    }
}

// -------------------------------------------------------------------------
// Маппинг состояния на ресурс
// -------------------------------------------------------------------------

/**
 * Сопоставляет состояние персонажа с идентификатором Drawable-ресурса.
 *
 * Логика:
 * - Определяет персонажа по нормализованному имени (без учёта регистра)
 * - Для Кузи использует ресурсы `kuzya_*`
 * - Для всех остальных (включая Василису) — `vasilisa_*`
 *
 * @param state Состояние персонажа.
 * @return Идентификатор Drawable-ресурса или 0, если ресурс не найден.
 */
@DrawableRes
fun getImageRes(state: CharacterState): Int {
    val normalizedName = state.normalizedName.lowercase()

    val isKuzya = normalizedName == "кузя" ||
            normalizedName == "kuzya"

    return if (isKuzya) {
        getKuzyaDrawable(state.emotion)
    } else {
        getVasilisaDrawable(state.emotion)
    }
}

/**
 * Возвращает Drawable для Кузи по эмоции.
 */
@DrawableRes
private fun getKuzyaDrawable(emotion: CharacterEmotion): Int {
    return when (emotion) {
        CharacterEmotion.HAPPY -> R.drawable.kuzya_happy
        CharacterEmotion.SAD   -> R.drawable.kuzya_sad
        CharacterEmotion.CLAP  -> R.drawable.kuzya_clap
        CharacterEmotion.IDLE  -> R.drawable.kuzya_idle
    }
}

/**
 * Возвращает Drawable для Василисы по эмоции.
 */
@DrawableRes
private fun getVasilisaDrawable(emotion: CharacterEmotion): Int {
    return when (emotion) {
        CharacterEmotion.HAPPY -> R.drawable.vasilisa_happy
        CharacterEmotion.SAD   -> R.drawable.vasilisa_sad
        CharacterEmotion.CLAP  -> R.drawable.vasilisa_clap
        CharacterEmotion.IDLE  -> R.drawable.vasilisa_idle
    }
}

// -------------------------------------------------------------------------
// Устаревший метод (сохранён для обратной совместимости)
// -------------------------------------------------------------------------

/**
 * Получает Drawable-ресурс по строковому имени через рефлексию.
 *
 * **Внимание:** этот метод нестабилен в production-сборках,
 * так как R8/ProGuard могут переименовать или удалить поля класса `R.drawable`.
 *
 * Рекомендуется использовать [getImageRes] с [CharacterState].
 *
 * @param name Имя ресурса (например, `"vasilisa_happy"`).
 * @return Идентификатор Drawable или 0, если ресурс не найден.
 */
@Deprecated(
    message = "Используйте getImageRes(state). Рефлексивный доступ нестабилен с R8/ProGuard.",
    replaceWith = ReplaceWith(
        "getImageRes(state)",
        "com.vasilisina.azbuka.characters.getImageRes"
    ),
    level = DeprecationLevel.WARNING
)
@DrawableRes
fun getDrawableResId(name: String): Int {
    if (name.isBlank()) return 0

    return try {
        val field = R.drawable::class.java.getField(name)
        field.getInt(null)
    } catch (_: NoSuchFieldException) {
        0
    } catch (_: IllegalAccessException) {
        0
    } catch (_: Exception) {
        0
    }
}
