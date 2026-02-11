package com.group8.comp2300.di

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.shop.ProductDetailScreen
import com.group8.comp2300.presentation.screens.shop.ShopScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class)
val shopNavigationModule = module {
    navigation<Screen.Shop>(metadata = ListDetailSceneStrategy.listPane()) {
        _root_ide_package_.com.group8.comp2300.presentation.screens.shop.ShopScreen()
    }

    navigation<Screen.ProductDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.shop.ProductDetailScreen(
            productId = route.productId,
            onBack = navigator::goBack
        )
    }
}
