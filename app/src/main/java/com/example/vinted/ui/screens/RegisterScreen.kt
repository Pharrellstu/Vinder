package com.example.vinted.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.auth.AuthRepository
import com.example.vinted.auth.AuthUiState
import com.example.vinted.auth.AuthViewModel
import com.example.vinted.ui.theme.boxDivColor
import com.example.vinted.ui.theme.grayColor
import com.example.vinted.ui.theme.headingColor
import com.example.vinted.ui.theme.inputColor
import com.example.vinted.ui.theme.instrumentSerifNormal
import com.example.vinted.ui.theme.inter
import com.example.vinted.ui.theme.roundedInputShape

private const val PASSWORD_MASK = '•'
private val CARD_BORDER_COLOR = Color.Black.copy(alpha = 0.1f)

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit = {}
) {
    val usernameFieldState = rememberTextFieldState("")
    val emailFieldState = rememberTextFieldState("")
    val passwordFieldState = rememberTextFieldState("")
    val confirmPasswordFieldState = rememberTextFieldState("")
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .background(
                    color = Color(boxDivColor.value),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    BorderStroke(1.dp, CARD_BORDER_COLOR),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 40.dp, horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vinder",
                fontSize = 48.sp,
                fontFamily = instrumentSerifNormal,
                fontStyle = FontStyle.Italic,
                color = headingColor,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Text(
                text = "Register",
                fontSize = 20.sp,
                fontFamily = inter,
                fontStyle = FontStyle.Italic,
                color = headingColor,
                modifier = Modifier.padding(top = 8.dp)
            )

            InputField(
                state = emailFieldState,
                placeholder = "Email",
                topPadding = 20.dp
            )

            InputField(
                state = passwordFieldState,
                placeholder = "Password",
                topPadding = 20.dp,
                masked = true
            )

            InputField(
                state = confirmPasswordFieldState,
                placeholder = "Password Again",
                topPadding = 20.dp,
                masked = true
            )

            PasswordStrengthBar(
                password = passwordFieldState.text.toString(),
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = {
                    viewModel.register(
                        emailFieldState.text.toString(),
                        passwordFieldState.text.toString(),
                        confirmPasswordFieldState.text.toString()
                    )
                },
                enabled = uiState !is AuthUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = headingColor),
                modifier = Modifier
                    .padding(top = 28.dp)
                    .width(170.dp)
                    .height(38.dp)
            ) {
                Text("Register")
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

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Already have an account? ",
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "Log In",
                    fontSize = 12.sp,
                    color = headingColor
                )
            }
        }
    }
}

@Composable
private fun InputField(
    state: TextFieldState,
    placeholder: String,
    topPadding: Dp,
    masked: Boolean = false
) {
    BasicTextField(
        state = state,
        textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
        outputTransformation = if (masked) {
            { replace(0, length, PASSWORD_MASK.toString().repeat(length)) }
        } else {
            null
        },
        modifier = Modifier
            .padding(top = topPadding)
            .fillMaxWidth()
            .height(38.dp)
            .background(color = inputColor, shape = roundedInputShape)
            .border(BorderStroke(1.dp, CARD_BORDER_COLOR), shape = roundedInputShape),
        decorator = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.text.isEmpty()) {
                    Text(text = placeholder, color = Color.Black, fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun PasswordStrengthBar(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = ratePasswordStrength(password)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Password strength",
            fontSize = 12.sp,
            color = Color.Black
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(4.dp)
                .background(color = grayColor, shape = RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(strength.fillFraction)
                    .height(4.dp)
                    .background(color = strength.color, shape = RoundedCornerShape(2.dp))
            )
        }
    }
}

private enum class PasswordStrength(val fillFraction: Float, val color: Color) {
    EMPTY(0f, Color.Transparent),
    WEAK(0.33f, Color(0xFFE53935)),
    MEDIUM(0.66f, Color(0xFFFFC107)),
    STRONG(1f, Color(0xFF4CAF50))
}

private fun ratePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY

    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val satisfiedRules = listOf(hasMinLength, hasUppercase, hasDigit, hasSpecial).count { it }

    return when {
        satisfiedRules >= 4 -> PasswordStrength.STRONG
        satisfiedRules >= 2 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(viewModel = AuthViewModel(AuthRepository()))
}
