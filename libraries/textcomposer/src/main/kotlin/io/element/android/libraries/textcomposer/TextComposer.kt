/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.libraries.textcomposer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import io.element.android.libraries.designsystem.VectorIcons
import io.element.android.libraries.designsystem.modifiers.applyIf
import io.element.android.libraries.designsystem.preview.DayNightPreviews
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.text.applyScaleUp
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.TransactionId
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnail
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnailInfo
import io.element.android.libraries.matrix.ui.components.AttachmentThumbnailType
import io.element.android.libraries.textcomposer.components.FormattingOption
import io.element.android.libraries.textcomposer.components.FormattingOptionState
import io.element.android.libraries.theme.ElementTheme
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.wysiwyg.compose.RichTextEditor
import io.element.android.wysiwyg.compose.RichTextEditorDefaults
import io.element.android.wysiwyg.view.models.InlineFormat
import kotlinx.coroutines.android.awaitFrame
import uniffi.wysiwyg_composer.ActionState
import uniffi.wysiwyg_composer.ComposerAction

@Composable
fun TextComposer(
    state: TextComposerState,
    composerMode: MessageComposerMode,
    modifier: Modifier = Modifier,
    showTextFormatting: Boolean = false,
    onRequestFocus: () -> Unit = {},
    onSendMessage: (Message) -> Unit = {},
    onResetComposerMode: () -> Unit = {},
    onAddAttachment: () -> Unit = {},
    onDismissTextFormatting: () -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val onSendClicked = {
        onSendMessage(Message(html = state.messageHtml, markdown = state.messageMarkdown))
    }

    Column(
        modifier = modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            bottom = if (showTextFormatting) 3.dp else 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            val showComposerOptionsButton by remember(showTextFormatting) {
                derivedStateOf { !showTextFormatting }
            }
            if (showComposerOptionsButton) {
                Image(
                    painter = rememberVectorPainter(Icons.Rounded.AddCircle),
                    contentDescription = stringResource(R.string.rich_text_editor_a11y_add_attachment),
                    colorFilter = ColorFilter.tint(ElementTheme.colors.iconPrimary),
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .clickable(onClick = onAddAttachment)
                        .size(30.dp.applyScaleUp()),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            val roundCornerSmall = 20.dp.applyScaleUp()
            val roundCornerLarge = 28.dp.applyScaleUp()

            val roundedCornerSize = remember(state.lineCount, composerMode) {
                if (state.lineCount > 1 || composerMode is MessageComposerMode.Special) {
                    roundCornerSmall
                } else {
                    roundCornerLarge
                }
            }
            val roundedCornerSizeState = animateDpAsState(
                targetValue = roundedCornerSize,
                animationSpec = tween(
                    durationMillis = 100,
                )
            )
            val roundedCorners = RoundedCornerShape(roundedCornerSizeState.value)
            val colors = ElementTheme.colors
            val bgColor = colors.bgSubtleSecondary

            val borderColor by remember(state.hasFocus, colors) {
                derivedStateOf {
                    if (state.hasFocus) colors.borderDisabled else bgColor
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(roundedCorners)
                    .background(color = bgColor)
                    .border(1.dp, borderColor, roundedCorners)
            ) {
                if (composerMode is MessageComposerMode.Special) {
                    ComposerModeView(composerMode = composerMode, onResetComposerMode = onResetComposerMode)
                }


                Box {
                    TextInput(
                        state = state,
                        roundedCorners = roundedCorners,
                        bgColor = bgColor,
                        onError = onError,
                    )

                    if (!showTextFormatting) {
                        SendButton(
                            canSendMessage = state.canSendMessage,
                            onClick = onSendClicked,
                            composerMode = composerMode,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .applyIf(composerMode !is MessageComposerMode.Edit, ifTrue = {
                                    padding(start = 1.dp.applyScaleUp()) // Center the arrow in the circle
                                })
                                .padding(end = 6.dp.applyScaleUp(), bottom = 6.dp.applyScaleUp())
                        )
                    }
                }
            }
        }

        if (showTextFormatting) {
            TextFormatting(
                state = state,
                onDismissTextFormatting = onDismissTextFormatting,
                sendButton = { modifier ->
                    SendButton(
                        canSendMessage = state.canSendMessage,
                        onClick = onSendClicked,
                        composerMode = composerMode,
                        modifier = modifier
                    )
                },
            )
        }
    }

    // Request focus when changing mode, and show keyboard.
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(composerMode) {
        if (composerMode is MessageComposerMode.Special) {
            onRequestFocus()
            keyboard?.let {
                awaitFrame()
                it.show()
            }
        }
    }
}

@Composable
private fun TextInput(
    state: TextComposerState,
    roundedCorners: RoundedCornerShape,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onError: (Throwable) -> Unit = {},
) {
    val minHeight = 42.dp.applyScaleUp()
    val defaultTypography = ElementTheme.typography.fontBodyLgRegular
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .background(color = bgColor, shape = roundedCorners)
            .padding(
                PaddingValues(
                    top = 4.dp.applyScaleUp(),
                    bottom = 4.dp.applyScaleUp(),
                    start = 12.dp.applyScaleUp(),
                    end = 42.dp.applyScaleUp()
                )
            ),
        contentAlignment = Alignment.CenterStart,
    ) {

        // Placeholder
        if (state.messageHtml.isEmpty()) {
            Text(
                stringResource(CommonStrings.common_message),
                style = defaultTypography.copy(
                    color = ElementTheme.colors.textDisabled,
                ),
            )
        }

        RichTextEditor(
            state = state.richTextEditorState,
            modifier = Modifier
                .fillMaxWidth(),
            style = RichTextEditorDefaults.style(
                text = RichTextEditorDefaults.textStyle(
                    color = if (state.hasFocus) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                ),
                cursor = RichTextEditorDefaults.cursorStyle(
                    color = ElementTheme.colors.iconAccentTertiary,
                )
            ),
            onError = onError
        )
    }
}

@Composable
private fun TextFormatting(
    state: TextComposerState,
    onDismissTextFormatting: () -> Unit,
    modifier: Modifier = Modifier,
    sendButton: @Composable (modifier: Modifier) -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        val (close, formatting, send) = createRefs()

        Image(
            modifier = Modifier
                .constrainAs(close) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .clickable(onClick = onDismissTextFormatting)
                .size(30.dp.applyScaleUp()),
            painter = rememberVectorPainter(Icons.Rounded.Cancel),
            contentDescription = stringResource(CommonStrings.action_close),
            colorFilter = ColorFilter.tint(ElementTheme.colors.iconPrimary),
        )

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .constrainAs(formatting) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(close.end, margin = 12.dp)
                    end.linkTo(send.start, margin = 20.dp)
                    width = fillToConstraints
                }
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.BOLD].toButtonState(),
                onClick = { state.richTextEditorState.toggleInlineFormat(InlineFormat.Bold) },
                imageVector = ImageVector.vectorResource(VectorIcons.Bold),
                contentDescription = stringResource(R.string.rich_text_editor_format_bold)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.ITALIC].toButtonState(),
                onClick = { state.richTextEditorState.toggleInlineFormat(InlineFormat.Italic) },
                imageVector = ImageVector.vectorResource(VectorIcons.Italic),
                contentDescription = stringResource(R.string.rich_text_editor_format_italic)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.UNDERLINE].toButtonState(),
                onClick = { state.richTextEditorState.toggleInlineFormat(InlineFormat.Underline) },
                imageVector = ImageVector.vectorResource(VectorIcons.Underline),
                contentDescription = stringResource(R.string.rich_text_editor_format_underline)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.STRIKE_THROUGH].toButtonState(),
                onClick = { state.richTextEditorState.toggleInlineFormat(InlineFormat.StrikeThrough) },
                imageVector = ImageVector.vectorResource(VectorIcons.Strikethrough),
                contentDescription = stringResource(R.string.rich_text_editor_format_strikethrough)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.UNORDERED_LIST].toButtonState(),
                onClick = { state.richTextEditorState.toggleList(ordered = false) },
                imageVector = ImageVector.vectorResource(VectorIcons.BulletList),
                contentDescription = stringResource(R.string.rich_text_editor_bullet_list)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.ORDERED_LIST].toButtonState(),
                onClick = { state.richTextEditorState.toggleList(ordered = true) },
                imageVector = ImageVector.vectorResource(VectorIcons.NumberedList),
                contentDescription = stringResource(R.string.rich_text_editor_numbered_list)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.INDENT].toButtonState(),
                onClick = { state.richTextEditorState.indent() },
                imageVector = ImageVector.vectorResource(VectorIcons.IndentIncrease),
                contentDescription = stringResource(R.string.rich_text_editor_indent)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.UNINDENT].toButtonState(),
                onClick = { state.richTextEditorState.unindent() },
                imageVector = ImageVector.vectorResource(VectorIcons.IndentDecrease),
                contentDescription = stringResource(R.string.rich_text_editor_unindent)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.INLINE_CODE].toButtonState(),
                onClick = { state.richTextEditorState.toggleInlineFormat(InlineFormat.InlineCode) },
                imageVector = ImageVector.vectorResource(VectorIcons.InlineCode),
                contentDescription = stringResource(R.string.rich_text_editor_inline_code)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.CODE_BLOCK].toButtonState(),
                onClick = { state.richTextEditorState.toggleCodeBlock() },
                imageVector = ImageVector.vectorResource(VectorIcons.CodeBlock),
                contentDescription = stringResource(R.string.rich_text_editor_code_block)
            )
            FormattingOption(
                state = state.richTextEditorState.actions[ComposerAction.QUOTE].toButtonState(),
                onClick = { state.richTextEditorState.toggleQuote() },
                imageVector = ImageVector.vectorResource(VectorIcons.Quote),
                contentDescription = stringResource(R.string.rich_text_editor_quote)
            )
        }

        sendButton(
            modifier = Modifier.constrainAs(send) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            },
        )
    }
}

