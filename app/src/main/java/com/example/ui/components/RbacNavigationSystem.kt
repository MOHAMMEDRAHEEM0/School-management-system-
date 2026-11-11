package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.RbacRoleCategory
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.model.rbacCategory
import com.example.ui.screens.AccountantDashboard
import com.example.ui.screens.InchargeDashboard
import com.example.ui.screens.PrincipalDashboard
import com.example.ui.screens.StudentDashboard
import com.example.ui.screens.TeacherDashboard
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.viewmodel.SchoolViewModel

/**
 * Role-Based Access Control (RBAC) Clearance Indicator Bar.
 * Strictly non-interactive visual category indicator displaying Level 1 (Scholar/Parent),
 * Level 2 (Faculty), and Level 3 (Exec) permission scopes.
 * Locked strictly based on authenticated user profile without dev role-switching chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RbacClearanceIndicatorBar(
    currentRole: UserRole,
    userClaims: SecurityClaims?,
    modifier: Modifier = Modifier
) {
    // Complete Role Isolation: Never render clearance bar or level hints for non-admin users.
    // Wrapped in BuildConfig.DEBUG so it only exists in developer builds for admins.
    if (!BuildConfig.DEBUG || currentRole.rbacCategory != RbacRoleCategory.ADMIN) {
        return
    }

    var showPermissionsSheet by remember { mutableStateOf(false) }
    val currentCategory = currentRole.rbacCategory

    Surface(
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("rbac_clearance_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Role & Clearance Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (currentCategory) {
                                    RbacRoleCategory.ADMIN -> GovNavyPrimary
                                    RbacRoleCategory.TEACHER -> GovGreen
                                    RbacRoleCategory.STUDENT -> GovSaffron
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentCategory) {
                                RbacRoleCategory.ADMIN -> Icons.Default.AdminPanelSettings
                                RbacRoleCategory.TEACHER -> Icons.Default.School
                                RbacRoleCategory.STUDENT -> Icons.Default.Person
                            },
                            contentDescription = currentRole.label,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTH CLEARANCE: ${currentRole.label.uppercase()}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (currentCategory) {
                                    RbacRoleCategory.ADMIN -> Color(0xFFDBEAFE)
                                    RbacRoleCategory.TEACHER -> Color(0xFFDCFCE7)
                                    RbacRoleCategory.STUDENT -> Color(0xFFFEF3C7)
                                }
                            ) {
                                Text(
                                    text = "Level ${currentCategory.clearanceLevel}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentCategory) {
                                        RbacRoleCategory.ADMIN -> GovNavyPrimary
                                        RbacRoleCategory.TEACHER -> GovGreen
                                        RbacRoleCategory.STUDENT -> Color(0xFFB45309)
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${currentRole.clearanceTitle} • Institutional Security Guard",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                // Inspect Permissions Button
                TextButton(
                    onClick = { showPermissionsSheet = true },
                    modifier = Modifier.testTag("inspect_permissions_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security Claims",
                        tint = GovNavyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Claims",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovNavyPrimary
                    )
                }
            }
        }
    }

    if (showPermissionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            RbacClaimsInspectorContent(
                currentRole = currentRole,
                userClaims = userClaims,
                onDismiss = { showPermissionsSheet = false }
            )
        }
    }
}

/**
 * Clean non-interactive level indicator badge card.
 */
@Composable
private fun RbacLevelScopeBadge(
    levelTitle: String,
    roleScope: String,
    isActive: Boolean,
    activeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) activeColor else Color(0xFFF1F5F9),
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else Color(0xFFCBD5E1)
        ),
        modifier = Modifier.testTag("rbac_level_badge_${levelTitle.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isActive) Color.White else GovSlateMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = levelTitle,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) Color.White else GovSlateDark
                )
                Text(
                    text = if (isActive) "ACTIVE CLEARANCE" else roleScope,
                    fontSize = 8.5.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) Color.White.copy(alpha = 0.9f) else GovSlateMuted
                )
            }
        }
    }
}

