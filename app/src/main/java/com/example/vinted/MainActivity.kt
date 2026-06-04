package com.example.vinted

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.models.Seller
import com.example.vinted.ui.screens.HomeScreen
import com.example.vinted.ui.screens.ItemDetailScreen
import com.example.vinted.ui.theme.VintedTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VintedTheme {
                // Hand-rolled navigation (no nav library yet): Home <-> Item detail,
                // same state-driven pattern as the AddProductScreen wizard.
                var openProduct by remember { mutableStateOf<Product?>(null) }
                val selected = openProduct

                if (selected == null) {
                    HomeScreen(onProductClick = { openProduct = it })
                } else {
                    BackHandler { openProduct = null }
                    ItemDetailScreen(
                        product = selected,
                        seller = sellerFor(selected),
                        description = descriptionFor(selected),
                        onBack = { openProduct = null },
                    )
                }
            }
        }
    }
}

private val sampleCities = listOf("Amsterdam", "Rotterdam", "Utrecht", "Eindhoven", "Groningen", "The Hague")

/** Builds a sample [Seller] for a product until a real seller/profile API exists. */
private fun sellerFor(product: Product): Seller {
    val seed = abs(product.id.hashCode())
    return Seller(
        initial = product.sellerInitial,
        name = product.sellerName,
        rating = product.rating,
        reviewCount = 20 + seed % 280,
        itemCount = 5 + seed % 120,
        location = sampleCities[seed % sampleCities.size],
        memberSince = (2019 + seed % 6).toString(),
        isVerified = product.rating >= 4.8f,
    )
}

/** Builds a sample listing description until listings carry their own copy. */
private fun descriptionFor(product: Product): String {
    val brandPart = product.brand?.let { " by $it" } ?: ""
    val sizePart = product.size?.let { " Size $it." } ?: ""
    return "${product.name}$brandPart in great condition.$sizePart " +
        "Barely used, no flaws. From a smoke-free home — fast shipping."
}
