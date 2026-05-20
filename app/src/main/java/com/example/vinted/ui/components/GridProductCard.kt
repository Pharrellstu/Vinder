package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey94
import com.example.vinted.ui.theme.VinderAzure

private val cardShape = RoundedCornerShape(10.dp)

@Composable
fun GridProductCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Grey91, cardShape),
    ) {
        ProductImageSection(product = product)
        ProductInfoSection(product = product)
    }
}

@Composable
private fun ProductImageSection(product: Product) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
            .background(Grey91),
    ) {
        FavoriteButton(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        if (product.discountPercent != null) {
            DiscountBadge(
                percent = product.discountPercent,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            )
        }
    }
}

@Composable
private fun FavoriteButton(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = "Save item",
            tint = Grey11,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun ProductInfoSection(product: Product) {
    Column(modifier = Modifier.padding(10.dp)) {
        PriceRow(product = product)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = product.name,
            fontSize = 12.sp,
            color = Grey11,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (product.size != null || product.brand != null) {
            Spacer(modifier = Modifier.height(2.dp))
            SizeBrandRow(size = product.size, brand = product.brand)
        }
        Spacer(modifier = Modifier.height(7.dp))
        HorizontalDivider(color = Grey94)
        Spacer(modifier = Modifier.height(7.dp))
        SellerRow(product = product)
    }
}

@Composable
private fun PriceRow(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "€%.0f".format(product.price),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Grey11,
        )
        if (product.originalPrice != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "€%.0f".format(product.originalPrice),
                fontSize = 11.sp,
                color = Grey57,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun SizeBrandRow(size: String?, brand: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (size != null) {
            Text(text = size, fontSize = 11.sp, color = Grey57)
        }
        if (size != null && brand != null) {
            Text(text = "·", fontSize = 11.sp, color = Grey57.copy(alpha = 0.5f))
        }
        if (brand != null) {
            Text(text = brand, fontSize = 11.sp, color = Grey57)
        }
    }
}

@Composable
private fun SellerRow(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SellerAvatar(initial = product.sellerInitial)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = product.sellerName,
            fontSize = 10.sp,
            color = Grey36,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "★${product.rating}",
            fontSize = 9.sp,
            color = Grey57,
        )
    }
}

@Composable
private fun SellerAvatar(initial: String) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(VinderAzure),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
