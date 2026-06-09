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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.models.SearchFilters
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.VinderAzure

private val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val categories = listOf("Women", "Men", "Kids", "Home")
private val conditions = listOf("New", "Like new", "Good", "Fair")
private val sizes = listOf("XS", "S", "M", "L", "XL")

@Composable
fun FilterBottomSheet(
    filters: SearchFilters,
    itemCount: Int,
    onFiltersChange: (SearchFilters) -> Unit,
    onReset: () -> Unit,
    onShowResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(Color.White)
            .border(1.dp, Grey91, sheetShape)
            .padding(horizontal = 15.dp)
            .padding(top = 9.dp, bottom = 20.dp),
    ) {
        DragHandle()
        Spacer(modifier = Modifier.height(16.dp))
        FilterSheetHeader(onReset = onReset)
        Spacer(modifier = Modifier.height(20.dp))
        CategorySection(selected = filters.categories) { item ->
            onFiltersChange(filters.copy(categories = filters.categories.toggle(item)))
        }
        Spacer(modifier = Modifier.height(16.dp))
        PriceSection(
            minPrice = filters.minPrice,
            maxPrice = filters.maxPrice,
            onRangeChange = { lo, hi -> onFiltersChange(filters.copy(minPrice = lo, maxPrice = hi)) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        ConditionSection(selected = filters.conditions) { item ->
            onFiltersChange(filters.copy(conditions = filters.conditions.toggle(item)))
        }
        Spacer(modifier = Modifier.height(16.dp))
        SizeSection(selected = filters.sizes) { item ->
            onFiltersChange(filters.copy(sizes = filters.sizes.toggle(item)))
        }
        Spacer(modifier = Modifier.height(20.dp))
        ShowResultsButton(itemCount = itemCount, onClick = onShowResults)
    }
}

@Composable
private fun DragHandle() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFD1D1D6)),
        )
    }
}

@Composable
private fun FilterSheetHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Filters", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        Text(
            text = "Reset",
            fontSize = 13.sp,
            color = VinderAzure,
            modifier = Modifier.clickable(onClick = onReset),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Grey57,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun ChipFlowRow(
    items: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            FilterPillChip(
                label = item,
                selected = item in selected,
                onClick = { onToggle(item) },
            )
        }
    }
}

@Composable
private fun CategorySection(selected: Set<String>, onToggle: (String) -> Unit) {
    SectionLabel("CATEGORY")
    Spacer(modifier = Modifier.height(8.dp))
    ChipFlowRow(items = categories, selected = selected, onToggle = onToggle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceSection(minPrice: Float, maxPrice: Float, onRangeChange: (Float, Float) -> Unit) {
    SectionLabel("PRICE")
    Spacer(modifier = Modifier.height(4.dp))
    RangeSlider(
        value = minPrice..maxPrice,
        onValueChange = { onRangeChange(it.start, it.endInclusive) },
        valueRange = 0f..500f,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = VinderAzure,
            inactiveTrackColor = Grey91,
        ),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "€${minPrice.toInt()}", fontSize = 11.sp, color = Grey57)
        Text(
            text = if (maxPrice >= 500f) "€500+" else "€${maxPrice.toInt()}",
            fontSize = 11.sp,
            color = Grey57,
        )
    }
}

@Composable
private fun ConditionSection(selected: Set<String>, onToggle: (String) -> Unit) {
    SectionLabel("CONDITION")
    Spacer(modifier = Modifier.height(8.dp))
    ChipFlowRow(items = conditions, selected = selected, onToggle = onToggle)
}

@Composable
private fun SizeSection(selected: Set<String>, onToggle: (String) -> Unit) {
    SectionLabel("SIZE")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sizes.forEach { item ->
            Box(
                modifier = Modifier.size(width = 42.dp, height = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                FilterPillChip(
                    label = item,
                    selected = item in selected,
                    onClick = { onToggle(item) },
                )
            }
        }
    }
}

@Composable
private fun ShowResultsButton(itemCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VinderAzure)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Show $itemCount items",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (contains(item)) this - item else this + item