private fun ActionState?.toButtonState(): FormattingOptionState =
    when (this) {
        ActionState.ENABLED -> FormattingOptionState.Default
        ActionState.REVERSED -> FormattingOptionState.Selected
        ActionState.DISABLED, null -> FormattingOptionState.Disabled
    }

@Composable
private fun ComposerModeView(
    composerMode: MessageComposerMode,
    onResetComposerMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (composerMode) {
        is MessageComposerMode.Edit -> {
            EditingModeView(onResetComposerMode = onResetComposerMode, modifier = modifier)
        }
        is MessageComposerMode.Reply -> {
            ReplyToModeView(
                modifier = modifier.padding(8.dp),
                senderName = composerMode.senderName,
                text = composerMode.defaultContent,
                attachmentThumbnailInfo = composerMode.attachmentThumbnailInfo,
                onResetComposerMode = onResetComposerMode,
            )
        }
        else -> Unit
    }
}

@Composable
private fun EditingModeView(
    onResetComposerMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
    ) {
        Icon(
            resourceId = VectorIcons.Edit,
            contentDescription = stringResource(CommonStrings.common_editing),
            tint = ElementTheme.materialColors.secondary,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(16.dp.applyScaleUp()),
        )
        Text(
            stringResource(CommonStrings.common_editing),
            style = ElementTheme.typography.fontBodySmRegular,
            textAlign = TextAlign.Start,
            color = ElementTheme.materialColors.secondary,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(CommonStrings.action_close),
            tint = ElementTheme.materialColors.secondary,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 12.dp)
                .size(16.dp.applyScaleUp())
                .clickable(
                    enabled = true,
                    onClick = onResetComposerMode,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false)
                ),
        )
    }
}

