@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.presentation.ui.screens.medical.components.ClinicMap
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Pure UI component for the Booking screen. Takes state and callbacks, no ViewModel dependency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    clinics: List<Clinic>,
    selectedClinic: Clinic?,
    onClinicClick: (String) -> Unit,
    onClinicSelect: (Clinic) -> Unit,
    modifier: Modifier = Modifier
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
                bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
            ),
        sheetContent = {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        stringResource(Res.string.medical_booking_nearby_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                items(clinics) { clinic ->
                    ClinicCompactRow(
                        clinic = clinic,
                        isSelected = selectedClinic?.id == clinic.id,
                        onClick = { onClinicSelect(clinic) },
                        onNavigate = { onClinicClick(clinic.id) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            ClinicMap(clinics = clinics, selectedClinic = selectedClinic)

            // Search Bar (Floating at Top)
            Surface(
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier =
                        Modifier.clickable { /* TODO: Open Search Screen */ }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.SearchW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.medical_booking_search_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.medical_booking_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ClinicCompactRow(
    clinic: Clinic,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
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
        horizontalArrangement = Arrangement.SpaceBetween
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
                    }
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
                maxLines = 1
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
                    }
            )
        }
    }
}
