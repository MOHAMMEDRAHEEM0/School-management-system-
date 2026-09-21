package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovGreenBorder
import com.example.ui.theme.GovGreenLight
import com.example.ui.theme.GovNavyContainer
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSaffronBorder
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite
import java.net.URLEncoder

// =========================================================================
// 2D GLASS DESIGN SYSTEM COMPONENTS (iOS-INSPIRED TRANSLUCENT SURFACES)
// =========================================================================

/**
 * Modern 2D Glassmorphic Card
 * Features translucent frosted background, delicate border stroke, optional header ribbon,
 * and clean typography with generous padding.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    headerRibbonColor: Color? = null,
    headerTitle: String? = null,
    headerBadge: String? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    borderColor: Color = GlassBorderLight,
    borderWidth: Dp = 1.dp,
    containerColor: Color = GlassSurfaceLight,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Surface(
        modifier = modifier
            .then(tagModifier)
            .shadow(elevation = 2.dp, shape = shape, spotColor = Color(0x1A000000))
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(clickModifier),
        color = containerColor,
        shape = shape
    ) {
        Column {
            if (headerRibbonColor != null && headerTitle != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerRibbonColor)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerTitle.uppercase(),
                        color = PureWhite,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (headerBadge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PureWhite.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = headerBadge,
                                color = PureWhite,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

/**
 * 2D Glass Badge / Status Pill
 */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GovNavyPrimary,
    bgColor: Color = color.copy(alpha = 0.12f),
    borderColor: Color = color.copy(alpha = 0.35f),
    textColor: Color = color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * 2D Glass Button with subtle highlight gradient & 48dp minimum interactive size
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GovNavyPrimary,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    testTag: String = "glass_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .testTag(testTag)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = PureWhite,
            disabledContainerColor = GovBorderGrey,
            disabledContentColor = GovSlateMuted
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = PureWhite
            )
        }
    }
}

/**
 * 2D Glass Outlined Button
 */
@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GovNavyPrimary,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "glass_outlined_button"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .height(42.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = color
            )
        }
    }
}

/**
 * 2D Glass Text Field with frosted background and clean outline
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "glass_text_field"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it, color = GovSlateText, fontSize = 11.5.sp) } },
            placeholder = { Text(placeholder, color = GovSlateMuted, fontSize = 11.5.sp) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            modifier = Modifier
                .testTag(testTag)
                .fillMaxWidth()
                .background(PureWhite.copy(alpha = 0.9f), RoundedCornerShape(8.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GovNavyPrimary,
                unfocusedBorderColor = GovBorderGrey,
                focusedTextColor = GovSlateDark,
                unfocusedTextColor = GovSlateDark,
                cursorColor = GovNavyPrimary,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = GovRedAlert,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Official Institutional Seal Stamp for Documents and Report Cards
 */
@Composable
fun OfficialSealStamp(
    title: String = "ACADEMY SEAL",
    code: String = "AUTHENTICATED",
    modifier: Modifier = Modifier,
    color: Color = GovGreen
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.08f))
            .border(1.8.dp, color, CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Seal",
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = color,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 9.sp
            )
            Text(
                text = code,
                color = color,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Institutional Top Emblem Bar with Real-Time Offline / Sync Status Indicator
 */
@Composable
fun GlassTopEmblemBanner(
    title: String = "ST. XAVIER'S INTERNATIONAL ACADEMY",
    subTitle: String = "PRIVATE AUTONOMOUS INSTITUTIONAL PORTAL",
    regCode: String = "INSTITUTION CODE: INST-SXA-2026",
    isOnline: Boolean = true,
    pendingSyncCount: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GovNavyDark)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Prestige Tricolor Micro Accent Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GovSaffron))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(PureWhite))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GovGreen))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(GovSaffron),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Academy Crest",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        color = PureWhite,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subTitle,
                        color = GovNavyContainer,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = regCode,
                        color = GovSaffronBorder,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sync Status Indicator Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PureWhite.copy(alpha = 0.12f))
                    .border(1.dp, PureWhite.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (!isOnline) Icons.Default.CloudOff
                        else if (pendingSyncCount > 0) Icons.Default.CloudSync
                        else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (!isOnline) GovRedAlert else if (pendingSyncCount > 0) GovSaffron else Color(0xFF86EFAC),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (!isOnline) "OFFLINE" else if (pendingSyncCount > 0) "SYNCING ($pendingSyncCount)" else "SYNCED",
                        color = PureWhite,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

/**
 * WhatsApp Official Notification Dispatch Action Button
 */
@Composable
fun OfficialWhatsAppButton(
    phone: String,
    message: String,
    label: String = "WhatsApp Notice",
    modifier: Modifier = Modifier,
    testTag: String = "official_whatsapp_btn"
) {
    val context = LocalContext.current
    Button(
        onClick = { launchWhatsAppMessage(context, phone, message) },
        modifier = modifier
            .testTag(testTag)
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF25D366), RoundedCornerShape(6.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF0FDF4),
            contentColor = Color(0xFF15803D)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "WhatsApp",
                tint = Color(0xFF25D366),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D)
            )
        }
    }
}

fun launchWhatsAppMessage(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val encodedMsg = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp dispatch formatted: $message", Toast.LENGTH_LONG).show()
    }
}
