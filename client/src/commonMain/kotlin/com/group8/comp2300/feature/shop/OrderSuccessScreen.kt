package com.group8.comp2300.feature.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.util.formatCurrency
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun OrderSuccessScreen(
    orderId: String,
    total: Double,
    onDone: () -> Unit,
    onContinueShopping: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.shop_order_success_title)) },
                onBackClick = onDone,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.CheckCircleW400Outlinedfill1,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(Res.string.shop_order_success_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${stringResource(Res.string.shop_order_success_reference)} $orderId",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${stringResource(Res.string.shop_order_success_total)} ${formatCurrency(total)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = onContinueShopping,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.shop_continue_shopping))
                }
            }
            item {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.shop_order_success_done))
                }
            }
        }
    }
}
