package com.mycelium.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit

/**
 * Calculates an adaptive font size that fits within the specified width.
 * If the text is wider than maxWidthPx, the font size is scaled down proportionally.
 *
 * @param text The text to measure
 * @param baseStyle The base text style with the original font size
 * @param maxWidthPx Maximum available width in pixels
 * @param textMeasurer TextMeasurer instance for measuring text
 * @param scaleFactor Additional scale factor applied after fitting (default 0.9 for padding)
 * @return Calculated TextUnit font size that fits within maxWidthPx
 */
fun calculateAdaptiveFontSize(
    text: String,
    baseStyle: TextStyle,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
    scaleFactor: Float = 0.9f
): TextUnit {
    if (text.isEmpty() || maxWidthPx <= 0) return baseStyle.fontSize

    var fontSize = baseStyle.fontSize

    // Measure text width with current font size
    val textLayoutResult = textMeasurer.measure(
        text = text,
        style = baseStyle.copy(fontSize = fontSize)
    )

    // If text is too wide, scale down the font size
    if (textLayoutResult.size.width > maxWidthPx) {
        val scale = maxWidthPx.toFloat() / textLayoutResult.size.width.toFloat()
        // Apply additional padding factor to ensure it fits
        fontSize = fontSize * scale * scaleFactor
    }

    return fontSize
}

/**
 * Remembers an adaptive font size that fits within the specified width.
 * Recalculates only when text, style, or width changes.
 *
 * @param text The text to measure
 * @param baseStyle The base text style with the original font size
 * @param maxWidthPx Maximum available width in pixels
 * @param scaleFactor Additional scale factor applied after fitting (default 0.9 for padding)
 * @return Calculated TextUnit font size that fits within maxWidthPx
 */
@Composable
fun rememberAdaptiveFontSize(
    text: String,
    baseStyle: TextStyle,
    maxWidthPx: Int,
    scaleFactor: Float = 0.9f
): TextUnit {
    val textMeasurer = rememberTextMeasurer()
    return remember(text, baseStyle, maxWidthPx) {
        calculateAdaptiveFontSize(text, baseStyle, maxWidthPx, textMeasurer, scaleFactor)
    }
}

/**
 * Remembers an adaptive font size using a provided TextMeasurer.
 * Use this overload when you already have a TextMeasurer instance.
 *
 * @param text The text to measure
 * @param baseStyle The base text style with the original font size
 * @param maxWidthPx Maximum available width in pixels
 * @param textMeasurer TextMeasurer instance for measuring text
 * @param scaleFactor Additional scale factor applied after fitting (default 0.9 for padding)
 * @return Calculated TextUnit font size that fits within maxWidthPx
 */
@Composable
fun rememberAdaptiveFontSize(
    text: String,
    baseStyle: TextStyle,
    maxWidthPx: Int,
    textMeasurer: TextMeasurer,
    scaleFactor: Float = 0.9f
): TextUnit {
    return remember(text, baseStyle, maxWidthPx) {
        calculateAdaptiveFontSize(text, baseStyle, maxWidthPx, textMeasurer, scaleFactor)
    }
}
