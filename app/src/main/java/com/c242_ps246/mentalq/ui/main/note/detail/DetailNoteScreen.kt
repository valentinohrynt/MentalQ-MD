package com.c242_ps246.mentalq.ui.main.note.detail

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.ui.component.CustomDialog
import com.c242_ps246.mentalq.ui.component.CustomToast
import com.c242_ps246.mentalq.ui.component.ToastType
import com.c242_ps246.mentalq.ui.component.VoiceToTextParser
import com.c242_ps246.mentalq.ui.utils.Utils.formatDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailNoteScreen(
    noteId: String,
    onBackClick: () -> Unit,
    application: Application
) {
    val viewModel: NoteDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title: String by viewModel.title.collectAsStateWithLifecycle()
    val content: String by viewModel.content.collectAsStateWithLifecycle()
    val date: String by viewModel.date.collectAsStateWithLifecycle()
    val selectedEmotion: String by viewModel.emotion.collectAsStateWithLifecycle()
    val voiceToTextParser = remember { VoiceToTextParser(application) }
    DisposableEffect(voiceToTextParser) {
        onDispose(voiceToTextParser::destroy)
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun handleBack() {
        keyboardController?.hide()
        if (uiState.note == null) {
            onBackClick()
        } else if (!uiState.isSaving) {
            viewModel.saveNoteImmediately()
        }
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBackClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        handleBack()
                    },
                    enabled = !uiState.isSaving
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.back),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            )
        )

        if (uiState.isSaving) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.updating_note),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else if (uiState.note == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${uiState.error ?: stringResource(R.string.no_notes)}")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        viewModel.updateTitle(it)
                    },
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.title_placeholder),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Text(
                    text = formatDate(date),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            viewModel.updateContent(it)
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.content_placeholder),
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    VoiceInputButton(
                        viewModel = viewModel,
                        voiceToTextParser = voiceToTextParser
                    )
                }


                Text(
                    text = stringResource(id = R.string.how_do_you_feel),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                val emotionList = listOf(
                    stringResource(id = R.string.happy),
                    stringResource(id = R.string.anxious),
                    stringResource(id = R.string.angry),
                    stringResource(id = R.string.sad)
                )
                val emoji = listOf("😆", "😰", "😡", "😕")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(emotionList.size, key = { emotionList[it] }) { index ->
                        Spacer(modifier = Modifier.width(8.dp))
                        EmotionButton(
                            emotion = emotionList[index],
                            emoji = emoji[index],
                            selectedEmotion = selectedEmotion
                        ) {
                            viewModel.updateEmotion(emotionList[index])
                        }
                    }
                }
            }
        }

        if (uiState.error != null && uiState.note != null) {
            CustomToast(
                message = uiState.error.orEmpty(),
                type = ToastType.ERROR,
                onDismiss = viewModel::clearError
            )
        }
    }
}

@Composable
fun VoiceInputButton(
    viewModel: NoteDetailViewModel,
    voiceToTextParser: VoiceToTextParser
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var currentLocale = remember { Locale.getDefault().language }
    val voiceState by voiceToTextParser.state.collectAsStateWithLifecycle()

    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf(ToastType.INFO) }

    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var hasPermission by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                hasPermission = true
            } else {
                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.RECORD_AUDIO
                    )
                ) {
                    showRationaleDialog = true
                } else {
                    showSettingsDialog = true
                }
            }
        }
    )

    if (showRationaleDialog) {
        CustomDialog(
            dialogTitle = stringResource(id = R.string.permission_required),
            dialogMessage = stringResource(id = R.string.audio_permission_rationale),
            onConfirm = {
                showRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            confirmButtonText = stringResource(id = R.string.try_again),
            onDismiss = {
                showRationaleDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        CustomDialog(
            dialogTitle = stringResource(id = R.string.permission_required),
            dialogMessage = stringResource(id = R.string.audio_permission_settings),
            onConfirm = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            },
            confirmButtonText = stringResource(id = R.string.open_settings),
            onDismiss = {
                showSettingsDialog = false
            }
        )
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        currentLocale = Locale.getDefault().toLanguageTag()
    }

    LaunchedEffect(voiceState.spokenText) {
        voiceState.spokenText?.let { spokenText ->
            if (spokenText.isNotBlank()) {
                val currentContent = viewModel.content.value
                viewModel.updateContent(
                    "$currentContent ${spokenText.trim()} "
                )
            }
        }
    }

    LaunchedEffect(voiceState.isSpeaking) {
        if (voiceState.isSpeaking) {
            scope.launch {
                delay(3000)
                if (!voiceState.hasStartedSpeaking) {
                    voiceToTextParser.stopListening()
                }
            }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            if (voiceState.isSpeaking) {
                val rippleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                Canvas(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(scale)
                ) {
                    drawCircle(
                        color = rippleColor,
                        radius = size.minDimension / 2
                    )
                }
            }

            IconButton(
                onClick = {
                    if (hasPermission) {
                        if (voiceState.isSpeaking) {
                            voiceToTextParser.stopListening()
                        } else {
                            currentLocale = Locale.getDefault().toLanguageTag()
                            voiceToTextParser.startListening(currentLocale)
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (voiceState.isSpeaking)
                        Icons.Default.Stop
                    else
                        Icons.Default.Mic,
                    contentDescription = stringResource(
                        if (voiceState.isSpeaking)
                            R.string.voice_stop_recording
                        else
                            R.string.voice_start_input
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    val errorMessage = when {
        voiceState.error?.contains("Error: 7") == true -> stringResource(R.string.voice_error_cant_hear)
        voiceState.error?.contains("Error: 5") == true -> stringResource(R.string.voice_error_generic)
        voiceState.error?.contains("Error: 6") == true -> stringResource(R.string.voice_error_generic)
        voiceState.error?.contains("Error: 8") == true -> stringResource(R.string.voice_error_timeout)
        voiceState.error?.contains("Error: 9") == true -> stringResource(R.string.voice_error_permission)
        voiceState.error != null -> stringResource(R.string.voice_error_generic)
        else -> ""
    }

    LaunchedEffect(voiceState.error) {
        if (voiceState.error != null) {
            toastMessage = errorMessage
            toastType = ToastType.ERROR
            showToast = true
        }
    }

    if (showToast) {
        CustomToast(
            message = toastMessage,
            type = toastType,
            onDismiss = { showToast = false },
            placement = Alignment.BottomCenter
        )
    }
}

@Composable
private fun EmotionButton(
    emotion: String,
    emoji: String,
    selectedEmotion: String?,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selectedEmotion == emotion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .wrapContentSize(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        onClick = { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(82.dp)
                .height(82.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier
                    .padding(bottom = 4.dp)
            )
            Text(
                text = emotion,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedEmotion == emotion) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
