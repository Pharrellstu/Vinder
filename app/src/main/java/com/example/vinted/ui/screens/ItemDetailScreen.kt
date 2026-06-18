package com.example.vinted.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.vinted.data.SessionManager
import com.example.vinted.ui.components.DiscountBadge
import com.example.vinted.ui.components.SellerProfileCard
import com.example.vinted.ui.models.ItemDetailUiState
import com.example.vinted.ui.models.ItemDetailViewModel
import com.example.vinted.ui.models.ItemDetailViewModelFactory
import com.example.vinted.ui.models.Product
import com.example.vinted.ui.models.Seller
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey36
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey95
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SHIPPING_FEE = ItemDetailViewModel.SHIPPING_FEE
private val BUYER_PROTECTION_FEE = ItemDetailViewModel.BUYER_PROTECTION_FEE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    product: Product,
    onBack: () -> Unit = {},
    onViewSellerProfile: () -> Unit = {},
    onMessageSeller: () -> Unit = {},
    viewModel: ItemDetailViewModel = viewModel(
        key = "item-${product.id}",
        factory = ItemDetailViewModelFactory(product.id.toInt(), product.sellerId),
    ),
) {
    val detailState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isFavourite by remember { mutableStateOf(false) }
    var showOfferSheet by remember { mutableStateOf(false) }
    var showBuySheet by remember { mutableStateOf(false) }

    // A seller can reach their own item's detail page (the feed returns all listed
    // items). Buying/offering on your own listing makes no sense, so suppress the
    // purchase actions when the viewer is the seller.
    val isOwnListing = product.sellerId == SessionManager.currentAccountId

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val photoUrls = (detailState as? ItemDetailUiState.Success)?.photoUrls ?: emptyList()
    val description = (detailState as? ItemDetailUiState.Success)?.description ?: ""
    val seller = (detailState as? ItemDetailUiState.Success)?.seller

    Scaffold(
        containerColor = Grey97,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ItemDetailTopBar(
                isFavourite = isFavourite,
                onToggleFavourite = {
                    isFavourite = !isFavourite
                    notify(if (isFavourite) "Saved to favourites" else "Removed from favourites")
                },
                onBack = onBack,
            )
        },
        bottomBar = {
            if (isOwnListing) {
                OwnListingBottomBar()
            } else {
                ItemDetailBottomBar(
                    onMakeOffer = { showOfferSheet = true },
                    onBuyNow = { showBuySheet = true },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ItemPhotoGallery(photoUrls = photoUrls)
            Column(modifier = Modifier.padding(16.dp)) {
                ItemPriceRow(product = product)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Grey11,
                )
                if (product.size != null || product.brand != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ItemSpecRow(product = product)
                }
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionTitle(text = "Description")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Grey36,
                        lineHeight = 19.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(text = "Seller")
                Spacer(modifier = Modifier.height(8.dp))
                if (seller != null) {
                    SellerProfileCard(
                        seller = seller,
                        onViewProfile = onViewSellerProfile,
                        onMessageSeller = onMessageSeller,
                    )
                } else if (detailState is ItemDetailUiState.Error) {
                    Text(
                        text = (detailState as ItemDetailUiState.Error).message,
                        color = Grey57,
                        fontSize = 13.sp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(Grey95, RoundedCornerShape(12.dp)),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showOfferSheet) {
        MakeOfferSheet(
            product = product,
            onDismiss = { showOfferSheet = false },
            onSubmit = { amount ->
                showOfferSheet = false
                viewModel.submitOffer(
                    offerPrice = amount.toDouble(),
                    onSuccess = { notify("Offer of €$amount sent!") },
                    onError = { notify(it) },
                )
            },
        )
    }
    if (showBuySheet) {
        BuyNowSheet(
            product = product,
            onDismiss = { showBuySheet = false },
            onConfirm = {
                showBuySheet = false
                viewModel.confirmBuy(
                    sellerId = product.sellerId,
                    itemPrice = product.price.toDouble(),
                    onSuccess = { onBack() },
                    onError = { notify(it) },
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailTopBar(
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = "Details", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Grey11)
            }
        },
        actions = {
            IconButton(onClick = onToggleFavourite) {
                if (isFavourite) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Saved", tint = VinderAzure)
                } else {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Save item", tint = Grey11)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
    )
}

@Composable
private fun ItemPhotoGallery(photoUrls: List<String>) {
    val pageCount = photoUrls.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Grey91),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val url = photoUrls.getOrNull(page)
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Grey91),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Grey57,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
        if (photoUrls.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            PagerDots(
                count = photoUrls.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) VinderAzure else Color.White.copy(alpha = 0.7f)),
            )
        }
    }
}

@Composable
private fun ItemPriceRow(product: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "€%.0f".format(product.price),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Grey11,
        )
        if (product.originalPrice != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "€%.0f".format(product.originalPrice),
                fontSize = 14.sp,
                color = Grey57,
                textDecoration = TextDecoration.LineThrough,
            )
        }
        if (product.discountPercent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            DiscountBadge(percent = product.discountPercent)
        }
    }
}

