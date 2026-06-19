package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.vinted.ui.models.ListingItem

@Composable
fun ProfileListingGrid(
    listings: List<ListingItem>,
    modifier: Modifier = Modifier,
    onListingClick: (ListingItem) -> Unit = {},
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listings.chunked(3).forEach { row ->
            ListingRow(items = row, onListingClick = onListingClick)
        }
    }
}

@Composable
private fun ListingRow(items: List<ListingItem>, onListingClick: (ListingItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            ListingCell(
                item = item,
                onClick = { onListingClick(item) },
                modifier = Modifier.weight(1f),
            )
        }
        repeat(3 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ListingCell(item: ListingItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(item.bgColor)
            .clickable(onClick = onClick),
    ) {
        if (item.coverUrl != null) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        PriceBadge(
            price = item.price,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
        )
    }
}

@Composable
private fun PriceBadge(price: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 5.dp, vertical = 3.dp),
    ) {
        Text(
            text = "€$price",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
