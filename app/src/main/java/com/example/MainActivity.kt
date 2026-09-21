package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.model.UserRole
import com.example.data.repository.SchoolRepository
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassTopEmblemBanner
import com.example.ui.screens.AccountantDashboard
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.InchargeDashboard
import com.example.ui.screens.PrincipalDashboard
import com.example.ui.screens.RegisterScanScreen
import com.example.ui.screens.StudentDashboard
import com.example.ui.screens.TeacherDashboard
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel
import com.example.ui.viewmodel.SchoolViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SchoolRepository(database.schoolDao())
        val factory = SchoolViewModelFactory(repository, applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: SchoolViewModel = viewModel(factory = factory)
                SchoolManagementApp(viewModel)
            }
        }
    }
}

enum class ActiveScreen {
    DASHBOARD,
    REGISTER_SCAN,
    AUDIT_LOGS,
    AUTH
}

@Composable
fun SchoolManagementApp(viewModel: SchoolViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val profileName by viewModel.userProfileName.collectAsState()
    val userClaims by viewModel.userClaims.collectAsState()
    val verifiedInst by viewModel.verifiedInstitution.collectAsState()
    val selectedBatch by viewModel.selectedAcademicBatch.collectAsState()

    var activeScreen by remember { mutableStateOf(ActiveScreen.DASHBOARD) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // If not authenticated, ALWAYS enforce the Multi-Stage Secure Entry System (AuthScreen)
    if (!isAuthenticated) {
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                activeScreen = ActiveScreen.DASHBOARD
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("school_management_scaffold"),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column {
                // Official Institutional Emblem Top Bar with Real-Time Sync Indicator
                GlassTopEmblemBanner(
                    title = verifiedInst?.name ?: "ST. XAVIER'S INTERNATIONAL ACADEMY",
                    subTitle = "AUTONOMOUS INSTITUTIONAL CLOUD PORTAL",
                    regCode = "REG NO: ${verifiedInst?.registrationCode ?: "3477"} | BATCH $selectedBatch",
                    isOnline = isOnline,
                    pendingSyncCount = pendingSyncCount
                )

                // Active User Session & Role Security Header
                Surface(
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Profile & Role Clearance Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            GlassBadge(
                                text = currentRole.roleClaim.replace("ROLE_", ""),
                                color = when (currentRole) {
                                    UserRole.PRINCIPAL -> GovNavyPrimary
                                    UserRole.INCHARGE -> GovSaffron
                                    UserRole.TEACHING -> GovGreen
                                    UserRole.STUDENT -> GovNavyDark
                                    UserRole.PARENT -> GovSaffron
                                    else -> Color(0xFFD97706)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = profileName.take(28),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Batch: $selectedBatch • ${verifiedInst?.cityCampus ?: "Campus"}",
                                    fontSize = 10.sp,
                                    color = GovSlateMuted
                                )
                            }
                        }

                        // Actions: Sync & Secure Logout
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.triggerSyncNow() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = GovNavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            IconButton(
                                onClick = { showLogoutDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Secure Logout",
                                    tint = GovRedAlert,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = activeScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    ActiveScreen.AUTH -> {
                        AuthScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { activeScreen = ActiveScreen.DASHBOARD }
                        )
                    }
                    ActiveScreen.REGISTER_SCAN -> {
                        RegisterScanScreen(
                            viewModel = viewModel,
                            onNavigateBack = { activeScreen = ActiveScreen.DASHBOARD }
                        )
                    }
                    ActiveScreen.AUDIT_LOGS -> {
                        AuditLogsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { activeScreen = ActiveScreen.DASHBOARD }
                        )
                    }
                    ActiveScreen.DASHBOARD -> {
                        when (currentRole) {
                            UserRole.PRINCIPAL -> {
                                PrincipalDashboard(
                                    viewModel = viewModel,
                                    onNavigateToAuditLogs = { activeScreen = ActiveScreen.AUDIT_LOGS }
                                )
                            }
                            UserRole.INCHARGE -> {
                                InchargeDashboard(
                                    viewModel = viewModel,
                                    onNavigateToScanScreen = { activeScreen = ActiveScreen.REGISTER_SCAN }
                                )
                            }
                            UserRole.TEACHING -> {
                                TeacherDashboard(
                                    viewModel = viewModel,
                                    onNavigateToScanScreen = { activeScreen = ActiveScreen.REGISTER_SCAN }
                                )
                            }
                            UserRole.STUDENT, UserRole.PARENT -> {
                                StudentDashboard(viewModel = viewModel)
                            }
                            UserRole.ACCOUNTANT -> {
                                AccountantDashboard(viewModel = viewModel)
                            }
                            else -> {
                                PrincipalDashboard(
                                    viewModel = viewModel,
                                    onNavigateToAuditLogs = { activeScreen = ActiveScreen.AUDIT_LOGS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Secure Logout Confirmation Dialog
    if (showLogoutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold,
                    color = GovNavyPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to terminate your authenticated session? All unsaved cached drafts are safely recorded in offline queue.",
                    fontSize = 13.sp,
                    color = GovSlateDark
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = GovRedAlert
                    )
                ) {
                    Text("Logout", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
