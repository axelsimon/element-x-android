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

package io.element.android.features.roomdetails.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import io.element.android.features.leaveroom.api.LeaveRoomEvent
import io.element.android.features.leaveroom.api.LeaveRoomPresenter
import io.element.android.features.roomdetails.impl.members.details.RoomMemberDetailsPresenter
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.matrix.api.room.MatrixRoomMembersState
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.room.StateEventType
import io.element.android.libraries.matrix.api.room.powerlevels.canInvite
import io.element.android.libraries.matrix.api.room.powerlevels.canSendState
import io.element.android.libraries.matrix.ui.room.getDirectRoomMember
import javax.inject.Inject

class RoomDetailsPresenter @Inject constructor(
    private val room: MatrixRoom,
    private val roomMembersDetailsPresenterFactory: RoomMemberDetailsPresenter.Factory,
    private val leaveRoomPresenter: LeaveRoomPresenter,
) : Presenter<RoomDetailsState> {

    @Composable
    override fun present(): RoomDetailsState {
        val leaveRoomState = leaveRoomPresenter.present()
        LaunchedEffect(Unit) {
            room.updateMembers()
        }

        val membersState by room.membersStateFlow.collectAsState()
        val canInvite by getCanInvite(membersState)
        val canEditName by getCanSendState(membersState, StateEventType.ROOM_NAME)
        val canEditAvatar by getCanSendState(membersState, StateEventType.ROOM_AVATAR)
        val canEditTopic by getCanSendState(membersState, StateEventType.ROOM_TOPIC)
        val dmMember by room.getDirectRoomMember(membersState)
        val roomMemberDetailsPresenter = roomMemberDetailsPresenter(dmMember)
        val roomType by getRoomType(dmMember)

        val topicState = remember(canEditTopic, room.topic, roomType) {
            val topic = room.topic

            when {
                !topic.isNullOrBlank() -> RoomTopicState.ExistingTopic(topic)
                canEditTopic && roomType is RoomDetailsType.Room -> RoomTopicState.CanAddTopic
                else -> RoomTopicState.Hidden
            }
        }

        fun handleEvents(event: RoomDetailsEvent) {
            when (event) {
                is RoomDetailsEvent.LeaveRoom ->
                    leaveRoomState.eventSink(LeaveRoomEvent.ShowConfirmation(room.roomId))
            }
        }

        val roomMemberDetailsState = roomMemberDetailsPresenter?.present()

        return RoomDetailsState(
            roomId = room.roomId.value,
            roomName = room.displayName,
            roomAlias = room.alias,
            roomAvatarUrl = room.avatarUrl,
            roomTopic = topicState,
            memberCount = room.joinedMemberCount,
            isEncrypted = room.isEncrypted,
            canInvite = canInvite,
            canEdit = (canEditAvatar || canEditName || canEditTopic) && roomType == RoomDetailsType.Room,
            roomType = roomType,
            roomMemberDetailsState = roomMemberDetailsState,
            leaveRoomState = leaveRoomState,
            eventSink = ::handleEvents,
        )
    }

    @Composable
    private fun roomMemberDetailsPresenter(dmMemberState: RoomMember?) = remember(dmMemberState) {
        dmMemberState?.let { roomMember ->
            roomMembersDetailsPresenterFactory.create(roomMember.userId)
        }
    }

    @Composable
    private fun getRoomType(dmMember: RoomMember?): State<RoomDetailsType> = remember(dmMember) {
        derivedStateOf {
            if (dmMember != null) {
                RoomDetailsType.Dm(dmMember)
            } else {
                RoomDetailsType.Room
            }
        }
    }

    @Composable
    private fun getCanInvite(membersState: MatrixRoomMembersState) = produceState(false, membersState) {
        value = room.canInvite().getOrElse { false }
    }

    @Composable
    private fun getCanSendState(membersState: MatrixRoomMembersState, type: StateEventType) = produceState(false, membersState) {
        value = room.canSendState(type).getOrElse { false }
    }
}
