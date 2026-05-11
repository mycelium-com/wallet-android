package com.mycelium.supportchat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mycelium.supportchat.data.model.Message
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import wallet_android_private.supportchat.generated.resources.Res
import wallet_android_private.supportchat.generated.resources.support_chat_today
import wallet_android_private.supportchat.generated.resources.support_chat_yesterday

/**
 * Scrollable list of chat messages with pagination support.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun MessageList(
    messages: List<Message>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onCopyMessage: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var previousLastMessageId by remember { mutableStateOf<String?>(null) }

    // Group messages by date
    val todayLabel = stringResource(Res.string.support_chat_today)
    val yesterdayLabel = stringResource(Res.string.support_chat_yesterday)
    val groupedMessages = remember(messages, todayLabel, yesterdayLabel) {
        groupMessagesByDate(messages, todayLabel, yesterdayLabel)
    }

    // Total items in LazyColumn: date headers + messages (+ loading indicator)
    val totalItemCount = remember(groupedMessages, isLoadingMore) {
        val headerCount = groupedMessages.size
        val messageCount = groupedMessages.sumOf { it.second.size }
        val loadingCount = if (isLoadingMore) 1 else 0
        loadingCount + headerCount + messageCount
    }

    // Auto-scroll to bottom on initial load or when a new message is appended
    LaunchedEffect(messages.lastOrNull()?.id) {
        val lastId = messages.lastOrNull()?.id
        if (messages.isNotEmpty() && lastId != previousLastMessageId) {
            if (previousLastMessageId == null) {
                // Initial load — instant scroll
                listState.scrollToItem(maxOf(0, totalItemCount - 1))
            } else {
                listState.animateScrollToItem(maxOf(0, totalItemCount - 1))
            }
        }
        previousLastMessageId = lastId
    }

    // Detect when scrolled near top for pagination
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .filter { it <= 3 }
            .collect {
                if (hasMore && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = false
    ) {
        // Loading more indicator at the top
        if (isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        groupedMessages.forEach { (dateLabel, dateMessages) ->
            // Date header
            item(key = "date_$dateLabel") {
                DateHeader(label = dateLabel)
            }

            // Messages for this date
            items(
                items = dateMessages,
                key = { it.id.ifEmpty { it.createdAt.toString() } }
            ) { message ->
                MessageBubble(
                    message = message,
                    onCopyMessage = onCopyMessage,
                    onImageClick = onImageClick
                )
            }
        }
    }
}

/**
 * Date header separator.
 */
@Composable
private fun DateHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Group messages by date and return as list of pairs (date label, messages).
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun groupMessagesByDate(
    messages: List<Message>,
    todayLabel: String,
    yesterdayLabel: String
): List<Pair<String, List<Message>>> {
    val nowMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(DatePeriod(days = 1))

    return messages.groupBy { message ->
        try {
            val messageDate = Instant.fromEpochMilliseconds(message.createdAt)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date

            when (messageDate) {
                today -> todayLabel
                yesterday -> yesterdayLabel
                else -> {
                    val day = messageDate.dayOfMonth.toString().padStart(2, '0')
                    val month = messageDate.monthNumber.toString().padStart(2, '0')
                    val year = messageDate.year
                    "$day.$month.$year"
                }
            }
        } catch (e: Exception) {
            todayLabel
        }
    }.toList()
}
