package com.group8.comp2300.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.ui.settings.SettingsDetailScaffold
import com.group8.comp2300.core.ui.settings.SettingsInfoCard
import com.group8.comp2300.core.ui.settings.SettingsSection
import com.group8.comp2300.core.ui.settings.SettingsToggleRow
import com.group8.comp2300.data.local.AccessibilitySettingsDataSource
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AccessibilityScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilitySettingsDataSource: AccessibilitySettingsDataSource = koinInject()
    val accessibilitySettings by accessibilitySettingsDataSource.state.collectAsState()

    SettingsDetailScaffold(
        title = stringResource(Res.string.accessibility_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection {
                SettingsToggleRow(
                    icon = Icons.VisibilityW400Outlinedfill1,
                    title = stringResource(Res.string.accessibility_grayscale_title),
                    description = stringResource(Res.string.accessibility_grayscale_desc),
                    checked = accessibilitySettings.grayscaleEnabled,
                    index = 0,
                    total = 1,
                    onCheckedChange = accessibilitySettingsDataSource::setGrayscaleEnabled,
                )
            }
        }
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.accessibility_grayscale_title),
                description = stringResource(Res.string.accessibility_grayscale_info),
            )
        }
    }
}
