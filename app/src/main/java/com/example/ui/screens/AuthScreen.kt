package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.local.entities.InstitutionEntity
import com.example.data.model.UserRole
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassDropdown
import com.example.ui.components.LiquidGlassSecondaryButton
import com.example.ui.components.LiquidGlassTextField
import com.example.ui.components.LiquidNavyGradient
import com.example.ui.components.LiquidWarmGradient
import com.example.ui.components.RegistrationLookupBottomSheet
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: SchoolViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val authStage by viewModel.authStage.collectAsState()
    val verifiedInstitution by viewModel.verifiedInstitution.collectAsState()
    val selectedBatch by viewModel.selectedAcademicBatch.collectAsState()
    val allInstitutions by viewModel.allInstitutions.collectAsState()

    var showLookupSheet by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Stage 1 Fields
    var registrationCodeInput by remember { mutableStateOf(if (BuildConfig.DEBUG) "3477" else "") }
    var isLookupLoading by remember { mutableStateOf(false) }
    var lookupErrorMessage by remember { mutableStateOf<String?>(null) }

    // Stage 2 Fields
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoginLoading by remember { mutableStateOf(false) }
    var loginErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEDF2F7),
                        Color(0xFFE2E8F0)
                    )
                )
            )
            .testTag("auth_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Institutional Crest & Header
            InstitutionalWelcomeHeader()

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = authStage,
                transitionSpec = {
                    (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                },
                label = "AuthStageAnimation"
            ) { stage ->
                when (stage) {
                    SchoolViewModel.AuthStage.REGISTRATION_LOOKUP -> {
                        RegistrationLookupStage(
                            registrationCode = registrationCodeInput,
                            onCodeChange = {
                                registrationCodeInput = it
                                lookupErrorMessage = null
                            },
                            isLoading = isLookupLoading,
                            errorMessage = lookupErrorMessage,
                            onContinue = {
                                isLookupLoading = true
                                lookupErrorMessage = null
                                viewModel.lookupRegistrationCode(registrationCodeInput) { success, message ->
                                    isLookupLoading = false
                                    if (!success) {
                                        lookupErrorMessage = message
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenSchoolList = {
                                showLookupSheet = true
                            }
                        )
                    }

                    SchoolViewModel.AuthStage.CREDENTIALS_LOGIN,
                    SchoolViewModel.AuthStage.AUTHENTICATED -> {
                        val inst = verifiedInstitution ?: allInstitutions.firstOrNull()
                        CredentialsLoginStage(
                            institution = inst,
                            selectedBatch = selectedBatch,
                            onBatchChange = { viewModel.selectAcademicBatch(it) },
                            username = usernameInput,
                            onUsernameChange = {
                                usernameInput = it
                                loginErrorMessage = null
                            },
                            password = passwordInput,
                            onPasswordChange = {
                                passwordInput = it
                                loginErrorMessage = null
                            },
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            isLoading = isLoginLoading,
                            errorMessage = loginErrorMessage,
                            onLogin = {
                                isLoginLoading = true
                                loginErrorMessage = null
                                viewModel.loginWithCredentials(usernameInput, passwordInput) { success, message ->
                                    isLoginLoading = false
                                    if (success) {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        loginErrorMessage = message
                                    }
                                }
                            },
                            onRegister = { name, email, pass ->
                                isLoginLoading = true
                                loginErrorMessage = null
                                viewModel.registerUser(email, pass, name) { success, message ->
                                    isLoginLoading = false
                                    if (success) {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        loginErrorMessage = message
                                    }
                                }
                            },
                            onChangeSchool = {
                                viewModel.returnToRegistrationLookup()
                            },
                            onForgotPassword = {
                                showForgotPasswordDialog = true
                            },
                            onQuickSelectRole = { user, pass ->
                                usernameInput = user
                                passwordInput = pass
                                loginErrorMessage = null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Institutional Pre-Seeded Default Test Accounts (1-Tap Login & Quick Fill)
            DefaultCredentialsTestingPanel(
                onAutoFill = { schoolCode, user, pass ->
                    val inst = allInstitutions.find { it.registrationCode == schoolCode }
                        ?: allInstitutions.firstOrNull()
                    if (inst != null) {
                        viewModel.selectInstitution(inst)
                    }
                    usernameInput = user
                    passwordInput = pass
                    loginErrorMessage = null
                    Toast.makeText(context, "Filled credentials for $user", Toast.LENGTH_SHORT).show()
                },
                onDirectLogin = { schoolCode, user, pass ->
                    isLoginLoading = true
                    loginErrorMessage = null
                    val inst = allInstitutions.find { it.registrationCode == schoolCode }
                        ?: allInstitutions.firstOrNull()
                    if (inst != null) {
                        viewModel.selectInstitution(inst)
                    }
                    usernameInput = user
                    passwordInput = pass
                    viewModel.loginWithCredentials(user, pass) { success, message ->
                        isLoginLoading = false
                        if (success) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            loginErrorMessage = message
                        }
                    }
                },
                isActionLoading = isLoginLoading
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Institutional Compliance Footer
            InstitutionalFooter()
        }

        // Bottom Sheet for Browsing Registered Schools (matching Screenshot 3)
        if (showLookupSheet) {
            RegistrationLookupBottomSheet(
                institutions = allInstitutions,
                onSelectInstitution = { selectedInst ->
                    viewModel.selectInstitution(selectedInst)
                    registrationCodeInput = selectedInst.registrationCode
                    showLookupSheet = false
                },
                onDismiss = { showLookupSheet = false }
            )
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                viewModel = viewModel,
                onDismiss = { showForgotPasswordDialog = false }
            )
        }
    }
}

// =========================================================================
// INSTITUTIONAL HEADER & BRANDING
// =========================================================================

@Composable
fun InstitutionalWelcomeHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // School Emblem Emblem Icon with Soft Specular Shadow
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape, spotColor = Color(0x330B2545))
                .clip(CircleShape)
                .background(LiquidNavyGradient)
                .border(2.dp, Color(0x66FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "School Emblem",
                tint = PureWhite,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "ST. XAVIER'S INTERNATIONAL ACADEMY",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GovNavyPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "SECURE MULTI-ROLE CAMPUS CLOUD PORTAL",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = GovSlateMuted,
            textAlign = TextAlign.Center,
            letterSpacing = 0.4.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Security Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEFF6FF))
                .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Encrypted",
                tint = GovNavyPrimary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "256-Bit Encrypted Session Registry",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GovNavyPrimary
            )
        }
    }
}

