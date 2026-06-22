package com.example.vinted.ui.screens
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.vinted.ui.models.MyListing
import com.example.vinted.ui.models.MyListingsUiState
import com.example.vinted.ui.models.MyListingsViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VinderError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBack: () -> Unit,
    onEditListing: (Int) -> Unit = {},
    viewModel: MyListingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The ViewModel is Activity-scoped, so reload on each entry to reflect edits/deletes
    // made since it was first created.
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }

    androidx.compose.runtime.LaunchedEffect(actionError) {
        val message = actionError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My listings", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Grey97,
    ) { padding ->
        when (val state = uiState) {
            is MyListingsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }
            is MyListingsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.message, color = Grey57, fontSize = 14.sp)
                }
            }
            is MyListingsUiState.Success -> {
                if (state.listings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("You haven't listed any items yet", color = Grey57, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.listings, key = { it.itemId }) { listing ->
                            MyListingCard(
                                listing = listing,
                                onEdit = { onEditListing(listing.itemId) },
                                onMarkSold = { viewModel.markSold(listing.itemId) },
                                onDelete = { viewModel.delete(listing.itemId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListingCard(
    listing: MyListing,
    onEdit: () -> Unit,
    onMarkSold: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Grey91),
        ) {
            if (listing.coverUrl != null) {
                AsyncImage(
                    model = listing.coverUrl,
                    contentDescription = listing.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = listing.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Grey11,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = "€${listing.price}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VinderAzure)
            if (listing.isSold) {
                Spacer(Modifier.height(6.dp))
                SoldBadge()
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Listing actions", tint = Grey57)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    },
                )
                if (!listing.isSold) {
                    DropdownMenuItem(
                        text = { Text("Mark as sold") },
                        onClick = {
                            menuExpanded = false
                            onMarkSold()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = VinderError) },
                    onClick = {
                        menuExpanded = false
                        showDeleteDialog = true
                    },
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete listing?") },
            text = { Text("\"${listing.name}\" will be permanently removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = VinderError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun SoldBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Grey11)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text("Sold", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
