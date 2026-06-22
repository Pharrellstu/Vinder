package com.example.vinted.ui.components
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey82
import com.example.vinted.ui.theme.VinderAzure

private val pillShape = RoundedCornerShape(999.dp)

@Composable
fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) VinderAzure else SurfaceColor
    val textColor = if (selected) Color.White else Grey36
    val borderColor = if (selected) VinderAzure else Grey82
    Box(
        modifier = modifier
            .clip(pillShape)
            .background(bg)
            .border(1.dp, borderColor, pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 11.sp, color = textColor)
    }
}
