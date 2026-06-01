package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91

private val searchBgColor = Color(0xFFF0F0F5)
private val pillShape = RoundedCornerShape(999.dp)

@Composable
fun SearchResultsTopBar(
    query: String,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Grey91, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = Grey11,
            modifier = Modifier.size(24.dp).clickable(onClick = onBack),
        )
        SearchPill(query = query, onClear = onClear, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchPill(query: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(35.dp)
            .clip(pillShape)
            .background(searchBgColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Grey57,
            modifier = Modifier.size(14.dp),
        )
        Text(text = query, fontSize = 12.sp, color = Grey11, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Clear",
            tint = Grey57,
            modifier = Modifier.size(16.dp).clickable(onClick = onClear),
        )
    }
}
