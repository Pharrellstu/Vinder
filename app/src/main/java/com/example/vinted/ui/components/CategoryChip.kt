package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey95
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.inter

private val chipShape = RoundedCornerShape(999.dp)

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) VinderAzure else Grey95
    val textColor = if (selected) Color.White else Grey11

    Text(
        text = label,
        color = textColor,
        fontSize = 13.sp,
        fontFamily = inter,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(chipShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
