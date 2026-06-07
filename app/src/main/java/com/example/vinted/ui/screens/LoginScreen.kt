package com.example.vinted.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.models.LoginUiState
import com.example.vinted.ui.models.LoginViewModel
import com.example.vinted.ui.theme.boxDivColor
import com.example.vinted.ui.theme.headingColor
import com.example.vinted.ui.theme.inputColor
import com.example.vinted.ui.theme.instrumentSerifNormal
import com.example.vinted.ui.theme.roundedInputShape

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {}
) {

    val textFieldState = rememberTextFieldState("")
    val passwordFieldState = rememberTextFieldState("")
    var checked by remember { mutableStateOf(true) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(340.dp, 430.dp)
                .background(
                    color = Color(boxDivColor.value),
                    shape = RoundedCornerShape(16.dp),
                )
                .border(
                    BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)

                )
                .padding(vertical = 40.dp,
                    horizontal = 40.dp
                )
        ) {
            Text(
                text = "Vinder",
                fontSize = 48.sp,
                fontFamily = instrumentSerifNormal,
                fontStyle = FontStyle.Italic,
                color = headingColor,

                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            BasicTextField(
                state = textFieldState,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(top = 20.dp)
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
                        if (textFieldState.text.isEmpty()) {
                            Text(
                                text = "Username",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }

                        innerTextField()
                    }
                }
            )

            BasicSecureTextField(
                state = passwordFieldState,
                textObfuscationMode = TextObfuscationMode.Hidden,
                modifier = Modifier
                    .padding(
                        top = 26.dp
                    )
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(color = inputColor, shape = roundedInputShape)
                    .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                        shape = roundedInputShape),
                decorator = { innerPasswordField ->
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                        if(passwordFieldState.text.isEmpty()) {
                            Text(
                                text = "Password",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                        }
                        innerPasswordField()
                    }
                }

            )

            Row(
                Modifier.padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically

            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = headingColor,
                                checkmarkColor = Color.White,
                                uncheckedColor = Color.Gray
                            )
                        )
                    }

                    Text(
                        text = "Keep me logged in ",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    text = "Forgot password?",
                    fontSize = 12.sp,
                )
            }

            Button(
                onClick = {
                    viewModel.login(
                        textFieldState.text.toString(),
                        passwordFieldState.text.toString()
                    )
                },
                enabled = uiState !is LoginUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = headingColor
                ),
                modifier = Modifier
                    .padding(top = 32.dp)
                    .width(170.dp)
                    .height(38.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Log In")
            }

            if (uiState is LoginUiState.Loading) {
                CircularProgressIndicator(
                    color = headingColor,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
