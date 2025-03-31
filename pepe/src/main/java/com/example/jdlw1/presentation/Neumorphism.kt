package com.example.jdlw1.presentation
//queria poner este estilo xd pero no salio igual dejo esto aqui
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neumorphicShadow(
    lightShadow: Color = Color.White.copy(alpha = 0.7f),
    darkShadow: Color = Color.Black.copy(alpha = 0.1f),
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 16.dp
) = composed {
    val shadowElevation = with(LocalDensity.current) { elevation.toPx() }
    val cornerRadiusPx = with(LocalDensity.current) { cornerRadius.toPx() }

    this.drawWithCache {
        onDrawBehind {
            // Sombra oscura (abajo-derecha)
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Transparent
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        setShadowLayer(
                            shadowElevation,
                            shadowElevation / 3,
                            shadowElevation / 3,
                            darkShadow.hashCode()
                        )
                    }
                }
                canvas.drawRoundRect(
                    0f, 0f,
                    size.width, size.height,
                    cornerRadiusPx, cornerRadiusPx,
                    paint
                )
            }

            // Sombra clara (arriba-izquierda)
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Transparent
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        setShadowLayer(
                            shadowElevation,
                            -shadowElevation / 3,
                            -shadowElevation / 3,
                            lightShadow.hashCode()
                        )
                    }
                }
                canvas.drawRoundRect(
                    0f, 0f,
                    size.width, size.height,
                    cornerRadiusPx, cornerRadiusPx,
                    paint
                )
            }
        }
    }
}