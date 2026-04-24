package com.group8.comp2300.feature.records

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.core.ui.components.ConfirmActionDialog
import com.group8.comp2300.core.ui.components.ConsumeSnackbarMessage
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.stringResource

@Composable
fun MedicalRecordScreen(viewModel: MedicalRecordViewModel, onNavigateBack: () -> Unit) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()
    var pendingDeleteRecord by remember { mutableStateOf<MedicalRecordResponse?>(null) }

    ConsumeSnackbarMessage(
        message = state.errorMessage,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::dismissError,
    )

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("pdf", "jpg", "jpeg", "png", "docx", "doc")),
    ) { file ->
        if (file != null) {
            viewModel.prepareUpload(file)
        }
    }

    pendingDeleteRecord?.let { record ->
        ConfirmActionDialog(
            title = stringResource(Res.string.medical_records_delete_title),
            message = stringResource(Res.string.medical_records_delete_message, record.fileName),
            confirmLabel = stringResource(Res.string.medical_records_delete_confirm),
            onConfirm = {
                pendingDeleteRecord = null
                viewModel.deleteRecord(record.id)
            },
            onDismiss = { pendingDeleteRecord = null },
        )
    }

    state.pendingUploadFileName?.let { pendingFileName ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingUpload,
            title = { Text(stringResource(Res.string.medical_record_choose_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = pendingFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MedicalRecordCategory.entries.forEach { category ->
                            FilterChip(
                                selected = state.pendingUploadCategory == category,
                                onClick = { viewModel.selectPendingCategory(category) },
                                label = { Text(category.displayName) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmUpload, enabled = !state.isUploading) {
                    Text(stringResource(Res.string.medical_records_upload))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingUpload, enabled = !state.isUploading) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    val scaleFraction = {
        if (state.isRefreshing) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
        }
    }
    val hasRecords = state.records.isNotEmpty()
    val showLoadErrorState = state.loadErrorMessage != null && !hasRecords
    val showFilteredEmptyState = !state.isRefreshing && !showLoadErrorState && !hasRecords && state.selectedCategory != null
    val showEmptyState = !state.isRefreshing && !showLoadErrorState && !showFilteredEmptyState && !hasRecords

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_records_title)) },
                onBackClick = onNavigateBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!state.isUploading) {
                        filePickerLauncher.launch()
                    }
                },
            ) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_records_upload_desc),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RecordSortOrder.entries.forEach { order ->
                            FilterChip(
                                selected = state.selectedSort == order,
                                onClick = { viewModel.loadRecords(order) },
                                label = { Text(order.displayName) },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.selectCategoryFilter(null) },
                            label = { Text(stringResource(Res.string.medical_records_filter_all)) },
                        )
                        MedicalRecordCategory.entries.forEach { category ->
                            FilterChip(
                                selected = state.selectedCategory == category,
                                onClick = { viewModel.selectCategoryFilter(category) },
                                label = { Text(category.displayName) },
                            )
                        }
                    }
                }

                if (state.isUploading || (state.isRefreshing && hasRecords)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when {
                    state.loadErrorMessage != null && hasRecords -> {
                        MedicalRecordInlineErrorBanner(
                            message = state.loadErrorMessage,
                            onRetry = viewModel::refresh,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    state.isRefreshing && !hasRecords -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 320.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(Res.string.medical_records_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    showLoadErrorState -> {
                        MedicalRecordLoadErrorState(
                            message = state.loadErrorMessage,
                            onRetry = viewModel::refresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    showFilteredEmptyState -> {
                        MedicalRecordFilteredEmptyState(
                            onClearFilter = { viewModel.selectCategoryFilter(null) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    showEmptyState -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 320.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.medical_records_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    text = stringResource(Res.string.medical_records_empty_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.records, key = { it.id }) { record ->
                                MedicalRecordItem(
                                    record = record,
                                    isOpening = state.openingRecordId == record.id,
                                    isDeleting = state.deletingRecordId == record.id,
                                    onOpen = { viewModel.openRecord(record) },
                                    onDelete = { pendingDeleteRecord = record },
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .graphicsLayer {
                        scaleX = scaleFraction()
                        scaleY = scaleFraction()
                    },
            ) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                )
            }
        }
    }
}

@Composable
private fun MedicalRecordInlineErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.medical_records_retry))
            }
        }
    }
}

@Composable
private fun MedicalRecordLoadErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.medical_records_retry))
            }
        }
    }
}

@Composable
private fun MedicalRecordFilteredEmptyState(onClearFilter: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.medical_records_filtered_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onClearFilter) {
                Text(stringResource(Res.string.medical_records_clear_filter))
            }
        }
    }
}