// =========================================================================
// STAGE 1: REGISTRATION NUMBER / SCHOOL CODE LOOKUP (SCREENSHOT 2 & 3)
// =========================================================================

@Composable
fun RegistrationLookupStage(
    registrationCode: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onContinue: () -> Unit,
    onOpenSchoolList: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderWidth = 1.2.dp,
        borderColor = Color(0xFFE2E8F0),
        elevation = 6.dp,
        testTag = "registration_lookup_card"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CorporateFare,
                        contentDescription = "Institution",
                        tint = GovNavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "School Registration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary
                    )
                    Text(
                        text = "Enter your school code to connect",
                        fontSize = 12.sp,
                        color = GovSlateMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Text Input for Registration Code
            LiquidGlassTextField(
                value = registrationCode,
                onValueChange = onCodeChange,
                placeholder = "Enter Registration No. (e.g. 3477)",
                label = "Registration Number / School Code",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GovSlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onContinue() }
                ),
                isError = errorMessage != null,
                errorMessage = errorMessage,
                testTag = "registration_code_input"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { 
                        onCodeChange("3477")
                        onContinue()
                    }
                    .testTag("quick_connect_st_xaviers"),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Default School",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "St. Xavier's International Academy",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Text(
                                text = "School Code: 3477 • Tap for 1-Tap Connect",
                                fontSize = 11.sp,
                                color = GovNavyPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        color = GovNavyPrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Connect",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Warm Gradient "Continue" Button (Screenshot 2)
            LiquidGlassButton(
                text = "Continue",
                onClick = onContinue,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
                testTag = "registration_continue_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Helper text matching Screenshot 2
            Text(
                text = "Don't have Registration Number? Please contact your school administrator for Registration Number and Username/Password.",
                fontSize = 11.5.sp,
                color = GovSlateMuted,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Button to Browse School List (Screenshot 3 trigger)
            LiquidGlassSecondaryButton(
                text = "Search / Browse Registered Schools",
                onClick = onOpenSchoolList,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Browse",
                        tint = GovNavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "browse_schools_button"
            )
        }
    }
}

