package com.group8.comp2300.feature.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShoppingCartW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val cartState by viewModel.cartState.collectAsState()
    val productState by produceState(ProductLoadState(), productId) {
        value = ProductLoadState(isLoading = true)
        value = ProductLoadState(product = viewModel.getProductById(productId), isLoading = false)
    }

    ProductDetailContent(
        product = productState.product,
        isLoading = productState.isLoading,
        cartItemCount = cartState.cartItemCount,
        onBack = onBack,
        onCartClick = onCartClick,
        onAddToCart = viewModel::addToCart,
    )
}

@Composable
private fun ProductDetailContent(
    product: Product?,
    isLoading: Boolean,
    cartItemCount: Int,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    when {
        isLoading -> ProductDetailStatusScaffold(
            onBack = onBack,
            content = {
                CircularProgressIndicator()
                Text(
                    text = "Loading product",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
        )

        product == null -> ProductDetailStatusScaffold(
            onBack = onBack,
            content = {
                Text(
                    text = stringResource(Res.string.shop_product_unavailable),
                    modifier = Modifier.widthIn(max = 320.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            },
        )

        else -> Scaffold(
            topBar = {
                AppTopBar(
                    title = {
                        Text(
                            text = product.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onBackClick = onBack,
                    actions = {
                        IconButton(onClick = onCartClick) {
                            if (cartItemCount > 0) {
                                BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) {
                                    Icon(Icons.ShoppingCartW400Outlinedfill1, stringResource(Res.string.shop_cart_desc))
                                }
                            } else {
                                Icon(Icons.ShoppingCartW400Outlinedfill1, stringResource(Res.string.shop_cart_desc))
                            }
                        }
                    },
                )
            },
            bottomBar = {
                Surface(shadowElevation = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.shop_details_total_label),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                if (product.insuranceCovered) {
                                    stringResource(Res.string.shop_product_insured_price)
                                } else {
                                    product.formattedPrice
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (product.insuranceCovered) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                        Button(
                            onClick = { onAddToCart(product) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                        ) {
                            Text(stringResource(Res.string.shop_details_add_to_cart_button))
                        }
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                ShopProductArtwork(
                    product = product,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    if (product.insuranceCovered) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(Res.string.shop_details_insurance_badge)) },
                            leadingIcon = { Icon(Icons.CheckCircleW400Outlinedfill1, null) },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = Color(0xFF4CAF50),
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(product.description, style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(24.dp))

                    FeatureRow(
                        stringResource(Res.string.shop_details_feature_packaging_title),
                        stringResource(Res.string.shop_details_feature_packaging_desc),
                    )
                    FeatureRow(
                        stringResource(Res.string.shop_details_feature_delivery_title),
                        stringResource(Res.string.shop_details_feature_delivery_desc),
                    )
                    FeatureRow(
                        stringResource(Res.string.shop_details_feature_refills_title),
                        stringResource(Res.string.shop_details_feature_refills_desc),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailStatusScaffold(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("") },
                centered = true,
                onBackClick = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
fun FeatureRow(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 8.dp)) {
        Icon(
            Icons.CheckCircleW400Outlinedfill1,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class ProductLoadState(val product: Product? = null, val isLoading: Boolean = true)
