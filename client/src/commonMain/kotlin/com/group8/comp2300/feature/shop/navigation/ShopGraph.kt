package com.group8.comp2300.feature.shop.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.shop.ProductDetailScreen
import com.group8.comp2300.feature.shop.ShopScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val shopGraphModule = module {
    navigation<Screen.Shop>(metadata = ListDetailSceneStrategy.listPane()) {
        ShopScreen()
    }

    navigation<Screen.ProductDetail>(metadata = overlayNavigationMetadata(ListDetailSceneStrategy.detailPane())) { route ->
        val navigator = LocalNavigator.current
        ProductDetailScreen(
            productId = route.productId,
            onBack = navigator::goBack,
        )
    }
}
