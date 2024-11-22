@file:Suppress("DEPRECATION")

package com.c242_ps246.mentalq.ui.main.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.size.Scale
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.c242_ps246.mentalq.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.ui.component.CustomDialog
import com.c242_ps246.mentalq.ui.theme.MentalQTheme
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.c242_ps246.mentalq.ui.utils.Utils.compressImageSize
import com.c242_ps246.mentalq.ui.utils.Utils.formatDate
import com.c242_ps246.mentalq.ui.utils.Utils.uriToFile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getUserData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileHeader()
                }

                item {
                    ProfileInfo(userData = userData, onLogout = {
                        if (!uiState.isLoading) {
                            viewModel.logout()
                            onLogout()
                        }
                    })
                }

                item {
                    PreferencesSection(
                        notificationsEnabled = notificationsEnabled,
                        onNotificationChange = { isEnabled ->
                            viewModel.setNotificationsEnabled(isEnabled)
                        },
                        onShowLogoutDialog = { showConfirmDialog = true }
                    )
                }

                item {
                    if (showConfirmDialog) {
                        LogoutDialog(
                            onConfirm = {
                                viewModel.logout()
                                onLogout()
                                showConfirmDialog = false
                            },
                            onDismiss = { showConfirmDialog = false }
                        )
                    }
                }
            }
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    Text(
        text = stringResource(id = R.string.your_profile),
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
    )
}

@Composable
fun ProfileImage(
    imageUrl: String?,
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Int = 100
) {
    val imageModel = if (!imageUri.isNullOrBlank()) {
        imageUri
    } else {
        imageUrl
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageModel)
            .placeholder(R.drawable.default_profile)
//            .error(R.drawable.default_profile)
            .crossfade(true)
            .scale(Scale.FILL)
            .size(size)
            .build(),
        contentDescription = "Profile Picture",
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ProfileInfo(userData: UserData?, onLogout: () -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileImage(
                imageUrl = userData?.profilePhotoUrl,
                imageUri = null,
                size = 100
            )
            Spacer(modifier = Modifier.height(8.dp))
            UserDetailInfo(userData = userData)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(stringResource(id = R.string.edit_profile))
            }
        }
        if (showEditDialog) {
            EditProfileDialog(
                userData = userData,
                onDismiss = { showEditDialog = false },
                onSave = { name, email, birthday, imageUri ->
                    Log.e("ProfileScreen", "Name: $name, Email: $email, Birthday: $birthday")
                    val nameRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), name)
                    val emailRequestBody =
                        RequestBody.create("text/plain".toMediaTypeOrNull(), email)
                    val birthdayRequestBody =
                        RequestBody.create("text/plain".toMediaTypeOrNull(), birthday)
                    val profileImagePart = imageUri?.let {
                        val file = uriToFile(it, context)
                        val fileCompressed = file.compressImageSize()
                        val requestFile =
                            RequestBody.create("image/jpeg".toMediaTypeOrNull(), fileCompressed)
                        MultipartBody.Part.createFormData("profileImage", file.name, requestFile)
                    }
                    Log.e("ProfileScreen", "Profile Image: $profileImagePart")
                    viewModel.updateProfile(
                        nameRequestBody,
                        emailRequestBody,
                        birthdayRequestBody,
                        profileImagePart
                    )
                    if (email != userData?.email) {
                        onLogout()
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    userData: UserData?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(userData?.name ?: "") }
    var email by remember { mutableStateOf(userData?.email ?: "") }
    var birthday by remember { mutableStateOf(userData?.birthday ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEmailConfirmationDialog by remember { mutableStateOf(false) }
    var pendingEmail by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.edit_profile),
                    style = MaterialTheme.typography.titleLarge
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { launcher.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                    ) {
                        ProfileImage(
                            imageUrl = userData?.profilePhotoUrl,
                            imageUri = null ?: imageUri?.toString(),
                            size = 100
                        )
                    }
                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Change Picture",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.label_name)) },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(id = R.string.label_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { },
                    label = { Text(stringResource(id = R.string.label_birthday)) },
                    leadingIcon = { Icon(Icons.Default.Cake, null) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Select Date")
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (email != userData?.email) {
                                pendingEmail = email
                                showEmailConfirmationDialog = true
                            } else {
                                onSave(name, email, birthday, imageUri)
                                onDismiss()
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.save))
                    }
                }
            }
        }
    }

    if (showEmailConfirmationDialog) {
        CustomDialog(
            dialogTitle = stringResource(id = R.string.confirm_email_update_title),
            dialogMessage = (stringResource(id = R.string.update_email_confirmation_message_1) + pendingEmail + "." + stringResource(
                id = R.string.update_email_confirmation_message_2
            )),
            onConfirm = {
                onSave(name, pendingEmail, birthday, imageUri)
                onDismiss()
                showEmailConfirmationDialog = false
            },
            onDismiss = {
                showEmailConfirmationDialog = false
                email = userData?.email ?: ""
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            birthday = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                dateFormatter = remember { dateFormatter() },
                title = {
                    DatePickerDefaults.DatePickerTitle(
                        displayMode = datePickerState.displayMode,
                        modifier = Modifier.padding(16.dp)
                    )
                },
                headline = {
                    DatePickerDefaults.DatePickerHeadline(
                        selectedDateMillis = datePickerState.selectedDateMillis,
                        displayMode = datePickerState.displayMode,
                        dateFormatter = dateFormatter(),
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = true,
                colors = DatePickerDefaults.colors()
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun UserDetailInfo(userData: UserData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = userData?.name ?: "",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userData?.email ?: "",
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatDate(userData?.birthday ?: ""),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun PreferencesSection(
    notificationsEnabled: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    onShowLogoutDialog: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onNotificationChange(true)
            }
        }
    )

    Text(
        text = stringResource(id = R.string.preferences),
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PreferenceItem(
                title = stringResource(id = R.string.notifications),
                isChecked = notificationsEnabled,
                onCheckedChange = { isEnabled ->
                    if (isEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            when (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            )) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    onNotificationChange(true)
                                }

                                else -> {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        } else {
                            onNotificationChange(true)
                        }
                    } else {
                        onNotificationChange(false)
                    }
                }
            )
            PreferenceItem(
                title = stringResource(id = R.string.language)
            )
            PreferenceItem(
                title = stringResource(id = R.string.privacy_and_policy)
            )
            PreferenceItem(
                title = stringResource(id = R.string.terms_of_service)
            )
            LogoutItem(onClick = onShowLogoutDialog)
        }
    }
}

@Composable
private fun PreferenceItem(
    title: String,
    isChecked: Boolean = false,
    onClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onCheckedChange == null,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            if (onCheckedChange != null) {
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun LogoutItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = R.string.logout),
            color = Color.Red
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = stringResource(id = R.string.logout),
            tint = Color.Red
        )
    }
}

@Composable
private fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomDialog(
        dialogTitle = stringResource(id = R.string.logout),
        dialogMessage = stringResource(id = R.string.logout_message),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun ProfileScreenPreview() {
    MentalQTheme {
        ProfileScreen(onLogout = {})
    }
}