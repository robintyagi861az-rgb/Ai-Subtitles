package com.example.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleGeneratorApp(
    viewModel: SubtitleViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFEF7FF), // Bento background
            Color(0xFFF3EDF7), // Lavender surface
            Color(0xFFECE6F0)  // Soft lilac
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.LOGIN -> LoginScreen(
                    viewModel = viewModel,
                    authState = authState
                )
                AppScreen.REGISTER -> RegisterScreen(
                    viewModel = viewModel,
                    authState = authState
                )
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    currentUser = currentUser
                )
                AppScreen.PROJECT_DETAIL -> ProjectDetailScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

// --- LOGIN SCREEN ---
@Composable
fun LoginScreen(
    viewModel: SubtitleViewModel,
    authState: AuthState
) {
    val websiteName by viewModel.websiteNameField.collectAsStateWithLifecycle()
    val websiteLogo by viewModel.websiteLogoField.collectAsStateWithLifecycle()
    val websiteLocked by viewModel.websiteLockedField.collectAsStateWithLifecycle()
    val googleClientId by viewModel.googleClientIdField.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Google OAuth simulation Dialog
    var showGoogleOAuthDialog by remember { mutableStateOf(false) }
    var googleEmail by remember { mutableStateOf("") }
    var googleName by remember { mutableStateOf("") }

    // Config error dialog
    var showOAuthErrorDialog by remember { mutableStateOf(false) }

    // Forgot Password Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var otpEntered by remember { mutableStateOf("") }
    var newResetPassword by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf("") }
    var resetSuccess by remember { mutableStateOf(false) }
    val verificationOtpValue by viewModel.verificationOtp.collectAsStateWithLifecycle()

    val logoVector = when (websiteLogo) {
        "Home" -> Icons.Default.Home
        "Settings" -> Icons.Default.Settings
        "Info" -> Icons.Default.Info
        "Lock" -> Icons.Default.Lock
        "Share" -> Icons.Default.Share
        else -> Icons.Default.PlayArrow
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Website Locked Banner
                if (websiteLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFDE8E8))
                            .border(1.dp, Color(0xFFE53E3E), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFE53E3E))
                            Text(
                                text = "Website Locked: Only Administrator access is allowed.",
                                color = Color(0xFFC53030),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Animated Glowing Header Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF9D4EDD), Color(0x009D4EDD))
                            )
                        )
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF6750A4).copy(alpha = 0.15f),
                                radius = size.minDimension / 1.5f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = logoVector,
                        contentDescription = "Logo",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = websiteName,
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20),
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Generate Viral Captions in Seconds",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF49454F)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = authState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", color = Color(0xFF6750A4)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = Color(0xFF6750A4)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xFF6750A4)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFF6750A4)) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = Color(0xFF6750A4),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text("Forgot Password?", color = Color(0xFF6750A4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.login(username, password)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Secure Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (googleClientId.trim().isEmpty()) {
                            showOAuthErrorDialog = true
                        } else {
                            showGoogleOAuthDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Google",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { viewModel.navigateTo(AppScreen.REGISTER) }) {
                    Text(
                        text = "New around here? Create an Account",
                        color = Color(0xFF6750A4),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showGoogleOAuthDialog) {
        Dialog(onDismissRequest = { showGoogleOAuthDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Google OAuth Authentication", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text("Secure verification handled via Client ID: $googleClientId", fontSize = 12.sp, color = Color(0xFF49454F))

                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Google Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = googleName,
                        onValueChange = { googleName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showGoogleOAuthDialog = false }) {
                            Text("Cancel", color = Color(0xFF49454F))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (googleEmail.isNotBlank()) {
                                    viewModel.loginWithGoogle(googleEmail, googleName)
                                    showGoogleOAuthDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("Confirm Sign In", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showOAuthErrorDialog) {
        Dialog(onDismissRequest = { showOAuthErrorDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFF4D4D)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("OAuth Configuration Required", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D4D))
                    Text(
                        text = "Google OAuth login is currently disabled because the System Administrator has not configured a Google OAuth Client ID yet.\n\nPlease log in via credentials or ask the administrator to input the client ID in the Admin Panel settings.",
                        color = Color(0xFF1D1B20),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Button(
                        onClick = { showOAuthErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Acknowledge", color = Color.White)
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Reset Your Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))

                    if (!otpSent) {
                        Text("Enter your registered email below to receive a secure OTP password verification code.", fontSize = 13.sp, color = Color(0xFF49454F))

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (resetMessage.isNotEmpty()) {
                            Text(resetMessage, color = Color(0xFFFF4D4D), fontSize = 12.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showForgotPasswordDialog = false }) {
                                Text("Cancel", color = Color(0xFF49454F))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (resetEmail.isNotBlank()) {
                                        viewModel.sendVerificationOtp(
                                            email = resetEmail,
                                            onSuccess = {
                                                otpSent = true
                                                resetMessage = ""
                                            },
                                            onError = {
                                                resetMessage = it
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                            ) {
                                Text("Send Code", color = Color.White)
                            }
                        }
                    } else {
                        if (viewModel.smtpUserField.value.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF9E6))
                                    .border(1.dp, Color(0xFFFFB703), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "💡 Local Mode: SMTP server is not configured in Admin Settings. Your generated verification OTP is: $verificationOtpValue",
                                    color = Color(0xFF806000),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Text("Enter the 6-digit verification code sent to your email and your new password.", fontSize = 13.sp, color = Color(0xFF49454F))

                        OutlinedTextField(
                            value = otpEntered,
                            onValueChange = { otpEntered = it },
                            label = { Text("6-Digit OTP Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newResetPassword,
                            onValueChange = { newResetPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (resetMessage.isNotEmpty()) {
                            Text(resetMessage, color = if (resetSuccess) Color(0xFF6750A4) else Color(0xFFFF4D4D), fontSize = 12.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                otpSent = false
                                resetMessage = ""
                            }) {
                                Text("Back", color = Color(0xFF49454F))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (otpEntered.isNotBlank() && newResetPassword.isNotBlank()) {
                                        viewModel.verifyOtpAndResetPassword(
                                            email = resetEmail,
                                            otp = otpEntered,
                                            newPasswordHash = newResetPassword,
                                            onSuccess = {
                                                resetSuccess = true
                                                resetMessage = "Password reset successfully! You can now log in."
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    showForgotPasswordDialog = false
                                                    otpSent = false
                                                    resetMessage = ""
                                                    resetEmail = ""
                                                    otpEntered = ""
                                                    newResetPassword = ""
                                                }
                                            },
                                            onError = {
                                                resetMessage = it
                                            }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                            ) {
                                Text("Reset Password", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- REGISTER SCREEN ---
@Composable
fun RegisterScreen(
    viewModel: SubtitleViewModel,
    authState: AuthState
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Register Account",
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20)
                    )
                )

                Text(
                    text = "Create a new account to get started",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF49454F)
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (authState is AuthState.Error) {
                    Text(
                        text = authState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", color = Color(0xFF6750A4)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = Color(0xFF6750A4)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color(0xFF6750A4)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF6750A4)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xFF6750A4)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedLabelColor = Color(0xFF6750A4),
                        unfocusedLabelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFF6750A4)) },
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = Color(0xFF6750A4),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.register(username, password, email, role) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Create Secured Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { viewModel.navigateTo(AppScreen.LOGIN) }) {
                    Text(
                        text = "Already have an account? Login",
                        color = Color(0xFF6750A4),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// --- DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SubtitleViewModel,
    currentUser: User?
) {
    val isAdmin = currentUser?.role == "ADMIN"
    var selectedTab by remember { mutableIntStateOf(0) }

    val websiteName by viewModel.websiteNameField.collectAsStateWithLifecycle()
    val websiteLogo by viewModel.websiteLogoField.collectAsStateWithLifecycle()

    val logoVector = when (websiteLogo) {
        "Home" -> Icons.Default.Home
        "Settings" -> Icons.Default.Settings
        "Info" -> Icons.Default.Info
        "Lock" -> Icons.Default.Lock
        "Share" -> Icons.Default.Share
        else -> Icons.Default.PlayArrow
    }

    val tabs = mutableListOf("Transcribe Video", "My Projects")
    if (isAdmin) {
        tabs.add("Admin Settings")
        tabs.add("Admin Logs")
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = logoVector,
                            contentDescription = "Logo",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = websiteName,
                            color = Color(0xFF1D1B20),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Role Badge
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAdmin) Color(0xFFFFD8E4)
                                else Color(0xFFE8DEF8)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentUser?.role ?: "GUEST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdmin) Color(0xFF31111D) else Color(0xFF6750A4)
                        )
                    }

                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color(0xFF49454F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val icon = when (index) {
                        0 -> Icons.Default.Add
                        1 -> Icons.Default.PlayArrow
                        2 -> Icons.Default.Settings
                        3 -> Icons.Default.List
                        else -> Icons.Default.Menu
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(icon, contentDescription = title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFE8DEF8)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> TranscribeTab(viewModel = viewModel)
                1 -> ProjectsTab(viewModel = viewModel)
                2 -> if (isAdmin) AdminSettingsTab(viewModel = viewModel) else ProjectsTab(viewModel = viewModel)
                3 -> if (isAdmin) AdminLogsTab(viewModel = viewModel) else ProjectsTab(viewModel = viewModel)
            }
        }
    }
}

// --- TAB 1: TRANSCRIBE VIDEO ---
@Composable
fun TranscribeTab(viewModel: SubtitleViewModel) {
    val context = LocalContext.current
    val transcribeState by viewModel.transcribeState.collectAsStateWithLifecycle()

    var videoName by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoDuration by remember { mutableLongStateOf(0L) }
    var videoTitle by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedPreset by remember { mutableStateOf("mrbeast") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoUri = it
            videoDuration = getVideoDuration(context, it)
            videoName = getFileName(context, it)
            if (videoTitle.isBlank()) {
                videoTitle = videoName.substringBeforeLast(".")
            }
        }
    }

    val languages = listOf("English", "Spanish", "French", "German", "Hindi", "Japanese", "Chinese", "Portuguese", "Auto")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Add Captions to Video",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
            )
            Text(
                text = "Upload any video under 25MB and generate precise visual subtitles styled in popular formats.",
                style = TextStyle(fontSize = 13.sp, color = Color(0xFF49454F)),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Upload Video Card
        item {
            Card(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFECE6F0)
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = Color(0xFFCAC4D0)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoUri == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF6750A4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Add Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Upload or Record Video",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20),
                                fontSize = 16.sp
                            )
                            Text(
                                "Tap to open system gallery",
                                color = Color(0xFF49454F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE8DEF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Video Loaded",
                                    tint = Color(0xFF6750A4),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = videoName,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Duration: ${formatDuration(videoDuration)}",
                                color = Color(0xFF6750A4),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            TextButton(
                                onClick = { videoPicker.launch("video/*") },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Replace Video", color = Color(0xFF6750A4), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (videoUri != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3EDF7)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Project title
                        OutlinedTextField(
                            value = videoTitle,
                            onValueChange = { videoTitle = it },
                            label = { Text("Project Title", color = Color(0xFF6750A4)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Target Language
                        Text("Target Translation Language", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(languages) { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF6750A4),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFFECE6F0),
                                        labelColor = Color(0xFF1D1B20)
                                    )
                                )
                            }
                        }

                        // Style presets title
                        Text("Select Visual Style (10 Viral Presets)", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        // Horizontal Preset previews
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CaptionPreset.list) { preset ->
                                val isSelected = selectedPreset == preset.id
                                Card(
                                    onClick = { selectedPreset = preset.id },
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(110.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFEF7FF)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = preset.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D1B20),
                                            textAlign = TextAlign.Center
                                        )

                                        // Render realistic tiny preview of the preset text!
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .background(preset.backgroundColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (preset.outlineWidth > 0) {
                                                    Text(
                                                        text = "POP",
                                                        color = preset.outlineColor,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontStyle = preset.fontStyle,
                                                        letterSpacing = preset.letterSpacing,
                                                        style = TextStyle(
                                                            drawStyle = Stroke(
                                                                width = preset.outlineWidth / 2f,
                                                                join = StrokeJoin.Round
                                                            )
                                                        )
                                                    )
                                                }
                                                Text(
                                                    text = "POP",
                                                    color = preset.textColor,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontStyle = preset.fontStyle,
                                                    letterSpacing = preset.letterSpacing
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isSelected) "Active" else "Select",
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.generateSubtitles(
                                    title = videoTitle,
                                    videoUri = videoUri!!,
                                    durationMs = videoDuration,
                                    targetLanguage = selectedLanguage,
                                    selectedPresetId = selectedPreset
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6750A4),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Sparkles", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Subtitles ✨", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal loader during processing state
    if (transcribeState is TranscribeState.Processing) {
        val step = (transcribeState as TranscribeState.Processing).step
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF6750A4),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Transcribing with AI Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = step,
                        fontSize = 13.sp,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// --- TAB 2: PROJECTS LIST ---
@Composable
fun ProjectsTab(viewModel: SubtitleViewModel) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Subtitle Projects",
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
        )
        Text(
            text = "Click to open interactive timeline, modify presets, or export SRT.",
            style = TextStyle(fontSize = 13.sp, color = Color(0xFF49454F)),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No projects yet", color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Your saved caption projects will appear here.", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(projects) { project ->
                    Card(
                        onClick = { viewModel.selectProject(project) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail with caption overlay preview
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFECE6F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Video", tint = Color(0xFF6750A4))
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(0.6f))
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = project.targetLanguage,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Preset: ${CaptionPreset.getById(project.selectedPresetId).name}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6750A4)
                                )
                                Text(
                                    text = "Duration: ${formatDuration(project.durationMs)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF49454F)
                                )
                            }

                            IconButton(onClick = { viewModel.deleteProject(project) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Color(0xFFFF4D4D))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: ADMIN SETTINGS ---
@Composable
fun AdminSettingsTab(viewModel: SubtitleViewModel) {
    val geminiKey by viewModel.geminiApiKeyField.collectAsStateWithLifecycle()
    val openaiKey by viewModel.openaiApiKeyField.collectAsStateWithLifecycle()
    val preferredAi by viewModel.preferredAiField.collectAsStateWithLifecycle()

    val websiteLocked by viewModel.websiteLockedField.collectAsStateWithLifecycle()
    val googleClientId by viewModel.googleClientIdField.collectAsStateWithLifecycle()
    val smtpHost by viewModel.smtpHostField.collectAsStateWithLifecycle()
    val smtpPort by viewModel.smtpPortField.collectAsStateWithLifecycle()
    val smtpUser by viewModel.smtpUserField.collectAsStateWithLifecycle()
    val smtpPass by viewModel.smtpPasswordField.collectAsStateWithLifecycle()
    val websiteName by viewModel.websiteNameField.collectAsStateWithLifecycle()
    val websiteLogo by viewModel.websiteLogoField.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var geminiKeyVisible by remember { mutableStateOf(false) }
    var openaiKeyVisible by remember { mutableStateOf(false) }
    var smtpPassVisible by remember { mutableStateOf(false) }

    // Admin Credentials Update State
    var currentAdminUsername by remember { mutableStateOf(currentUser?.username ?: "admin") }
    var newAdminUsername by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("") }
    var credentialsMessage by remember { mutableStateOf("") }
    var credentialsSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Admin Configuration Control",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
            )
            Text(
                text = "Manage global system security settings, dynamic branding, SMTP channels, and OAuth configurations.",
                style = TextStyle(fontSize = 13.sp, color = Color(0xFF49454F)),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        // --- SECTION: FIRESTORE SYNC STATUS ---
        item {
            val firebaseActive = com.example.data.FirestoreService.isAvailable()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (firebaseActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, if (firebaseActive) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (firebaseActive) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = "Cloud Status",
                        tint = if (firebaseActive) Color(0xFF2E7D32) else Color(0xFFE65100),
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (firebaseActive) "Cloud Sync Active" else "Cloud Sync Standby",
                            fontWeight = FontWeight.Bold,
                            color = if (firebaseActive) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (firebaseActive) {
                                "User profiles and generated subtitle project histories are automatically synced to Firebase Firestore."
                            } else {
                                "Firebase is in local-only standby mode. To enable cross-device cloud syncing, configure your 'google-services.json' in the app module."
                            },
                            fontSize = 11.sp,
                            color = if (firebaseActive) Color(0xFF1B5E20) else Color(0xFF5D4037)
                        )
                    }
                }
            }
        }

        // --- SECTION: WEBSITE BRANDING & LOCK CONTROLS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Branding & Lock Customization",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = websiteName,
                        onValueChange = { viewModel.saveConfig("website_name", it) },
                        label = { Text("Website Name", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Logo Icon Row Selector
                    Text("Select Website Header Logo Symbol:", fontSize = 12.sp, color = Color(0xFF49454F), fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val logosList = listOf("PlayArrow", "Home", "Settings", "Info", "Lock", "Share")
                        logosList.forEach { logoName ->
                            val logoIcon = when (logoName) {
                                "Home" -> Icons.Default.Home
                                "Settings" -> Icons.Default.Settings
                                "Info" -> Icons.Default.Info
                                "Lock" -> Icons.Default.Lock
                                "Share" -> Icons.Default.Share
                                else -> Icons.Default.PlayArrow
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (websiteLogo == logoName) Color(0xFFECE6F0) else Color.Transparent)
                                    .clickable { viewModel.saveConfig("website_logo", logoName) }
                                    .border(
                                        1.dp,
                                        if (websiteLogo == logoName) Color(0xFF6750A4) else Color(0xFFCAC4D0),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = logoIcon,
                                    contentDescription = logoName,
                                    tint = if (websiteLogo == logoName) Color(0xFF6750A4) else Color(0xFF49454F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(logoName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (websiteLogo == logoName) Color(0xFF6750A4) else Color(0xFF49454F))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFECE6F0))

                    // Website Lock control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Website Access", fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20), fontSize = 14.sp)
                            Text("When enabled, only administrators are allowed to enter or perform translation tasks.", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                        Switch(
                            checked = websiteLocked,
                            onCheckedChange = { viewModel.saveConfig("website_locked", if (it) "true" else "false") },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4)
                            )
                        )
                    }
                }
            }
        }

        // --- SECTION: CHANGE ADMIN CREDENTIALS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Change Admin Security Credentials",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = currentAdminUsername,
                        onValueChange = { currentAdminUsername = it },
                        label = { Text("Current Admin Username", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAdminUsername,
                        onValueChange = { newAdminUsername = it },
                        label = { Text("New Admin Username", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAdminPassword,
                        onValueChange = { newAdminPassword = it },
                        label = { Text("New Secure Password", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (credentialsMessage.isNotEmpty()) {
                        Text(
                            text = credentialsMessage,
                            color = if (credentialsSuccess) Color(0xFF6750A4) else Color(0xFFFF4D4D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (currentAdminUsername.isNotBlank() && newAdminUsername.isNotBlank() && newAdminPassword.isNotBlank()) {
                                viewModel.updateAdminCredentials(
                                    oldUsername = currentAdminUsername,
                                    newUsername = newAdminUsername,
                                    newPasswordHash = newAdminPassword
                                )
                                credentialsSuccess = true
                                credentialsMessage = "Admin credentials updated successfully!"
                            } else {
                                credentialsSuccess = false
                                credentialsMessage = "Please complete all fields to update credentials."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Credentials Change", color = Color.White)
                    }
                }
            }
        }

        // --- SECTION: GOOGLE OAUTH SECURITY KEYS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Google OAuth Identity Client Configuration",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = googleClientId,
                        onValueChange = { viewModel.saveConfig("google_oauth_client_id", it) },
                        label = { Text("Google Web Client ID", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Once Client ID is configured, the 'Continue with Google' button will automatically become active on the Secure Login screen.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }

        // --- SECTION: GOOGLE SMTP EMAIL CHANNEL ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Google SMTP Service Setup",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = { viewModel.saveConfig("smtp_host", it) },
                        label = { Text("SMTP Host Server", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = { viewModel.saveConfig("smtp_port", it) },
                        label = { Text("SMTP Server Port (SSL: 465, TLS: 587)", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpUser,
                        onValueChange = { viewModel.saveConfig("smtp_username", it) },
                        label = { Text("SMTP Outbound Email User", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = smtpPass,
                        onValueChange = { viewModel.saveConfig("smtp_password", it) },
                        label = { Text("SMTP Secure App Password", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        visualTransformation = if (smtpPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        trailingIcon = {
                            TextButton(onClick = { smtpPassVisible = !smtpPassVisible }) {
                                Text(
                                    text = if (smtpPassVisible) "Hide" else "Show",
                                    color = Color(0xFF6750A4),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Used for sending OTP validation codes securely. Leave empty to use simulated secure OTP codes generated locally inside the application for sandbox testing.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }
        }

        // Active AI Engine Select Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Default Translation AI Model",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = preferredAi == "Gemini 3.5 Flash",
                            onClick = { viewModel.saveConfig("preferred_ai", "Gemini 3.5 Flash") },
                            label = { Text("Gemini 3.5 Flash (Direct API)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6750A4),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFECE6F0),
                                labelColor = Color(0xFF1D1B20)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = preferredAi == "OpenAI Whisper-1",
                            onClick = { viewModel.saveConfig("preferred_ai", "OpenAI Whisper-1") },
                            label = { Text("OpenAI Whisper-1 (Speech API)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6750A4),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFECE6F0),
                                labelColor = Color(0xFF1D1B20)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // API Keys Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFECE6F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Configure AI API Access Keys",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20),
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { viewModel.saveConfig("gemini_api_key", it) },
                        label = { Text("Google Gemini API Key", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        visualTransformation = if (geminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        trailingIcon = {
                            TextButton(onClick = { geminiKeyVisible = !geminiKeyVisible }) {
                                Text(
                                    text = if (geminiKeyVisible) "Hide" else "Show",
                                    color = Color(0xFF6750A4),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = openaiKey,
                        onValueChange = { viewModel.saveConfig("openai_api_key", it) },
                        label = { Text("OpenAI Whisper API Key", color = Color(0xFF6750A4)) },
                        singleLine = true,
                        visualTransformation = if (openaiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        trailingIcon = {
                            TextButton(onClick = { openaiKeyVisible = !openaiKeyVisible }) {
                                Text(
                                    text = if (openaiKeyVisible) "Hide" else "Show",
                                    color = Color(0xFF6750A4),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF9E6))
                            .border(1.dp, Color(0xFFFFB703), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Alert", tint = Color(0xFFB38600))
                            Text(
                                text = "Admin Notice: Keys are stored securely in local app storage. When keys are empty, the app defaults to Smart Simulated Translation mode automatically so users can test full visual layout presets immediately.",
                                fontSize = 11.sp,
                                color = Color(0xFF806000),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: ADMIN LOGS ---
@Composable
fun AdminLogsTab(viewModel: SubtitleViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "API Request Logs",
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                )
                Text(
                    text = "Verify operational response details from Whisper and Gemini endpoints.",
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF49454F))
                )
            }

            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear logs", tint = Color(0xFF49454F))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No request logs captured.", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (log.status == "SUCCESS") Color(0xFF6750A4).copy(0.3f) else Color(0xFFFF3333).copy(0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.fileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (log.status == "SUCCESS") Color(0xFFE8DEF8)
                                            else Color(0xFFFFD8E4)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = log.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.status == "SUCCESS") Color(0xFF6750A4) else Color(0xFF31111D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Engine: ${log.aiEngine}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6750A4),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Lang: ${log.language}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6750A4),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTimestamp(log.timestamp),
                                    fontSize = 11.sp,
                                    color = Color(0xFF49454F)
                                )
                            }

                            if (log.errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Error: ${log.errorMessage}",
                                    color = Color(0xFFFF3333),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PROJECT DETAIL (VIDEO PLAYER & EDITOR) SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(viewModel: SubtitleViewModel) {
    val context = LocalContext.current
    val project by viewModel.selectedProject.collectAsStateWithLifecycle()
    val lines by viewModel.currentProjectLines.collectAsStateWithLifecycle()

    var activePresetId by remember { mutableStateOf("mrbeast") }
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var selectedLineEdit by remember { mutableStateOf<SubtitleLine?>(null) }
    
    var isAddLineDialogOpen by remember { mutableStateOf(false) }
    var isExportDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(project) {
        project?.let {
            activePresetId = it.selectedPresetId
        }
    }

    val activePreset = CaptionPreset.getById(activePresetId)

    // Periodic position polling loop to sync subtitles overlay dynamically!
    LaunchedEffect(videoViewInstance) {
        while (isActive) {
            videoViewInstance?.let { vv ->
                if (vv.isPlaying) {
                    currentPlaybackPositionMs = vv.currentPosition.toLong()
                }
            }
            delay(50) // High-precision 50ms interval polling!
        }
    }

    // Find active subtitle line based on current position!
    val activeLine = lines.find { currentPlaybackPositionMs in it.startMs..it.endMs }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = project?.title ?: "Subtitle Editor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectProject(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF6750A4) )
                    }
                },
                actions = {
                    // Export dialog button
                    IconButton(onClick = { isExportDialogOpen = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export SRT", tint = Color(0xFF6750A4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            project?.let { currentProject ->
                // VIDEO PLAYER SCREEN PANEL WITH OVERLAYS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Embed raw video player
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(Uri.parse(currentProject.videoUri))
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    start()
                                }
                                videoViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtitle overlay dynamically styled based on preset!
                    if (activeLine != null) {
                        val displayedText = if (activePreset.isUppercase) activeLine.text.uppercase() else activeLine.text
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(activePreset.backgroundColor)
                                .padding(
                                    horizontal = activePreset.paddingDp.dp,
                                    vertical = (activePreset.paddingDp / 2).dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (activePreset.outlineWidth > 0) {
                                    Text(
                                        text = displayedText,
                                        color = activePreset.outlineColor,
                                        fontSize = activePreset.fontSize,
                                        fontWeight = activePreset.fontWeight,
                                        fontStyle = activePreset.fontStyle,
                                        letterSpacing = activePreset.letterSpacing,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            drawStyle = Stroke(
                                                width = activePreset.outlineWidth,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    )
                                }
                                Text(
                                    text = displayedText,
                                    color = activePreset.textColor,
                                    fontSize = activePreset.fontSize,
                                    fontWeight = activePreset.fontWeight,
                                    fontStyle = activePreset.fontStyle,
                                    letterSpacing = activePreset.letterSpacing,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Horizontal Preset Fast-Switch panel right under video
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Instant Preset Switcher",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F),
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CaptionPreset.list) { preset ->
                            val isSelected = activePresetId == preset.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    activePresetId = preset.id
                                    viewModel.updateSelectedProjectPreset(preset.id)
                                },
                                label = { Text(preset.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6750A4),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFECE6F0),
                                    labelColor = Color(0xFF1D1B20)
                                )
                            )
                        }
                    }
                }

                // Timed sub editor panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subtitles Timeline",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20)
                    )

                    Button(
                        onClick = { isAddLineDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add line", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Sub", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Scrollable Subtitle line list editor
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lines) { line ->
                        val isCurrentlyActive = activeLine?.id == line.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyActive) Color(0xFFE8DEF8) else Color.White
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrentlyActive) Color(0xFF6750A4) else Color(0xFFECE6F0)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Timestamp
                                    Text(
                                        text = "${formatDuration(line.startMs)} - ${formatDuration(line.endMs)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6750A4)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Quick seek play
                                        IconButton(
                                            onClick = {
                                                videoViewInstance?.seekTo(line.startMs.toInt())
                                                videoViewInstance?.start()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Seek Here", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                        }

                                        // Edit text
                                        IconButton(
                                            onClick = { selectedLineEdit = line },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Text", tint = Color(0xFF49454F), modifier = Modifier.size(14.dp))
                                        }

                                        // Delete Line
                                        IconButton(
                                            onClick = { viewModel.deleteLine(line) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Line", tint = Color(0xFFFF4D4D), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = line.text,
                                    fontSize = 14.sp,
                                    color = Color(0xFF1D1B20),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Subtitle dialog
    if (selectedLineEdit != null) {
        val lineToEdit = selectedLineEdit!!
        var editedText by remember { mutableStateOf(lineToEdit.text) }

        Dialog(onDismissRequest = { selectedLineEdit = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Edit Caption Line", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedLineEdit = null }) {
                            Text("Cancel", color = Color(0xFF49454F))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateLine(lineToEdit.copy(text = editedText))
                                selectedLineEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("Apply Changes", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Add Subtitle dialog
    if (isAddLineDialogOpen) {
        var newText by remember { mutableStateOf("") }
        var startSecs by remember { mutableStateOf("0.0") }
        var endSecs by remember { mutableStateOf("3.0") }

        Dialog(onDismissRequest = { isAddLineDialogOpen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Add Custom Subtitle", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))

                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        label = { Text("Caption Text", color = Color(0xFF6750A4)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = startSecs,
                            onValueChange = { startSecs = it },
                            label = { Text("Start (Seconds)", color = Color(0xFF6750A4)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            )
                        )

                        OutlinedTextField(
                            value = endSecs,
                            onValueChange = { endSecs = it },
                            label = { Text("End (Seconds)", color = Color(0xFF6750A4)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1D1B20),
                                unfocusedTextColor = Color(0xFF1D1B20),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddLineDialogOpen = false }) {
                            Text("Cancel", color = Color(0xFF49454F))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val startMs = ((startSecs.toFloatOrNull() ?: 0f) * 1000).toLong()
                                val endMs = ((endSecs.toFloatOrNull() ?: 3f) * 1000).toLong()
                                if (newText.isNotBlank()) {
                                    viewModel.addLine(startMs, endMs, newText)
                                }
                                isAddLineDialogOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("Insert Sub", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Export SRT/VTT dialog
    if (isExportDialogOpen) {
        val clipboardManager = LocalClipboardManager.current
        var copiedNotice by remember { mutableStateOf(false) }

        val srtOutput = SubtitleExporter.exportToSrt(lines)
        val vttOutput = SubtitleExporter.exportToVtt(lines)

        Dialog(onDismissRequest = { isExportDialogOpen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Export Timed Subtitles", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))

                    Text(
                        text = "Your captions are compiled! Choose a format below to copy directly to your clipboard or share:",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F)
                    )

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(srtOutput))
                            copiedNotice = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECE6F0)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy SRT Format (.srt)", color = Color(0xFF1D1B20))
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(vttOutput))
                            copiedNotice = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECE6F0)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Copy", tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy VTT Format (.vtt)", color = Color(0xFF1D1B20))
                    }

                    if (copiedNotice) {
                        Text(
                            text = "Copied to clipboard successfully! ✓",
                            color = Color(0xFF6750A4),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                isExportDialogOpen = false
                                copiedNotice = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- HELPERS ---
private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val millis = (ms % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%01d", minutes, seconds, millis)
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getVideoDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        time?.toLong() ?: 10000L
    } catch (e: Exception) {
        15000L // default fallback
    } finally {
        try {
            retriever.release()
        } catch (ex: Exception) {
            // ignore
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "video.mp4"
}
