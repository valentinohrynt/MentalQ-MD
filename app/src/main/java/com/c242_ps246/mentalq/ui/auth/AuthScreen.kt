@file:Suppress("DEPRECATION")

package com.c242_ps246.mentalq.ui.auth

import android.app.Activity
import android.app.DatePickerDialog
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c242_ps246.mentalq.BuildConfig
import com.c242_ps246.mentalq.R
import com.c242_ps246.mentalq.ui.component.CustomToast
import com.c242_ps246.mentalq.ui.component.TermsWebView
import com.c242_ps246.mentalq.ui.component.ToastType
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.util.Calendar

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    onSuccess: (String) -> Unit
) {
    val viewModel: AuthViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()

    var isLogin by rememberSaveable { mutableStateOf(true) }
    var loginButtonLoadingState by rememberSaveable { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var birthday by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    var loginFailed = stringResource(R.string.login_failed)
    var loginSuccess = stringResource(R.string.login_success)
    var updatePasswordSuccess = stringResource(R.string.update_password_success)
    var registerSuccess = stringResource(R.string.register_success)

    LaunchedEffect(role) {
        if (role != null) {
            val authenticatedRole = role
            if (authenticatedRole != null) {
                onSuccess(authenticatedRole)
            }
        }
    }

    var showToast by rememberSaveable { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf("") }
    var toastType by rememberSaveable { mutableStateOf(ToastType.INFO) }
    var showForgotPassword by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val data = result.data
                if (data != null) {
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        if (task.isSuccessful) {
                            val account = task.result
                            viewModel.loginWithGoogle(account)
                        } else {
                            showToast = true
                            toastMessage = "Google Sign-In Failed: ${task.exception?.message}"
                        }
                    } catch (e: Exception) {
                        showToast = true
                        toastMessage = "Google Sign-In Failed: ${e.message}"
                    }
                } else {
                    showToast = true
                    toastMessage = "No data returned from Google Sign-In"
                }
            }

            Activity.RESULT_CANCELED -> {
                showToast = true
                toastMessage = "Google Sign-In Cancelled"
            }

            else -> {
                showToast = true
                toastMessage = "Unexpected error during Google Sign-In"
            }
        }
    }

    LaunchedEffect(uiState, showForgotPassword) {
        if (!showForgotPassword) when {
            uiState.error != null -> {
                showToast = true
                toastMessage = uiState.error ?: loginFailed
                toastType = ToastType.ERROR
                loginButtonLoadingState = false
                viewModel.clearError()
            }

            uiState.success -> {
                showToast = true
                toastMessage =
                    if (isLogin) loginSuccess else if (isRegister) registerSuccess else updatePasswordSuccess
                toastType = ToastType.SUCCESS
                loginButtonLoadingState = false
                if (isLogin) {
                    val authenticatedRole = role
                    if (authenticatedRole != null) {
                        onSuccess(authenticatedRole)
                    }
                }
                viewModel.clearSuccess()
            }
        }
    }

    var emailError by rememberSaveable { mutableStateOf<Int?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<Int?>(null) }
    var nameError by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthdayError by rememberSaveable { mutableStateOf<Int?>(null) }
    var termsError by rememberSaveable { mutableStateOf<Int?>(null) }

    fun validateTerms(acceptedTerms: Boolean): Boolean {
        return if (!acceptedTerms) {
            termsError = R.string.error_terms_not_accepted
            false
        } else {
            termsError = null
            true
        }
    }

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

    fun validatePassword(password: String, isRegister: Boolean): Boolean {
        return when {
            password.isEmpty() -> {
                passwordError = R.string.error_password_empty
                false
            }

            password.length < 8 -> {
                passwordError = R.string.error_password_length
                false
            }

            isRegister && !password.any { it.isUpperCase() } -> {
                passwordError = R.string.error_password_uppercase
                false
            }

            isRegister && !password.any { it.isDigit() } -> {
                passwordError = R.string.error_password_digit
                false
            }

            else -> {
                passwordError = null
                true
            }
        }
    }

    fun validateForm(): Boolean {
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password, isRegister)
        val isNameValid = if (!isLogin) validateName(name) else true
        val isBirthdayValid = if (!isLogin) validateBirthday(birthday) else true
        val isTermsValid = if (!isLogin) validateTerms(acceptedTerms) else true
        return isEmailValid && isPasswordValid && isNameValid && isBirthdayValid && isTermsValid
    }

    if (showForgotPassword) {
        ForgotPasswordFlow(
            onBack = {
                showForgotPassword = false
                isLogin = true
                isRegister = false
            },
            viewModel = viewModel
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mentalq),
                        contentDescription = "MentalQ Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 8.dp)
                    )
                    AnimatedContent(
                        targetState = isLogin,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() with
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = ""
                    ) { isLoginState ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(
                                    if (isLoginState) R.string.welcome_back
                                    else R.string.create_account
                                ),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    if (isLoginState) R.string.login_subtitle
                                    else R.string.register_subtitle
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isRegister,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (nameError != null) validateName(it)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = { Text(stringResource(R.string.label_name)) },
                                isError = nameError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            nameError?.let {
                                Text(
                                    text = stringResource(it),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isRegister,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            DateInputField(
                                birthdayDate = birthday,
                                onDateChange = { newDate ->
                                    birthday = newDate
                                }
                            )
                            birthdayError?.let {
                                Text(
                                    text = stringResource(it),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    Column {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (emailError != null) validateEmail(it)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = { Text(stringResource(R.string.label_email)) },
                            isError = emailError != null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        emailError?.let {
                            Text(
                                text = stringResource(it),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (passwordError != null) validatePassword(it, isRegister)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword)
                                            Icons.Default.Visibility
                                        else
                                            Icons.Default.VisibilityOff,
                                        contentDescription = if (showPassword)
                                            stringResource(R.string.hide_password)
                                        else
                                            stringResource(R.string.show_password),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            label = { Text(stringResource(R.string.label_password)) },
                            visualTransformation = if (showPassword)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            isError = passwordError != null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        passwordError?.let {
                            Text(
                                text = stringResource(it),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isLogin,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        TextButton(
                            onClick = {
                                isLogin = false
                                isRegister = false
                                email = ""
                                password = ""
                                showForgotPassword = true
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(R.string.forgot_password),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            loginButtonLoadingState = true
                            keyboardController?.hide()
                            if (validateForm()) {
                                if (isLogin) {
                                    viewModel.login(email, password)
                                } else {
                                    viewModel.register(name, email, password, birthday)
                                }
                            } else {
                                loginButtonLoadingState = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading && loginButtonLoadingState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    if (isLogin && !isRegister) R.string.button_login
                                    else R.string.button_register
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isLogin,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    loginButtonLoadingState = false
                                    if (clientId.isBlank()) {
                                        showToast = true
                                        toastType = ToastType.ERROR
                                        toastMessage = "Google Sign-In is not configured."
                                        return@Button
                                    }
                                    try {
                                        val gso =
                                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(clientId)
                                                .requestEmail()
                                                .requestProfile()
                                                .build()

                                        val googleSignInClient =
                                            GoogleSignIn.getClient(context, gso)

                                        val signInIntent = googleSignInClient.signInIntent
                                        googleSignInLauncher.launch(signInIntent)
                                    } catch (e: Exception) {
                                        showToast = true
                                        toastMessage =
                                            "Error preparing Google Sign-In: ${e.message}"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading && !loginButtonLoadingState) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.google_icon),
                                        contentDescription = stringResource(R.string.sign_in_with_google),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.sign_in_with_google),
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    }
                    if (showToast) {
                        CustomToast(
                            message = toastMessage,
                            type = toastType,
                            onDismiss = { showToast = false }
                        )
                    }

                    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
                    val baseUrl = BuildConfig.BASE_URL
                    val termsUrl = "$baseUrl/terms-of-service"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = {
                                acceptedTerms = it
                                if (it) termsError = null
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.agree_to_terms_prefix),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { showTermsDialog = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.terms_of_service),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            termsError?.let {
                                Text(
                                    text = stringResource(it),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }
                        }
                        if (showTermsDialog) {
                            TermsWebView(
                                url = termsUrl,
                                onDismiss = { showTermsDialog = false }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                if (isLogin && !isRegister) R.string.text_no_account_prefix
                                else R.string.text_have_account_prefix
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                isLogin = !isLogin
                                isRegister = !isRegister
                                emailError = null
                                passwordError = null
                                nameError = null
                                name = ""
                                email = ""
                                password = ""
                                birthday = ""
                                acceptedTerms = false
                                termsError = null
                            }
                        ) {
                            Text(
                                text = stringResource(
                                    if (isLogin && !isRegister) R.string.text_no_account_action
                                    else R.string.text_have_account_action
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateInputField(
    birthdayDate: String,
    onDateChange: (String) -> Unit,
    minimumAge: Int = 18,
    onValidationError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val datePickerDialog = remember { mutableStateOf<DatePickerDialog?>(null) }

    fun isAgeValid(selectedDate: Calendar): Boolean {
        val today = Calendar.getInstance()
        val age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
            return (age - 1) >= minimumAge
        }
        return age >= minimumAge
    }

    val openDatePicker = {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                if (isAgeValid(selectedCalendar)) {
                    val selectedDate = "$dayOfMonth/${month + 1}/$year"
                    onDateChange(selectedDate)
                } else {
                    onValidationError("Must be at least $minimumAge years old")
                }
            },
            calendar.get(Calendar.YEAR) - minimumAge,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()

        datePickerDialog.value = datePicker
        datePicker.show()
    }

    OutlinedTextField(
        value = birthdayDate,
        onValueChange = { },
        label = {
            Text(
                text = stringResource(R.string.label_birthday),
            )
        },
        readOnly = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Pick a date",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            IconButton(onClick = openDatePicker) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Pick a date"
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            errorBorderColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForgotPasswordFlow(
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    var currentStep by rememberSaveable { mutableStateOf(ForgotPasswordStep.EMAIL) }
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showToast by rememberSaveable { mutableStateOf(false) }
    var toastMessage by rememberSaveable { mutableStateOf("") }
    var toastType by rememberSaveable { mutableStateOf(ToastType.INFO) }

    var errorOccurred = stringResource(R.string.error_occurred)
    var passwordResetSuccess = stringResource(R.string.update_password_success)
    var passwordsNotMatch = stringResource(R.string.error_password_match)

    LaunchedEffect(uiState) {
        when {
            uiState.error != null -> {
                showToast = true
                toastMessage = uiState.error ?: errorOccurred
                toastType = ToastType.ERROR
                viewModel.clearError()
            }

            uiState.success -> {
                when (currentStep) {
                    ForgotPasswordStep.EMAIL -> {
                        currentStep = ForgotPasswordStep.OTP
                    }

                    ForgotPasswordStep.OTP -> {
                        currentStep = ForgotPasswordStep.NEW_PASSWORD
                    }

                    ForgotPasswordStep.NEW_PASSWORD -> {
                        showToast = true
                        toastMessage = passwordResetSuccess
                        toastType = ToastType.SUCCESS
                        onBack()
                    }
                }
                viewModel.clearSuccess()
            }
        }
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = ""
            ) { step ->
                when (step) {
                    ForgotPasswordStep.EMAIL -> {
                        EmailStep(
                            email = email,
                            onEmailChange = { email = it },
                            onSubmit = {
                                viewModel.requestResetPassword(email)
                            },
                            isLoading = uiState.isLoading
                        )
                    }

                    ForgotPasswordStep.OTP -> {
                        OTPStep(
                            otp = otp,
                            onOtpChange = { otp = it },
                            onSubmit = {
                                viewModel.verifyOTP(email, otp)
                            },
                            isLoading = uiState.isLoading
                        )
                    }

                    ForgotPasswordStep.NEW_PASSWORD -> {
                        NewPasswordStep(
                            newPassword = newPassword,
                            confirmPassword = confirmPassword,
                            showNewPassword = showNewPassword,
                            showConfirmPassword = showConfirmPassword,
                            onNewPasswordChange = { newPassword = it },
                            onConfirmPasswordChange = { confirmPassword = it },
                            onToggleNewPassword = { showNewPassword = !showNewPassword },
                            onToggleConfirmPassword = {
                                showConfirmPassword = !showConfirmPassword
                            },
                            onSubmit = {
                                if (newPassword == confirmPassword) {
                                    viewModel.resetPassword(email, otp, newPassword)
                                } else {
                                    showToast = true
                                    toastMessage = passwordsNotMatch
                                    toastType = ToastType.ERROR
                                }
                            },
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }

            if (showToast) {
                CustomToast(
                    message = toastMessage,
                    type = toastType,
                    onDismiss = { showToast = false }
                )
            }
        }
    }
}

@Composable
private fun EmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    var forgotPasswordTitle = stringResource(R.string.forgot_password_title)
    var forgotPasswordSubtitle = stringResource(R.string.forgot_password_subtitle)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = forgotPasswordTitle,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = forgotPasswordSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(id = R.string.label_email)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.send_verification_code))
            }
        }
    }
}

@Composable
private fun OTPStep(
    otp: String,
    onOtpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    var otpStepTitle = stringResource(R.string.otp_step_title)
    var otpStepSubtitle = stringResource(R.string.otp_step_subtitle)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = otpStepTitle,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = otpStepSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = otp,
            onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                    onOtpChange(it)
                }
            },
            label = { Text(stringResource(id = R.string.verification_code)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = otp.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.verify_code))
            }
        }
    }
}

@Composable
private fun NewPasswordStep(
    newPassword: String,
    confirmPassword: String,
    showNewPassword: Boolean,
    showConfirmPassword: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleNewPassword: () -> Unit,
    onToggleConfirmPassword: () -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    var passwordError by rememberSaveable { mutableStateOf<Int?>(null) }
    var newPasswordStepTitle = stringResource(R.string.new_password_step_title)
    var newPasswordStepSubtitle = stringResource(R.string.new_password_step_subtitle)

    fun validatePassword(password: String): Boolean {
        return if (password.isEmpty()) {
            passwordError = R.string.error_password_empty
            false
        } else if (password.length < 8) {
            passwordError = R.string.error_password_length
            false
        } else if (password != confirmPassword) {
            passwordError = R.string.error_password_match
            false
        } else {
            passwordError = null
            true
        }
    }

    fun validateForm(): Boolean {
        val isNewPasswordValid = validatePassword(newPassword)
        val isConfirmPasswordValid = validatePassword(confirmPassword)
        return isNewPasswordValid && isConfirmPasswordValid
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = newPasswordStepTitle,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = newPasswordStepSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Column {
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text(stringResource(R.string.new_password)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onToggleNewPassword) {
                        Icon(
                            imageVector = if (showNewPassword)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (showNewPassword)
                                "Hide password"
                            else
                                "Show password"
                        )
                    }
                },
                visualTransformation = if (showNewPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            passwordError?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text(stringResource(R.string.confirm_password)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onToggleConfirmPassword) {
                        Icon(
                            imageVector = if (showConfirmPassword)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword)
                                "Hide password"
                            else
                                "Show password"
                        )
                    }
                },
                visualTransformation = if (showConfirmPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            passwordError?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Button(
            onClick = {
                if (validateForm()) {
                    onSubmit()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.reset_password))
            }
        }
    }
}
