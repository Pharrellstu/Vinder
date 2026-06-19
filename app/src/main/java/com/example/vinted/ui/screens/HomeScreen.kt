package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.components.CategoryChip
import com.example.vinted.ui.components.GridProductCard
import com.example.vinted.ui.components.HeroBanner
import com.example.vinted.ui.components.SaleProductCard
import com.example.vinted.ui.models.HomeUiState
import com.example.vinted.ui.models.HomeViewModel
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTabSelected: (Int) -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF5F6F8),
        bottomBar = { BottomNavBar(selectedIndex = 0, onItemSelected = onTabSelected) },
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Grey57, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.load() }) {
                            Text("Retry", color = VinderAzure)
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val showBackToTop by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 3 }
                }
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        VinderTopBar()
                        CategoryFilterRow(
                            categories = state.categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                        )
                        LazyColumn(state = listState) {
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            item { HeroBanner(onSellClick = {}) }
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                            if (state.saleItems.isNotEmpty()) {
                                item { SectionHeader(title = "On sale today", onSeeAll = {}) }
                                item { SaleProductsRow(products = state.saleItems, onProductClick = onProductClick) }
                                item { Spacer(modifier = Modifier.height(20.dp)) }
                            }
                            item { LatestFindsHeader(itemCount = state.gridItems.size) }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                            items(state.gridItems.chunked(2)) { row ->
                                ProductGridRow(
                                    products = row,
                                    onProductClick = onProductClick,
                                    favoritedIds = state.favoritedIds,
                                    onToggleFavorite = { product ->
                                        viewModel.toggleFavorite(
                                            itemId = product.id.toInt(),
                                            currentlyFavorited = product.id.toInt() in state.favoritedIds,
                                        )
                                    },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                    if (showBackToTop) {
                        BackToTopButton(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackToTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(VinderAzure)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Back to top",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
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
private fun ProductGridRow(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    favoritedIds: Set<Int> = emptySet(),
    onToggleFavorite: (Product) -> Unit = {},
) {
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
                isFavorited = product.id.toIntOrNull() in favoritedIds,
                onToggleFavorite = { onToggleFavorite(product) },
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