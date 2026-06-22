package com.example.vinted.ui.screens
import com.example.vinted.ui.theme.Grey11
import com.example.vinted.ui.theme.SurfaceColor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vinted.ui.models.ForgotPasswordState
import com.example.vinted.ui.models.ForgotPasswordViewModel
import com.example.vinted.ui.theme.boxDivColor
import com.example.vinted.ui.theme.inputColor
import com.example.vinted.ui.theme.instrumentSerifNormal
import com.example.vinted.ui.theme.inter
import com.example.vinted.ui.theme.roundedInputShape
import com.example.vinted.ui.theme.VinderAzure

private const val PASSWORD_MASK = '•'

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = viewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    ForgotPasswordContent(
        state = state,
        onSendOtp = viewModel::sendOtp,
        onVerifyOtp = viewModel::verifyOtp,
        onUpdatePassword = viewModel::updatePassword,
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordState,
    onSendOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onUpdatePassword: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .background(
                    color = Color(boxDivColor.value),
                    shape = RoundedCornerShape(15.dp)
                )
                .border(
                    BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(vertical = 40.dp, horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VinderLogo(onClick = onNavigateToLogin)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state.phase) {
                    ForgotPasswordState.Phase.EMAIL -> EmailStep(
                        isLoading = state.isLoading,
                        error = state.error,
                        onSend = onSendOtp
                    )
                    ForgotPasswordState.Phase.OTP -> OtpStep(
                        email = state.email,
                        isLoading = state.isLoading,
                        error = state.error,
                        onVerify = onVerifyOtp
                    )
                    ForgotPasswordState.Phase.NEW_PASSWORD -> NewPasswordStep(
                        isLoading = state.isLoading,
                        error = state.error,
                        onSubmit = onUpdatePassword
                    )
                    ForgotPasswordState.Phase.SUCCESS -> SuccessStep(
                        onNavigateToLogin = onNavigateToLogin
                    )
                }
            }
        }
    }
}

@Composable
private fun VinderLogo(onClick: () -> Unit) {
    Text(
        text = "Vinder",
        fontSize = 56.sp,
        lineHeight = 56.sp,
        fontFamily = instrumentSerifNormal,
        fontStyle = FontStyle.Italic,
        color = VinderAzure,
        style = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        ),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun EmailStep(
    isLoading: Boolean,
    error: String?,
    onSend: (String) -> Unit
) {
    val emailState = rememberTextFieldState()

    Text(
        text = "Forgot Password",
        fontSize = 20.sp,
        fontFamily = inter,
        fontStyle = FontStyle.Italic,
        color = VinderAzure
    )

    Text(
        text = "Enter your email and we will send you a one-time code.",
        fontSize = 12.sp,
        color = Grey11,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
    )

    BasicTextField(
        state = emailState,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = TextStyle(fontSize = 16.sp, color = Grey11),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .height(38.dp)
            .background(color = inputColor, shape = roundedInputShape)
            .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)), shape = roundedInputShape),
        decorator = { inner ->
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (emailState.text.isEmpty()) {
                    Text("Email", color = Color.Black.copy(alpha = 0.4f), fontSize = 14.sp)
                }
                inner()
            }
        }
    )

    Button(
        onClick = { onSend(emailState.text.toString()) },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = VinderAzure),
        modifier = Modifier
            .padding(top = 24.dp)
            .width(170.dp)
            .height(38.dp)
    ) {
        Text("Send Code", fontSize = 18.sp)
    }

    StepFooter(isLoading = isLoading, error = error)
}

@Composable
private fun OtpStep(
    email: String,
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit
) {
    val otpState = rememberTextFieldState()
    val otpText = otpState.text.toString()

    Text(
        text = "Enter Code",
        fontSize = 20.sp,
        fontFamily = inter,
        fontStyle = FontStyle.Italic,
        color = VinderAzure,
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        text = "We sent a 6-digit code to $email",
        fontSize = 12.sp,
        color = Grey11,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
    )

    OtpInputRow(
        state = otpState,
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
    )

    Button(
        onClick = { onVerify(otpText) },
        enabled = !isLoading && otpText.length == ForgotPasswordViewModel.OTP_LENGTH,
        colors = ButtonDefaults.buttonColors(containerColor = VinderAzure),
        modifier = Modifier
            .padding(top = 24.dp)
            .width(170.dp)
            .height(38.dp)
    ) {
        Text("Verify", fontSize = 18.sp)
    }

    StepFooter(isLoading = isLoading, error = error)
}

