package com.example.vinted.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.data.SessionManager
import com.example.vinted.ui.models.AddProductFormOptions
import com.example.vinted.ui.models.AddProductUiState
import com.example.vinted.ui.models.AddProductViewModel
import com.example.vinted.ui.models.Product
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey94
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VinderAzureLight
import com.example.vinted.ui.theme.VintedTheme
import kotlinx.coroutines.launch

private const val MAX_PHOTOS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit = {},
    onPosted: (Product) -> Unit = {},
    viewModel: AddProductViewModel = viewModel(),
) {
    var currentStep by remember { mutableStateOf(1) }
    val selectedPhotos = remember { mutableStateListOf<Uri>() }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val formOptions by viewModel.formOptions.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddProductUiState.Submitted -> {
                // Build the Product for the just-created listing before clearing the
                // form, then navigate straight to its detail page.
                val postedProduct = Product(
                    id = state.itemId.toString(),
                    name = title,
                    price = price.toFloatOrNull() ?: 0f,
                    sellerInitial = "",
                    sellerName = "",
                    rating = 0f,
                    sellerId = SessionManager.currentAccountId,
                )
                currentStep = 1
                selectedPhotos.clear()
                title = ""; description = ""; price = ""; category = ""; condition = ""
                viewModel.resetState()
                onPosted(postedProduct)
            }
            is AddProductUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Product",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = Grey11,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = VinderAzure,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Grey97,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            StepProgressIndicator(currentStep = currentStep)
            HorizontalDivider(color = Grey91, thickness = 1.dp)

            when (currentStep) {
                1 -> PhotosStep(
                    selectedPhotos = selectedPhotos,
                    onNext = { currentStep = 2 },
                    onPermissionDenied = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                )
                2 -> DetailsStep(
                    formOptions = formOptions,
                    title = title,
                    onTitleChange = { title = it.take(60) },
                    description = description,
                    onDescriptionChange = { description = it },
                    price = price,
                    onPriceChange = { price = it },
                    category = category,
                    onCategoryChange = { category = it },
                    condition = condition,
                    onConditionChange = { condition = it },
                    onNext = { currentStep = 3 },
                )
                3 -> ConfirmationStep(
                    selectedPhotos = selectedPhotos,
                    title = title,
                    description = description,
                    price = price,
                    category = category,
                    condition = condition,
                    isLoading = uiState is AddProductUiState.Uploading,
                    onEdit = { currentStep = 2 },
                    onPost = {
                        viewModel.postListing(
                            context = context,
                            photoUris = selectedPhotos.toList(),
                            title = title,
                            description = description,
                            price = price,
                            category = category,
                            condition = condition,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(currentStep: Int) {
    val stepLabels = listOf("Photos", "Details", "Confirm")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stepLabels.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isCompleted = stepNumber < currentStep
            val isActive = stepNumber == currentStep

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isActive || isCompleted) VinderAzure else Grey94)
                        .border(
                            width = if (isActive || isCompleted) 0.dp else 1.dp,
                            color = Grey91,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else Grey57,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive || isCompleted) VinderAzure else Grey57,
                )
            }

            if (index < stepLabels.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 18.dp)
                        .background(if (stepNumber < currentStep) VinderAzure else Grey91),
                )
            }
        }
    }
}

