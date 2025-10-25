package com.distributedMessenger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val cornerSize = 32.dp // Размер "уголков" рамки
    val strokeWidth = 4.dp  // Толщина линий

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Определяем размер центрального квадрата для сканирования.
        // Он будет занимать 70% от наименьшей стороны (ширины или высоты).
        val rectSize = min(canvasWidth, canvasHeight) * 0.7f
        val rectLeft = (canvasWidth - rectSize) / 2
        val rectTop = (canvasHeight - rectSize) / 2
        val rectRight = rectLeft + rectSize
        val rectBottom = rectTop + rectSize

        val cornerLength = cornerSize.toPx()
        val strokePx = strokeWidth.toPx()

        // 1. Рисуем полупрозрачный фон вокруг рамки
        val backgroundPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight)) // Внешний прямоугольник
            addRect(Rect(rectLeft, rectTop, rectRight, rectBottom)) // Внутренний "прозрачный" прямоугольник
        }
        drawPath(
            path = backgroundPath,
            color = Color.Black.copy(alpha = 0.5f)
        )

        // 2. Рисуем четыре уголка рамки
        val cornerPath = Path().apply {
            // Верхний левый
            moveTo(rectLeft, rectTop + cornerLength)
            lineTo(rectLeft, rectTop)
            lineTo(rectLeft + cornerLength, rectTop)

            // Верхний правый
            moveTo(rectRight - cornerLength, rectTop)
            lineTo(rectRight, rectTop)
            lineTo(rectRight, rectTop + cornerLength)

            // Нижний левый
            moveTo(rectLeft, rectBottom - cornerLength)
            lineTo(rectLeft, rectBottom)
            lineTo(rectLeft + cornerLength, rectBottom)

            // Нижний правый
            moveTo(rectRight, rectBottom - cornerLength)
            lineTo(rectRight, rectBottom)
            lineTo(rectRight - cornerLength, rectBottom)
        }

        drawPath(
            path = cornerPath,
            color = Color.White,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}
