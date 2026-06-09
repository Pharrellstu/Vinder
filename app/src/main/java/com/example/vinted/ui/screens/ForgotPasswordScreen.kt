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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinted.ui.theme.*

@Preview
@Composable
fun ForgotPasswordScreen() {

    val emailFieldState = rememberTextFieldState("")

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(340.dp, 360.dp)
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

                style = TextStyle (
                    platformStyle = PlatformTextStyle (
                        includeFontPadding = false
                    )
                )
            )

            Text(
                text = "Forgot your Password",
                fontSize = 20.sp,
                fontFamily = inter,
                fontStyle = FontStyle.Italic,
                color = headingColor,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                    text = "Enter your email and we will send you a reset link.",
                    fontSize = 12.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
            )

            BasicTextField(
                state = emailFieldState,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(top = 24.dp)
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
                        if (emailFieldState.text.isEmpty()) {
                            Text(
                                text = "Email",
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
                    // Reset request handled later using emailFieldState.text
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = headingColor
                ),
                modifier = Modifier
                    .padding(top = 32.dp)
                    .width(170.dp)
                    .height(38.dp)
            ) {
                Text("Reset")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen()
}
