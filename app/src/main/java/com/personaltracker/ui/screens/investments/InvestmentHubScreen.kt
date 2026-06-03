package com.personaltracker.ui.screens.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltracker.domain.repository.DepositRepository
import com.personaltracker.domain.repository.InsuranceRepository
import com.personaltracker.domain.repository.MutualFundRepository
import com.personaltracker.domain.repository.StockRepository
import com.personaltracker.ui.components.PTTopBar
import com.personaltracker.ui.components.formatCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

data class InvestmentHubState(
    val totalInsuranceCoverage: Double = 0.0,
    val totalMfInvestment: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val totalStockInvestment: Double = 0.0,
    val isLoading: Boolean = true
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class InvestmentHubViewModel @Inject constructor(
    private val insuranceRepository: InsuranceRepository,
    private val mutualFundRepository: MutualFundRepository,
    private val depositRepository: DepositRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InvestmentHubState())
    val state: StateFlow<InvestmentHubState> = _state.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            combine(
                insuranceRepository.getTotalCoverage(),
                mutualFundRepository.getTotalSipAmount(),
                mutualFundRepository.getTotalLumpsumAmount(),
                depositRepository.getTotalDeposited(),
                stockRepository.getTotalInvested()
            ) { values ->
                val insuranceCoverage = values[0]
                val sipAmount = values[1]
                val lumpsumAmount = values[2]
                val deposits = values[3]
                val stocks = values[4]
                InvestmentHubState(
                    totalInsuranceCoverage = insuranceCoverage,
                    totalMfInvestment = sipAmount + lumpsumAmount,
                    totalDeposits = deposits,
                    totalStockInvestment = stocks,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private UI helpers
// ---------------------------------------------------------------------------

private data class SummaryCardData(
    val title: String,
    val amount: Double,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
    val subtitle: String? = null
)

private data class CategoryTileData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun InvestmentSummaryCard(
    data: SummaryCardData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(data.gradientStart, data.gradientEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.title,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatCurrency(data.amount),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (data.subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = data.subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    data: CategoryTileData,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = data.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(data.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Screen composable
// ---------------------------------------------------------------------------

@Composable
fun InvestmentHubScreen(
    onBack: () -> Unit,
    onNavigateToInsurance: () -> Unit,
    onNavigateToMutualFunds: () -> Unit,
    onNavigateToDeposits: () -> Unit,
    onNavigateToStocks: () -> Unit,
    viewModel: InvestmentHubViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Colour palette
    val blueStart = Color(0xFF1565C0)
    val blueEnd = Color(0xFF1E88E5)
    val purpleStart = Color(0xFF6A1B9A)
    val purpleEnd = Color(0xFF9C27B0)
    val orangeStart = Color(0xFFE65100)
    val orangeEnd = Color(0xFFFB8C00)
    val redStart = Color(0xFFB71C1C)
    val redEnd = Color(0xFFE53935)

    val summaryCards = listOf(
        SummaryCardData(
            title = "Insurance Coverage",
            amount = state.totalInsuranceCoverage,
            icon = Icons.Default.HealthAndSafety,
            gradientStart = blueStart,
            gradientEnd = blueEnd,
            subtitle = "Total sum assured (active policies)"
        ),
        SummaryCardData(
            title = "Mutual Funds",
            amount = state.totalMfInvestment,
            icon = Icons.Default.ShowChart,
            gradientStart = purpleStart,
            gradientEnd = purpleEnd,
            subtitle = "SIP + Lumpsum invested"
        ),
        SummaryCardData(
            title = "Deposits (FD/RD/PPF/NPS)",
            amount = state.totalDeposits,
            icon = Icons.Default.AccountBalance,
            gradientStart = orangeStart,
            gradientEnd = orangeEnd,
            subtitle = "Total deposited amount"
        ),
        SummaryCardData(
            title = "Stocks",
            amount = state.totalStockInvestment,
            icon = Icons.Default.TrendingUp,
            gradientStart = redStart,
            gradientEnd = redEnd,
            subtitle = "Total invested in equities"
        )
    )

    val categoryTiles = listOf(
        CategoryTileData(
            title = "Life Insurance",
            description = "Policies & coverage",
            icon = Icons.Default.Favorite,
            color = Color(0xFF1565C0),
            onClick = onNavigateToInsurance
        ),
        CategoryTileData(
            title = "Health Insurance",
            description = "Medical coverage",
            icon = Icons.Default.LocalHospital,
            color = Color(0xFF0097A7),
            onClick = onNavigateToInsurance
        ),
        CategoryTileData(
            title = "Term Insurance",
            description = "Pure protection plans",
            icon = Icons.Default.Shield,
            color = Color(0xFF00695C),
            onClick = onNavigateToInsurance
        ),
        CategoryTileData(
            title = "Mutual Funds",
            description = "SIP & lumpsum funds",
            icon = Icons.Default.ShowChart,
            color = Color(0xFF6A1B9A),
            onClick = onNavigateToMutualFunds
        ),
        CategoryTileData(
            title = "Deposits",
            description = "RD / PPF / FD / NPS",
            icon = Icons.Default.AccountBalance,
            color = Color(0xFFE65100),
            onClick = onNavigateToDeposits
        ),
        CategoryTileData(
            title = "Stocks",
            description = "Equity investments",
            icon = Icons.Default.TrendingUp,
            color = Color(0xFFB71C1C),
            onClick = onNavigateToStocks
        )
    )

    Scaffold(
        topBar = {
            PTTopBar(title = "Investment Hub", onBack = onBack)
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary section header
                Text(
                    text = "Portfolio Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Summary cards (2 per row)
                val summaryRows = summaryCards.chunked(2)
                summaryRows.forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCards.forEach { cardData ->
                            InvestmentSummaryCard(
                                data = cardData,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of cards
                        if (rowCards.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Categories section header
                Text(
                    text = "Investment Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Category tiles grid (2 columns)
                val tileRows = categoryTiles.chunked(2)
                tileRows.forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowTiles.forEach { tile ->
                            CategoryTile(
                                data = tile,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowTiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
