package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey94

@Composable
fun ProfileStatsCard(
    listed: Int,
    sold: Int,
    followers: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
