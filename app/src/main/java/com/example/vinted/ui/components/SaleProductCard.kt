package com.example.vinted.ui.components
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.VinderAzure

private val cardShape = RoundedCornerShape(10.dp)
private val badgeShape = RoundedCornerShape(4.dp)

@Composable
fun SaleProductCard(
    product: Product,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(cardShape)
            .background(SurfaceColor)
            .border(1.dp, Grey91, cardShape)
            .clickable(onClick = onClick),
    ) {
        SaleCardImage(discountPercent = product.discountPercent)
        SaleCardInfo(product = product)
    }
}

@Composable
private fun SaleCardImage(discountPercent: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Grey91),
    ) {
        if (discountPercent != null) {
            DiscountBadge(
                percent = discountPercent,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun SaleCardInfo(product: Product) {
    Column(modifier = Modifier.padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "€%.0f".format(product.price),
                fontSize = 13.sp,
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = product.name,
            fontSize = 11.sp,
            color = Grey11,
        )
    }
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(badgeShape)
            .background(VinderAzure)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = "−$percent%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
