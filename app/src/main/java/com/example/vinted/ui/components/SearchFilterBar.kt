package com.example.vinted.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91

@Composable
fun SearchFilterBar(
    sortLabel: String,
    sortActive: Boolean,
    filterCount: Int,
    itemCount: Int,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Grey91, RoundedCornerShape(0.dp))
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterPillChip(label = sortLabel, selected = sortActive, onClick = onSortClick)
        FilterPillChip(
            label = if (filterCount > 0) "Filter · $filterCount  ✕" else "Filter",
            selected = filterCount > 0,
            onClick = onFilterClick,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "$itemCount items found", fontSize = 11.sp, color = Grey57)
    }
}
