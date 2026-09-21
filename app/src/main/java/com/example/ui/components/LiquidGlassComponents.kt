package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.InstitutionEntity
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite

// =========================================================================
// REFINED LIQUID GLASS DESIGN SYSTEM (Translucent, Frosted, 1px Borders)
// =========================================================================

val LiquidGlassBg = Color(0xF2FFFFFF)
val LiquidGlassBgDark = Color(0xEB0B2545)
val LiquidGlassBorder = Color(0x80FFFFFF)
val LiquidGlassBorderSubtle = Color(0x2E0B2545)
val LiquidGlassHighlight = Color(0x40FFFFFF)
val LiquidWarmGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFFFF6D00), // Vibrant Saffron / Warm Orange
        Color(0xFFF59E0B)  // Golden Amber Accent
    )
)
val LiquidNavyGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0B2545),
        Color(0xFF05172C)
    )
)

/**
 * Refined Liquid Glass Card with translucent frost, subtle depth, and 1px borders.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = LiquidGlassBg,
    borderColor: Color = LiquidGlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Surface(
        modifier = modifier
            .then(tagMod)
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color(0x180B2545),
                ambientColor = Color(0x0A000000)
            )
            .clip(shape)
            .border(borderWidth, borderColor, shape)
            .then(clickMod),
        color = containerColor,
        shape = shape
    ) {
        content()
    }
}

/**
 * Primary Warm Gradient Button for Key Actions ("Continue", "Login")
 * Styled after refined mobile actions with soft depth, high-contrast typography, and 48dp touch target.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    gradient: Brush = LiquidWarmGradient,
    testTag: String = "liquid_glass_button"
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .testTag(testTag)
            .height(50.dp)
            .shadow(
                elevation = if (enabled) 4.dp else 0.dp,
                shape = shape,
                spotColor = Color(0x40FF6D00)
            )
            .clip(shape)
            .background(
                if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
            )
            .border(1.dp, Color(0x33FFFFFF), shape)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = PureWhite,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                leadingIcon?.invoke()
                if (leadingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Refined Secondary Liquid Glass Button
 */
@Composable
fun LiquidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "liquid_glass_secondary_button"
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .testTag(testTag)
            .height(46.dp)
            .clip(shape)
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFCBD5E1), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
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
                color = GovNavyPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
        }
    }
}

/**
 * Liquid Glass Input Text Field with frosted background, delicate outline, and crisp focus states.
 */
@Composable
fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "liquid_glass_text_field"
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GovSlateDark,
                modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFFFFF)),
            placeholder = {
                Text(
                    text = placeholder,
                    color = GovSlateMuted,
                    fontSize = 13.5.sp
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = Color(0xFFFAFAFC),
                disabledContainerColor = Color(0xFFF1F5F9),
                focusedBorderColor = GovNavyPrimary,
                unfocusedBorderColor = Color(0xFFD1D5DB),
                focusedTextColor = GovSlateDark,
                unfocusedTextColor = GovSlateDark,
                cursorColor = GovNavyPrimary,
                errorBorderColor = GovRedAlert
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = GovRedAlert,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Liquid Glass Dropdown Selection Field (for Organization & Batch selection)
 */
@Composable
fun LiquidGlassDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "liquid_glass_dropdown"
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = GovSlateDark,
            modifier = Modifier.padding(bottom = 5.dp, start = 2.dp)
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureWhite)
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    leadingIcon?.invoke()
                    if (leadingIcon != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = selectedOption,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = GovSlateDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown Arrow",
                    tint = GovSlateMuted
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(PureWhite)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                                color = if (option == selectedOption) GovNavyPrimary else GovSlateDark
                            )
                        },
                        trailingIcon = {
                            if (option == selectedOption) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = GovNavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Registration Lookup Bottom Sheet
 * Inspiring authentic school selection workflow as shown in Screenshot 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationLookupBottomSheet(
    institutions: List<InstitutionEntity>,
    onSelectInstitution: (InstitutionEntity) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredInstitutions = remember(searchQuery, institutions) {
        if (searchQuery.isBlank()) {
            institutions
        } else {
            val query = searchQuery.trim().lowercase()
            institutions.filter {
                it.name.lowercase().contains(query) ||
                        it.registrationCode.lowercase().contains(query) ||
                        it.cityCampus.lowercase().contains(query) ||
                        it.institutionId.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: "Registration Number"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registration Number",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = GovSlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Informational warning note matching Screenshot 3
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Text(
                    text = "If your school is not in list then, inform your school administrator to register for login",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field
            LiquidGlassTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search by School Name or Registration Code...",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = GovSlateMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // List of Institutions matching Screenshot 3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredInstitutions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching schools found for '$searchQuery'",
                            color = GovSlateMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    filteredInstitutions.forEach { inst ->
                        InstitutionListItem(
                            institution = inst,
                            onContinue = {
                                onSelectInstitution(inst)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual School item in Lookup list
 */
@Composable
fun InstitutionListItem(
    institution: InstitutionEntity,
    onContinue: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
        color = Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                // Registration Code Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0E7FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Reg No: ${institution.registrationCode}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = institution.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovSlateDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = institution.cityCampus,
                    fontSize = 11.5.sp,
                    color = GovSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Warm Orange "Continue" Button matching Screenshot 3
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LiquidWarmGradient)
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
