@file:Suppress("FunctionName", "FunctionName")

package com.group8.comp2300.presentation.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.shop.Product
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductDetailScreen(productId: String, viewModel: ShopViewModel = koinViewModel(), onBack: () -> Unit) {
    val productState by
        produceState<Product?>(initialValue = null, productId) { value = viewModel.getProductById(productId) }

    ProductDetailContent(product = productState, onBack = onBack, onAddToCart = { prod -> viewModel.addToCart(prod) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailContent(product: Product?, onBack: () -> Unit, onAddToCart: (Product) -> Unit) {
    if (product == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.ArrowBackW400Outlinedfill1, null) } }
            )
        },
        bottomBar = {
            // Sticky Checkout Bar
            Surface(shadowElevation = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.shop_details_total_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            if (product.insuranceCovered) {
                                stringResource(Res.string.shop_product_insured_price)
                            } else {
                                product.formattedPrice
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color =
                                if (product.insuranceCovered) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                        )
                    }
                    Button(onClick = { onAddToCart(product) }, modifier = Modifier.weight(1f).height(50.dp)) {
                        Text(stringResource(Res.string.shop_details_add_to_cart_button))
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            // 1. Product Image Placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.CheckCircleW400Outlinedfill1,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Content
            Column(modifier = Modifier.padding(24.dp)) {
                if (product.insuranceCovered) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(Res.string.shop_details_insurance_badge)) },
                        leadingIcon = { Icon(Icons.CheckCircleW400Outlinedfill1, null) },
                        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = Color(0xFF4CAF50))
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(product.description, style = MaterialTheme.typography.bodyLarge)

                Spacer(Modifier.height(24.dp))

                // 3. Features List
                FeatureRow(
                    stringResource(Res.string.shop_details_feature_packaging_title),
                    stringResource(Res.string.shop_details_feature_packaging_desc)
                )
                FeatureRow(
                    stringResource(Res.string.shop_details_feature_delivery_title),
                    stringResource(Res.string.shop_details_feature_delivery_desc)
                )
                FeatureRow(
                    stringResource(Res.string.shop_details_feature_refills_title),
                    stringResource(Res.string.shop_details_feature_refills_desc)
                )
            }
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
