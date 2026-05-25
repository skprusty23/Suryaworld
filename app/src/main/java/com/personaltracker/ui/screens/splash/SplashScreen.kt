package com.personaltracker.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personaltracker.ui.navigation.NavRoutes
import com.personaltracker.ui.theme.PrimaryBlue
import com.personaltracker.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isPinSetup by viewModel.isPinSetup.collectAsState()

    val logoScale = remember { Animatable(0.3f) }
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Kick off entrance animations
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400)
        )
    }

    // Wait for pin state and navigate after 1.5s
    LaunchedEffect(isPinSetup) {
        if (isPinSetup != null) {
            delay(1500L)
            val destination = if (isPinSetup == true) NavRoutes.AUTH else NavRoutes.SETUP_PIN
            onNavigate(destination)
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(PrimaryBlue, PrimaryBlueDark)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo icon inside a white-tinted circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale.value)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "SuryaWorld Logo",
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "SuryaWorld",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha.value),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Manage. Track. Grow.",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.alpha(subtitleAlpha.value),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
