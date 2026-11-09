package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.core.AppFeature
import com.example.core.FeatureAvailability
import com.example.data.model.RbacRoleCategory
import com.example.data.model.SchoolPermissions
import com.example.data.model.UserRole
import com.example.data.model.rbacCategory
import com.example.ui.components.RbacDashboardHost
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.ExamResultsScreen
import com.example.ui.screens.RegisterScanScreen
import com.example.ui.screens.TreasuryScreen
import com.example.ui.screens.WhatsAppHubScreen
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel

// =============================================================================
// NAVIGATION CONSTANTS & DESTINATIONS
// =============================================================================

object AppDestinations {
    const val COMMAND = "command"
    const val DASHBOARD = "command" // alias for backwards compatibility
    const val ATTENDANCE = "attendance"
    const val TREASURY = "treasury"
    const val WHATSAPP = "whatsapp"
    const val SECURITY = "security"
    const val EXAM_RESULTS = "exam_results"
    const val AUDIT_LOGS = "audit_logs"
    const val REGISTER_SCAN = "register_scan"
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val badgeLabel: String? = null
) {
    object Command : BottomNavItem(
        route = AppDestinations.COMMAND,
        title = "Command",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_item_command"
    )

    object Attendance : BottomNavItem(
        route = AppDestinations.ATTENDANCE,
        title = "Attendance",
        selectedIcon = Icons.Default.EventAvailable,
        unselectedIcon = Icons.Default.EventAvailable,
        testTag = "nav_item_attendance",
        badgeLabel = "Live"
    )

    object Treasury : BottomNavItem(
        route = AppDestinations.TREASURY,
        title = "Treasury",
        selectedIcon = Icons.Filled.Payment,
        unselectedIcon = Icons.Outlined.Payment,
        testTag = "nav_item_treasury"
    )

    object WhatsApp : BottomNavItem(
        route = AppDestinations.WHATSAPP,
        title = "WhatsApp",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        testTag = "nav_item_whatsapp",
        badgeLabel = "Hub"
    )

    object Security : BottomNavItem(
        route = AppDestinations.SECURITY,
        title = "Security",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_item_security",
        badgeLabel = "L3"
    )

    // Backwards-compatible aliases for tests and existing references
    object Dashboard : BottomNavItem(
        route = AppDestinations.COMMAND,
        title = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_item_dashboard"
    )

    object ExamResults : BottomNavItem(
        route = AppDestinations.EXAM_RESULTS,
        title = "Exam Results",
        selectedIcon = Icons.Filled.Grading,
        unselectedIcon = Icons.Default.Assessment,
        testTag = "nav_item_exam_results"
    )
}

val BottomNavDestinations = listOf(
    BottomNavItem.Command,
    BottomNavItem.Attendance,
    BottomNavItem.Treasury,
    BottomNavItem.WhatsApp,
    BottomNavItem.Security
)

val ActiveBottomNavDestinations: List<BottomNavItem>
    get() = BottomNavDestinations.filter { item ->
        when (item.route) {
            AppDestinations.COMMAND -> true
            AppDestinations.ATTENDANCE -> FeatureAvailability.isEnabled(AppFeature.DAILY_ATTENDANCE)
            AppDestinations.TREASURY -> FeatureAvailability.isEnabled(AppFeature.FEE_RECEIPTS)
            AppDestinations.WHATSAPP -> FeatureAvailability.isEnabled(AppFeature.WHATSAPP_AUTOMATION)
            AppDestinations.SECURITY -> FeatureAvailability.isEnabled(AppFeature.SECURITY_AUDIT_LOGS)
            else -> true
        }
    }

// =============================================================================
// BOTTOM NAVIGATION BAR COMPOSABLE
// =============================================================================

@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    currentRole: UserRole,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom navigation on primary destinations
    val showBottomBar = ActiveBottomNavDestinations.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    if (!showBottomBar) return

    Surface(
        color = Color(0xFFFFFFFF).copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_bottom_nav_bar")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color(0xFFE2E8F0).copy(alpha = 0.8f))
            )
            NavigationBar(
                containerColor = Color.Transparent,
                contentColor = GovNavyPrimary,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.fillMaxWidth()
            ) {
            ActiveBottomNavDestinations.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        if (item.badgeLabel != null && !selected) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = GovGreen,
                                        contentColor = PureWhite
                                    ) {
                                        Text(item.badgeLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GovNavyPrimary,
                        selectedTextColor = GovNavyPrimary,
                        indicatorColor = GovNavyPrimary.copy(alpha = 0.12f),
                        unselectedIconColor = GovSlateMuted,
                        unselectedTextColor = GovSlateMuted
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}
}

