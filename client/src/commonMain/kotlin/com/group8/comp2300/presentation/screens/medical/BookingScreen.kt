package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.presentation.screens.medical.components.ClinicMap
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowForwardW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CloseW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SearchW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookingScreen(
    allClinics: List<Clinic>,
    filteredClinics: List<Clinic>,
    selectedClinic: Clinic?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 160.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = 16.dp,
        sheetShape =
        MaterialTheme.shapes.extraLarge.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
        ),
        sheetContent = {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    Text(
                        stringResource(Res.string.medical_booking_nearby_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                items(allClinics) { clinic ->
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
        },
    ) { _ ->
        val focusManager = LocalFocusManager.current

        Box(modifier = Modifier.fillMaxSize()) {
            ClinicMap(
                clinics = filteredClinics,
                selectedClinic = selectedClinic,
            )

            // Search Bar (Floating at Top)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    shadowElevation = 4.dp,
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
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    )
                }

                // Search Results Overlay
                if (searchQuery.isNotEmpty() && filteredClinics.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(filteredClinics) { clinic ->
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
                }

                // No results message
                if (searchQuery.isNotEmpty() && filteredClinics.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
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
            val clinicAddress = clinic.address
            if (clinicAddress != null) {
                Text(
                    text = clinicAddress,
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
fun ClinicCompactRow(
    clinic: Clinic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clinic.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${clinic.formattedDistance} • ${clinic.tags.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                maxLines = 1,
            )
        }

        IconButton(onClick = onNavigate) {
            Icon(
                imageVector = Icons.ArrowForwardW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_booking_view_details_desc),
                tint =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}
