package com.mycelium.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Creates a modifier that draws a blinking cursor at the end of text and handles focus events.
 * Uses textMeasurer to calculate cursor position without requiring onTextLayout.
 *
 * @param hasFocus whether the text field currently has focus
 * @param cursorColor color of the cursor
 * @param textFieldValue current text field value with text + selection
 * @param textStyle style used for the text (needed for accurate measurement)
 * @param textMeasurer text measurer instance for calculating text width
 * @param horizontalPaddingStart horizontal start padding inside the TextField
 * @param horizontalPaddingEnd horizontal end padding inside the TextField
 * @param labelStyle style of the label text (used to calculate label height when focused)
 * @param onFocusChanged callback when focus state changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Modifier.withCursor(
    hasFocus: Boolean,
    cursorColor: Color = MaterialTheme.colorScheme.primary,
    textFieldValue: TextFieldValue,
    textStyle: androidx.compose.ui.text.TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    horizontalPaddingStart: Float = with(LocalDensity.current) {
        val direction = LocalLayoutDirection.current
        TextFieldDefaults.contentPaddingWithLabel()
            .calculateStartPadding(direction).toPx()
    },
    horizontalPaddingEnd: Float = with(LocalDensity.current) {
        val direction = LocalLayoutDirection.current
        TextFieldDefaults.contentPaddingWithLabel()
            .calculateEndPadding(direction).toPx()
    },
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    onFocusChanged: (Boolean) -> Unit = {}
): Modifier = composed {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val densityKey = density.density
    val fontScaleKey = density.fontScale
    val cursorWidthPx = with(density) { 2.dp.toPx() }
    val labelSpacingPx = with(density) { 8.dp.toPx() }

    // Cursor animation state
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(hasFocus) {
        if (!hasFocus) {
            cursorVisible = false
            return@LaunchedEffect
        }
        while (isActive) {
            cursorVisible = true
            delay(800)
            cursorVisible = false
            delay(500)
        }
    }

    val selectionEnd = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
    val cursorTextLayout = remember(
        textFieldValue.text,
        selectionEnd,
        textStyle,
        densityKey,
        fontScaleKey
    ) {
        val textBeforeCursor = if (textFieldValue.text.isEmpty()) {
            ""
        } else {
            textFieldValue.text.substring(0, selectionEnd)
        }
        textMeasurer.measure(
            text = textBeforeCursor,
            style = textStyle,
            maxLines = 1,
            softWrap = false
        )
    }

    val textMetrics = remember(textStyle, densityKey, fontScaleKey) {
        textMeasurer.measure(
            text = "Hg",
            style = textStyle,
            maxLines = 1,
            softWrap = false
        )
    }

    // Measure label height dynamically based on label style
    val labelLayout = remember(labelStyle, densityKey, fontScaleKey) {
        textMeasurer.measure(
            text = "Ag", // Representative text with ascenders and descenders
            style = labelStyle
        )
    }

    this
        .drawWithContent {
            drawContent()
            if (hasFocus && cursorVisible) {
                val cursorTextWidth = cursorTextLayout.size.width.toFloat()
                val cursorHeight = textMetrics.size.height
                    .takeIf { it > 0 }
                    ?.toFloat()
                    ?: textStyle.fontSize.toPx()

                val startBoundaryRaw = if (layoutDirection == LayoutDirection.Ltr) {
                    horizontalPaddingStart
                } else {
                    size.width - horizontalPaddingStart
                }
                val endBoundaryRaw = if (layoutDirection == LayoutDirection.Ltr) {
                    size.width - horizontalPaddingEnd
                } else {
                    horizontalPaddingEnd
                }
                val startBoundary = startBoundaryRaw.coerceIn(0f, size.width)
                val endBoundary = endBoundaryRaw.coerceIn(0f, size.width)
                val minBound = minOf(startBoundary, endBoundary)
                val maxBound = maxOf(startBoundary, endBoundary)
                val availableWidth = (maxBound - minBound).coerceAtLeast(0f)
                val overflow = (cursorTextWidth - availableWidth).coerceAtLeast(0f)
                val cursorX = when (layoutDirection) {
                    LayoutDirection.Ltr ->
                        startBoundary + cursorTextWidth - overflow

                    LayoutDirection.Rtl ->
                        startBoundary - (cursorTextWidth - overflow)
                }.coerceIn(minBound, maxBound)

                val isLabelFloating = hasFocus || textFieldValue.text.isNotEmpty()
                val labelAreaHeight = if (isLabelFloating) {
                    labelLayout.size.height.toFloat() + labelSpacingPx
                } else {
                    0f
                }
                val textAreaHeight = size.height - labelAreaHeight
                val maxVerticalOffset = (size.height - cursorHeight).coerceAtLeast(0f)
                val verticalOffset = (labelAreaHeight + (textAreaHeight - cursorHeight) / 2f)
                    .coerceIn(0f, maxVerticalOffset)

                // Calculate cursor position
                // Draw cursor at the end of text
                drawRect(
                    color = cursorColor,
                    topLeft = Offset(cursorX, verticalOffset),
                    size = Size(cursorWidthPx, cursorHeight)
                )
            }
        }
        .onFocusEvent { onFocusChanged(it.isFocused) }
}

