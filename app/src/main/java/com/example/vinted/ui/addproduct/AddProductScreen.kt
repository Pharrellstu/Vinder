package com.example.vinted.ui.addproduct

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.vinted.ui.theme.*
import java.io.File

// ─────────────────────────────────────────────────────────────
// Entry point — manages which step is visible
// ─────────────────────────────────────────────────────────────

@Composable
fun AddProductScreen(onClose: () -> Unit = {}) {
    var step by remember { mutableIntStateOf(1) }

    when (step) {
        1 -> AddProductPhotosStep(
            onBack = onClose,
            onContinue = { step = 2 }
        )
        2 -> AddProductPriceDescriptionStep(
            onBack = { step = 1 },
            onContinue = { step = 3 }
        )
        3 -> AddProductConfirmationStep(
            onBack = { step = 2 },
            onPublish = onClose
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Shared UI helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Text(
        text = "$current / $total",
        fontSize = 13.sp,
        color = TextSecondary,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductTopBar(
    title: String,
    step: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BrandPrimary
                    )
                }
            },
            actions = {
                StepIndicator(current = step, total = totalSteps)
                Spacer(Modifier.width(16.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
        )
        LinearProgressIndicator(
            progress = { step / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = BrandPrimary,
            trackColor = DividerColor
        )
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimary,
            disabledContainerColor = BrandSecondary.copy(alpha = 0.5f)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// Step 1 — Photos
// ─────────────────────────────────────────────────────────────

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
    val file = File.createTempFile("photo_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun AddProductPhotosStep(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val context = LocalContext.current
    var photoSlots by remember { mutableStateOf(List<Uri?>(8) { null }) }
    val addedCount = photoSlots.count { it != null }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSlotIndex by remember { mutableIntStateOf(0) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            pendingCameraUri?.let { uri ->
                photoSlots = photoSlots.toMutableList().also { it[pendingSlotIndex] = uri }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { photoSlots = photoSlots.toMutableList().also { it[pendingSlotIndex] = uri } }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera(slotIndex: Int) {
        pendingSlotIndex = slotIndex
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchGallery(slotIndex: Int) {
        pendingSlotIndex = slotIndex
        galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AddProductTopBar("Add Photos", 1, 3, onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { launchCamera(photoSlots.indexOfFirst { it == null }.takeIf { it >= 0 } ?: 0) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                    OutlinedButton(
                        onClick = { launchGallery(photoSlots.indexOfFirst { it == null }.takeIf { it >= 0 } ?: 0) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary)
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(text = "Continue", enabled = addedCount > 0, onClick = onContinue)
                Spacer(Modifier.height(16.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Add up to 8 photos. The first photo will be your cover image.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(photoSlots) { index, uri ->
                    PhotoSlot(
                        index = index,
                        uri = uri,
                        isCover = index == 0,
                        onTapEmpty = { launchGallery(index) },
                        onRemove = { photoSlots = photoSlots.toMutableList().also { it[index] = null } }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSlot(
    index: Int,
    uri: Uri?,
    isCover: Boolean,
    onTapEmpty: () -> Unit,
    onRemove: () -> Unit
) {
    val filled = uri != null
    Box(
        modifier = Modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (filled) 0.dp else 1.5.dp,
                color = if (filled) Color.Transparent else DividerColor,
                shape = RoundedCornerShape(10.dp)
            )
            .background(if (filled) BrandSecondary.copy(alpha = 0.25f) else SurfaceWhite)
            .clickable { if (!filled) onTapEmpty() },
        contentAlignment = Alignment.Center
    ) {
        if (filled) {
            AsyncImage(
                model = uri,
                contentDescription = "Photo $index",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(TextPrimary.copy(alpha = 0.6f))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
            }
            if (isCover) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(BrandPrimary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Cover", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add photo",
                    tint = if (index == 0) BrandPrimary else TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
                if (index == 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("Add cover", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Step 2 — Price + Description
// ─────────────────────────────────────────────────────────────

private val conditions = listOf("New with tags", "Like new", "Good", "Fair")

@Composable
fun AddProductPriceDescriptionStep(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val canContinue = title.isNotBlank() && price.isNotBlank() && selectedCondition.isNotBlank()

    Scaffold(
        containerColor = AppBackground,
        topBar = { AddProductTopBar("Details", 2, 3, onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(12.dp))
                PrimaryButton(text = "Continue", enabled = canContinue, onClick = onContinue)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Price field — prominent at the top
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite, tonalElevation = 0.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Price", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.00", color = TextSecondary) },
                        prefix = { Text("€ ", fontWeight = FontWeight.Bold, color = TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = priceFieldColors()
                    )
                }
            }

            // Item details card
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Item details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)

                    FormField(label = "Title *", value = title, onValueChange = { title = it }, placeholder = "e.g. Blue denim jacket")
                    HorizontalDivider(color = DividerColor)
                    FormField(label = "Brand", value = brand, onValueChange = { brand = it }, placeholder = "e.g. Zara")
                    HorizontalDivider(color = DividerColor)
                    FormField(label = "Size", value = size, onValueChange = { size = it }, placeholder = "e.g. M / 38")
                }
            }

            // Condition selector
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Condition *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        conditions.forEach { condition ->
                            val selected = condition == selectedCondition
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCondition = condition },
                                label = {
                                    Text(
                                        text = condition,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = AppBackground,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = DividerColor,
                                    selectedBorderColor = BrandPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Description card
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Describe your item — condition, measurements, any flaws…", color = TextSecondary, fontSize = 13.sp) },
                        maxLines = 10,
                        shape = RoundedCornerShape(8.dp),
                        colors = priceFieldColors()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${description.length} / 500",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicFormInput(value = value, onValueChange = onValueChange, placeholder = placeholder)
    }
}

@Composable
private fun BasicFormInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = priceFieldColors()
    )
}

@Composable
private fun priceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = BrandPrimary,
    cursorColor = BrandPrimary
)

// ─────────────────────────────────────────────────────────────
// Step 3 — Confirmation
// ─────────────────────────────────────────────────────────────

@Composable
fun AddProductConfirmationStep(
    onBack: () -> Unit = {},
    onPublish: () -> Unit = {}
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = { AddProductTopBar("Review Listing", 3, 3, onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(2.dp))
                PrimaryButton(text = "Publish Listing", onClick = onPublish)
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary)
                ) {
                    Text("Save as Draft", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Listing preview card
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column {
                    // Photo preview placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(BrandSecondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .background(BrandPrimary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Cover", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Blue Denim Jacket", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("€ 25.00", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BrandPrimary)
                    }
                }
            }

            // Details summary card
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Listing Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    ConfirmationRow("Brand", "Zara")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                    ConfirmationRow("Size", "M / 38")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                    ConfirmationRow("Condition", "Like new")
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 10.dp))
                    ConfirmationRow("Photos", "3 photos added")
                }
            }

            // Description summary card
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceWhite) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Classic blue denim jacket, worn a handful of times. No visible wear or damage. Perfect for layering.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // Ready-to-publish notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandSecondary.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Your listing looks great! Tap \"Publish\" to make it visible to buyers.",
                        fontSize = 13.sp,
                        color = BrandPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConfirmationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Step 1 – Photos")
@Composable
fun PreviewPhotosStep() {
    VintedTheme { AddProductPhotosStep() }
}

@Preview(showBackground = true, name = "Step 2 – Price & Description")
@Composable
fun PreviewPriceDescriptionStep() {
    VintedTheme { AddProductPriceDescriptionStep() }
}

@Preview(showBackground = true, name = "Step 3 – Confirmation")
@Composable
fun PreviewConfirmationStep() {
    VintedTheme { AddProductConfirmationStep() }
}
