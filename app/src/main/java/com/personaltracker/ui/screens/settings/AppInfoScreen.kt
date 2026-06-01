package com.personaltracker.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personaltracker.ui.components.PTTopBar

@Composable
fun AppInfoScreen(onBack: () -> Unit) {
    Scaffold(topBar = { PTTopBar(title = "Application Info", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App identity card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "WealthHub",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Personal Finance & Life Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(onClick = {}, label = { Text("Version 1.0.0") })
                }
            }

            // App details
            AppInfoSection(title = "Application Details") {
                AppInfoRow(icon = Icons.Default.Apps, label = "Application Name", value = "WealthHub")
                AppInfoRow(icon = Icons.Default.Tag, label = "Version", value = "1.0.0")
                AppInfoRow(icon = Icons.Default.Person, label = "Developer", value = "SuryaKP")
                AppInfoRow(icon = Icons.Default.Business, label = "Organization", value = "WealthHub")
                AppInfoRow(icon = Icons.Default.PhoneAndroid, label = "Platform", value = "Android (Min SDK 26)")
                AppInfoRow(icon = Icons.Default.Storage, label = "Storage Type", value = "Local Device Storage")
                AppInfoRow(icon = Icons.Default.Code, label = "Package", value = "com.personaltracker")
            }

            // Security details
            AppInfoSection(title = "Security") {
                AppInfoRow(icon = Icons.Default.Lock, label = "App Lock", value = "PIN + Biometric")
                AppInfoRow(icon = Icons.Default.EnhancedEncryption, label = "Database", value = "AES-256 (SQLCipher)")
                AppInfoRow(icon = Icons.Default.Backup, label = "Backup", value = "Encrypted Local Backup")
                AppInfoRow(icon = Icons.Default.Cloud, label = "Cloud Sync", value = "Disabled — Offline Only")
            }

            // Modules
            AppInfoSection(title = "Modules") {
                val modules = listOf(
                    "Documents", "Credentials", "Expenses", "Investments",
                    "EMI Tracker", "Gold", "School Fees", "Travel",
                    "Group Expenses", "Notes", "Events & Reminders"
                )
                modules.forEach { module ->
                    AppInfoRow(
                        icon = Icons.Default.CheckCircle,
                        label = module,
                        value = "Enabled"
                    )
                }
            }

            // Copyright
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "© WealthHub. All Rights Reserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Developed by SuryaKP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppInfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun AppInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium)
    }
}
