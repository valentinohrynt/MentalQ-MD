package com.c242_ps246.mentalq.ui.main.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ChatRoomItem
import com.c242_ps246.mentalq.data.repository.AuthRepository
import com.c242_ps246.mentalq.data.manager.FirebaseServiceProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val firebaseServices: FirebaseServiceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _chatRooms = MutableStateFlow<List<ChatRoomItem>>(emptyList())
    val chatRooms = _chatRooms.asStateFlow()

    val userId = authRepository.getUserId()
        .map { it.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val roomsById = mutableMapOf<String, ChatRoomItem>()
    private var userChatsRegistration: ListenerRegistration? = null
    private val roomRegistrations = mutableMapOf<String, ListenerRegistration>()

    init {
        viewModelScope.launch {
            userId.collect(::observeUserChats)
        }
    }

    private fun observeUserChats(currentUserId: String?) {
        detachAllListeners()
        roomsById.clear()
        _chatRooms.value = emptyList()

        if (currentUserId == null) {
            _uiState.value = ChatListUiState(error = "No signed-in user")
            return
        }

        _uiState.value = ChatListUiState(isLoading = true)
        val database = firebaseServices.database()
        if (database == null) {
            _uiState.value = ChatListUiState(error = FirebaseServiceProvider.CONFIGURATION_ERROR)
            return
        }
        val reference = database.getReference("userChats").child(currentUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomIds = snapshot.children
                    .mapNotNull { it.value?.toString()?.takeIf(String::isNotBlank) }
                    .toSet()
                synchronizeRoomListeners(roomIds)
                _uiState.value = ChatListUiState(isLoading = false)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = ChatListUiState(error = error.message)
            }
        }
        reference.addValueEventListener(listener)
        userChatsRegistration = ListenerRegistration(reference, listener)
    }

    private fun synchronizeRoomListeners(roomIds: Set<String>) {
        (roomRegistrations.keys - roomIds).forEach { roomId ->
            roomRegistrations.remove(roomId)?.detach()
            roomsById.remove(roomId)
        }
        (roomIds - roomRegistrations.keys).forEach(::observeRoom)
        publishRooms()
    }

    private fun observeRoom(roomId: String) {
        val database = firebaseServices.database() ?: return
        val reference = database.getReference("chatroom").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    roomsById[roomId] = snapshot.toChatRoom(roomId)
                } else {
                    roomsById.remove(roomId)
                }
                publishRooms()
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = ChatListUiState(error = error.message)
            }
        }
        reference.addValueEventListener(listener)
        roomRegistrations[roomId] = ListenerRegistration(reference, listener)
    }

    private fun publishRooms() {
        _chatRooms.value = roomsById.values.sortedByDescending { it.updatedAt.toLongOrNull() ?: 0L }
    }

    private fun DataSnapshot.toChatRoom(roomId: String) = ChatRoomItem(
        id = roomId,
        userId = child("members/user/id").stringValue(),
        userName = child("members/user/name").stringValue(),
        userProfile = child("members/user/profile").nullableStringValue(),
        psychologistId = child("psychologistId").stringValue(),
        psychologistName = child("members/psychologist/name").stringValue(),
        psychologistPrefix = child("members/psychologist/prefix").nullableStringValue(),
        psychologistSuffix = child("members/psychologist/suffix").nullableStringValue(),
        psychologistProfile = child("members/psychologist/profile").nullableStringValue(),
        lastMessage = child("lastMessage").nullableStringValue(),
        lastMessageSenderId = child("lastMessageSenderId").nullableStringValue(),
        createdAt = child("createdAt").stringValue(),
        updatedAt = child("updatedAt").stringValue()
    )

    private fun DataSnapshot.stringValue(): String = value?.toString().orEmpty()

    private fun DataSnapshot.nullableStringValue(): String? =
        value?.toString()?.takeUnless { it == "null" || it.isBlank() }

    private fun detachAllListeners() {
        userChatsRegistration?.detach()
        userChatsRegistration = null
        roomRegistrations.values.forEach(ListenerRegistration::detach)
        roomRegistrations.clear()
    }

    override fun onCleared() {
        detachAllListeners()
        super.onCleared()
    }

    private data class ListenerRegistration(
        val reference: DatabaseReference,
        val listener: ValueEventListener
    ) {
        fun detach() = reference.removeEventListener(listener)
    }
}

data class ChatListUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