@Composable
private fun ReplyToModeView(
    senderName: String,
    text: String?,
    attachmentThumbnailInfo: AttachmentThumbnailInfo?,
    onResetComposerMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        if (attachmentThumbnailInfo != null) {
            AttachmentThumbnail(
                info = attachmentThumbnailInfo,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = senderName,
                modifier = Modifier.fillMaxWidth(),
                style = ElementTheme.typography.fontBodySmMedium,
                textAlign = TextAlign.Start,
                color = ElementTheme.materialColors.primary,
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text.orEmpty(),
                style = ElementTheme.typography.fontBodyMdRegular,
                textAlign = TextAlign.Start,
                color = ElementTheme.materialColors.secondary,
                maxLines = if (attachmentThumbnailInfo != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(CommonStrings.action_close),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(end = 4.dp, top = 4.dp, start = 16.dp, bottom = 16.dp)
                .size(16.dp.applyScaleUp())
                .clickable(
                    enabled = true,
                    onClick = onResetComposerMode,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false)
                ),
        )
    }
}

@Composable
private fun SendButton(
    canSendMessage: Boolean,
    onClick: () -> Unit,
    composerMode: MessageComposerMode,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (canSendMessage) ElementTheme.colors.iconAccentTertiary else Color.Transparent)
            .size(30.dp.applyScaleUp())
            .clickable(
                enabled = canSendMessage,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val iconId = when (composerMode) {
            is MessageComposerMode.Edit -> R.drawable.ic_tick
            else -> R.drawable.ic_send
        }
        val contentDescription = when (composerMode) {
            is MessageComposerMode.Edit -> stringResource(CommonStrings.action_edit)
            else -> stringResource(CommonStrings.action_send)
        }
        Icon(
            modifier = Modifier.size(16.dp.applyScaleUp()),
            resourceId = iconId,
            contentDescription = contentDescription,
            // Exception here, we use Color.White instead of ElementTheme.colors.iconOnSolidPrimary
            tint = if (canSendMessage) Color.White else ElementTheme.colors.iconDisabled
        )
    }
}

