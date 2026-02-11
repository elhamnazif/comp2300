@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.shop

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.components.shimmerEffect
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(viewModel: ShopViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.state.collectAsState()

    ShopContent(
        products = uiState.products,
        selectedCategory = uiState.selectedCategory,
        cartItemCount = uiState.cartItemCount,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onProductClick = { prodId -> navigator.navigate(Screen.ProductDetail(prodId)) },
        onCategorySelect = viewModel::selectCategory,
        onAddToCart = viewModel::addToCart,
        onRefresh = viewModel::refreshProducts,
        isGuest = navigator.isGuest,
        onRequireAuth = navigator::requireAuth
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShopContent(
    products: List<Product>,
    selectedCategory: ProductCategory,
    cartItemCount: Int,
    isLoading: Boolean,
    error: String?,
    onProductClick: (String) -> Unit,
    onCategorySelect: (ProductCategory) -> Unit,
    onAddToCart: (Product) -> Unit,
    onRefresh: () -> Unit,
    isGuest: Boolean = false,
    onRequireAuth: () -> Unit = {}
) {
    val state = rememberPullToRefreshState()
    val scaleFraction = {
        if (isLoading) {
            1f
        } else {
            LinearOutSlowInEasing.transform(state.distanceFraction).coerceIn(0f, 1f)
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).systemBarsPadding()
            .pullToRefresh(
                state = state,
                isRefreshing = isLoading,
                onRefresh = onRefresh
            )
    ) {
        Column(Modifier.padding(16.dp).weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(Res.string.shop_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) {
                    Icon(Icons.ShoppingCartW400Outlinedfill1, stringResource(Res.string.shop_cart_desc))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductCategory.entries.forEach { category ->
                    val categoryRes =
                        when (category) {
                            ProductCategory.ALL -> Res.string.shop_category_all
                            ProductCategory.MEDICATION -> Res.string.shop_category_medication
                            ProductCategory.TESTING -> Res.string.shop_category_testing
                            ProductCategory.PREVENTION -> Res.string.shop_category_prevention
                        }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(stringResource(categoryRes)) },
                        leadingIcon =
                            if (selectedCategory == category) {
                                { Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(16.dp)) }
                            } else {
                                null
                            }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.TextButton(onClick = { onCategorySelect(selectedCategory) }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isLoading) {
                            items(6) { ShimmerProductCard() }
                        } else {
                            items(products) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProductClick(product.id) },
                                    onAddClick = {
                                        if (isGuest) {
                                            onRequireAuth()
                                        } else {
                                            onAddToCart(product)
                                        }
                                    }
                                )
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(12.dp)) }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.InfoW400Outlinedfill1,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(Res.string.shop_discreet_packaging),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(32.dp)) }
                    }

                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer {
                                scaleX = scaleFraction()
                                scaleY = scaleFraction()
                            }
                    ) {
                        PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = isLoading)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier.fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (product.category == ProductCategory.MEDICATION) {
                        Icons.MailOutlineW400Outlinedfill1
                    } else {
                        Icons.DoneW400Outlinedfill1
                    },
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (product.insuranceCovered) {
                        stringResource(Res.string.shop_product_insured_price)
                    } else {
                        product.formattedPrice
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (product.insuranceCovered) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                )
                FilledTonalIconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AddW400Outlinedfill1, null, Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ShimmerProductCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(0.7f).height(20.dp).shimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.4f).height(16.dp).shimmerEffect())
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(60.dp).height(20.dp).shimmerEffect())
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            }
        }
    }
}
