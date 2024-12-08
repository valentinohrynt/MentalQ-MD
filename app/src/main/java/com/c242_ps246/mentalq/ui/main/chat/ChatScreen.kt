package com.c242_ps246.mentalq.ui.main.chat

//import coil.compose.AsyncImage
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.ui.utils.Utils.formatTimestamp
import com.c242_ps246.mentalq.data.remote.response.ChatRoomItem
import com.c242_ps246.mentalq.ui.component.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToChatRoom: (String) -> Unit,
    onChatSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {

    val viewModel: ChatViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatRooms by viewModel.chatRooms.collectAsStateWithLifecycle()


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
            Log.e("ChatScreen", "$chatRooms")
            if (chatRooms.isEmpty() && !uiState.isLoading) {
                Log.e("ChatScreen", "ChatScreen: Its Empty!")
                EmptyState(
                    title = stringResource(R.string.no_messages),
                    subtitle = stringResource(R.string.no_messages_desc)
                )
            } else {
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
                            onClick = { onNavigateToChatRoom(chatRoom.id) }
                        )
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

//
@Composable
private fun ChatPreviewItem(
    chatRoom: ChatRoomItem,
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
                    Text(
                        text = chatRoom.psychologistName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatTimestamp(chatRoom.updatedAt.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chatRoom.lastMessage ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}


//@Preview()
//@Composable
//fun PreviewChatScreen() {
//
//    MentalQTheme {
//        ChatScreen(
//            onChatSelected = {},
//            onNavigateToChatRoom = {}, onNavigateToPsychologist = {})
//    }
//}