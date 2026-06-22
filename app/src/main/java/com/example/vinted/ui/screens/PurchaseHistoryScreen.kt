package com.example.vinted.ui.screens
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.vinted.ui.models.PurchaseHistoryItem
import com.example.vinted.ui.models.PurchaseHistoryUiState
import com.example.vinted.ui.models.PurchaseHistoryViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    onBack: () -> Unit = {},
    buyerId: Int? = null,
    viewModel: PurchaseHistoryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PurchaseHistoryViewModel(buyerId = buyerId) as T
        },
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Activity-scoped ViewModels keep their state across re-entry, so reload on each
    // entry to surface purchases made since the screen was last shown.
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Purchase history",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Grey11,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Grey11,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor),
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is PurchaseHistoryUiState.Loading -> {
                CenteredBox(modifier = Modifier.padding(padding)) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }
            is PurchaseHistoryUiState.Error -> {
                CenteredBox(modifier = Modifier.padding(padding)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Grey57, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.load() }) {
                            Text("Retry", color = VinderAzure)
                        }
                    }
                }
            }
            is PurchaseHistoryUiState.Success -> {
                if (state.items.isEmpty()) {
                    EmptyPurchases(modifier = Modifier.padding(padding))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.purchaseId }) { purchase ->
                            PurchaseRow(purchase)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(purchase: PurchaseHistoryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(purchase.photoUrl)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = purchase.itemName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Grey11,
                )
                Text(
                    text = formatPurchaseDate(purchase.purchasedAt),
                    fontSize = 13.sp,
                    color = Grey57,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = formatPrice(purchase.totalAmount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Grey11,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Grey91),
        )
        Spacer(modifier = Modifier.height(10.dp))

        FeeLine(label = "Item", amount = purchase.itemPrice)
        FeeLine(label = "Shipping", amount = purchase.shippingFee)
        FeeLine(label = "Buyer protection", amount = purchase.protectionFee)
    }
}

@Composable
private fun FeeLine(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = Grey57)
        Text(formatPrice(amount), fontSize = 13.sp, color = Grey57)
    }
}

@Composable
private fun Thumbnail(photoUrl: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Grey91),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = Grey57,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun EmptyPurchases(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Grey57,
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No purchases yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
            Text(
                text = "Items you buy will appear here.",
                fontSize = 13.sp,
                color = Grey57,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun formatPrice(amount: Double): String = "£%.2f".format(amount)

// created_at is an ISO-8601 timestamp (e.g. "2026-06-17T10:30:00+00:00"); show just
// the calendar date, which is all a purchase list needs.
private fun formatPurchaseDate(timestamp: String): String =
    timestamp.substringBefore('T').ifBlank { timestamp }
