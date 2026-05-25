package com.personaltracker.ui.screens.auth

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.theme.PrimaryBlue
import com.personaltracker.ui.theme.PrimaryBlueDark
import kotlin.math.roundToInt

private const val PIN_LENGTH = 6

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }

    var enteredPin by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    // Shake animation offset for wrong-PIN feedback
    val shakeOffset = remember { Animatable(0f) }

    // Resolve biometric availability once (reads DataStore)
    val isBiometricEnabled by produceState(initialValue = false) {
        value = viewModel.isBiometricAvailable() && viewModel.isBiometricEnabled()
    }

    // React to auth state transitions
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> onAuthSuccess()
            is AuthState.Error -> {
                // Shake the PIN dots left-right to signal wrong entry
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 400
                        10f at 50
                        -10f at 100
                        10f at 150
                        -10f at 200
                        6f at 250
                        -6f at 300
                        0f at 400
                    }
                )
                snackbarHostState.showSnackbar(state.message)
                enteredPin = ""
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    // Auto-prompt biometric when enabled (fires once after produceState resolves)
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && activity != null) {
            viewModel.showBiometricPrompt(
                activity = activity,
                onSuccess = onAuthSuccess
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PrimaryBlue, PrimaryBlueDark)))
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

                Text(
                    text = "Enter PIN",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your 6-digit PIN to continue",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(40.dp))

                // PIN dot indicators with shake animation applied via offset
                PinDots(
                    filledCount = enteredPin.length,
                    total = PIN_LENGTH,
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Shared numeric keypad (also used by SetupPinScreen)
                NumericKeypad(
                    onDigit = { digit ->
                        if (enteredPin.length < PIN_LENGTH) {
                            enteredPin += digit
                            if (enteredPin.length == PIN_LENGTH) {
                                viewModel.verifyPin(enteredPin)
                            }
                        }
                    },
                    onBackspace = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Biometric shortcut — shown only when hardware is available & user opted in
                if (isBiometricEnabled && activity != null) {
                    IconButton(
                        onClick = {
                            viewModel.showBiometricPrompt(
                                activity = activity,
                                onSuccess = onAuthSuccess
                            )
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Use Biometric",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Text(
                        text = "Use Fingerprint",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PIN indicator dots
// ---------------------------------------------------------------------------

@Composable
private fun PinDots(
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

// ---------------------------------------------------------------------------
// Numeric Keypad — layout: rows 1-9, then blank | 0 | backspace
// `internal` visibility so SetupPinScreen (same package) can reuse it.
// ---------------------------------------------------------------------------

@Composable
fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(72.dp))
                        "⌫" -> {
                            IconButton(
                                onClick = onBackspace,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> KeypadDigitButton(label = key, onClick = { onDigit(key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            shape = CircleShape,
            border = null
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
