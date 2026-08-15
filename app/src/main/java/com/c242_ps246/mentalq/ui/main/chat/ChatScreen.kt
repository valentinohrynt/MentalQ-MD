package com.c242_ps246.mentalq.ui.main.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.data.remote.response.ChatRoomItem
import com.c242_ps246.mentalq.ui.component.EmptyState
import com.c242_ps246.mentalq.ui.utils.Utils.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToChatRoom: (String) -> Unit,
    onBackClick: () -> Unit
) {

    val viewModel: ChatViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatRooms by viewModel.chatRooms.collectAsStateWithLifecycle()
    val userId by viewModel.userId.collectAsStateWithLifecycle()

    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.your_messages)) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (chatRooms.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    title = stringResource(R.string.no_messages),
                    subtitle = uiState.error ?: stringResource(R.string.no_messages_desc)
                )
            } else {
                userId?.let { currentUserId ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = chatRooms,
                            key = { it.id }
                        ) { chatRoom ->
                            ChatPreviewItem(
                                chatRoom = chatRoom,
                                currentUserId = currentUserId,
                                onClick = { onNavigateToChatRoom(chatRoom.id) }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ChatPreviewItem(
    chatRoom: ChatRoomItem,
    currentUserId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            val profileImg = if (currentUserId == chatRoom.userId) {
                chatRoom.psychologistProfile
            } else {
                chatRoom.userProfile
            }

            val imageModel = if (profileImg == "null" || profileImg == null) {
                R.drawable.default_profile
            } else {
                profileImg
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageModel)
                    .placeholder(R.drawable.default_profile)
                    .crossfade(true)
                    .scale(Scale.FILL)
                    .size(48)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    val title = if (currentUserId == chatRoom.userId) {
                        val prefix = chatRoom.psychologistPrefix
                        val suffix = chatRoom.psychologistSuffix

                        val prefixTitle = if (prefix != "null") "$prefix " else ""
                        val suffixTitle = if (suffix != "null") " $suffix" else ""

                        "$prefixTitle${chatRoom.psychologistName}$suffixTitle"
                    } else {
                        chatRoom.userName
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = chatRoom.updatedAt.toLongOrNull()?.let(::formatTimestamp).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastMassage = if (currentUserId == chatRoom.lastMessageSenderId) {
                        stringResource(R.string.you_chat) + ": ${chatRoom.lastMessage}"
                    } else {
                        chatRoom.lastMessage
                    }

                    Text(
                        text = lastMassage ?: "No messages yet",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
