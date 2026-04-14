package com.group8.comp2300.presentation.screens.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.group8.comp2300.data.local.AccessibilitySettingsDataSource
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityW400Outlinedfill1
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.accessibility_grayscale_desc
import comp2300.i18n.generated.resources.accessibility_grayscale_info
import comp2300.i18n.generated.resources.accessibility_grayscale_title
import comp2300.i18n.generated.resources.accessibility_supporting_text
import comp2300.i18n.generated.resources.accessibility_title
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
            SettingsInfoCard(
                description = stringResource(Res.string.accessibility_supporting_text),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
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
