package com.personaltracker.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.theme.PrimaryBlue
import com.personaltracker.ui.theme.PrimaryBlueDark
import kotlin.math.roundToInt

private const val SETUP_PIN_LENGTH = 6

private enum class SetupStep { ENTER, CONFIRM }

@Composable
fun SetupPinScreen(
    onPinConfigured: () -> Unit,
    viewModel: SetupPinViewModel = hiltViewModel()
) {
    val setupState by viewModel.setupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var step by remember { mutableStateOf(SetupStep.ENTER) }
    var firstPin by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }

    val shakeOffset = remember { Animatable(0f) }

    // Navigate to dashboard on success
    LaunchedEffect(setupState) {
        when (val state = setupState) {
            is SetupState.Success -> onPinConfigured()
            is SetupState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    val gradient = Brush.verticalGradient(listOf(PrimaryBlue, PrimaryBlueDark))

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Step-aware header with slide transition
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    },
                    label = "setup_header"
                ) { currentStep ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentStep == SetupStep.ENTER) "Create PIN" else "Confirm PIN",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentStep == SetupStep.ENTER)
                                "Choose a 6-digit PIN to secure your app"
                            else
                                "Re-enter your PIN to confirm",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // PIN dots with shake
                SetupPinDots(
                    filledCount = currentInput.length,
                    total = SETUP_PIN_LENGTH,
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Reuse the shared NumericKeypad from AuthScreen
                NumericKeypad(
                    onDigit = { digit ->
                        if (currentInput.length < SETUP_PIN_LENGTH) {
                            currentInput += digit

                            if (currentInput.length == SETUP_PIN_LENGTH) {
                                when (step) {
                                    SetupStep.ENTER -> {
                                        firstPin = currentInput
                                        currentInput = ""
                                        step = SetupStep.CONFIRM
                                    }
                                    SetupStep.CONFIRM -> {
                                        if (currentInput == firstPin) {
                                            viewModel.setPin(currentInput)
                                        } else {
                                            // PINs don't match — shake and reset
                                            currentInput = ""
                                            step = SetupStep.ENTER
                                            firstPin = ""
                                            // Launch shake via coroutine
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (currentInput.isNotEmpty()) {
                            currentInput = currentInput.dropLast(1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupPinDots(
    filledCount: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) Color.White else Color.Transparent)
                    .border(width = 2.dp, color = Color.White, shape = CircleShape)
            )
        }
    }
}
