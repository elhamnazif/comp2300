package com.group8.comp2300.feature.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.shop.CartLine
import com.group8.comp2300.util.formatCurrency
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.cartState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.shop_cart_title)) },
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
                        state.cartError?.let { cartError ->
                            Text(
                                text = cartError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(Res.string.shop_cart_subtotal),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                formatCurrency(state.cartSubtotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.hasUnavailableItems,
                        ) {
                            Text(stringResource(Res.string.shop_cart_checkout))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoadingCart -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading cart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            state.cartLines.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(Res.string.shop_cart_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onContinueShopping) {
                            Text(stringResource(Res.string.shop_continue_shopping))
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.cartLines, key = CartLine::productId) { line ->
                        CartLineItem(
                            line = line,
                            onIncrement = { viewModel.incrementCartItem(line.productId) },
                            onDecrement = { viewModel.decrementCartItem(line.productId) },
                            onRemove = { viewModel.removeCartItem(line.productId) },
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CartLineItem(line: CartLine, onIncrement: () -> Unit, onDecrement: () -> Unit, onRemove: () -> Unit) {
    val product = line.product
    val title = line.product?.name ?: stringResource(Res.string.shop_unavailable_item)
    val subtitle = line.product?.description ?: stringResource(Res.string.shop_unavailable_item_action)
    val hasProduct = product != null

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (product != null) {
                    ShopProductArtwork(
                        product = product,
                        modifier = Modifier
                            .size(72.dp),
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    ) {}
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasProduct) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = formatCurrency(line.lineTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (line.quantity > 1) {
                        Text(
                            text = "Each ${formatCurrency(line.priceAtAdd)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = line.quantity,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    enabled = hasProduct,
                )
                TextButton(onClick = onRemove) {
                    Text(stringResource(Res.string.shop_cart_remove))
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(
                onClick = onDecrement,
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = quantity.toString(),
                modifier = Modifier.widthIn(min = 28.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            TextButton(
                onClick = onIncrement,
                enabled = enabled,
                modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
