package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VinderAzureLight

private val bannerShape = RoundedCornerShape(16.dp)
private val cardShape = RoundedCornerShape(10.dp)

@Composable
fun HeroBanner(
    onSellClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(VinderAzureLight, Color.White, VinderAzureLight),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(bannerShape)
            .background(gradient)
            .border(1.dp, Grey91, bannerShape)
            .padding(horizontal = 19.dp, vertical = 21.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BannerContent(onSellClick = onSellClick, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        StackedProductImages()
    }
}

@Composable
private fun BannerContent(onSellClick: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SpringDropBadge()
        BannerHeading()
        Text(
            text = "List in 90 seconds.",
            fontSize = 13.sp,
            color = Grey36,
        )
        SellButton(onClick = onSellClick)
    }
}

@Composable
private fun SpringDropBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "◇ SPRING DROP",
            color = VinderAzure,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun BannerHeading() {
    Text(
        text = buildAnnotatedString {
            append("Ready to ")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = VinderAzure)) {
                append("declutter?")
            }
        },
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 29.4.sp,
        letterSpacing = (-0.56).sp,
    )
}

@Composable
private fun SellButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(VinderAzure)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Sell something →",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StackedProductImages() {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(110.dp),
        contentAlignment = Alignment.Center,
    ) {
        ProductImagePlaceholder(rotationDegrees = -4f, modifier = Modifier.align(Alignment.TopStart))
        ProductImagePlaceholder(rotationDegrees = 6f, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun ProductImagePlaceholder(rotationDegrees: Float, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(width = 72.dp, height = 96.dp)
            .rotate(rotationDegrees)
            .clip(cardShape)
            .background(Grey91),
    )
}
