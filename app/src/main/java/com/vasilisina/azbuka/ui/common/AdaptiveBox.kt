// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/common/AdaptiveBox.kt

package com.vasilisina.azbuka.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AdaptiveBox(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val designWidth = 1920f
        val designHeight = 1080f

        val scaleX = maxWidth.value / designWidth
        val scaleY = maxHeight.value / designHeight
        val scale = minOf(scaleX, scaleY)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
