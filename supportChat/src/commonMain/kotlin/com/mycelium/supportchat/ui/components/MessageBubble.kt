package com.mycelium.supportchat.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.mycelium.supportchat.data.model.Message
import com.mycelium.supportchat.data.model.MessageSender
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val URL_REGEX = Regex(
    """https?://[^\s<>\"\')}\]]+"""
)

/**
 * A chat message bubble component.
 * User messages appear on the right, support messages on the left.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onCopyMessage: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val hasImage = message.imageUrl != null
        val bubbleShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
        )

        Surface(
            shape = bubbleShape,
            color = if (hasImage)
                MaterialTheme.colorScheme.surface
            else if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            border = if (hasImage)
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else
                null,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = { /* no-op on single click */ },
                    onLongClick = {
                        if (message.text.isNotBlank()) {
                            onCopyMessage(message.text)
                        }
                    }
                )
        ) {
            Column {
                if (hasImage) {
                    SubcomposeAsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .clickable { onImageClick(message.imageUrl!!) },
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    )
                }
                if (message.text.isNotBlank()) {
                    LinkedText(
                        text = message.text,
                        isUser = isUser && !hasImage,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = if (hasImage) 8.dp else 12.dp)
                    )
                }
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser && !hasImage)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp, end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkedText(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val linkColor = if (isUser)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.primary

    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
    )

    val annotatedString = remember(text, textColor, linkStyle) {
        buildAnnotatedString {
            val matches = URL_REGEX.findAll(text).toList()
            var lastIndex = 0
            for (match in matches) {
                if (match.range.first > lastIndex) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(lastIndex, match.range.first))
                    }
                }
                val url = match.value
                withLink(LinkAnnotation.Url(url = url, styles = linkStyle)) {
                    append(url)
                }
                lastIndex = match.range.last + 1
            }
            if (lastIndex < text.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/**
 * Format timestamp to human-readable time (HH:mm).
 */
@Composable
private fun formatMessageTime(timestampMs: Long): String {
    return remember(timestampMs) {
        formatTime(timestampMs)
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatTime(timestampMs: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestampMs)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        "$hour:$minute"
    } catch (e: Exception) {
        ""
    }
}