// =========================================================================
// STAGE 2: CREDENTIALS LOGIN WITH VERIFIED SCHOOL (SCREENSHOT 4)
// =========================================================================

@Composable
fun CredentialsLoginStage(
    institution: InstitutionEntity?,
    selectedBatch: String,
    onBatchChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: () -> Unit,
    onRegister: ((String, String, String) -> Unit)? = null,
    onChangeSchool: () -> Unit,
    onForgotPassword: () -> Unit,
    onQuickSelectRole: (String, String) -> Unit
) {
    val batches = remember(institution) {
        institution?.availableBatches?.split(",")?.map { it.trim() }
            ?: listOf("2026-27", "2025-26", "2024-25")
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        borderWidth = 1.2.dp,
        borderColor = Color(0xFFE2E8F0),
        elevation = 6.dp,
        testTag = "credentials_login_card"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Verified Institution Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified School",
                            tint = GovGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = institution?.name ?: "Verified Institution",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Reg: ${institution?.registrationCode ?: "3477"} • ${institution?.cityCampus ?: "Main Campus"}",
                                fontSize = 11.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    TextButton(
                        onClick = onChangeSchool,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Authentication Mode Toggle: Login vs Register Student Account
            var isRegisterMode by remember { mutableStateOf(false) }
            var registerFullName by remember { mutableStateOf("") }
            var registerEmail by remember { mutableStateOf("") }
            var registerPassword by remember { mutableStateOf("") }
            var registerConfirmPassword by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isRegisterMode = false },
                    color = if (!isRegisterMode) GovNavyPrimary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Sign In",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isRegisterMode) PureWhite else GovSlateDark
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isRegisterMode = true },
                    color = if (isRegisterMode) GovNavyPrimary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Register Student",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRegisterMode) PureWhite else GovSlateDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isRegisterMode) {
                // SIGN IN WORKFLOW
                LiquidGlassDropdown(
                    label = "Select organization",
                    selectedOption = institution?.name ?: "St. Xavier's International Academy",
                    options = listOf(institution?.name ?: "St. Xavier's International Academy"),
                    onOptionSelected = {},
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = "Org",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassDropdown(
                    label = "Select batch",
                    selectedOption = selectedBatch,
                    options = batches,
                    onOptionSelected = onBatchChange,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Batch",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "batch_selector_dropdown"
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = "Enter Email or Username",
                    label = "Enter Username / Email",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Username",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "username_input_field"
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "Enter Password",
                    label = "Enter Password",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = GovSlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onLogin() }
                    ),
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "password_input_field"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onForgotPassword,
                        modifier = Modifier.testTag("forgot_password_button")
                    ) {
                        Text(
                            text = "Forgot Password ?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GovNavyPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fast Role Switcher Pills
                Text(
                    text = "Select demo profile to fill credentials:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GovSlateMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val roles = listOf(
                        "👑 Principal" to "principal",
                        "🛡️ Admin" to "admin",
                        "📋 In-Charge 10-A" to "incharge_10a",
                        "👩‍🏫 Teacher" to "faculty_meenakshi",
                        "🎓 Student 1 (Aarav)" to "aarav_sharma",
                        "🎒 Student 2 (Ananya)" to "ananya_iyer",
                        "👨‍👩‍👧 Parent" to "vikram_sharma",
                        "💼 Accountant" to "accountant_mehta"
                    )
                    roles.forEach { (label, uName) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuickSelectRole(uName, "Password@123") },
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GovNavyDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassButton(
                    text = "Sign In to Campus",
                    onClick = onLogin,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_login_button"
                )
            } else {
                // REGISTRATION WORKFLOW - STRICT STUDENT SAFE DEFAULT ROLE
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Safe Default Role",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Default Safe Role: STUDENT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyPrimary
                            )
                            Text(
                                text = "All public account registrations receive the safe STUDENT role. Administrative and staff roles require trusted authorization.",
                                fontSize = 10.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = registerFullName,
                    onValueChange = { registerFullName = it },
                    placeholder = "Enter Full Name (e.g. Aarav Sharma)",
                    label = "Full Name",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Name",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "register_fullname_field"
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = registerEmail,
                    onValueChange = { registerEmail = it },
                    placeholder = "Enter Email Address (e.g. aarav@stxaviers.edu)",
                    label = "Email Address",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Email",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "register_email_field"
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = registerPassword,
                    onValueChange = { registerPassword = it },
                    placeholder = "Choose Password (min 6 characters)",
                    label = "Password",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "register_password_field"
                )

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassTextField(
                    value = registerConfirmPassword,
                    onValueChange = { registerConfirmPassword = it },
                    placeholder = "Confirm Password",
                    label = "Confirm Password",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm Password",
                            tint = GovSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "register_confirm_password_field"
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 11.sp,
                        color = GovRedAlert,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

            val currentContext = LocalContext.current
            LiquidGlassButton(
                text = "Create Student Account",
                onClick = {
                    if (registerPassword != registerConfirmPassword) {
                        android.widget.Toast.makeText(
                            currentContext,
                            "Passwords do not match",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onRegister?.invoke(registerFullName, registerEmail, registerPassword)
                    }
                },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "submit_register_button"
                )
            }
        }
    }
}