@Composable
private fun NewPasswordStep(
    isLoading: Boolean,
    error: String?,
    onSubmit: (String, String) -> Unit
) {
    val newPasswordState = rememberTextFieldState()
    val confirmPasswordState = rememberTextFieldState()
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Text(
        text = "New Password",
        fontSize = 20.sp,
        fontFamily = inter,
        fontStyle = FontStyle.Italic,
        color = VinderAzure,
        modifier = Modifier.padding(top = 8.dp)
    )

    Text(
        text = "Choose a strong new password.",
        fontSize = 12.sp,
        color = Grey11,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
    )

    PasswordField(
        state = newPasswordState,
        placeholder = "New password",
        visible = newPasswordVisible,
        onToggleVisibility = { newPasswordVisible = !newPasswordVisible },
        modifier = Modifier.padding(top = 24.dp)
    )

    PasswordField(
        state = confirmPasswordState,
        placeholder = "Confirm password",
        visible = confirmPasswordVisible,
        onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
        modifier = Modifier.padding(top = 12.dp)
    )

    Button(
        onClick = {
            onSubmit(
                newPasswordState.text.toString(),
                confirmPasswordState.text.toString()
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = VinderAzure),
        modifier = Modifier
            .padding(top = 24.dp)
            .width(170.dp)
            .height(38.dp)
    ) {
        Text("Set Password", fontSize = 18.sp)
    }

    StepFooter(isLoading = isLoading, error = error)
}

@Composable
private fun SuccessStep(onNavigateToLogin: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = VinderAzure,
        modifier = Modifier
            .padding(top = 24.dp)
            .size(48.dp)
    )

    Text(
        text = "Password updated!",
        fontSize = 16.sp,
        fontFamily = inter,
        color = VinderAzure,
        modifier = Modifier.padding(top = 16.dp)
    )

    Text(
        text = "You can now sign in with your new password.",
        fontSize = 12.sp,
        color = Grey11,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    )

    Text(
        text = "Back to Login",
        fontSize = 16.sp,
        color = VinderAzure,
        modifier = Modifier
            .padding(top = 32.dp)
            .clickable { onNavigateToLogin() }
    )
}

@Composable
private fun OtpInputRow(
    state: TextFieldState,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val otpText = state.text.toString()

    Box(modifier = modifier.height(48.dp)) {
        BasicTextField(
            state = state,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            inputTransformation = InputTransformation {
                val current = asCharSequence().toString()
                val filtered = current.filter { it.isDigit() }
                    .take(ForgotPasswordViewModel.OTP_LENGTH)
                if (current != filtered) replace(0, length, filtered)
            },
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester)
                .alpha(0f),
            decorator = { innerTextField -> innerTextField() }
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(ForgotPasswordViewModel.OTP_LENGTH) { index ->
                val isCurrent = otpText.length == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(inputColor, RoundedCornerShape(8.dp))
                        .border(
                            width = if (isCurrent) 1.5.dp else 1.dp,
                            color = if (isCurrent) VinderAzure else Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otpText.getOrNull(index)?.toString() ?: "",
                        fontSize = 20.sp,
                        color = Grey11
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun PasswordField(
    state: TextFieldState,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = TextStyle(fontSize = 16.sp, color = Grey11),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        outputTransformation = if (!visible) {
            { replace(0, length, PASSWORD_MASK.toString().repeat(length)) }
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(color = inputColor, shape = roundedInputShape)
            .border(BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f)), shape = roundedInputShape),
        decorator = { inner ->
            Row(
                modifier = Modifier.padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (state.text.isEmpty()) {
                        Text(placeholder, color = Color.Black.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                    inner()
                }
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun StepFooter(isLoading: Boolean, error: String?) {
    if (isLoading) {
        CircularProgressIndicator(
            color = VinderAzure,
            modifier = Modifier
                .padding(top = 16.dp)
                .size(24.dp)
        )
    }

    if (error != null) {
        Text(
            text = error,
            color = Color.Red,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordContent(
        state = ForgotPasswordState(),
        onSendOtp = {},
        onVerifyOtp = {},
        onUpdatePassword = { _, _ -> },
        onNavigateToLogin = {}
    )
}