@Composable
private fun ItemSpecRow(product: Product) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        product.size?.let { SpecPill(label = "Size", value = it) }
        product.brand?.let { SpecPill(label = "Brand", value = it) }
    }
}

@Composable
private fun SpecPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Grey95)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$label: ", fontSize = 12.sp, color = Grey57)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Grey11)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
}

@Composable
private fun ItemDetailBottomBar(onMakeOffer: () -> Unit, onBuyNow: () -> Unit) {
    Column(modifier = Modifier.background(Color.White)) {
        HorizontalDivider(color = Grey91)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onMakeOffer,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VinderAzure),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VinderAzure),
            ) {
                Text(text = "Make offer", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onBuyNow,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VinderAzure, contentColor = Color.White),
            ) {
                Text(text = "Buy now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OwnListingBottomBar() {
    Column(modifier = Modifier.background(Color.White)) {
        HorizontalDivider(color = Grey91)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "This is your listing",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Grey57,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MakeOfferSheet(product: Product, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var offer by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(text = "Make an offer", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Listed at €%.0f".format(product.price), fontSize = 13.sp, color = Grey57)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = offer,
                onValueChange = { input -> offer = input.filter { it.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("€ ", fontWeight = FontWeight.Bold, color = Grey11) },
                placeholder = { Text("Your offer", color = Grey57) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VinderAzure,
                    unfocusedBorderColor = Grey91,
                    cursorColor = VinderAzure,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OfferSuggestionChip(label = "−10%") {
                    offer = (product.price * 0.9f).roundToInt().toString()
                }
                OfferSuggestionChip(label = "−20%") {
                    offer = (product.price * 0.8f).roundToInt().toString()
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            SheetPrimaryButton(text = "Send offer", enabled = offer.isNotBlank()) {
                onSubmit(offer)
            }
        }
    }
}

@Composable
private fun OfferSuggestionChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = VinderAzure,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Grey95)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuyNowSheet(product: Product, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val total = product.price + SHIPPING_FEE + BUYER_PROTECTION_FEE

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(text = "Order summary", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
            Spacer(modifier = Modifier.height(16.dp))
            SummaryRow(label = product.name, value = "€%.2f".format(product.price))
            Spacer(modifier = Modifier.height(10.dp))
            SummaryRow(label = "Shipping", value = "€%.2f".format(SHIPPING_FEE))
            Spacer(modifier = Modifier.height(10.dp))
            SummaryRow(label = "Buyer protection", value = "€%.2f".format(BUYER_PROTECTION_FEE))
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Grey91)
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(label = "Total", value = "€%.2f".format(total), emphasised = true)
            Spacer(modifier = Modifier.height(20.dp))
            SheetPrimaryButton(text = "Confirm purchase", enabled = true) {
                onConfirm()
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasised: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = if (emphasised) 15.sp else 13.sp,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasised) Grey11 else Grey36,
        )
        Text(
            text = value,
            fontSize = if (emphasised) 15.sp else 13.sp,
            fontWeight = if (emphasised) FontWeight.Bold else FontWeight.Medium,
            color = Grey11,
        )
    }
}

@Composable
private fun SheetPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
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
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ItemDetailScreenPreview() {
    VintedTheme {
        ItemDetailScreen(
            product = Product(
                id = "1",
                name = "Vintage denim jacket",
                price = 45f,
                originalPrice = 90f,
                discountPercent = 50,
                size = "L",
                brand = "Levi's",
                sellerInitial = "C",
                sellerName = "chloe.p",
                rating = 4.9f,
                sellerId = 1,
            ),
        )
    }
}