// Deprecated alias kept for binary compatibility during migration
@Composable
fun RbacRoleSwitcherBar(
    currentRole: UserRole,
    userClaims: SecurityClaims?,
    onSwitchRole: (UserRole) -> Unit = {},
    modifier: Modifier = Modifier
) {
    RbacClearanceIndicatorBar(
        currentRole = currentRole,
        userClaims = userClaims,
        modifier = modifier
    )
}

/**
 * Bottom Sheet displaying active security claims and permission grants.
 */
@Composable
fun RbacClaimsInspectorContent(
    currentRole: UserRole,
    userClaims: SecurityClaims?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GovNavyPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RBAC Security Inspector",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateDark
                    )
                    Text(
                        text = "Role-Based Access Control State & Token Claims",
                        fontSize = 11.sp,
                        color = GovSlateMuted
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Role & Clearance Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Role Claim",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateMuted
                    )
                    Text(
                        text = currentRole.roleClaim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Clearance Level",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateMuted
                    )
                    Text(
                        text = "Level ${currentRole.rbacCategory.clearanceLevel} - ${currentRole.rbacCategory.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "User Identity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateMuted
                    )
                    Text(
                        text = userClaims?.displayName ?: "Active Session",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GovSlateDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Active Permission Grants (${userClaims?.permissions?.size ?: 0})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GovSlateDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        val permissionsList = userClaims?.permissions?.toList() ?: emptyList()
        if (permissionsList.isEmpty()) {
            Text(
                text = "No explicit permissions registered for this session.",
                fontSize = 11.sp,
                color = GovSlateMuted
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(permissionsList) { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GovGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = permission,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GovSlateDark
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Modal Alert displayed when an authenticated role attempts to access an unauthorized route or action.
 */
@Composable
fun RbacAccessDeniedDialog(
    requiredRole: String,
    currentRole: UserRole,
    resourceName: String,
    onDismiss: () -> Unit,
    onSwitchToAuthorizedRole: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Access Denied",
                tint = GovRedAlert,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Access Restricted (403 Forbidden)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GovSlateDark
            )
        },
        text = {
            Column {
                Text(
                    text = "Your current role (${currentRole.badge}) lacks the necessary security clearance to view '$resourceName'.",
                    fontSize = 12.5.sp,
                    color = GovSlateDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Required Clearance: $requiredRole",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovRedAlert
                        )
                        Text(
                            text = "Active Clearance: ${currentRole.clearanceTitle}",
                            fontSize = 10.sp,
                            color = GovSlateMuted
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary)
            ) {
                Text("Understood")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = GovSlateMuted)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Dynamic RBAC Dashboard Container.
 * Seamlessly switches between the Admin Dashboard, Teacher Dashboard,
 * and Student Dashboard based on the active role category.
 */
@Composable
fun RbacDashboardHost(
    currentRole: UserRole,
    viewModel: SchoolViewModel,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToScanScreen: () -> Unit,
    onAccessDenied: (resource: String, required: String) -> Unit,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToExamResults: () -> Unit = {},
    onNavigateToWhatsAppHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = currentRole.rbacCategory,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "RbacDashboardTransition",
        modifier = modifier
    ) { category ->
        when (category) {
            RbacRoleCategory.ADMIN -> {
                // Admin Dashboard: Complete Administrative View or Accountant Desk
                if (currentRole == UserRole.ACCOUNTANT) {
                    AccountantDashboard(viewModel = viewModel)
                } else {
                    PrincipalDashboard(
                        viewModel = viewModel,
                        onNavigateToAuditLogs = onNavigateToAuditLogs,
                        onNavigateToWhatsAppHub = onNavigateToWhatsAppHub
                    )
                }
            }
            RbacRoleCategory.TEACHER -> {
                // Teacher Dashboard: Faculty & Class In-Charge View
                if (currentRole == UserRole.INCHARGE) {
                    InchargeDashboard(
                        viewModel = viewModel,
                        onNavigateToScanScreen = onNavigateToScanScreen
                    )
                } else {
                    TeacherDashboard(
                        viewModel = viewModel,
                        onNavigateToScanScreen = onNavigateToScanScreen
                    )
                }
            }
            RbacRoleCategory.STUDENT -> {
                // Student Dashboard: Scholar & Guardian View
                StudentDashboard(viewModel = viewModel)
            }
        }
    }
}
