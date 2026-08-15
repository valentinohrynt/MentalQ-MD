package com.c242_ps246.mentalq.ui.main.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.c242_ps246.mentalq.data.remote.response.ChatMessageItem
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
import java.util.UUID
import javax.inject.Inject

data class ChatRoomUiState(
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val firebaseServices: FirebaseServiceProvider
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val messages = _messages.asStateFlow()

    val userId = authRepository.getUserId()
        .map { it.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userRole = authRepository.getUserRole()
        .map { it.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState = _uiState.asStateFlow()

    private val _profileUrl = MutableStateFlow<String?>(null)
    val profileUrl = _profileUrl.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName = _userName.asStateFlow()

    private val _psychologistPrefix = MutableStateFlow<String?>(null)
    val psychologistPrefix = _psychologistPrefix.asStateFlow()

    private val _psychologistSuffix = MutableStateFlow<String?>(null)
    val psychologistSuffix = _psychologistSuffix.asStateFlow()

    private val _isEnded = MutableStateFlow(false)
    val isEnded = _isEnded.asStateFlow()

    private var messagesRegistration: ListenerRegistration? = null
    private var sessionRegistration: ListenerRegistration? = null
    private var messagesRoomId: String? = null
    private var sessionRoomId: String? = null

    fun endSession(chatRoomId: String) {
        val database = requireDatabase() ?: return
        database.getReference("chatroom").child(chatRoomId).child("isEnded")
            .setValue(true)
            .addOnSuccessListener { _isEnded.value = true }
            .addOnFailureListener(::setError)
    }

    fun getSessionStatus(chatRoomId: String) {
        if (sessionRoomId == chatRoomId && sessionRegistration != null) return
        sessionRegistration?.detach()
        sessionRoomId = chatRoomId

        val database = requireDatabase() ?: return
        val reference = database.getReference("chatroom").child(chatRoomId).child("isEnded")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isEnded.value = snapshot.getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(error: DatabaseError) = setError(error.toException())
        }
        reference.addValueEventListener(listener)
        sessionRegistration = ListenerRegistration(reference, listener)
    }

    fun getProfileUrl(chatRoomId: String, currentUserId: String) {
        _uiState.value = ChatRoomUiState(isLoading = true)
        val database = requireDatabase() ?: return
        database.getReference("chatroom").child(chatRoomId).child("members")
            .get()
            .addOnSuccessListener { members ->
                val isAppUser = members.child("user/id").value?.toString() == currentUserId
                val otherMember = if (isAppUser) members.child("psychologist") else members.child("user")
                _profileUrl.value = otherMember.child("profile").nullableStringValue()
                _userName.value = otherMember.child("name").nullableStringValue()
                _psychologistPrefix.value = if (isAppUser) {
                    otherMember.child("prefix").nullableStringValue()
                } else null
                _psychologistSuffix.value = if (isAppUser) {
                    otherMember.child("suffix").nullableStringValue()
                } else null
                _uiState.value = ChatRoomUiState(isLoading = false)
            }
            .addOnFailureListener(::setError)
    }

    fun sendMessage(userId: String, chatRoomId: String, messageText: String) {
        val content = messageText.trim()
        if (content.isEmpty() || _isEnded.value) return

        val database = requireDatabase() ?: return
        val messageId = database.reference.push().key ?: UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis().toString()
        val message = ChatMessageItem(
            id = messageId,
            senderId = userId,
            chatRoomId = chatRoomId,
            content = content,
            createdAt = timestamp
        )
        val updates = mapOf<String, Any?>(
            "messages/$chatRoomId/$messageId" to message,
            "chatroom/$chatRoomId/lastMessage" to content,
            "chatroom/$chatRoomId/lastMessageSenderId" to userId,
            "chatroom/$chatRoomId/updatedAt" to timestamp
        )
        database.reference.updateChildren(updates).addOnFailureListener(::setError)
    }

    fun getMessages(chatRoomId: String) {
        if (messagesRoomId == chatRoomId && messagesRegistration != null) return
        messagesRegistration?.detach()
        messagesRoomId = chatRoomId

        val database = requireDatabase() ?: return
        val reference = database.getReference("messages").child(chatRoomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _messages.value = snapshot.children
                    .mapNotNull { it.getValue(ChatMessageItem::class.java) }
                    .sortedBy { it.createdAt?.toLongOrNull() ?: 0L }
            }

            override fun onCancelled(error: DatabaseError) = setError(error.toException())
        }
        reference.addValueEventListener(listener)
        messagesRegistration = ListenerRegistration(reference, listener)
    }

    private fun DataSnapshot.nullableStringValue(): String? =
        value?.toString()?.takeUnless { it == "null" || it.isBlank() }

    private fun setError(error: Exception) {
        _uiState.value = ChatRoomUiState(isLoading = false, error = error.message ?: "Chat failed")
    }

    private fun requireDatabase() = firebaseServices.database().also { database ->
        if (database == null) {
            _uiState.value = ChatRoomUiState(
                isLoading = false,
                error = FirebaseServiceProvider.CONFIGURATION_ERROR
            )
        }
    }

    override fun onCleared() {
        messagesRegistration?.detach()
        sessionRegistration?.detach()
        super.onCleared()
    }

    private data class ListenerRegistration(
        val reference: DatabaseReference,
        val listener: ValueEventListener
    ) {
        fun detach() = reference.removeEventListener(listener)
    }
}
