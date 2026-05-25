package com.personaltracker.ui.screens.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.data.database.entity.InvestmentEntity
import com.personaltracker.domain.repository.InvestmentRepository
import com.personaltracker.ui.components.ConfirmDeleteDialog
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import com.personaltracker.ui.theme.InvestmentPurple
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

// ---- ViewModel ----

@HiltViewModel
class InvestmentDetailViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val investmentId: Long = checkNotNull(savedStateHandle["id"])

    private val _investment = MutableStateFlow<InvestmentEntity?>(null)
    val investment: StateFlow<InvestmentEntity?> = _investment.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        viewModelScope.launch {
            _investment.value = investmentRepository.getInvestmentById(investmentId)
        }
    }

    fun delete() {
        viewModelScope.launch {
            _investment.value?.let {
                investmentRepository.deleteInvestment(it)
                _deleted.value = true
            }
        }
    }
}

// ---- Screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: InvestmentDetailViewModel = hiltViewModel()
) {
    val investment by viewModel.investment.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted) onDeleted()
    }

    Scaffold(
        topBar = {
            PTTopBar(
                title = "Investment Details",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (val inv = investment) {
            null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                InvestmentDetailContent(
                    investment = inv,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete Investment",
            message = "Delete \"${investment?.name}\"? This cannot be undone.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun InvestmentDetailContent(
    investment: InvestmentEntity,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val today = LocalDate.now()
    val roi = investment.currentValue?.let { cv ->
        if (investment.principalAmount > 0) (cv - investment.principalAmount) / investment.principalAmount * 100 else 0.0
    }
    val daysToMaturity = investment.maturityDate?.let { ChronoUnit.DAYS.between(today, it) }
    val color = investmentTypeColor(investment.investmentType)

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = investmentTypeIcon(investment.investmentType),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = investment.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = investment.investmentType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
                investment.providerName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Financial Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialCard(
                label = "Principal",
                value = formatCurrency(investment.principalAmount),
                color = InvestmentPurple,
                modifier = Modifier.weight(1f)
            )
            investment.currentValue?.let { cv ->
                FinancialCard(
                    label = "Current Value",
                    value = formatCurrency(cv),
                    color = Color(0xFF00897B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ROI Card
        roi?.let { roiVal ->
            val gain = (investment.currentValue ?: 0.0) - investment.principalAmount
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (gain >= 0) Color(0xFF43A047).copy(alpha = 0.08f)
                    else Color(0xFFE53935).copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ROI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "%.2f%%".format(roiVal),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (gain >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Gain/Loss", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = (if (gain >= 0) "+" else "") + formatCurrency(gain),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (gain >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                        )
                    }
                    Icon(
                        imageVector = if (gain >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (gain >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Maturity Countdown
        daysToMaturity?.let { days ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (days <= 30) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (days < 0) "Matured" else "Matures In",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = when {
                                days < 0 -> "${-days} days ago"
                                days == 0L -> "Today"
                                days < 30 -> "$days days"
                                days < 365 -> "${days / 30} months"
                                else -> "${days / 365} years ${(days % 365) / 30} months"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (days <= 30) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Details Card
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow(label = "Start Date", value = investment.startDate.format(DATE_FMT))
                investment.maturityDate?.let {
                    DetailRow(label = "Maturity Date", value = it.format(DATE_FMT))
                }
                investment.interestRate?.let {
                    DetailRow(label = "Interest Rate", value = "%.2f%%".format(it))
                }
                investment.policyNumber?.let {
                    DetailRow(label = "Policy / Account No.", value = it)
                }
                investment.nominee?.let {
                    DetailRow(label = "Nominee", value = it)
                }
                investment.notes?.let {
                    DetailRow(label = "Notes", value = it)
                }
                DetailRow(
                    label = "Status",
                    value = if (investment.isActive) "Active" else "Inactive"
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FinancialCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
