@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.presentation.components.ScreenHeader
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    onNavigateToShop: () -> Unit,
    onNavigateToEducation: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToMedication: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSymptomChecker: () -> Unit = {},
    onNavigateToClinicMap: () -> Unit = {}
) {
    // STATE: Privacy Mode (Blur sensitive text)
    var isPrivacyMode by remember { mutableStateOf(false) }

    // STATE: Mock "Daily Insight" Data
    val dailyFact = stringResource(Res.string.home_daily_insight_content)

    // Scroll state for smaller screens
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(scrollState) // Made scrollable to fit new buttons
                .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. TOP BAR: Privacy Toggle
        ScreenHeader(horizontalPadding = 0.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { isPrivacyMode = !isPrivacyMode }) {
                    Icon(
                        imageVector =
                            if (isPrivacyMode) {
                                Icons.VisibilityW500Outlined
                            } else {
                                Icons.VisibilityOffW500Outlined
                            },
                        contentDescription = stringResource(Res.string.home_toggle_privacy_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2. STATUS RING
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp).clickable { onNavigateToCalendar() }
        ) {
            CircularProgressIndicator(
                progress = { 0.85f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.ShieldW500Outlined,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))

                val blurRadius by animateFloatAsState(if (isPrivacyMode) 10f else 0f)

                Text(
                    text = stringResource(Res.string.home_protected_label),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.blur(blurRadius.dp)
                )
                Text(
                    text = stringResource(Res.string.home_streak_label_format, 12),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.blur(blurRadius.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 3. DAILY INSIGHT CARD
        Card(
            onClick = onNavigateToEducation,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.LightbulbW500Outlined,
                        null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = stringResource(Res.string.home_daily_insight_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(text = dailyFact, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 4. SMART ACTIONS GRID
        // Row 1: Daily Management
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmartActionButton(
                icon = Icons.AddW400Outlined,
                label = stringResource(Res.string.home_menu_log_pill),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMedication
            )

            SmartActionButton(
                icon = Icons.LocalPharmacyW500Outlined,
                label = stringResource(Res.string.home_menu_shop),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToShop
            )

            SmartActionButton(
                icon = Icons.CalendarMonthW500Outlined,
                label = stringResource(Res.string.home_menu_history),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCalendar
            )
        }

        Spacer(Modifier.height(12.dp))

        // Row 2: Health Services (New)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. STI Self Check (User Request)
            SmartActionButton(
                icon = Icons.StethoscopeW500Outlined,
                label = stringResource(Res.string.home_menu_symptom_check),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSymptomChecker
            )

            // 2. Find Clinic (Location based)
            SmartActionButton(
                icon = Icons.LocationOnW400Outlined,
                label = stringResource(Res.string.home_menu_find_clinic),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToClinicMap
            )

            // 3. Partner Notify (Anonymous SMS tool)
            SmartActionButton(
                icon = Icons.SendW400Outlined,
                label = stringResource(Res.string.home_menu_partner_notify),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Bottom padding for scroll
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SmartActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(110.dp) // Slightly taller to accommodate 2 lines of text
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = MaterialTheme.typography.labelMedium.lineHeight * 1.1
            )
        }
    }
}
