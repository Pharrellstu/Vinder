package com.example.vinted.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.models.Seller
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey94
import com.example.vinted.ui.theme.VinderAmber
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private val cardShape = RoundedCornerShape(12.dp)
private val buttonShape = RoundedCornerShape(12.dp)

/**
 * Seller summary shown on the item detail screen. Tapping the header opens the
 * seller's public profile; the action button starts a chat with the seller.
 */
@Composable
fun SellerProfileCard(
    seller: Seller,
    onViewProfile: () -> Unit = {},
    onMessageSeller: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Grey91, cardShape)
            .padding(14.dp),
    ) {
        SellerHeader(seller = seller, onViewProfile = onViewProfile)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Grey94)
        Spacer(modifier = Modifier.height(12.dp))
        SellerStatsRow(seller = seller)
        Spacer(modifier = Modifier.height(14.dp))
        MessageSellerButton(onClick = onMessageSeller)
    }
}

@Composable
private fun SellerHeader(seller: Seller, onViewProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onViewProfile),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SellerAvatar(initial = seller.initial, size = 44.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = seller.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Grey11,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (seller.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified seller",
                        tint = VinderAzure,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            SellerRatingRow(rating = seller.rating, reviewCount = seller.reviewCount)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Grey57,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SellerRatingRow(rating: Float, reviewCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = VinderAmber,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "%.1f".format(rating),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Grey11,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($reviewCount reviews)",
            fontSize = 12.sp,
            color = Grey57,
        )
    }
}

@Composable
private fun SellerStatsRow(seller: Seller) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SellerStat(icon = Icons.Outlined.Inventory2, text = "${seller.itemCount} items")
        SellerStat(icon = Icons.Outlined.LocationOn, text = seller.location)
        SellerStat(icon = Icons.Outlined.Schedule, text = "Since ${seller.memberSince}")
    }
}

@Composable
private fun SellerStat(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Grey57,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Grey36,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MessageSellerButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = buttonShape,
        border = BorderStroke(1.dp, VinderAzure),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = VinderAzure),
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Message seller", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SellerAvatar(initial: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(VinderAzure),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F6F8)
@Composable
fun SellerProfileCardPreview() {
    VintedTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SellerProfileCard(
                seller = Seller(
                    initial = "L",
                    name = "lena.k",
                    rating = 4.9f,
                    reviewCount = 128,
                    itemCount = 64,
                    location = "Amsterdam",
                    memberSince = "2021",
                    isVerified = true,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            SellerProfileCard(
                seller = Seller(
                    initial = "T",
                    name = "tom.s",
                    rating = 4.5f,
                    reviewCount = 12,
                    itemCount = 8,
                    location = "Rotterdam",
                    memberSince = "2023",
                    isVerified = false,
                ),
            )
        }
    }
}