@Composable
fun DemoCredentialChip(
    label: String,
    user: String,
    pass: String,
    onSelect: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect(user, pass) },
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(GovNavyPrimary)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GovNavyDark
            )
        }
    }
}

// =========================================================================
// FORGOT PASSWORD DIALOG
// =========================================================================

@Composable
fun ForgotPasswordDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset Account Password",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GovNavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enter your registered Username or Phone Number and choose a new password.",
                    fontSize = 12.sp,
                    color = GovSlateMuted
                )

                LiquidGlassTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    placeholder = "Username or Registered Phone",
                    label = "Account Identifier"
                )

                LiquidGlassTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = "New Password (min 6 characters)",
                    label = "New Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                LiquidGlassTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Confirm New Password",
                    label = "Confirm Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                if (dialogError != null) {
                    Text(
                        text = dialogError ?: "",
                        color = GovRedAlert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            LiquidGlassButton(
                text = "Update Password",
                isLoading = isSubmitting,
                onClick = {
                    if (identifier.isBlank()) {
                        dialogError = "Please enter your username or phone."
                        return@LiquidGlassButton
                    }
                    if (newPassword.length < 6) {
                        dialogError = "Password must be at least 6 characters."
                        return@LiquidGlassButton
                    }
                    if (newPassword != confirmPassword) {
                        dialogError = "Passwords do not match."
                        return@LiquidGlassButton
                    }

                    isSubmitting = true
                    viewModel.resetUserPassword(identifier, newPassword) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            dialogError = msg
                        }
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GovSlateMuted)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(16.dp)
    )
}

// =========================================================================
// INSTITUTIONAL FOOTER
// =========================================================================

@Composable
fun InstitutionalFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = "Official National Academic Information System",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GovSlateMuted
        )
        Text(
            text = "Multi-Tenant Institutional Cloud Platform",
            fontSize = 10.sp,
            color = GovSlateMuted.copy(alpha = 0.8f)
        )
    }
}

// =========================================================================
// DEFAULT TESTING CREDENTIALS PANEL (TESTING ACCELERATOR)
// =========================================================================

data class TestAccountItem(
    val roleTitle: String,
    val roleTag: String,
    val username: String,
    val password: String,
    val scope: String,
    val color: Color
)

