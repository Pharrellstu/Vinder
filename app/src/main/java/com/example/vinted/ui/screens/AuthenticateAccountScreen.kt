package com.example.vinted.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.AuthUiState
import com.example.vinted.auth.AuthViewModel
import com.example.vinted.ui.theme.*

@Composable
fun AuthenticateAccountScreen(
    viewModel: AuthViewModel = viewModel(),
    email: String = "",
    onVerifySuccess: () -> Unit = {}
) {

    val codeFieldState = rememberTextFieldState("")
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onVerifySuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(340.dp, 333.dp)
                .background(
                    color = Color(boxDivColor.value),
                    shape = RoundedCornerShape(15.dp),
                )
                .border(
                    BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(vertical = 40.dp,
                    horizontal = 40.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vinder",
                fontSize = 48.sp,
                fontFamily = instrumentSerifNormal,
                fontStyle = FontStyle.Italic,
                color = headingColor,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = "Authenticate your Account",
                fontSize = 20.sp,
                fontFamily = inter,
                fontStyle = FontStyle.Italic,
                color = headingColor,
                modifier = Modifier.padding(top = 10.dp)
            )

            BasicTextField(
                state = codeFieldState,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(color = inputColor, shape = roundedInputShape)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                        shape = roundedInputShape),
                decorator = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (codeFieldState.text.isEmpty()) {
                            Text(
                                text = "Code",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }

                        innerTextField()
                    }
                }
            )

            Button(
                onClick = {
                    viewModel.verifyOtp(email, codeFieldState.text.toString())
                },
                enabled = uiState !is AuthUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = headingColor
                ),
                modifier = Modifier
                    .padding(top = 32.dp)
                    .width(170.dp)
                    .height(38.dp)
            ) {
                Text("Proceed")
            }

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = headingColor,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(24.dp)
                )
            }

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthenticateAccountScreenPreview() {
    AuthenticateAccountScreen(viewModel = AuthViewModel(AuthRepository()))
}
