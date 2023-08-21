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

import androidx.compose.runtime.Composable
import io.element.android.wysiwyg.compose.RichTextEditorState
import io.element.android.wysiwyg.compose.rememberRichTextEditorState


data class TextComposerState internal constructor(
    val richTextEditorState: RichTextEditorState
) {
    val messageHtml: String
        get() = richTextEditorState.messageHtml

    val messageMarkdown: String
        get() = richTextEditorState.messageMarkdown

    val hasFocus: Boolean
        get() = richTextEditorState.hasFocus

    val lineCount: Int
        get() = richTextEditorState.lineCount

    val canSendMessage: Boolean
        get() = messageHtml.isNotEmpty()

    fun setHtml(value: String) {
        richTextEditorState.setHtml(value)
    }

    fun requestFocus(): Boolean =
        richTextEditorState.requestFocus()
}

@Composable
fun rememberTextComposerState(): TextComposerState {
    val richTextEditorState = rememberRichTextEditorState()

    return TextComposerState(richTextEditorState)
}

fun previewTextComposerState(text: String): TextComposerState {
    val richTextEditorState = RichTextEditorState.createForLocalInspectionMode(text)

    return TextComposerState(richTextEditorState)
}
