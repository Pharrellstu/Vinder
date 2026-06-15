package com.example.vinted.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.models.EditProfileUiState
import com.example.vinted.ui.models.EditProfileViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey91
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VintedTheme

private val ERROR_RED = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    accountId: Int? = null,
    viewModel: EditProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditProfileViewModel(accountId = accountId) as T
        },
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Saving completed — let the host dismiss the screen and refresh the profile.
    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.Saved) {
            onSaved()
        }
    }

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit profile", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Grey11)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is EditProfileUiState.Loading, is EditProfileUiState.Saved -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = VinderAzure)
                }
            }
            is EditProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
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
            is EditProfileUiState.Editing -> {
                EditProfileForm(
                    state = state,
                    padding = padding,
                    onNameChange = viewModel::onNameChange,
                    onLocationChange = viewModel::onLocationChange,
                    onBioChange = viewModel::onBioChange,
                    onSave = viewModel::save,
                )
            }
        }
    }
}

@Composable
private fun EditProfileForm(
    state: EditProfileUiState.Editing,
    padding: PaddingValues,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AvatarPreview(initial = state.name.firstOrNull()?.uppercase() ?: "?")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Photo upload coming soon",
            fontSize = 11.sp,
            color = Grey57,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(22.dp))

        FieldLabel("Name")
        Spacer(modifier = Modifier.height(6.dp))
        EditTextField(value = state.name, onValueChange = onNameChange, placeholder = "Your display name")

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel("Location")
        Spacer(modifier = Modifier.height(6.dp))
        EditTextField(value = state.location, onValueChange = onLocationChange, placeholder = "City, Country")

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel("Bio")
        Spacer(modifier = Modifier.height(6.dp))
        EditTextField(
            value = state.bio,
            onValueChange = onBioChange,
            placeholder = "Tell buyers a bit about yourself",
            minLines = 3,
            maxLines = 5,
        )

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = state.errorMessage, color = ERROR_RED, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))
        SaveButton(isSaving = state.isSaving, onClick = onSave)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AvatarPreview(initial: String) {
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(Grey97)
            .padding(4.dp)
            .clip(CircleShape)
            .background(VinderAzure),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Grey11)
}

@Composable
private fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Grey57) },
        minLines = minLines,
        maxLines = maxLines,
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
private fun SaveButton(isSaving: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
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
        Text(
            text = if (isSaving) "Saving…" else "Save changes",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditProfileScreenPreview() {
    VintedTheme {
        EditProfileScreen()
    }
}