@Composable
private fun PhotosStep(
    selectedPhotos: MutableList<Uri>,
    onNext: () -> Unit,
    onPermissionDenied: (String) -> Unit,
) {
    val context = LocalContext.current
    var showSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS),
    ) { uris ->
        val remaining = MAX_PHOTOS - selectedPhotos.size
        selectedPhotos.addAll(uris.take(remaining))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToCache(context, bitmap)
            if (uri != null && selectedPhotos.size < MAX_PHOTOS) {
                selectedPhotos.add(uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else onPermissionDenied("Camera permission is required to take photos.")
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else onPermissionDenied("Storage permission is required to access photos.")
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Add Photo", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoSourceButton(
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = VinderAzure) },
                        label = "Take a photo",
                        onClick = {
                            showSourceDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                    )
                    PhotoSourceButton(
                        icon = { Icon(Icons.Default.Image, contentDescription = null, tint = VinderAzure) },
                        label = "Choose from gallery",
                        onClick = {
                            showSourceDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancel") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Add photos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        Spacer(Modifier.height(4.dp))
        Text(
            "Up to $MAX_PHOTOS photos. First photo is your cover image.",
            fontSize = 13.sp,
            color = Grey57,
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(selectedPhotos) { index, uri ->
                PhotoTile(uri = uri, isCover = index == 0) {
                    selectedPhotos.removeAt(index)
                }
            }
            if (selectedPhotos.size < MAX_PHOTOS) {
                item {
                    AddPhotoTile(onClick = { showSourceDialog = true })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Next",
            enabled = selectedPhotos.isNotEmpty(),
            onClick = onNext,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PhotoTile(uri: Uri, isCover: Boolean, modifier: Modifier = Modifier, onRemove: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(
                BorderStroke(if (isCover) 2.dp else 0.dp, VinderAzure),
                RoundedCornerShape(10.dp),
            ),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isCover) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(VinderAzure)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("Cover", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.5.dp, Grey91), RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Add photo", tint = VinderAzure, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text("Add photo", fontSize = 11.sp, color = VinderAzure)
        }
    }
}

@Composable
private fun PhotoSourceButton(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, Grey91), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(label, fontSize = 14.sp, color = Grey11)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsStep(
    formOptions: AddProductFormOptions,
    title: String, onTitleChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    category: String, onCategoryChange: (String) -> Unit,
    condition: String, onConditionChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    // Categories/conditions come straight from the DB, so the step waits for them
    // before letting the user pick — picking a stale label would fail the lookup.
    when (formOptions) {
        is AddProductFormOptions.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VinderAzure)
            }
            return
        }
        is AddProductFormOptions.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(formOptions.message, fontSize = 14.sp, color = Grey57)
            }
            return
        }
        is AddProductFormOptions.Loaded -> Unit
    }

    val categories = formOptions.categories
    val conditions = formOptions.conditions
    val isNextEnabled = title.isNotBlank() && description.isNotBlank() && price.isNotBlank() &&
        category.isNotBlank() && condition.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Details", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        Spacer(Modifier.height(16.dp))

        SectionLabel("Title")
        Spacer(Modifier.height(6.dp))
        VinderTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "e.g. Linen midi dress, size M",
        )
        Text("${title.length}/60", fontSize = 11.sp, color = Grey57, modifier = Modifier.align(Alignment.End))

        Spacer(Modifier.height(14.dp))
        SectionLabel("Description")
        Spacer(Modifier.height(6.dp))
        VinderTextField(
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = "Describe condition, fit, or anything the buyer should know…",
            minLines = 4,
            maxLines = 8,
        )

        Spacer(Modifier.height(14.dp))
        SectionLabel("Price")
        Spacer(Modifier.height(6.dp))
        VinderTextField(
            value = price,
            onValueChange = { onPriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal,
            prefix = { Text("€", color = Grey57, fontWeight = FontWeight.SemiBold) },
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Category")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { cat ->
                SelectableChip(label = cat, selected = category == cat, onSelect = { onCategoryChange(cat) })
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Condition")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            conditions.forEach { cond ->
                SelectableChip(label = cond, selected = condition == cond, onSelect = { onConditionChange(cond) })
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Next", enabled = isNextEnabled, onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConfirmationStep(
    selectedPhotos: List<Uri>,
    title: String,
    description: String,
    price: String,
    category: String,
    condition: String,
    isLoading: Boolean = false,
    onEdit: () -> Unit,
    onPost: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Review your listing", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        Spacer(Modifier.height(4.dp))
        Text("Everything looks good? Go ahead and post it.", fontSize = 13.sp, color = Grey57)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(16.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedPhotos.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
                Spacer(Modifier.height(4.dp))
                Text("€$price", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VinderAzure)

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (category.isNotBlank()) {
                        InfoChip(label = category)
                    }
                    if (condition.isNotBlank()) {
                        InfoChip(label = condition)
                    }
                }

                if (description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Grey91)
                    Spacer(Modifier.height(12.dp))
                    Text(description, fontSize = 13.sp, color = Grey11, lineHeight = 19.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = if (isLoading) "Posting…" else "Post Listing",
            enabled = !isLoading,
            onClick = onPost,
        )
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Edit details", color = VinderAzure, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
}

@Composable
private fun VinderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Grey57) },
        minLines = minLines,
        maxLines = maxLines,
        prefix = prefix,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VinderAzure,
            unfocusedBorderColor = Grey91,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VinderAzureLight,
            selectedLabelColor = VinderAzure,
            containerColor = Color.White,
            labelColor = Grey11,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Grey91,
            selectedBorderColor = VinderAzure,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

@Composable
private fun InfoChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Grey94)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 12.sp, color = Grey57)
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VinderAzure,
            contentColor = Color.White,
            disabledContainerColor = Grey91,
            disabledContentColor = Grey57,
        ),
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = java.io.File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddProductScreenPreview() {
    VintedTheme {
        AddProductScreen()
    }
}