// =============================================================================
// MAIN NAVIGATION GRAPH (JETPACK COMPOSE NAVHOST)
// =============================================================================

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: SchoolViewModel,
    currentRole: UserRole,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToScanScreen: () -> Unit,
    onAccessDenied: (resource: String, required: String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.DASHBOARD,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // =====================================================================
        // 1. DASHBOARD VIEW (RBAC SWITCHER & ROLE-SPECIFIC PORTALS)
        // =====================================================================
        composable(route = AppDestinations.DASHBOARD) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Quick Action Bar on top of Dashboard to jump to Attendance or Exam Results
                QuickJumpNavigationHeader(
                    currentRole = currentRole,
                    onNavigateToAttendance = {
                        navController.navigate(AppDestinations.ATTENDANCE) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToExamResults = {
                        navController.navigate(AppDestinations.EXAM_RESULTS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                RbacDashboardHost(
                    currentRole = currentRole,
                    viewModel = viewModel,
                    onNavigateToAuditLogs = onNavigateToAuditLogs,
                    onNavigateToScanScreen = onNavigateToScanScreen,
                    onAccessDenied = onAccessDenied,
                    onNavigateToWhatsAppHub = {
                        navController.navigate(AppDestinations.WHATSAPP) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // =====================================================================
        // 2. ATTENDANCE VIEW (ROLL CALL, WHATSAPP NOTICES & CLASS REGISTERS)
        // =====================================================================
        composable(route = AppDestinations.ATTENDANCE) {
            AttendanceScreen(
                viewModel = viewModel,
                currentRole = currentRole,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExamResults = {
                    navController.navigate(AppDestinations.EXAM_RESULTS) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // =====================================================================
        // 3. TREASURY VIEW (FEE LEDGERS, PAYMENT GATEWAY & SANDBOX DASHBOARD)
        // =====================================================================
        composable(route = AppDestinations.TREASURY) {
            TreasuryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // =====================================================================
        // 4. WHATSAPP HUB & ATTENDANCE PUSH SYSTEM
        // =====================================================================
        composable(route = AppDestinations.WHATSAPP) {
            WhatsAppHubScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // =====================================================================
        // 5. SECURITY DIRECTORATE & CRYPTOGRAPHIC AUDIT LOGS
        // =====================================================================
        composable(route = AppDestinations.SECURITY) {
            AuditLogsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // =====================================================================
        // 6. EXAM RESULTS VIEW (MARKS ENTRY, GRADE CARDS & DIGITAL TRANSCRIPTS)
        // =====================================================================
        composable(route = AppDestinations.EXAM_RESULTS) {
            ExamResultsScreen(
                viewModel = viewModel,
                currentRole = currentRole,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAttendance = {
                    navController.navigate(AppDestinations.ATTENDANCE) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // =====================================================================
        // 7. AUDIT LOGS SCREEN (Direct Route)
        // =====================================================================
        composable(route = AppDestinations.AUDIT_LOGS) {
            AuditLogsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // =====================================================================
        // 8. REGISTER SCAN CAMERA SCREEN
        // =====================================================================
        composable(route = AppDestinations.REGISTER_SCAN) {
            RegisterScanScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// =============================================================================
// QUICK NAVIGATION ACTION CHIPS ON DASHBOARD
// =============================================================================

@Composable
private fun QuickJumpNavigationHeader(
    currentRole: UserRole,
    onNavigateToAttendance: () -> Unit,
    onNavigateToExamResults: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_jump_navigation_header")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GovGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACADEMIC SUITE:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovSlateMuted,
                    letterSpacing = 0.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Quick Jump to Attendance
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GovNavyPrimary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GovNavyPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("btn_quick_jump_attendance")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onNavigateToAttendance,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Attendance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GovNavyPrimary
                            )
                        }
                    }
                }

                // Quick Jump to Exam Results (hidden when disabled in Phase A)
                if (FeatureAvailability.isExamResultsEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GovSaffron.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GovSaffron.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("btn_quick_jump_exam_results")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = onNavigateToExamResults,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Exam Results",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
