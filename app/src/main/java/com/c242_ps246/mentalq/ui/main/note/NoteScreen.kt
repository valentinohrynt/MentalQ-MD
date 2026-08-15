package com.c242_ps246.mentalq.ui.main.note

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.data.remote.response.ListNoteItem
import com.c242_ps246.mentalq.ui.component.CustomToast
import com.c242_ps246.mentalq.ui.component.EmptyState
import com.c242_ps246.mentalq.ui.component.ToastType
import com.c242_ps246.mentalq.ui.theme.Black
import com.c242_ps246.mentalq.ui.theme.White
import com.c242_ps246.mentalq.ui.utils.Utils.formatDate
import com.c242_ps246.mentalq.ui.utils.Utils.getColorBasedOnPercentage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteViewModel = hiltViewModel(),
    onNavigateToNoteDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listNote by viewModel.listNote.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf(ToastType.INFO) }

    val navigateToNoteDetail by viewModel.navigateToNoteDetail.collectAsStateWithLifecycle()

    val screenPadding = when {
        screenWidth < 600.dp -> 16.dp
        screenWidth < 840.dp -> 24.dp
        else -> 32.dp
    }

    var cannotAddNoteMessage = stringResource(id = R.string.cannot_add_note)

    LaunchedEffect(uiState) {
        when {
            uiState.error != null -> {
                showToast = true
                toastMessage = uiState.error.orEmpty()
                toastType = ToastType.ERROR
                viewModel.clearError()
            }

            uiState.canAddNewNote == false -> {
                showToast = true
                toastMessage = cannotAddNoteMessage
                toastType = ToastType.INFO
            }
        }
    }

    LaunchedEffect(navigateToNoteDetail) {
        navigateToNoteDetail?.let { noteId ->
            onNavigateToNoteDetail(noteId)
            viewModel.navigateToNoteDetailCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.your_note)) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.addNote(
                                ListNoteItem(
                                    id = "",
                                    title = "",
                                    content = "",
                                    emotion = ""
                                )
                            )
                        },
                        enabled = !uiState.isCreatingNewNote && uiState.error.isNullOrEmpty(),
                        colors = IconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.add_note)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(screenPadding)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            !uiState.error.isNullOrEmpty() -> {
                                ErrorState(error = uiState.error.orEmpty())
                            }

                            uiState.isCreatingNewNote && !uiState.error.isNullOrEmpty() -> {
                                CreatingNoteState()
                            }

                            listNote.isNullOrEmpty() -> {
                                EmptyState(
                                    title = stringResource(R.string.no_notes),
                                    subtitle = stringResource(R.string.no_notes_desc)
                                )
                            }

                            else -> {
                                Text(
                                    text = buildAnnotatedString {
                                        append(stringResource(id = R.string.prediction_disclaimer_prediction))
                                        append(" ")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(stringResource(id = R.string.prediction_disclaimer_bold))
                                            append(" ")
                                        }
                                        append(stringResource(id = R.string.prediction_disclaimer_or))
                                        append(" ")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append(stringResource(id = R.string.prediction_disclaimer_bold_2))
                                        }
                                        append(stringResource(id = R.string.prediction_disclaimer_consult))
                                    },
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ResponsiveNoteList(
                                    notes = listNote ?: emptyList(),
                                    onItemClick = onNavigateToNoteDetail,
                                    onItemDelete = { note -> viewModel.deleteNote(note.id) },
                                    screenWidth = screenWidth,
                                    isLoading = uiState.isLoading
                                )
                            }
                        }
                    }
                }
            }

            if (showToast) {
                ResponsiveToast(
                    message = toastMessage,
                    type = toastType,
                    onDismiss = { showToast = false }
                )
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
private fun ResponsiveNoteList(
    notes: List<ListNoteItem>,
    onItemClick: (String) -> Unit,
    onItemDelete: (ListNoteItem) -> Unit,
    screenWidth: Dp,
    isLoading: Boolean
) {
    val listWidth = if (screenWidth >= 840.dp) {
        Modifier.fillMaxWidth(0.8f)
    } else {
        Modifier.fillMaxWidth()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            LazyColumn(
                modifier = listWidth
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(1) {
                    SkeletonNoteItem(screenWidth)
                }
            }
        } else {
            LazyColumn(
                modifier = listWidth
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { item ->
                    ResponsiveNoteItem(
                        data = item,
                        onItemClick = onItemClick,
                        onItemDelete = onItemDelete,
                        screenWidth = screenWidth
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResponsiveNoteItem(
    data: ListNoteItem,
    onItemClick: (String) -> Unit,
    onItemDelete: (ListNoteItem) -> Unit,
    screenWidth: Dp
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }

    val cardPadding = if (screenWidth < 600.dp) 12.dp else 16.dp

    val predictedPercentage = data.confidenceScore?.times(100)?.toInt() ?: 0

    val isSystemDarkMode = isSystemInDarkTheme()

    val color = if (data.confidenceScore != null && data.predictedStatus != "Normal") {
        getColorBasedOnPercentage(isSystemDarkMode, predictedPercentage)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onItemClick(data.id) },
                        onLongClick = {
                            showMenu = true
                        }
                    )
                    .onGloballyPositioned { coordinates ->
                        val cardPos = coordinates.positionInParent()
                        val cardHeight = coordinates.size.height.toFloat()
                        menuOffset = Offset(
                            cardPos.x + coordinates.size.width.toFloat(),
                            (-(cardHeight / 3))
                        )
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding)
                ) {
                    Text(
                        text = data.title ?: "",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.content ?: "",
                        style = TextStyle(fontSize = 14.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(data.createdAt.toString()),
                        style = TextStyle(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (showMenu) {
                    DropdownMenu(
                        modifier = Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        ),
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        offset = DpOffset(menuOffset.x.dp, menuOffset.y.dp),
                        shape = RoundedCornerShape(10.dp),
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 4.dp
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onItemDelete(data)
                            },
                            text = {
                                Row {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Default.Delete,
                                        tint = MaterialTheme.colorScheme.error,
                                        contentDescription = "Delete"
                                    )
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Text(
                                        color = MaterialTheme.colorScheme.error,
                                        text = stringResource(id = R.string.delete)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        if (data.predictedStatus != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (data.confidenceScore != null) {
                        "$predictedPercentage% ${data.predictedStatus}"
                    } else {
                        data.predictedStatus.orEmpty()
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemDarkMode) White else Black
                )
            }
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun CreatingNoteState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.creating_note),
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ResponsiveToast(
    message: String,
    type: ToastType,
    onDismiss: () -> Unit
) {
    CustomToast(
        message = message,
        type = type,
        onDismiss = onDismiss,
    )
}

@Composable
private fun SkeletonNoteItem(screenWidth: Dp) {
    val cardPadding = if (screenWidth < 600.dp) 12.dp else 16.dp
    val placeholderBaseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val shimmerAlpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val placeholderColor = placeholderBaseColor.copy(alpha = shimmerAlpha.value)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(placeholderColor, shape = RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(placeholderColor, shape = RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .background(placeholderColor, shape = RoundedCornerShape(8.dp))
            )
        }
    }
}


