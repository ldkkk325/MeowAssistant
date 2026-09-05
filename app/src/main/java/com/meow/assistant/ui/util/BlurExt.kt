package com.meow.assistant.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    tintAlpha: Float = 0.87f,
    content: @Composable () -> Unit,
) {
    val tintColor = MiuixTheme.colorScheme.surface.copy(tintAlpha.coerceIn(0f, 1f))
    Box(
        modifier = if (blurActive && backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    blur(25.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(tintColor)
                },
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
