package com.group8.comp2300.feature.booking

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.CenteredLoadingPanel
import com.group8.comp2300.core.ui.components.CenteredMessagePanel
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun BookingScreen(
    clinics: List<Clinic>,
    filteredClinics: List<Clinic>,
    selectedClinic: Clinic?,
    searchQuery: String,
    selectedTag: String?,
    isLoading: Boolean,
    isMapMode: Boolean,
    isSignedIn: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (String?) -> Unit,
    onMapModeChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableTags = clinics.flatMap(Clinic::tags).distinct().sorted()
    val pullToRefreshState = rememberPullToRefreshState()
    val scaleFraction = {
        if (isLoading) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isLoading,
                onRefresh = onRefresh,
            ),
    ) {
        if (isMapMode) {
            BookingMapMode(
                filteredClinics = filteredClinics,
                selectedClinic = selectedClinic,
                availableTags = availableTags,
                searchQuery = searchQuery,
                selectedTag = selectedTag,
                isLoading = isLoading,
                onSearchQueryChange = onSearchQueryChange,
                onTagToggle = onTagToggle,
                onMapModeChange = onMapModeChange,
                onClinicClick = onClinicClick,
                onClinicSelect = onClinicSelect,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BookingListMode(
                filteredClinics = filteredClinics,
                availableTags = availableTags,
                searchQuery = searchQuery,
                selectedTag = selectedTag,
                isLoading = isLoading,
                isSignedIn = isSignedIn,
                onSearchQueryChange = onSearchQueryChange,
                onTagToggle = onTagToggle,
                onMapModeChange = onMapModeChange,
                onClinicClick = onClinicClick,
                onClinicSelect = onClinicSelect,
                onViewBookings = onViewBookings,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                },
        ) {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookingListMode(
    filteredClinics: List<Clinic>,
    availableTags: List<String>,
    searchQuery: String,
    selectedTag: String?,
    isLoading: Boolean,
    isSignedIn: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (String?) -> Unit,
    onMapModeChange: (Boolean) -> Unit,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(modifier = modifier) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Book clinic",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (isSignedIn) {
                        TextButton(onClick = onViewBookings) {
                            Text("My bookings")
                        }
                    }
                }
            }

            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BookingModeSearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onModeToggle = { onMapModeChange(true) },
                            toggleIcon = Icons.LocationOnW400Outlinedfill1,
                            toggleContentDescription = "Map",
                            onSearchImeAction = { focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (availableTags.isNotEmpty()) {
                            TagFilterRow(
                                availableTags = availableTags,
                                selectedTag = selectedTag,
                                onTagToggle = onTagToggle,
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    LoadingPanel(title = "Loading clinics")
                }
            } else if (filteredClinics.isEmpty()) {
                item {
                    SearchEmptyPanel(
                        title = stringResource(Res.string.medical_booking_no_results),
                        body = "Try another search.",
                    )
                }
            } else {
                items(filteredClinics, key = { it.id }) { clinic ->
                    ClinicListCard(
                        clinic = clinic,
                        onClick = {
                            onClinicSelect(clinic)
                            onClinicClick(clinic.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingMapMode(
    filteredClinics: List<Clinic>,
    selectedClinic: Clinic?,
    availableTags: List<String>,
    searchQuery: String,
    selectedTag: String?,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (String?) -> Unit,
    onMapModeChange: (Boolean) -> Unit,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val focusManager = LocalFocusManager.current

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 164.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 16.dp,
        sheetShape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
        sheetContent = {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.medical_booking_nearby_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                if (isLoading) {
                    item {
                        LoadingPanel(title = "Loading clinics")
                    }
                } else if (filteredClinics.isEmpty()) {
                    item {
                        SearchEmptyPanel(
                            title = stringResource(Res.string.medical_booking_no_results),
                            body = "Try another search.",
                        )
                    }
                } else {
                    items(filteredClinics, key = { it.id }) { clinic ->
                        ClinicCompactRow(
                            clinic = clinic,
                            isSelected = selectedClinic?.id == clinic.id,
                            onClick = { onClinicSelect(clinic) },
                            onNavigate = { onClinicClick(clinic.id) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            ClinicMap(
                clinics = filteredClinics,
                selectedClinic = selectedClinic,
                onClinicSelect = onClinicSelect,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BookingModeSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onModeToggle = { onMapModeChange(false) },
                        toggleIcon = Icons.FormatListBulletedW400Outlinedfill1,
                        toggleContentDescription = "List",
                        onSearchImeAction = { focusManager.clearFocus() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (availableTags.isNotEmpty()) {
                    TagFilterRow(
                        availableTags = availableTags,
                        selectedTag = selectedTag,
                        onTagToggle = onTagToggle,
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    if (filteredClinics.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(filteredClinics, key = { it.id }) { clinic ->
                                    ClinicSearchResultRow(
                                        clinic = clinic,
                                        onClick = {
                                            onClinicSelect(clinic)
                                            focusManager.clearFocus()
                                        },
                                    )
                                }
                            }
                        }
                    } else if (!isLoading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            SearchEmptyPanel(
                                title = stringResource(Res.string.medical_booking_no_results),
                                body = "Try another search.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingModeSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onModeToggle: () -> Unit,
    toggleIcon: androidx.compose.ui.graphics.vector.ImageVector,
    toggleContentDescription: String,
    onSearchImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.medical_booking_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.SearchW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.medical_booking_search_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.CloseW400Outlinedfill1,
                                contentDescription = stringResource(Res.string.medical_booking_search_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchImeAction() }),
            )

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            )

            IconButton(
                onClick = onModeToggle,
                modifier = Modifier.padding(start = 10.dp),
            ) {
                Icon(
                    imageVector = toggleIcon,
                    contentDescription = toggleContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    availableTags: List<String>,
    selectedTag: String?,
    onTagToggle: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            AssistChip(
                onClick = { onTagToggle(null) },
                modifier = Modifier.height(40.dp),
                label = { Text("All", style = MaterialTheme.typography.labelLarge) },
                colors = chipColors(selectedTag == null),
                border = chipBorder(selectedTag == null),
            )
        }
        items(availableTags) { tag ->
            AssistChip(
                onClick = { onTagToggle(tag) },
                modifier = Modifier.height(40.dp),
                label = {
                    Text(
                        tag.replaceFirstChar(Char::uppercase),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                colors = chipColors(selectedTag == tag),
                border = chipBorder(selectedTag == tag),
            )
        }
    }
}

@Composable
private fun ClinicSearchResultRow(clinic: Clinic, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clinic.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            clinic.address?.takeIf(String::isNotBlank)?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = clinic.formattedDistance,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun ClinicCompactRow(
    clinic: Clinic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            Color.Transparent
        }
    val titleColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val supportingColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val chipContainerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = clinic.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.LocationOnW400Outlinedfill1,
                    contentDescription = null,
                    tint = supportingColor,
                )
                Text(
                    text = "${clinic.formattedDistance} away",
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor,
                    maxLines = 1,
                )
            }

            if (clinic.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    clinic.tags.take(3).forEach { tag ->
                        Surface(
                            color = chipContainerColor,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        ) {
                            Text(
                                text = tag.replaceFirstChar(Char::uppercase),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = titleColor,
                            )
                        }
                    }
                }
            }
        }

        IconButton(onClick = onNavigate) {
            Icon(
                imageVector = Icons.ArrowForwardW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_booking_view_details_desc),
                tint = if (isSelected) supportingColor else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ClinicListCard(clinic: Clinic, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(width = 84.dp, height = 76.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                ClinicImage(
                    clinic = clinic,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = clinic.name,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = clinic.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = clinicListSlotLabel(clinic.nextAvailableSlot),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                clinicListMetadata(clinic)?.let { metadata ->
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Icon(
                    imageVector = Icons.ArrowForwardW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_booking_view_details_desc),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }
}

private fun clinicListMetadata(clinic: Clinic): String? {
    val tagText = clinic.tags.firstUsefulTag()
    val locationText = clinic.formattedDistance.takeIf(String::isNotBlank)
        ?: clinic.address?.takeIf(String::isNotBlank).orEmpty()
    return listOfNotNull(tagText, locationText.takeIf(String::isNotBlank)).joinToString(" • ").ifBlank { null }
}

private fun clinicListSlotLabel(timestamp: Long): String {
    val timeZone = TimeZone.currentSystemDefault()
    val slot = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    val tomorrow = today.plus(1, DateTimeUnit.DAY)
    val dayLabel = when (slot.date) {
        today -> "Today"
        tomorrow -> "Tomorrow"
        else -> shortDayName(slot.date.dayOfWeek.name)
    }
    return "$dayLabel, ${DateFormatter.formatTime(slot.hour, slot.minute)}"
}

private fun List<String>.firstUsefulTag(): String? = firstOrNull { it.lowercase() != "general" }
    ?.replaceFirstChar(Char::uppercase)
    ?: firstOrNull()?.replaceFirstChar(Char::uppercase)

private fun shortDayName(dayName: String): String = dayName
    .lowercase()
    .replaceFirstChar(Char::uppercase)
    .take(3)

@Composable
internal fun EmptyPanel(title: String, body: String) {
    CenteredMessagePanel(title = title, body = body, verticalPadding = 20.dp)
}

@Composable
private fun LoadingPanel(title: String, modifier: Modifier = Modifier) {
    CenteredLoadingPanel(title = title, modifier = modifier)
}

@Composable
private fun SearchEmptyPanel(title: String, modifier: Modifier = Modifier, body: String? = null) {
    CenteredMessagePanel(title = title, modifier = modifier, body = body)
}

@Composable
private fun chipColors(selected: Boolean) = if (selected) {
    AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else {
    AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        labelColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun chipBorder(selected: Boolean): BorderStroke = BorderStroke(
    1.dp,
    if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    },
)
