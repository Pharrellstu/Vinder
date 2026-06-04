
package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.components.CategoryChip
import com.example.vinted.ui.components.GridProductCard
import com.example.vinted.ui.components.HeroBanner
import com.example.vinted.ui.components.SaleProductCard
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private val categories = listOf("All", "Women", "Men", "Kids", "Home", "Electronics", "Books", "Sports", "Beauty")

private val sampleSaleProducts = listOf(
    Product("s1", "Linen midi dress", 28f, 45f, 38, "M", "& Other Stories", "L", "lena.k", 4.9f),
    Product("s2", "Silk blouse", 18f, 55f, 67, "S", "Zara", "A", "anna.m", 4.7f),
    Product("s3", "Sony WH-1000XM4", 110f, 350f, 49, null, "Sony", "J", "jake.t", 4.8f),
)

private val sampleGridProducts = listOf(
    Product("g1", "Linen midi dress", 28f, 45f, 38, "M", "& Other Stories", "L", "lena.k", 4.9f),
    Product("g2", "Floral wrap dress", 32f, null, null, "S", "Mango", "S", "sophie.b", 4.6f),
    Product("g3", "Sony WH-1000XM4", 110f, 350f, 49, null, "Sony", "J", "jake.t", 4.8f),
    Product("g4", "Canvas tote bag", 22f, null, null, null, "COS", "M", "maya.r", 5.0f),
    Product("g5", "Wool overshirt", 65f, 120f, 46, "M", "Uniqlo U", "N", "noah.dev", 4.8f),
    Product("g6", "Leather derby shoes", 89f, null, null, "42", "Clarks", "T", "tom.s", 4.5f),
    Product("g7", "Vintage denim jacket", 45f, 90f, 50, "L", "Levi's", "C", "chloe.p", 4.9f),
    Product("g8", "Cashmere sweater", 75f, 200f, 63, "M", "Loro Piana", "E", "emma.w", 5.0f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onProductClick: (Product) -> Unit = {}) {
    var selectedCategory by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {
        VinderTopBar()
        CategoryFilterRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
        )
        LazyColumn {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { HeroBanner(onSellClick = {}) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { SectionHeader(title = "On sale today", onSeeAll = {}) }
            item { SaleProductsRow(products = sampleSaleProducts, onProductClick = onProductClick) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { LatestFindsHeader(itemCount = 24) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            items(sampleGridProducts.chunked(2)) { row ->
                ProductGridRow(products = row, onProductClick = onProductClick)
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VinderTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Vinder",
                color = VinderAzure,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
            )
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Grey11)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Grey11)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(
                label = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Grey91),
    )
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Grey11,
        )
        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
            Text(
                text = "See all",
                fontSize = 12.sp,
                color = VinderAzure,
            )
        }
    }
}

@Composable
private fun LatestFindsHeader(itemCount: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Latest finds",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Grey11,
            letterSpacing = (-0.24).sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$itemCount items",
            fontSize = 12.sp,
            color = Grey57,
        )
    }
}

@Composable
private fun SaleProductsRow(products: List<Product>, onProductClick: (Product) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(products) { product ->
            SaleProductCard(product = product, onClick = { onProductClick(product) })
        }
    }
}

@Composable
private fun ProductGridRow(products: List<Product>, onProductClick: (Product) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        products.forEach { product ->
            GridProductCard(
                product = product,
                onClick = { onProductClick(product) },
                modifier = Modifier.weight(1f),
            )
        }
        if (products.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    VintedTheme {
        HomeScreen()
    }
}
