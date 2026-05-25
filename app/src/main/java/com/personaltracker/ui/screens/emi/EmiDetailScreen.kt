package com.personaltracker.ui.screens.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.personaltracker.data.database.entity.EmiPaymentEntity
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.EmiOrange
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiDetailScreen(
    navController: NavController,
    emiId: Long,
    viewModel: EmiViewModel = hiltViewModel()
) {
    LaunchedEffect(emiId) { viewModel.loadEmiDetail(emiId) }

    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val emi = state.selectedEmi ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val payments = state.payments
    val paidCount = payments.count { it.isPaid }

    Scaffold(
        topBar = {
            PTTopBar(
                title = emi.name,
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmiOrange)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(emi.lenderName, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(emi.emiAmount) + " / month", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EmiInfoChip("Loan: ${formatCurrency(emi.loanAmount)}")
                            EmiInfoChip("${emi.interestRate}% p.a.")
                            EmiInfoChip("Due: ${emi.dueDay}th")
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (emi.tenureMonths > 0) paidCount.toFloat() / emi.tenureMonths else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("$paidCount of ${emi.tenureMonths} months paid", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Text(
                    "Monthly Payment Grid",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                MonthlyPaymentGrid(
                    emiId = emi.id,
                    emiAmount = emi.emiAmount,
                    year = LocalDate.now().year,
                    payments = payments,
                    onMarkPaid = { month, year -> viewModel.markPayment(emi.id, month, year, emi.emiAmount) }
                )
            }

            if (payments.isNotEmpty()) {
                item {
                    Text(
                        "Payment History",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(payments.sortedByDescending { it.paidDate }) { payment ->
                    PaymentHistoryRow(payment)
                }
            }
        }
    }
}

@Composable
private fun MonthlyPaymentGrid(
    emiId: Long,
    emiAmount: Double,
    year: Int,
    payments: List<EmiPaymentEntity>,
    onMarkPaid: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$year", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            (1..12).chunked(4).forEach { monthRow ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    monthRow.forEach { month ->
                        val isPaid = payments.any { it.month == month && it.year == year && it.isPaid }
                        val isFuture = LocalDate.of(year, month, 1).isAfter(LocalDate.now())
                        val monthLabel = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        Box(modifier = Modifier.weight(1f).padding(vertical = 3.dp)) {
                            if (isPaid) {
                                Button(
                                    onClick = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmiOrange),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, Modifier.size(12.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(monthLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { if (!isFuture) onMarkPaid(month, year) },
                                    enabled = !isFuture,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text(monthLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(payment: EmiPaymentEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(if (payment.isPaid) EmiOrange.copy(0.15f) else MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (payment.isPaid) Icons.Default.Check else Icons.Default.Close,
                null,
                Modifier.size(18.dp),
                tint = if (payment.isPaid) EmiOrange else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${Month.of(payment.month).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${payment.year}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Paid on ${payment.paidDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatCurrency(payment.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmiInfoChip(text: String) {
    Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.2f)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
