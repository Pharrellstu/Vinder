package com.example.vinted.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.models.ChangePasswordUiState
import com.example.vinted.ui.models.ChangePasswordViewModel
import com.example.vinted.ui.models.RegistrationViewModel
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.Grey57
import com.example.vinted.ui.theme.Grey97
import com.example.vinted.ui.theme.SurfaceColor
import com.example.vinted.ui.theme.VinderAzure
import com.example.vinted.ui.theme.VinderError
import com.example.vinted.ui.theme.VinderGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit = {},
    viewModel: ChangePasswordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Grey97,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Change password",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Grey11,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Grey11,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor),
            )
        },
    ) { padding ->
        if (state is ChangePasswordUiState.Success) {
            SuccessContent(modifier = Modifier.padding(padding), onDone = onBack)
            return@Scaffold
        }

        val loading = state is ChangePasswordUiState.Loading

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            PasswordField(
                value = current,
                onValueChange = { current = it; viewModel.clearError() },
                label = "Current password",
                enabled = !loading,
            )

            Spacer(Modifier.height(16.dp))

            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it; viewModel.clearError() },
                label = "New password",
                enabled = !loading,
            )

            PasswordRequirements(password = newPassword)

            Spacer(Modifier.height(16.dp))

            PasswordField(
                value = confirm,
                onValueChange = { confirm = it; viewModel.clearError() },
                label = "Confirm new password",
                enabled = !loading,
            )

            (state as? ChangePasswordUiState.Error)?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(text = error.message, color = VinderError, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.changePassword(current, newPassword, confirm) },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VinderAzure,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text("Update password", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = Grey57,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceColor,
            unfocusedContainerColor = SurfaceColor,
            focusedTextColor = Grey11,
            unfocusedTextColor = Grey11,
            focusedBorderColor = VinderAzure,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Live feedback on which password rules the new password still needs to meet. */
@Composable
private fun PasswordRequirements(password: String) {
    if (password.isEmpty()) return
    val missing = RegistrationViewModel.findMissingPasswordRequirements(password)
    Spacer(Modifier.height(8.dp))
    if (missing.isEmpty()) {
        Text(text = "✓ Strong password", color = VinderGreen, fontSize = 12.sp)
    } else {
        Text(
            text = "Needs: ${missing.joinToString(", ")}",
            color = Grey57,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SuccessContent(modifier: Modifier, onDone: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = VinderGreen,
            modifier = Modifier.height(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Password updated", color = Grey11, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your password has been changed successfully.",
            color = Grey57,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = VinderAzure,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Done", fontSize = 16.sp)
        }
    }
}