@DayNightPreviews
@Composable
internal fun TextComposerSimplePreview() = ElementPreview {
    Column {
        TextComposer(
            previewTextComposerState(""),
            onSendMessage = {},
            composerMode = MessageComposerMode.Normal(""),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Normal(""),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message\nWith several lines\nTo preview larger textfields and long lines with overflow"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Normal(""),
            onResetComposerMode = {},
        )
    }
}

@DayNightPreviews
@Composable
internal fun TextComposerFormattingPreview() = ElementPreview {
    Column {
        TextComposer(
            previewTextComposerState(""),
            showTextFormatting = true,
            composerMode = MessageComposerMode.Normal(""),
        )
        TextComposer(
            previewTextComposerState("A message"),
            showTextFormatting = true,
            composerMode = MessageComposerMode.Normal(""),
        )
        TextComposer(
            previewTextComposerState("A message\nWith several lines\nTo preview larger textfields and long lines with overflow"),
            showTextFormatting = true,
            composerMode = MessageComposerMode.Normal(""),
        )
    }
}

@DayNightPreviews
@Composable
internal fun TextComposerEditPreview() = ElementPreview {
    TextComposer(
        previewTextComposerState("A message"),
        onSendMessage = {},
        composerMode = MessageComposerMode.Edit(EventId("$1234"), "Some text", TransactionId("1234")),
        onResetComposerMode = {},
    )
}

@DayNightPreviews
@Composable
internal fun TextComposerReplyPreview() = ElementPreview {
    Column {
        TextComposer(
            previewTextComposerState(""),
            onSendMessage = {},
            composerMode = MessageComposerMode.Reply(
                senderName = "Alice",
                eventId = EventId("$1234"),
                attachmentThumbnailInfo = null,
                defaultContent = "A message\n" +
                    "With several lines\n" +
                    "To preview larger textfields and long lines with overflow"
            ),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Reply(
                senderName = "Alice",
                eventId = EventId("$1234"),
                attachmentThumbnailInfo = AttachmentThumbnailInfo(
                    thumbnailSource = MediaSource("https://domain.com/image.jpg"),
                    textContent = "image.jpg",
                    type = AttachmentThumbnailType.Image,
                    blurHash = "TQF5:I_NtRE4kXt7Z#MwkCIARPjr",
                ),
                defaultContent = "image.jpg"
            ),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Reply(
                senderName = "Alice",
                eventId = EventId("$1234"),
                attachmentThumbnailInfo = AttachmentThumbnailInfo(
                    thumbnailSource = MediaSource("https://domain.com/video.mp4"),
                    textContent = "video.mp4",
                    type = AttachmentThumbnailType.Video,
                    blurHash = "TQF5:I_NtRE4kXt7Z#MwkCIARPjr",
                ),
                defaultContent = "video.mp4"
            ),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Reply(
                senderName = "Alice",
                eventId = EventId("$1234"),
                attachmentThumbnailInfo = AttachmentThumbnailInfo(
                    thumbnailSource = null,
                    textContent = "logs.txt",
                    type = AttachmentThumbnailType.File,
                    blurHash = null,
                ),
                defaultContent = "logs.txt"
            ),
            onResetComposerMode = {},
        )
        TextComposer(
            previewTextComposerState("A message"),
            onSendMessage = {},
            composerMode = MessageComposerMode.Reply(
                senderName = "Alice",
                eventId = EventId("$1234"),
                attachmentThumbnailInfo = AttachmentThumbnailInfo(
                    thumbnailSource = null,
                    textContent = null,
                    type = AttachmentThumbnailType.Location,
                    blurHash = null,
                ),
                defaultContent = "Shared location"
            ),
            onResetComposerMode = {},
        )
    }
}
