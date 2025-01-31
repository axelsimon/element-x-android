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

package io.element.android.features.ftue.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.ftue.impl.migration.InMemoryMigrationScreenStore
import io.element.android.features.ftue.impl.migration.MigrationScreenStore
import io.element.android.features.ftue.impl.state.DefaultFtueState
import io.element.android.features.ftue.impl.state.FtueStep
import io.element.android.features.ftue.impl.welcome.state.FakeWelcomeState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.test.A_SESSION_ID
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.services.analytics.api.AnalyticsService
import io.element.android.services.analytics.test.FakeAnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultFtueStateTests {

    @Test
    fun `given any check being false, should display flow is true`() = runTest {
        val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
        val state = createState(coroutineScope)

        assertThat(state.shouldDisplayFlow.value).isTrue()

        // Cleanup
        coroutineScope.cancel()
    }

    @Test
    fun `given all checks being true, should display flow is false`() = runTest {
        val welcomeState = FakeWelcomeState()
        val analyticsService = FakeAnalyticsService()
        val migrationScreenStore = InMemoryMigrationScreenStore()
        val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

        val state = createState(coroutineScope, welcomeState, analyticsService, migrationScreenStore)

        welcomeState.setWelcomeScreenShown()
        analyticsService.setDidAskUserConsent()
        migrationScreenStore.setMigrationScreenShown(A_SESSION_ID)
        state.updateState()

        assertThat(state.shouldDisplayFlow.value).isFalse()

        // Cleanup
        coroutineScope.cancel()
    }

    @Test
    fun `traverse flow`() = runTest {
        val welcomeState = FakeWelcomeState()
        val analyticsService = FakeAnalyticsService()
        val migrationScreenStore = InMemoryMigrationScreenStore()
        val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

        val state = createState(coroutineScope, welcomeState, analyticsService, migrationScreenStore)
        val steps = mutableListOf<FtueStep?>()

        // First step, migration screen
        steps.add(state.getNextStep(steps.lastOrNull()))
        migrationScreenStore.setMigrationScreenShown(A_SESSION_ID)

        // Second step, welcome screen
        steps.add(state.getNextStep(steps.lastOrNull()))
        welcomeState.setWelcomeScreenShown()

        // Third step, analytics opt in
        steps.add(state.getNextStep(steps.lastOrNull()))
        analyticsService.setDidAskUserConsent()

        // Final step (null)
        steps.add(state.getNextStep(steps.lastOrNull()))

        assertThat(steps).containsExactly(
            FtueStep.MigrationScreen,
            FtueStep.WelcomeScreen,
            FtueStep.AnalyticsOptIn,
            null, // Final state
        )

        // Cleanup
        coroutineScope.cancel()
    }

    @Test
    fun `if a check for a step is true, start from the next one`() = runTest {
        val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
        val analyticsService = FakeAnalyticsService()
        val migrationScreenStore = InMemoryMigrationScreenStore()

        val state = createState(
            coroutineScope = coroutineScope,
            analyticsService = analyticsService,
            migrationScreenStore = migrationScreenStore,
        )

        migrationScreenStore.setMigrationScreenShown(A_SESSION_ID)
        assertThat(state.getNextStep()).isEqualTo(FtueStep.WelcomeScreen)

        state.setWelcomeScreenShown()
        assertThat(state.getNextStep()).isEqualTo(FtueStep.AnalyticsOptIn)

        analyticsService.setDidAskUserConsent()
        assertThat(state.getNextStep(FtueStep.WelcomeScreen)).isNull()

        // Cleanup
        coroutineScope.cancel()
    }

    private fun createState(
        coroutineScope: CoroutineScope,
        welcomeState: FakeWelcomeState = FakeWelcomeState(),
        analyticsService: AnalyticsService = FakeAnalyticsService(),
        migrationScreenStore: MigrationScreenStore = InMemoryMigrationScreenStore(),
        matrixClient: MatrixClient = FakeMatrixClient(),
    ) = DefaultFtueState(
        coroutineScope = coroutineScope,
        analyticsService = analyticsService,
        welcomeScreenState = welcomeState,
        migrationScreenStore = migrationScreenStore,
        matrixClient = matrixClient,
    )
}
