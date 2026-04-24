package com.group8.comp2300.feature.shop

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.core.network.resolveRemoteAssetUrl
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.core.ui.components.shimmerEffect
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CloseW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShoppingCartW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val TestingAccent = Color(0xFF5F87D9)
private val MedicationAccent = Color(0xFF6C9BF0)
private val PreventionAccent = Color(0xFFD8912C)

@Composable
fun ShopScreen(viewModel: ShopViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val browseState by viewModel.browseState.collectAsState()
    val cartState by viewModel.cartState.collectAsState()
    val selectedProductId =
        (navigator.backStack.lastOrNull { it is Screen.ProductDetail } as? Screen.ProductDetail)?.productId

    ShopContent(
        products = browseState.products,
        selectedCategory = browseState.selectedCategory,
        selectedProductId = selectedProductId,
        cartItemCount = cartState.cartItemCount,
        isLoading = browseState.isLoadingProducts,
        error = browseState.productsError,
        browseFeedback = browseState.browseFeedback,
        onDismissBrowseFeedback = viewModel::dismissBrowseFeedback,
        onProductClick = { productId -> navigator.navigate(Screen.ProductDetail(productId)) },
        onCategorySelect = viewModel::selectCategory,
        onAddToCart = viewModel::addToCartFromBrowse,
        onRefresh = viewModel::refreshProducts,
        onBack = navigator::goBack,
        onCartClick = { navigator.navigate(Screen.Cart) },
    )
}

@Composable
private fun ShopContent(
    products: List<Product>,
    selectedCategory: ProductCategory,
    selectedProductId: String?,
    cartItemCount: Int,
    isLoading: Boolean,
    error: String?,
    browseFeedback: BrowseFeedback?,
    onDismissBrowseFeedback: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategorySelect: (ProductCategory) -> Unit,
    onAddToCart: (Product) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onCartClick: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val hasProducts = products.isNotEmpty()
    val showFullError = error != null && !hasProducts
    val showEmptyState = !isLoading && !showFullError && products.isEmpty()
    val scaleFraction = {
        if (isLoading) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            AppTopBar(
                title = {
                    Text(
                        text = stringResource(Res.string.shop_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = onCartClick) {
                        if (cartItemCount > 0) {
                            BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) {
                                Icon(
                                    imageVector = Icons.ShoppingCartW400Outlinedfill1,
                                    contentDescription = stringResource(Res.string.shop_cart_desc),
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.ShoppingCartW400Outlinedfill1,
                                contentDescription = stringResource(Res.string.shop_cart_desc),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            browseFeedback?.let { feedback ->
                BrowseFeedbackBar(
                    feedback = feedback,
                    onViewCart = onCartClick,
                    onDismiss = onDismissBrowseFeedback,
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
                    isRefreshing = isLoading,
                    onRefresh = onRefresh,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProductCategory.entries.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelect(category) },
                            label = {
                                Text(
                                    text = productCategoryLabel(category),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 0.dp,
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                when {
                    showFullError -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier.widthIn(max = 320.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                )
                                Button(onClick = onRefresh) {
                                    Text(stringResource(Res.string.shop_retry))
                                }
                            }
                        }
                    }

                    showEmptyState -> {
                        ShopEmptyState(modifier = Modifier.fillMaxSize())
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(156.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                if (error != null) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        ShopInlineErrorBanner(
                                            message = error,
                                            onRetry = onRefresh,
                                        )
                                    }
                                }

                                if (isLoading && !hasProducts) {
                                    items(4) { ShimmerProductCard() }
                                } else {
                                    items(products, key = Product::id) { product ->
                                        ProductCard(
                                            product = product,
                                            categoryLabel = productCategoryLabel(product.category),
                                            isSelected = selectedProductId == product.id,
                                            justAdded = browseFeedback?.productId == product.id &&
                                                !browseFeedback.isError,
                                            onClick = { onProductClick(product.id) },
                                            onPrimaryActionClick = { onAddToCart(product) },
                                        )
                                    }
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
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
                    isRefreshing = isLoading,
                )
            }
        }
    }
}

@Composable
private fun productCategoryLabel(category: ProductCategory): String = stringResource(
    when (category) {
        ProductCategory.ALL -> Res.string.shop_category_all
        ProductCategory.MEDICATION -> Res.string.shop_category_medication
        ProductCategory.TESTING -> Res.string.shop_category_testing
        ProductCategory.PREVENTION -> Res.string.shop_category_prevention
    },
)

@Composable
private fun ShopInlineErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
                Text(stringResource(Res.string.shop_retry))
            }
        }
    }
}

@Composable
private fun ShopEmptyState(modifier: Modifier = Modifier) {
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
                text = stringResource(Res.string.shop_browse_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.shop_browse_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BrowseFeedbackBar(
    feedback: BrowseFeedback,
    onViewCart: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (feedback.isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = feedback.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (feedback.isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (!feedback.isError) {
                TextButton(onClick = onViewCart) {
                    Text(stringResource(Res.string.shop_view_cart))
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.CloseW400Outlinedfill1, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    categoryLabel: String,
    isSelected: Boolean,
    justAdded: Boolean,
    onClick: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(356.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShopProductArtwork(
                product = product,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = categoryLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = productAccent(product).copy(alpha = 0.95f),
                    maxLines = 1,
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (product.insuranceCovered) "RM 0.00" else product.formattedPrice,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (product.insuranceCovered) {
                        CoveredPill()
                    }
                }
                OutlinedButton(
                    onClick = onPrimaryActionClick,
                    enabled = !justAdded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                ) {
                    Icon(
                        imageVector = if (justAdded) Icons.CheckW400Outlinedfill1 else Icons.AddW400Outlinedfill1,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            justAdded -> stringResource(Res.string.shop_added)
                            else -> stringResource(Res.string.shop_product_add)
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoveredPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.shop_covered),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun ShopProductArtwork(product: Product, modifier: Modifier = Modifier) {
    val platformContext = LocalPlatformContext.current
    var hasImageError by remember(product.imageUrl) { mutableStateOf(false) }
    val request = resolveShopImageUrl(product.imageUrl)?.let { imageUrl ->
        ImageRequest.Builder(platformContext)
            .data(imageUrl)
            .build()
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            productAccent(product).copy(alpha = 0.08f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (request != null && !hasImageError) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { hasImageError = true },
                    onSuccess = { hasImageError = false },
                )
            }
        }
    }
}

internal fun resolveShopImageUrl(imageUrl: String?): String? = resolveRemoteAssetUrl(imageUrl)

@Composable
private fun productAccent(product: Product): Color = when {
    product.name.contains("Doxy", ignoreCase = true) -> PreventionAccent
    product.category == ProductCategory.TESTING -> TestingAccent
    product.category == ProductCategory.PREVENTION -> PreventionAccent
    else -> MedicationAccent
}

@Composable
private fun ShimmerProductCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect(),
            )
            Box(Modifier.fillMaxWidth(0.38f).height(16.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Box(Modifier.fillMaxWidth(0.72f).height(28.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Box(Modifier.fillMaxWidth(0.58f).height(20.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Box(Modifier.fillMaxWidth().height(1.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(90.dp).height(32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                Box(Modifier.width(96.dp).height(42.dp).clip(RoundedCornerShape(18.dp)).shimmerEffect())
            }
        }
    }
}
