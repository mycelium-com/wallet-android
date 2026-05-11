package com.mycelium.supportchat.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import wallet_android_private.supportchat.generated.resources.Res
import wallet_android_private.supportchat.generated.resources.support_chat_attach_image
import wallet_android_private.supportchat.generated.resources.support_chat_input_hint
import wallet_android_private.supportchat.generated.resources.support_chat_send

private const val MAX_MESSAGE_LENGTH = 2000
private const val CHAR_COUNT_THRESHOLD = 1800

/**
 * Message input component with text field and send button.
 */
@Composable
fun MessageInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachImage: () -> Unit,
    isSending: Boolean,
    isUploadingImage: Boolean,
    isOverLimit: Boolean = false,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isBusy = isSending || isUploadingImage
    val showCharCount = text.length >= CHAR_COUNT_THRESHOLD

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAttachImage,
                    enabled = enabled && !isBusy
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = stringResource(Res.string.support_chat_attach_image),
                        tint = if (enabled && !isBusy)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.support_chat_input_hint),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    isError = isOverLimit,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (enabled && text.isNotBlank()) {
                                onSendClick()
                            }
                        }
                    ),
                    enabled = !isBusy
                )

                IconButton(
                    onClick = onSendClick,
                    enabled = enabled && !isBusy && text.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(Res.string.support_chat_send)
                        )
                    }
                }
            }

            // Character counter near the limit
            if (showCharCount) {
                Text(
                    text = "${text.length}/$MAX_MESSAGE_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverLimit)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 56.dp, top = 2.dp)
                )
            }
        }
    }
}
