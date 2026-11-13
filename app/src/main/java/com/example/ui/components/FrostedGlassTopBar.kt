package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.InstitutionEntity
import com.example.data.model.UserRole
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.PureWhite

/**
 * Senior Architect-Level Frosted Glass Institutional Header.
 * Replaces the dark, heavy top border bar with a clean, translucent, modern Material 3
 * frosted glass surface with subtle micro-borders, real-time sync telemetry,
 * role badge, multi-child parent switcher, and fast action controls.
 */
@Composable
fun FrostedGlassInstitutionalTopBar(
    institution: InstitutionEntity?,
    profileName: String,
    currentRole: UserRole,
    selectedBatch: String,
    isOnline: Boolean,
    pendingSyncCount: Int,
    selectedChildName: String = "Aarav Sharma",
    selectedChildGrade: String = "Grade 10-A",
    onSelectChild: (name: String, grade: String) -> Unit = { _, _ -> },
    onTriggerSync: () -> Unit,
    onQuickLockClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showChildDropdown by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, spotColor = Color(0x0F0F172A))
            .testTag("frosted_glass_top_bar"),
        color = Color(0xFFFFFFFF).copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
        ) {
            // Main Top Row: Emblem + School Title + Sync Status + Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // School Identity Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GovNavyPrimary)
                            .border(1.2.dp, GovSaffron.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "School Emblem",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = institution?.name ?: "St. Xavier's International Academy",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GovNavyPrimary,
                            letterSpacing = 0.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Text(
                                text = "Campus Cloud • $selectedBatch",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = GovSlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Micro-pill campus code (softWrap=false guarantees horizontal layout)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                            ) {
                                val regCode = institution?.registrationCode?.let {
                                    if (it.startsWith("REG")) it else "REG: $it"
                                } ?: "REG: 3477"
                                Text(
                                    text = regCode,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Status Telemetry & Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sync Pill (Frosted glass micro-chip)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            !isOnline -> Color(0xFFFEF2F2)
                            pendingSyncCount > 0 -> Color(0xFFFFFBEB)
                            else -> Color(0xFFF0FDF4)
                        },
                        border = BorderStroke(
                            0.8.dp,
                            when {
                                !isOnline -> Color(0xFFFCA5A5)
                                pendingSyncCount > 0 -> Color(0xFFFCD34D)
                                else -> Color(0xFF86EFAC)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    !isOnline -> Icons.Default.CloudOff
                                    pendingSyncCount > 0 -> Icons.Default.CloudSync
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = null,
                                tint = when {
                                    !isOnline -> GovRedAlert
                                    pendingSyncCount > 0 -> Color(0xFFB45309)
                                    else -> GovGreen
                                },
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !isOnline -> "OFFLINE"
                                    pendingSyncCount > 0 -> "SYNC ($pendingSyncCount)"
                                    else -> "LIVE"
                                },
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    !isOnline -> GovRedAlert
                                    pendingSyncCount > 0 -> Color(0xFFB45309)
                                    else -> GovGreen
                                }
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = onTriggerSync,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Quick Lock Button
                    IconButton(
                        onClick = onQuickLockClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Quick App Lock",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = GovRedAlert.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-Row: Active User Identity & Context (Role Badge & Vidyalaya-Style Child Switcher)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User & Role identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (currentRole) {
                            UserRole.ADMIN, UserRole.PRINCIPAL -> Color(0xFFDBEAFE)
                            UserRole.INCHARGE -> Color(0xFFFEF3C7)
                            UserRole.TEACHER, UserRole.TEACHING -> Color(0xFFDCFCE7)
                            UserRole.STUDENT -> Color(0xFFE0E7FF)
                            UserRole.PARENT -> Color(0xFFF3E8FF)
                            else -> Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            0.8.dp,
                            when (currentRole) {
                                UserRole.ADMIN, UserRole.PRINCIPAL -> Color(0xFF93C5FD)
                                UserRole.INCHARGE -> Color(0xFFFDE68A)
                                UserRole.TEACHER, UserRole.TEACHING -> Color(0xFF86EFAC)
                                UserRole.STUDENT -> Color(0xFFC7D2FE)
                                UserRole.PARENT -> Color(0xFFDDD6FE)
                                else -> Color(0xFFCBD5E1)
                            }
                        )
                    ) {
                        Text(
                            text = currentRole.roleClaim.replace("ROLE_", ""),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentRole) {
                                UserRole.ADMIN, UserRole.PRINCIPAL -> GovNavyPrimary
                                UserRole.INCHARGE -> Color(0xFFB45309)
                                UserRole.TEACHER, UserRole.TEACHING -> GovGreen
                                UserRole.STUDENT -> Color(0xFF3730A3)
                                UserRole.PARENT -> Color(0xFF6B21A8)
                                else -> GovSlateDark
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = profileName.take(24),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateDark
                    )
                }

                // If role is PARENT, provide Vidyalaya-Style Multi-Child Switcher
                if (currentRole == UserRole.PARENT) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showChildDropdown = true }
                                .testTag("btn_switch_ward_topbar")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = GovNavyPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$selectedChildName ($selectedChildGrade)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GovNavyPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Child",
                                    tint = GovNavyPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showChildDropdown,
                            onDismissRequest = { showChildDropdown = false }
                        ) {
                            val children = listOf(
                                "Aarav Sharma" to "Grade 10-A",
                                "Ananya Sharma" to "Grade 6-B"
                            )
                            children.forEach { (name, grade) ->
                                val isCurrent = name == selectedChildName
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(text = grade, fontSize = 9.5.sp, color = GovSlateMuted)
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Active",
                                                    tint = GovGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectChild(name, grade)
                                        showChildDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else if (currentRole == UserRole.TEACHER || currentRole == UserRole.TEACHING || currentRole == UserRole.INCHARGE) {
                    // Faculty Department Tag
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.6.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = if (currentRole == UserRole.INCHARGE) "Incharge: Sec 10-A" else "Class 10-A • Physics",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GovSlateDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hairline subtle frosted glass bottom separator (NO top border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(Color(0xFFE2E8F0).copy(alpha = 0.8f))
            )
        }
    }
}
