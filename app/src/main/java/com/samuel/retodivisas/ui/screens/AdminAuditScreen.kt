package com.samuel.retodivisas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samuel.retodivisas.ui.theme.AuditRed
import com.samuel.retodivisas.ui.theme.BluePrimary
import com.samuel.retodivisas.ui.theme.BorderSubtle
import com.samuel.retodivisas.ui.theme.SuccessGreen
import com.samuel.retodivisas.ui.theme.SurfaceCard
import com.samuel.retodivisas.ui.theme.TextPrimary
import com.samuel.retodivisas.ui.theme.TextSecondary
import com.samuel.retodivisas.viewmodel.AdminAuditViewModel
import com.samuel.retodivisas.viewmodel.AuditFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditScreen(
    onBack: (() -> Unit)? = null,
    onGoToTrade: (() -> Unit)? = null,
    onProfile: (() -> Unit)? = null,
    onHardware: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    vm: AdminAuditViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val allTransactions by vm.totalRows.collectAsStateWithLifecycle(initialValue = emptyList())
    val displayedTransactions by vm.filteredRows.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalCount = allTransactions.size
    val flaggedCount = allTransactions.count { it.requiresAudit }
    val normalCount = totalCount - flaggedCount

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel de Auditoría",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "DiviXpress · Regla de Desfase 2.5%",
                            style = MaterialTheme.typography.labelSmall,
                            color = BluePrimary
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                },
                actions = {
                    if (onGoToTrade != null) {
                        IconButton(onClick = onGoToTrade) {
                            Icon(Icons.Default.CurrencyExchange, "Mesa de Operaciones", tint = BluePrimary)
                        }
                    }
                    if (onHardware != null) {
                        IconButton(onClick = onHardware) {
                            Icon(Icons.Default.Build, "Diagnóstico Hardware")
                        }
                    }
                    if (onProfile != null) {
                        IconButton(onClick = onProfile) {
                            Icon(Icons.Default.Person, "Perfil")
                        }
                    }
                    if (onLogout != null) {
                        IconButton(onClick = {
                            vm.logout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (!uiState.isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Acceso restringido únicamente a administradores",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AuditKpiCard(
                            title = "Total",
                            count = totalCount,
                            accentColor = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        AuditKpiCard(
                            title = "En Norma (≤2.5%)",
                            count = normalCount,
                            accentColor = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        AuditKpiCard(
                            title = "Alerta (>2.5%)",
                            count = flaggedCount,
                            accentColor = AuditRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.filter == AuditFilter.ALL,
                            onClick = { vm.setFilter(AuditFilter.ALL) },
                            label = { Text("Todas ($totalCount)") }
                        )
                        FilterChip(
                            selected = uiState.filter == AuditFilter.FLAGGED_ONLY,
                            onClick = { vm.setFilter(AuditFilter.FLAGGED_ONLY) },
                            label = { Text("En Alerta ($flaggedCount)") }
                        )
                        FilterChip(
                            selected = uiState.filter == AuditFilter.NORMAL_ONLY,
                            onClick = { vm.setFilter(AuditFilter.NORMAL_ONLY) },
                            label = { Text("Normales ($normalCount)") }
                        )
                    }
                }

                if (displayedTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay transacciones registradas para este filtro",
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(displayedTransactions, key = { it.id }) { item ->
                        TransactionCard(item, email = item.email.ifBlank { null }, adminView = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditKpiCard(
    title: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (count > 0) accentColor else TextPrimary
            )
        }
    }
}
