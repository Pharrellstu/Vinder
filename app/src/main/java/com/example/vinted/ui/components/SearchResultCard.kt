package com.example.vinted.ui.components
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91

private val cardShape = RoundedCornerShape(10.dp)

@Composable
fun SearchResultCard(product: Product, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(SurfaceColor)
            .border(1.dp, Grey91, cardShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .background(Grey91),
            contentAlignment = Alignment.Center,
        ) {
            if (product.coverImageUrl != null) {
                AsyncImage(
                    model = product.coverImageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Grey57,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 8.dp)) {
            Text(
                text = "€%.0f".format(product.price),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Grey11,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.name,
                fontSize = 11.sp,
                color = Grey36,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (product.brand != null || product.size != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(product.size, product.brand).joinToString(" · "),
                    fontSize = 10.sp,
                    color = Grey57,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
