package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.components.BottomNavBar
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey94
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private data class ListingItem(val price: Int, val bgColor: Color)

private val sampleListings = listOf(
    ListingItem(28, Color(0xFFE5E5EA)),
    ListingItem(65, Color(0xFFE4E8DE)),
    ListingItem(19, Color(0xFFE5E5EA)),
    ListingItem(34, Color(0xFFE5E5EA)),
    ListingItem(42, Color(0xFFDCE6EA)),
    ListingItem(38, Color(0xFFF1E5D5)),
)

private val profileTabs = listOf("Listings", "Sold", "Reviews")

@Composable
fun ProfileScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = { BottomNavBar(selectedIndex = 4) },
        containerColor = Grey97,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { ProfileHeader() }
            item {
                ProfileStatsCard(listed = 18, sold = 47, followers = 312)
            }
            item { ProfileActionButtons() }
            item {
                ProfileTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            }
            if (selectedTab == 0) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
                items(sampleListings.chunked(3)) { chunk ->
                    ListingGridRow(items = chunk)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SettingsButton()
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProfileAvatar(initial = "Y")
        Text(
            text = "your.handle",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Grey11,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("★ 4.9 · 142 reviews", fontSize = 13.sp, color = Grey57)
            Text("·", fontSize = 13.sp, color = Grey57.copy(alpha = 0.5f))
            Text("Berlin", fontSize = 13.sp, color = Grey57)
        }
        Text(
            text = "Decluttering my closet — mostly minimalist staples and vintage finds. Smoke-free home 🤍",
            fontSize = 13.sp,
            color = Grey36,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 7.dp, bottom = 15.dp),
        )
    }
}

@Composable
private fun SettingsButton() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(elevation = 1.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
            .clip(CircleShape)
            .background(Color.White)
            .clickable {}
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Settings",
            tint = Grey11,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ProfileAvatar(initial: String) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Grey97)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color(0xFF9B6D7A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun ProfileStatsCard(listed: Int, sold: Int, followers: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Grey91, RoundedCornerShape(12.dp))
            .padding(vertical = 15.dp, horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCell(value = listed.toString(), label = "Listed", modifier = Modifier.weight(1f))
        VerticalDivider(modifier = Modifier.height(37.dp), color = Grey94)
        StatCell(value = sold.toString(), label = "Sold", modifier = Modifier.weight(1f))
        VerticalDivider(modifier = Modifier.height(37.dp), color = Grey94)
        StatCell(value = followers.toString(), label = "Followers", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Grey11)
        Text(text = label, fontSize = 11.sp, color = Grey57)
    }
}

@Composable
private fun ProfileActionButtons() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            label = "Edit profile",
            bgColor = Color.White,
            textColor = Color.Black,
            borderColor = Grey91,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            label = "Share profile",
            bgColor = VinderAzure,
            textColor = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {}
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

@Composable
private fun ProfileTabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val grey91 = Grey91
    val azure = VinderAzure
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = grey91,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            profileTabs.forEachIndexed { index, label ->
                val isSelected = index == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .drawBehind {
                            if (isSelected) {
                                drawLine(
                                    color = azure,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) VinderAzure else Grey57,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingGridRow(items: List<ListingItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            ListingCell(item = item, modifier = Modifier.weight(1f))
        }
        repeat(3 - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ListingCell(item: ListingItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(item.bgColor),
    ) {
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    VintedTheme {
        ProfileScreen()
    }
}
