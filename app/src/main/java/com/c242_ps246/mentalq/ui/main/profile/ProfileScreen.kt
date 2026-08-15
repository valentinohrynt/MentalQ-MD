package com.c242_ps246.mentalq.ui.main.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.c242_ps246.mentalq.BuildConfig
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.data.remote.response.UserData
import com.c242_ps246.mentalq.ui.component.CustomDialog
import com.c242_ps246.mentalq.ui.component.TermsWebView
import com.c242_ps246.mentalq.ui.notification.dailyreminder.DailyReminderNotificationHelper
import com.c242_ps246.mentalq.ui.notification.streak.StreakNotificationHelper
import com.c242_ps246.mentalq.ui.utils.Utils.formatDate
import com.c242_ps246.mentalq.ui.utils.Utils.prepareProfileImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.your_profile)) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileInfo(
                            userData = userData,
                            onLogout = {
                                if (!uiState.isLoading) {
                                    viewModel.logout(onComplete = onLogout)
                                }
                            },
                            viewModel = viewModel
                        )
                    }

                    item {
                        PreferencesSection(
                            notificationsEnabled = notificationsEnabled,
                            onNotificationChange = { isEnabled ->
                                viewModel.setNotificationsEnabled(isEnabled)
                            },
                            onShowLogoutDialog = { showConfirmDialog = true },
                        )
                    }

                    item {
                        if (showConfirmDialog) {
                            LogoutDialog(
                                onConfirm = {
                                    viewModel.logout(onComplete = onLogout)
                                    showConfirmDialog = false
                                },
                                onDismiss = { showConfirmDialog = false }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
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
    } else if (!imageUrl.isNullOrBlank()) {
        imageUrl
    } else {
        R.drawable.default_profile
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageModel)
            .placeholder(R.drawable.default_profile)
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

@Suppress("DEPRECATION")
@Composable
private fun ProfileInfo(
    userData: UserData?,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                    .padding(8.dp),
                enabled = !uiState.isLoading
            ) {
                Text(stringResource(id = R.string.edit_profile))
            }
        }
        if (showEditDialog) {
            EditProfileDialog(
                userData = userData,
                onDismiss = { showEditDialog = false },
                onSave = { name, email, birthday, imageUri ->
                    scope.launch {
                        val nameRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), name)
                        val emailRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), email)
                        val birthdayRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), birthday)
                        val profileImagePart = imageUri?.let { prepareProfileImage(it, context) }
                        viewModel.updateProfile(
                            nameRequestBody,
                            emailRequestBody,
                            birthdayRequestBody,
                            profileImagePart,
                            onSuccess = {
                                if (email != userData?.email) onLogout()
                            }
                        )
                    }
                }
            )
        }
    }
}

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

    val today = LocalDate.now()
    today.minusYears(100)

    var emailError by remember { mutableStateOf<Int?>(null) }
    var nameError by remember { mutableStateOf<Int?>(null) }
    var birthdayError by remember { mutableStateOf<Int?>(null) }

    fun validateName(name: String): Boolean {
        return if (name.isEmpty()) {
            nameError = R.string.error_name_empty
            false
        } else {
            nameError = null
            true
        }
    }

    fun validateBirthday(birthday: String): Boolean {
        return if (birthday.isEmpty()) {
            birthdayError = R.string.error_birthday_empty
            false
        } else {
            birthdayError = null
            true
        }
    }

    fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            emailError = R.string.error_email_empty
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = R.string.error_email_invalid
            false
        } else {
            emailError = null
            true
        }
    }

    fun validateForm(): Boolean {
        val isEmailValid = validateEmail(email)
        val isNameValid = validateName(name)
        val isBirthdayValid = validateBirthday(birthday)
        return isEmailValid && isNameValid && isBirthdayValid
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
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
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.label_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        maxLines = 1,
                        singleLine = true
                    )
                    nameError?.let { error ->
                        nameError?.let {
                            Text(
                                text = stringResource(it),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(id = R.string.label_email)) },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        maxLines = 1,
                        singleLine = true
                    )
                    emailError?.let { error ->
                        emailError?.let {
                            Text(
                                text = stringResource(it),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Column {
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
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        maxLines = 1,
                        singleLine = true
                    )
                    birthdayError?.let { error ->
                        Text(
                            text = stringResource(error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

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
                            if (validateForm()) {
                                if (email != userData?.email) {
                                    pendingEmail = email
                                    showEmailConfirmationDialog = true
                                } else {
                                    onSave(name, email, birthday, imageUri)
                                    onDismiss()
                                }
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
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            if (!selectedDate.isAfter(today) && selectedDate.plusYears(17)
                                    .isBefore(today)
                            ) {
                                birthday =
                                    selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            }
                            showDatePicker = false
                        }
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

@Composable
private fun UserDetailInfo(userData: UserData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = userData?.name ?: "",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userData?.email ?: "",
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatDate(userData?.birthday ?: ""),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
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
    val activity = context as? Activity
    val streakNotificationHelper = StreakNotificationHelper(context)
    val dailyReminderNotificationHelper = DailyReminderNotificationHelper(context)

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onNotificationChange(true)
                streakNotificationHelper.createNotificationChannel()
                dailyReminderNotificationHelper.createNotificationChannel()
            } else {
                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    showRationaleDialog = true
                } else {
                    showSettingsDialog = true
                }
                onNotificationChange(false)
            }
        }
    )

    if (showRationaleDialog) {
        CustomDialog(
            dialogTitle = stringResource(id = R.string.permission_required),
            dialogMessage = stringResource(id = R.string.notification_permission_rationale),
            onConfirm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                showRationaleDialog = false
            },
            onDismiss = { showRationaleDialog = false }
        )
    }

    if (showSettingsDialog) {
        CustomDialog(
            dialogTitle = stringResource(id = R.string.permission_required),
            dialogMessage = stringResource(id = R.string.notification_permission_settings),
            onConfirm = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
                showSettingsDialog = false
            },
            confirmButtonText = stringResource(id = R.string.open_settings),
            onDismiss = { showSettingsDialog = false }
        )
    }


    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyAndPolicyDialog by remember { mutableStateOf(false) }
    val baseUrl = BuildConfig.BASE_URL
    val termsUrl = "${baseUrl.trimEnd('/')}/terms-of-service"
    val privacyPolicyUrl = "${baseUrl.trimEnd('/')}/privacy-policy"
    Text(
        text = stringResource(id = R.string.preferences),
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.tertiary
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
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
                                    streakNotificationHelper.createNotificationChannel()
                                    dailyReminderNotificationHelper.createNotificationChannel()
                                }

                                else -> {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        } else {
                            onNotificationChange(true)
                            streakNotificationHelper.createNotificationChannel()
                            dailyReminderNotificationHelper.createNotificationChannel()
                        }
                    } else {
                        onNotificationChange(false)
                    }
                }
            )
            PreferenceItem(
                title = stringResource(id = R.string.language),
                onClick = {
                    val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                    context.startActivity(intent)
                }
            )

            PreferenceItem(
                title = stringResource(id = R.string.privacy_and_policy),
                onClick = {
                    showPrivacyAndPolicyDialog = true
                }
            )
            PreferenceItem(
                title = stringResource(id = R.string.terms_of_service),
                onClick = {
                    showTermsDialog = true
                }
            )
            LogoutItem(onClick = onShowLogoutDialog)
        }
        if (showTermsDialog) {
            TermsWebView(url = termsUrl, onDismiss = { showTermsDialog = false })
        }
        if (showPrivacyAndPolicyDialog) {
            TermsWebView(
                url = privacyPolicyUrl,
                onDismiss = { showPrivacyAndPolicyDialog = false }
            )
        }
    }
}

@Suppress("DEPRECATION")
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
            .clickable(
                enabled = onCheckedChange == null,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, modifier = Modifier.weight(1f))
                if (onCheckedChange != null) {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors = SwitchColors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.surface,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            checkedIconColor = MaterialTheme.colorScheme.onPrimary,

                            uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                            uncheckedBorderColor = MaterialTheme.colorScheme.tertiary,
                            uncheckedIconColor = MaterialTheme.colorScheme.onPrimary,

                            disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            disabledCheckedTrackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.6f
                            ),
                            disabledCheckedIconColor = MaterialTheme.colorScheme.onPrimary.copy(
                                alpha = 0.6f
                            ),

                            disabledUncheckedThumbColor = MaterialTheme.colorScheme.tertiary.copy(
                                alpha = 0.6f
                            ),
                            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.6f
                            ),
                            disabledUncheckedBorderColor = MaterialTheme.colorScheme.tertiary.copy(
                                alpha = 0.6f
                            ),
                            disabledUncheckedIconColor = MaterialTheme.colorScheme.onPrimary.copy(
                                alpha = 0.6f
                            )
                        )
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
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
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
