package com.valsi.invoicesystem.data

import com.valsi.invoicesystem.R
import com.valsi.invoicesystem.data.entity.Product

/**
 * First-launch seed content. The 19 products are fixed; prices start at 0.00 and are edited
 * later from the Products screen. All share the placeholder drawable for now.
 */
object SeedData {

    val PRODUCT_NAMES: List<String> = listOf(
        "Punjabi Samosa",
        "Cocktail Samosa",
        "Bhaji",
        "Plain Paratha",
        "Aloo Paratha",
        "Lachha Paratha",
        "Layered Paratha",
        "Malabar Paratha",
        "Phulka Roti",
        "Thepla",
        "Gujarati Mix",
        "Valsi Mixture",
        "Hot Mixture",
        "Nadiyadi Mix",
        "Masala Puri",
        "Mini Bhakarwadi",
        "Butter Ratlami Sev",
        "Tam Tam",
        "Soya Stick",
    )

    fun products(): List<Product> = PRODUCT_NAMES.map { name ->
        Product(
            name = name,
            imageResId = R.drawable.ic_product_placeholder,
            currentPrice = 0.0,
            unit = "pack",
            isActive = true,
        )
    }
}
