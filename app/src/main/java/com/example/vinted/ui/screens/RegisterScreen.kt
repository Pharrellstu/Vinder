package com.example.vinted.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.models.RegistrationUiState
import com.example.vinted.ui.models.RegistrationViewModel
import com.example.vinted.ui.theme.boxDivColor
import com.example.vinted.ui.theme.grayColor
import com.example.vinted.ui.theme.inputColor
import com.example.vinted.ui.theme.instrumentSerifNormal
import com.example.vinted.ui.theme.inter
import com.example.vinted.ui.theme.roundedInputShape
import com.example.vinted.ui.theme.VinderAzure

private const val PASSWORD_MASK = '•'
private val CARD_BORDER_COLOR = Color.Black.copy(alpha = 0.1f)

@Composable
fun RegisterScreen(
    viewModel: RegistrationViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    RegisterContent(
        uiState = uiState,
        onRegister = viewModel::register,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
private fun RegisterContent(
    uiState: RegistrationUiState,
    onRegister: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val nicknameFieldState = rememberTextFieldState("")
    val emailFieldState = rememberTextFieldState("")
    val passwordFieldState = rememberTextFieldState("")
    val confirmPasswordFieldState = rememberTextFieldState("")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(380.dp)
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
                fontSize = 56.sp,
                fontFamily = instrumentSerifNormal,
                fontStyle = FontStyle.Italic,
                color = VinderAzure,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.clickable { onNavigateToLogin() }
            )

            if (uiState is RegistrationUiState.Success) {
                RegistrationSuccessContent(
                    email = (uiState as RegistrationUiState.Success).email,
                    onNavigateToLogin = onNavigateToLogin
                )
            } else {
                Text(
                    text = "Register",
                    fontSize = 20.sp,
                    fontFamily = inter,
                    fontStyle = FontStyle.Italic,
                    color = VinderAzure
                )

                InputField(
                    state = nicknameFieldState,
                    placeholder = "Nickname",
                    topPadding = 20.dp
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
                    modifier = Modifier
                        .padding(top = 16.dp)
                )

                Button(
                    onClick = {
                        onRegister(
                            nicknameFieldState.text.toString(),
                            emailFieldState.text.toString(),
                            passwordFieldState.text.toString(),
                            confirmPasswordFieldState.text.toString()
                        )
                    },
                    enabled = uiState !is RegistrationUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = VinderAzure),
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .width(170.dp)
                        .height(38.dp)
                ) {
                    Text("Register", fontSize = 18.sp)
                }

                if (uiState is RegistrationUiState.Loading) {
                    CircularProgressIndicator(
                        color = VinderAzure,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(24.dp)
                    )
                }

                if (uiState is RegistrationUiState.Error) {
                    Text(
                        text = (uiState as RegistrationUiState.Error).message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }

                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Log In",
                        fontSize = 16.sp,
                        color = VinderAzure,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationSuccessContent(
    email: String,
    onNavigateToLogin: () -> Unit
) {

    Text(
        text = "Check your email!",
        fontSize = 20.sp,
        fontFamily = inter,
        color = VinderAzure,
        textAlign = TextAlign.Center
    )

    Text(
        text = "A confirmation link has been sent to\n$email",
        fontSize = 13.sp,
        color = Color.Black.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp)
    )

    Button(
        onClick = onNavigateToLogin,
        colors = ButtonDefaults.buttonColors(containerColor = VinderAzure),
        modifier = Modifier
            .padding(top = 28.dp)
            .width(170.dp)
            .height(38.dp)
    ) {
        Text("Back to Login")
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

private const val TOTAL_PASSWORD_RULES = 4

private fun ratePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY

    val satisfiedRules =
        TOTAL_PASSWORD_RULES - RegistrationViewModel.findMissingPasswordRequirements(password).size

    return when {
        satisfiedRules >= 4 -> PasswordStrength.STRONG
        satisfiedRules >= 2 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterContent(
        uiState = RegistrationUiState.Idle,
        onRegister = { _, _, _, _ -> },
        onNavigateToLogin = {}
    )
}
