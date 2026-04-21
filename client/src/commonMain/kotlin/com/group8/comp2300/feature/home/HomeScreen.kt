package com.group8.comp2300.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.ScreenHeader
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowForwardW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ChevronRightW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LocalPharmacyW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.StethoscopeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SupportAgentW400Outlinedfill1
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.home_chatbot_description
import comp2300.i18n.generated.resources.home_header_subtitle
import comp2300.i18n.generated.resources.home_header_title
import comp2300.i18n.generated.resources.home_medication_description
import comp2300.i18n.generated.resources.home_menu_chatbot
import comp2300.i18n.generated.resources.home_menu_medication_cabinet
import comp2300.i18n.generated.resources.home_menu_schedules
import comp2300.i18n.generated.resources.home_menu_shop
import comp2300.i18n.generated.resources.home_menu_symptom_check
import comp2300.i18n.generated.resources.home_quick_access_title
import comp2300.i18n.generated.resources.home_schedules_description
import comp2300.i18n.generated.resources.home_shop_description
import comp2300.i18n.generated.resources.home_symptom_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    onNavigateToShop: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSymptomChecker: () -> Unit = {},
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader(horizontalPadding = 0.dp, topPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.home_header_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.home_header_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            HeroActionCard(
                icon = Icons.SupportAgentW400Outlinedfill1,
                title = stringResource(Res.string.home_menu_chatbot),
                description = stringResource(Res.string.home_chatbot_description),
                iconTint = MaterialTheme.colorScheme.primary,
                accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                onClick = onNavigateToChatbot,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryActionCard(
                    icon = Icons.StethoscopeW400Outlinedfill1,
                    title = stringResource(Res.string.home_menu_symptom_check),
                    description = stringResource(Res.string.home_symptom_description),
                    iconTint = MaterialTheme.colorScheme.primary,
                    accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSymptomChecker,
                )
                SecondaryActionCard(
                    icon = Icons.AddW400Outlined,
                    title = stringResource(Res.string.home_menu_medication_cabinet),
                    description = stringResource(Res.string.home_medication_description),
                    iconTint = MaterialTheme.colorScheme.primary,
                    accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMedication,
                )
            }
        }

        item {
            Text(
                text = stringResource(Res.string.home_quick_access_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    HomeNavigationRow(
                        icon = Icons.DateRangeW400Outlinedfill1,
                        title = stringResource(Res.string.home_menu_schedules),
                        description = stringResource(Res.string.home_schedules_description),
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToRoutines,
                    )
                    HomeNavigationDivider()
                    HomeNavigationRow(
                        icon = Icons.LocalPharmacyW400Outlinedfill1,
                        title = stringResource(Res.string.home_menu_shop),
                        description = stringResource(Res.string.home_shop_description),
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToShop,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(
                icon = icon,
                iconTint = iconTint,
                accentColor = accentColor,
                modifier = Modifier.size(56.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = accentColor,
            ) {
                Icon(
                    imageVector = Icons.ArrowForwardW400Outlinedfill1,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.heightIn(min = 156.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ActionIcon(
                icon = icon,
                iconTint = iconTint,
                accentColor = accentColor,
                modifier = Modifier.size(48.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeNavigationRow(
    icon: ImageVector,
    title: String,
    description: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(
            icon = icon,
            iconTint = iconTint,
            accentColor = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.ChevronRightW400Outlined,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun HomeNavigationDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    iconTint: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accentColor,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
