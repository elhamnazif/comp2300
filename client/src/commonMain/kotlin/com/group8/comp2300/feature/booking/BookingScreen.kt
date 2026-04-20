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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
                isSignedIn = isSignedIn,
                onSearchQueryChange = onSearchQueryChange,
                onTagToggle = onTagToggle,
                onMapModeChange = onMapModeChange,
                onClinicClick = onClinicClick,
                onClinicSelect = onClinicSelect,
                onViewBookings = onViewBookings,
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                bottomStart = 28.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp,
                            ),
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = stringResource(Res.string.medical_booking_search_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.SearchW400Outlinedfill1,
                                        contentDescription = stringResource(Res.string.medical_booking_search_desc),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.CloseW400Outlinedfill1,
                                                contentDescription = stringResource(
                                                    Res.string.medical_booking_search_desc,
                                                ),
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
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 28.dp,
                                bottomEnd = 28.dp,
                            ),
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            IconButton(
                                onClick = { onMapModeChange(true) },
                                modifier = Modifier.padding(horizontal = 2.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.LocationOnW400Outlinedfill1,
                                    contentDescription = "Map",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            if (availableTags.isNotEmpty()) {
                item {
                    TagFilterRow(
                        availableTags = availableTags,
                        selectedTag = selectedTag,
                        onTagToggle = onTagToggle,
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredClinics.isEmpty()) {
                item {
                    EmptyPanel(
                        title = stringResource(Res.string.medical_booking_no_results),
                        body = "Try another search.",
                    )
                }
            } else {
                items(filteredClinics, key = { it.id }) { clinic ->
                    ClinicListCard(
                        clinic = clinic,
                        onSelect = { onClinicSelect(clinic) },
                        onViewAvailability = { onClinicClick(clinic.id) },
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
    isSignedIn: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (String?) -> Unit,
    onMapModeChange: (Boolean) -> Unit,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    onViewBookings: () -> Unit,
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

                if (availableTags.isNotEmpty()) {
                    item {
                        TagFilterRow(
                            availableTags = availableTags,
                            selectedTag = selectedTag,
                            onTagToggle = onTagToggle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredClinics.isEmpty()) {
                    item {
                        EmptyPanel(
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
                if (isSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onViewBookings) {
                            Text("My bookings")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.SearchW400Outlinedfill1,
                                        contentDescription = stringResource(Res.string.medical_booking_search_desc),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.CloseW400Outlinedfill1,
                                                contentDescription = stringResource(
                                                    Res.string.medical_booking_search_desc,
                                                ),
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
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            )

                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            )

                            Surface(
                                modifier = Modifier.padding(6.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                            ) {
                                IconButton(
                                    onClick = { onMapModeChange(false) },
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.FormatListBulletedW400Outlinedfill1,
                                        contentDescription = "List",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
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
                            Text(
                                text = stringResource(Res.string.medical_booking_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                label = { Text("All") },
                colors = chipColors(selectedTag == null),
            )
        }
        items(availableTags) { tag ->
            AssistChip(
                onClick = { onTagToggle(tag) },
                label = { Text(tag.replaceFirstChar(Char::uppercase)) },
                colors = chipColors(selectedTag == tag),
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
                androidx.compose.material3.Icon(
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
            androidx.compose.material3.Icon(
                imageVector = Icons.ArrowForwardW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_booking_view_details_desc),
                tint = if (isSelected) supportingColor else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ClinicListCard(clinic: Clinic, onSelect: () -> Unit, onViewAvailability: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            ) {
                ClinicImage(
                    clinic = clinic,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = clinic.name,
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = clinic.formattedDistance,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = clinic.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        clinic.address?.takeIf(String::isNotBlank)?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (clinic.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        clinic.tags.take(3).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Text(
                                    text = tag.replaceFirstChar(Char::uppercase),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Next: ${slotSummary(clinic.nextAvailableSlot)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    clinic.phone?.takeIf(String::isNotBlank)?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Button(
                        onClick = onViewAvailability,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("View availability")
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyPanel(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun chipColors(selected: Boolean) = if (selected) {
    AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
} else {
    AssistChipDefaults.assistChipColors()
}