@Composable
fun DefaultCredentialsTestingPanel(
    onAutoFill: (schoolCode: String, username: String, password: String) -> Unit,
    onDirectLogin: (schoolCode: String, username: String, password: String) -> Unit,
    isActionLoading: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.2.dp, Color(0xFF93C5FD), RoundedCornerShape(16.dp))
            .testTag("default_credentials_panel"),
        color = Color(0xFFF8FAFC),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Test Mode",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Default Demo Accounts (1-Tap Login)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "Ready to Test",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "School: 3477 • Password: Password@123",
                            fontSize = 11.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = GovNavyPrimary
                    )
                }
            }

            // Quick 1-Tap Login Horizontal Scroll Row
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap role pill to instant login (⚡) or view detailed cards below:",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = GovSlateMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickTestPill("👑 Principal", "3477", "principal", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("🛡️ Admin", "3477", "admin", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("📋 In-Charge 10-A", "3477", "incharge_10a", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("👩‍🏫 Teacher (Physics)", "3477", "faculty_meenakshi", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("🎓 Student 1 (Aarav)", "3477", "aarav_sharma", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("🎒 Student 2 (Ananya)", "3477", "ananya_iyer", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("👨‍👩‍👧 Parent", "3477", "vikram_sharma", "Password@123", onDirectLogin, onAutoFill)
                QuickTestPill("💼 Accountant", "3477", "accountant_mehta", "Password@123", onDirectLogin, onAutoFill)
            }

            // Detailed Accounts Expansion
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Pre-seeded Role Accounts Matrix:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val testAccounts = listOf(
                        TestAccountItem(
                            roleTitle = "Prof. Robert Vance (Principal)",
                            roleTag = "PRINCIPAL / ADMIN",
                            username = "principal",
                            password = "Password@123",
                            scope = "Full Institutional Clearance, Approvals & Staff Oversight",
                            color = GovNavyPrimary
                        ),
                        TestAccountItem(
                            roleTitle = "Mrs. Sunita Kapoor (Class In-Charge)",
                            roleTag = "INCHARGE 10-A",
                            username = "incharge_10a",
                            password = "Password@123",
                            scope = "Class 10-A Daily Attendance, Verification & Results",
                            color = Color(0xFF0D9488)
                        ),
                        TestAccountItem(
                            roleTitle = "Dr. Meenakshi Sundaram (Faculty)",
                            roleTag = "TEACHING",
                            username = "faculty_meenakshi",
                            password = "Password@123",
                            scope = "Daily Class Roll-Call & Digital Diary",
                            color = Color(0xFF2563EB)
                        ),
                        TestAccountItem(
                            roleTitle = "Aarav Sharma (Scholar Roll #1)",
                            roleTag = "STUDENT 1",
                            username = "aarav_sharma",
                            password = "Password@123",
                            scope = "Digital ID Card, Attendance Turnout, Report Card",
                            color = Color(0xFF7C3AED)
                        ),
                        TestAccountItem(
                            roleTitle = "Ananya Iyer (Scholar Roll #2)",
                            roleTag = "STUDENT 2",
                            username = "ananya_iyer",
                            password = "Password@123",
                            scope = "Digital ID Card, Grade 10-A Attendance, Fees",
                            color = Color(0xFF9333EA)
                        ),
                        TestAccountItem(
                            roleTitle = "Vikram Sharma (Guardian)",
                            roleTag = "PARENT",
                            username = "vikram_sharma",
                            password = "Password@123",
                            scope = "Ward Progress, Attendance, Fee Receipts & Circulars",
                            color = Color(0xFFD97706)
                        ),
                        TestAccountItem(
                            roleTitle = "Rajesh Mehta (Senior Accounts Officer)",
                            roleTag = "ACCOUNTANT",
                            username = "accountant_mehta",
                            password = "Password@123",
                            scope = "Institutional Collections, Fee Ledger & Challans",
                            color = Color(0xFF475569)
                        )
                    )

                    testAccounts.forEach { acc ->
                        DetailedAccountRow(
                            account = acc,
                            onAutoFill = { onAutoFill("3477", acc.username, acc.password) },
                            onDirectLogin = { onDirectLogin("3477", acc.username, acc.password) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTestPill(
    label: String,
    schoolCode: String,
    user: String,
    pass: String,
    onDirectLogin: (String, String, String) -> Unit,
    onAutoFill: (String, String, String) -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onDirectLogin(schoolCode, user, pass) },
        color = Color(0xFFEFF6FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GovNavyDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Test Login",
                tint = GovNavyPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun DetailedAccountRow(
    account: TestAccountItem,
    onAutoFill: () -> Unit,
    onDirectLogin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.roleTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = account.color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = account.roleTag,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = account.color,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "User: ${account.username} • Pass: ${account.password}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = GovNavyPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = account.scope,
                    fontSize = 10.sp,
                    color = GovSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAutoFill() },
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "Fill",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onDirectLogin() },
                    color = GovNavyPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Login",
                            tint = PureWhite,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Login",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}
