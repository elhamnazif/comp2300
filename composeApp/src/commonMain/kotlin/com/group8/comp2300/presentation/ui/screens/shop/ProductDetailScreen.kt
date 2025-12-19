package com.group8.comp2300.presentation.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(viewModel: ShopViewModel = koinViewModel(), productId: String, onBack: () -> Unit) {
    val product = viewModel.getProductById(productId)
    if (product == null) {
        // Handle case where product is not found
        onBack()
        return
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
        bottomBar = {
            // Sticky Checkout Bar
            Surface(shadowElevation = 12.dp) {
                Row(
                    modifier =
                    Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (product.insuranceCovered) {
                                "$0 (Insured)"
                            } else {
                                product.formattedPrice
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color =
                            if (product.insuranceCovered) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    Button(
                        onClick = { /* Add to Cart Logic */ },
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Add to Cart") }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            // 1. Product Image Placeholder
            Box(
                modifier =
                Modifier.fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 2. Content
            Column(modifier = Modifier.padding(24.dp)) {
                if (product.insuranceCovered) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Covered by Insurance") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                        colors =
                        AssistChipDefaults.assistChipColors(
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

                // 3. Features List
                FeatureRow("Discrete Packaging", "Shipped in a plain, unbranded box.")
                FeatureRow("Free Delivery", "Arrives in 2-3 business days.")
                FeatureRow("Automatic Refills", "Never run out of your prevention.")
            }
        }
    }
}

@Composable
fun FeatureRow(title: String, subtitle: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
