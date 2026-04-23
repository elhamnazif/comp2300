package com.group8.comp2300.feature.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.util.formatCurrency
import com.group8.comp2300.core.ui.components.AppTopBar
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (orderId: String, total: Double) -> Unit,
    onContinueShopping: () -> Unit,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.checkoutState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.shop_checkout_title)) },
                onBackClick = onBack,
            )
        },
        bottomBar = {
            if (state.cartLines.isNotEmpty()) {
                Surface(shadowElevation = 12.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.orderError?.let { orderError ->
                            Text(
                                orderError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.placeOrder { order ->
                                    onOrderPlaced(order.id, order.total)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isPlacingOrder && !state.hasUnavailableItems && state.cartLines.isNotEmpty(),
                        ) {
                            if (state.isPlacingOrder) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(Res.string.shop_checkout_place_order))
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.cartLines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .widthIn(max = 320.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.shop_cart_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onContinueShopping) {
                    Text(stringResource(Res.string.shop_continue_shopping))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(Res.string.shop_checkout_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            state.cartLines.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${line.quantity} x ${line.product?.name ?: stringResource(Res.string.shop_unavailable_item)}")
                                    Text(formatCurrency(line.lineTotal))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    stringResource(Res.string.shop_cart_checkout),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatCurrency(state.cartSubtotal),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.shippingAddress,
                        onValueChange = viewModel::updateShippingAddress,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.shop_checkout_address_label)) },
                        minLines = 3,
                        isError = state.shippingAddressError != null,
                        supportingText = {
                            state.shippingAddressError?.let { message ->
                                Text(message)
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}
