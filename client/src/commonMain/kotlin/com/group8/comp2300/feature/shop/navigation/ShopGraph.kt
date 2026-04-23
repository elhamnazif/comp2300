package com.group8.comp2300.feature.shop.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.feature.shop.*
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val shopGraphModule = module {
    navigation<Screen.Shop>(metadata = ListDetailSceneStrategy.listPane()) {
        ShopScreen()
    }

    navigation<Screen.ProductDetail>(
        metadata = overlayNavigationMetadata(ListDetailSceneStrategy.detailPane()),
    ) { route ->
        val navigator = LocalNavigator.current
        ProductDetailScreen(
            productId = route.productId,
            onBack = navigator::goBack,
            onCartClick = { navigator.navigate(Screen.Cart) },
        )
    }

    navigation<Screen.Cart>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        CartScreen(
            onBack = navigator::goBack,
            onCheckout = { navigator.navigate(Screen.Checkout) },
            onContinueShopping = navigator::goBack,
        )
    }

    navigation<Screen.Checkout>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current

        CheckoutScreen(
            onBack = navigator::goBack,
            onOrderComplete = { orderId, total ->
                navigator.navigate(Screen.OrderSuccess(orderId = orderId, total = total))
            },
            onContinueShopping = {
                navigator.clearAndGoTo(Screen.Home)
                navigator.navigate(Screen.Shop)
            },
        )
    }

    navigation<Screen.OrderSuccess>(metadata = overlayNavigationMetadata()) { route ->
        val navigator = LocalNavigator.current
        OrderSuccessScreen(
            orderId = route.orderId,
            total = route.total,
            onDone = { navigator.clearAndGoTo(Screen.Home) },
            onContinueShopping = {
                navigator.clearAndGoTo(Screen.Home)
                navigator.navigate(Screen.Shop)
            },
        )
    }
}
