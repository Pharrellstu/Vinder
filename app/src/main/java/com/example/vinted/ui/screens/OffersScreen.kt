package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.data.OFFER_STATUS_ACCEPTED
import com.example.vinted.data.OFFER_STATUS_PENDING
import com.example.vinted.data.OFFER_STATUS_REJECTED
import com.example.vinted.ui.models.OfferWithDetails
import com.example.vinted.ui.models.OffersUiState
import com.example.vinted.ui.models.OffersViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onBack: () -> Unit,
    viewModel: OffersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offers received", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Grey97,
    ) { padding ->
        when (val state = uiState) {
            is OffersUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }

            is OffersUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Grey57, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.load() }) {
                            Text("Retry", color = VinderAzure)
                        }
                    }
                }
            }

            is OffersUiState.Success -> {
                if (state.offers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No offers yet", color = Grey57, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 12.dp,
                        ),
                    ) {
                        items(state.offers, key = { it.offerId }) { offer ->
                            OfferCard(
                                offer = offer,
                                onAccept = {
                                    viewModel.acceptOffer(
                                        offer = offer,
                                        onSuccess = { notify("Offer accepted — item marked as sold") },
                                        onError = { notify(it) },
                                    )
                                },
                                onReject = {
                                    viewModel.rejectOffer(
                                        offerId = offer.offerId,
                                        onSuccess = { notify("Offer rejected") },
                                        onError = { notify(it) },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: OfferWithDetails,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.06f))
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.itemName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Grey11,
                )
                Text(
                    text = "from ${offer.buyerName}",
                    fontSize = 13.sp,
                    color = Grey57,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            StatusBadge(statusId = offer.statusId)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "€${"%.2f".format(offer.offerPrice)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Grey11,
            )
            Text(
                text = formatDate(offer.createdAt),
                fontSize = 12.sp,
                color = Grey57,
            )
        }

        if (offer.statusId == OFFER_STATUS_PENDING) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Grey57),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Grey91),
                ) {
                    Text("Reject", fontSize = 14.sp)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                ) {
                    Text("Accept", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(statusId: Int) {
    val (label, bgColor, textColor) = when (statusId) {
        OFFER_STATUS_PENDING -> Triple("Pending", Color(0xFFFFF8E1), Color(0xFFF57F17))
        OFFER_STATUS_ACCEPTED -> Triple("Accepted", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        OFFER_STATUS_REJECTED -> Triple("Rejected", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple("Cancelled", Color(0xFFF5F5F5), Grey57)
    }
    Box(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

private fun formatDate(isoTimestamp: String): String = runCatching {
    val instant = Instant.parse(isoTimestamp)
    DateTimeFormatter.ofPattern("d MMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}.getOrDefault(isoTimestamp)
