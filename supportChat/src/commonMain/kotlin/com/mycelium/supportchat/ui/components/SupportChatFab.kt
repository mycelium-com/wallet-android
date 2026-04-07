package com.mycelium.supportchat.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import wallet_android_private.supportchat.generated.resources.Res
import wallet_android_private.supportchat.generated.resources.support_chat_fab_description

private val FAB_SIZE_EXPANDED = 56.dp
private val FAB_SIZE_COLLAPSED = 40.dp
private val ICON_SIZE = 24.dp
private val VISIBLE_WHEN_COLLAPSED = 8.dp
private val COLLAPSED_OFFSET = FAB_SIZE_COLLAPSED - VISIBLE_WHEN_COLLAPSED -16.dp

private const val ANIMATION_DURATION_MS = 300
private const val DELAY_BEFORE_EXPAND_MS = 5000L
private const val VISIBLE_DURATION_MS = 5000L

/**
 * Sliding Floating Action Button for opening support chat.
 *
 * The FAB starts in a collapsed state (partially hidden on the right edge).
 * It automatically expands after a delay to draw user attention, then collapses back.
 *
 * When collapsed, tapping on the visible portion expands it.
 * When expanded, tapping opens the support chat.
 *
 * @param unreadCount Number of unread messages to show in badge
 * @param isExpanded Whether the FAB is currently expanded (fully visible)
 * @param onExpandedChange Called when the expanded state should change
 * @param onNavigateToChat Called when user taps to open chat (only when expanded)
 * @param shouldAutoAnimate Whether to run the auto-expand animation
 * @param modifier Modifier for the FAB container
 */
@Composable
fun SupportChatFab(
    unreadCount: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNavigateToChat: () -> Unit,
    shouldAutoAnimate: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Auto-animation: expand after delay, then collapse after visibility duration
    LaunchedEffect(shouldAutoAnimate) {
        if (shouldAutoAnimate) {
            delay(DELAY_BEFORE_EXPAND_MS)
            onExpandedChange(true)
            delay(VISIBLE_DURATION_MS)
            onExpandedChange(false)
        }
    }

    // Animate the horizontal offset
    val offsetX: Dp by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else COLLAPSED_OFFSET,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "fab_slide_animation"
    )

    // Animate the FAB size (smaller when collapsed)
    val fabSize: Dp by animateDpAsState(
        targetValue = if (isExpanded) FAB_SIZE_EXPANDED else FAB_SIZE_COLLAPSED,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "fab_size_animation"
    )

    Box(
        modifier = modifier.offset(x = offsetX),
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = {
                if (isExpanded) {
                    onNavigateToChat()
                } else {
                    onExpandedChange(true)
                }
            },
            modifier = Modifier.size(fabSize),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = stringResource(Res.string.support_chat_fab_description),
                modifier = Modifier.size(ICON_SIZE)
            )
        }

        // Badge with unread count
        if (unreadCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 0.dp, y = (0).dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * Simplified version of SupportChatFab that manages its own state.
 * Use this when you don't need external control over the expanded state.
 */
@Composable
fun SupportChatFab(
    unreadCount: Int,
    onClick: () -> Unit,
    shouldAutoAnimate: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    SupportChatFab(
        unreadCount = unreadCount,
        isExpanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        onNavigateToChat = onClick,
        shouldAutoAnimate = shouldAutoAnimate,
        modifier = modifier
    )
}

@Preview
@Composable
fun SupportChatFabPreview() {
    Box(Modifier.size(80.dp).padding(16.dp)) {
        SupportChatFab(
            unreadCount = 5,
            onClick = {},
            shouldAutoAnimate = false
        )
    }
}
