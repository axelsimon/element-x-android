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

package io.element.android.features.messages.textcomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.element.android.features.messages.impl.messagecomposer.TextComposerStateFactory
import io.element.android.libraries.textcomposer.TextComposerState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

class TestTextComposerStateFactory : TextComposerStateFactory {
    private val states: MutableStateFlow<TextComposerState> = MutableStateFlow(
        createFakeTextComposerState(
            onSetHtml = ::onSetHtml,
            onRequestFocus = ::onRequestFocus,
        )
    )

    private fun onSetHtml(html: String) {
        states.value = states.value.copyMock(messageHtml = html, onSetHtml = ::onSetHtml, onRequestFocus = ::onRequestFocus)
    }

    private fun onRequestFocus(): Boolean {
        states.value = states.value.copyMock(hasFocus = true, onSetHtml = ::onSetHtml, onRequestFocus = ::onRequestFocus)
        return true
    }

    @Composable
    override fun create(): TextComposerState {
        val state by states.collectAsState(
            createFakeTextComposerState(
                onSetHtml = ::onSetHtml,
                onRequestFocus = ::onRequestFocus,
            )
        )
        return state
    }
}

private fun createFakeTextComposerState(
    messageHtml: String = "",
    messageMarkdown: String = "",
    hasFocus: Boolean = false,
    onSetHtml: (String) -> Unit,
    onRequestFocus: () -> Boolean,
) = mockk<TextComposerState> {
    every { this@mockk.messageHtml } returns messageHtml
    every { this@mockk.messageMarkdown } returns messageMarkdown
    every { this@mockk.hasFocus } returns hasFocus
    every { this@mockk.lineCount } returns messageHtml.count { it == '\n' } + 1
    every { this@mockk.canSendMessage } returns messageHtml.isNotEmpty()
    every { this@mockk.setHtml(any()) } answers { onSetHtml(firstArg()) }
    every { this@mockk.requestFocus() } answers { onRequestFocus() }
}

private fun TextComposerState.copyMock(
    messageHtml: String = this.messageHtml,
    messageMarkdown: String = this.messageMarkdown,
    hasFocus: Boolean = this.hasFocus,
    onSetHtml: (String) -> Unit = {},
    onRequestFocus: () -> Boolean = { true },
) = createFakeTextComposerState(
    messageHtml = messageHtml,
    messageMarkdown = messageMarkdown,
    hasFocus = hasFocus,
    onSetHtml = onSetHtml,
    onRequestFocus = onRequestFocus,
)