@Preview
@Composable
fun CursorPreviewBasic() {
    val textFieldValue by remember { mutableStateOf(TextFieldValue("Sample text")) }
    val hasFocus by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyLarge

    MaterialTheme {
        Box(
            modifier = Modifier
                .withCursor(
                    hasFocus = hasFocus,
                    textFieldValue = textFieldValue,
                    textStyle = textStyle,
                    textMeasurer = textMeasurer,
                    horizontalPaddingStart = 0f,
                    horizontalPaddingEnd = 0f
                )
        ) {
            Text(
                text = textFieldValue.text,
                style = textStyle
            )
        }
    }
}

@Preview
@Composable
fun CursorPreviewEmpty() {
    val textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val hasFocus by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyLarge

    MaterialTheme {
        Box(
            modifier = Modifier
                .size(width = 200.dp, height = 48.dp)
                .withCursor(
                    hasFocus = hasFocus,
                    textFieldValue = textFieldValue,
                    textStyle = textStyle,
                    textMeasurer = textMeasurer,
                    horizontalPaddingStart = 0f,
                    horizontalPaddingEnd = 0f
                )
        ) {
            Text(
                text = textFieldValue.text.ifEmpty { " " },
                style = textStyle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CursorPreviewTextField() {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("123.45")) }
    var hasFocus by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyLarge

    MaterialTheme {
        TextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            readOnly = true,
            label = { Text("Amount BTC") },
            textStyle = textStyle,
            singleLine = true,
            suffix = { Text("BTC") },
            modifier = Modifier
                .withCursor(
                    hasFocus = hasFocus,
                    textFieldValue = textFieldValue,
                    textStyle = textStyle,
                    textMeasurer = textMeasurer,
                    onFocusChanged = { hasFocus = it }
                )
        )
    }
}

@Preview
@Composable
fun CursorPreviewCustomColor() {
    val textFieldValue by remember { mutableStateOf(TextFieldValue("Custom cursor")) }
    val hasFocus by remember { mutableStateOf(true) }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.headlineSmall

    MaterialTheme {
        Box(
            modifier = Modifier
                .withCursor(
                    hasFocus = hasFocus,
                    cursorColor = Color.Red,
                    textFieldValue = textFieldValue,
                    textStyle = textStyle,
                    textMeasurer = textMeasurer,
                    horizontalPaddingStart = 0f,
                    horizontalPaddingEnd = 0f
                )
        ) {
            Text(
                text = textFieldValue.text,
                style = textStyle
            )
        }
    }
}
