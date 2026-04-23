package com.group8.comp2300.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.group8.comp2300.app.effectiveAppearanceThemeMode
import com.group8.comp2300.core.ui.settings.*
import com.group8.comp2300.data.local.AppearanceSettingsDataSource
import com.group8.comp2300.data.local.AppearanceThemeMode
import com.group8.comp2300.platform.theme.getWallpaperSeedColor
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LightbulbW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppearanceScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val appearanceSettingsDataSource: AppearanceSettingsDataSource = koinInject()
    val appearanceSettings by appearanceSettingsDataSource.state.collectAsState()
    val wallpaperSeedColor = getWallpaperSeedColor()
    val wallpaperSupported = wallpaperSeedColor != null
    val effectiveMode = effectiveAppearanceThemeMode(
        appearanceThemeMode = appearanceSettings.themeMode,
        wallpaperSeedColor = wallpaperSeedColor,
    )

    SettingsDetailScaffold(
        title = stringResource(Res.string.appearance_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection {
                SettingsChoiceRow(
                    icon = Icons.LightbulbW400Outlinedfill1,
                    title = stringResource(Res.string.appearance_theme_label),
                    options = listOf(
                        SettingsChoiceOption(
                            key = AppearanceThemeMode.WALLPAPER.name,
                            label = stringResource(Res.string.appearance_theme_wallpaper),
                            enabled = wallpaperSupported,
                        ),
                        SettingsChoiceOption(
                            key = AppearanceThemeMode.MINT.name,
                            label = stringResource(Res.string.appearance_theme_mint),
                        ),
                    ),
                    selectedKey = effectiveMode.name,
                    index = 0,
                    total = 1,
                    onOptionSelected = { selectedMode ->
                        appearanceSettingsDataSource.setThemeMode(
                            AppearanceThemeMode.entries.first { it.name == selectedMode },
                        )
                    },
                )
            }
        }
        if (!wallpaperSupported) {
            item {
                SettingsInfoCard(
                    description = stringResource(Res.string.appearance_theme_unavailable_info),
                )
            }
        }
    }
}
