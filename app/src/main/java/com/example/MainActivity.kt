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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.model.RbacRoleCategory
import com.example.data.model.SchoolPermissions
import com.example.data.model.UserRole
import com.example.data.model.rbacCategory
import com.example.data.repository.SchoolRepository
import com.example.ui.components.FrostedGlassInstitutionalTopBar
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassTopEmblemBanner
import com.example.ui.components.RbacAccessDeniedDialog
import com.example.ui.components.RbacClearanceIndicatorBar
import com.example.ui.components.RbacDashboardHost
import com.example.ui.navigation.AppBottomNavigationBar
import com.example.ui.navigation.AppDestinations
import com.example.ui.navigation.AppNavGraph
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val monitor = com.example.data.remote.sync.NetworkConnectivityMonitor(applicationContext)
        val outboxManager = com.example.data.remote.sync.DurableOutboxManager(database.schoolDao(), monitor)
        val attendanceRepo = com.example.data.repository.AttendanceRepositoryImpl(database.schoolDao(), outboxManager)
        val repository = SchoolRepository(database.schoolDao(), outboxManager, attendanceRepo)
        val factory = SchoolViewModelFactory(repository, applicationContext, attendanceRepository = attendanceRepo)

        setContent {
            MyApplicationTheme {
                val viewModel: SchoolViewModel = viewModel(factory = factory)
                SchoolManagementApp(viewModel)
            }
        }
    }
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

    val navController = rememberNavController()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var accessDeniedInfo by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun navigateToAuditLogs() {
        if (userClaims?.hasPermission(SchoolPermissions.VIEW_AUDIT_LOGS) == true || currentRole.rbacCategory == RbacRoleCategory.ADMIN) {
            navController.navigate(AppDestinations.AUDIT_LOGS)
        } else {
            accessDeniedInfo = "Institutional Audit Logs Directorate" to "ROLE_ADMIN (Clearance Level 3)"
        }
    }

    fun navigateToRegisterScan() {
        if (userClaims?.hasPermission(SchoolPermissions.SCAN_REGISTER_CAMERA) == true || currentRole.rbacCategory != RbacRoleCategory.STUDENT) {
            navController.navigate(AppDestinations.REGISTER_SCAN)
        } else {
            accessDeniedInfo = "Physical Register Camera OCR Scanner" to "ROLE_TEACHER or ROLE_ADMIN (Faculty Clearance Level 2+)"
        }
    }

    // If not authenticated, ALWAYS enforce the Multi-Stage Secure Entry System (AuthScreen)
    if (!isAuthenticated) {
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                // User enters the main navigation graph automatically upon authentication
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("school_management_scaffold"),
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                currentRole = currentRole
            )
        },
        topBar = {
            Column {
                FrostedGlassInstitutionalTopBar(
                    institution = verifiedInst,
                    profileName = profileName,
                    currentRole = currentRole,
                    selectedBatch = selectedBatch,
                    isOnline = isOnline,
                    pendingSyncCount = pendingSyncCount,
                    onTriggerSync = { viewModel.triggerSyncNow() },
                    onLogoutClick = { showLogoutDialog = true }
                )

                // Production Security: RBAC Clearance Indicator Bar is completely REMOVED for non-admin users.
                // Students, Teachers, and Parents see ZERO clearance level cards.
                // Only administrators in DEBUG mode can see the clearance header.
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (BuildConfig.DEBUG && currentRole.rbacCategory == RbacRoleCategory.ADMIN && (currentRoute == null || currentRoute == AppDestinations.COMMAND || currentRoute == "dashboard")) {
                    RbacClearanceIndicatorBar(
                        currentRole = currentRole,
                        userClaims = userClaims
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppNavGraph(
                navController = navController,
                viewModel = viewModel,
                currentRole = currentRole,
                onNavigateToAuditLogs = { navigateToAuditLogs() },
                onNavigateToScanScreen = { navigateToRegisterScan() },
                onAccessDenied = { resource, required ->
                    accessDeniedInfo = resource to required
                }
            )
        }
    }

    // RBAC 403 Forbidden Access Denied Dialog (Strictly locked to authenticated clearance)
    accessDeniedInfo?.let { (resource, required) ->
        RbacAccessDeniedDialog(
            requiredRole = required,
            currentRole = currentRole,
            resourceName = resource,
            onDismiss = { accessDeniedInfo = null }
        )
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
