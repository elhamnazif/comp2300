package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory

val sampleProducts =
    listOf(
        Product(
            "1",
            "HIV Self-Test",
            "Results in 15 mins",
            0.0,
            ProductCategory.TESTING,
            true,
            "/images/shop/icare1_hiv_test_wholeblood.png",
        ),
        Product(
            "2",
            "PrEP Refill",
            "3-month supply",
            0.0,
            ProductCategory.MEDICATION,
            true,
            "/images/shop/prep1_generic_tenoem.png",
        ),
        Product(
            "3",
            "Full STI Panel",
            "Lab kit for 5 STIs",
            45.0,
            ProductCategory.TESTING,
            false,
            "/images/shop/icare4_full_test_kit.png",
        ),
        Product(
            "4",
            "DoxyPEP",
            "Post-exposure meds",
            20.0,
            ProductCategory.MEDICATION,
            false,
            "/images/shop/doxypep.png",
        ),
        Product(
            "5",
            "Premium Condoms",
            "Pack of 12",
            12.0,
            ProductCategory.PREVENTION,
            false,
            "/images/shop/condom1_durex_classic.png",
        ),
        Product("6", "Lube", "Water-based", 8.0, ProductCategory.PREVENTION, false, "/images/shop/lube1_durex50ml.png"),
    )
