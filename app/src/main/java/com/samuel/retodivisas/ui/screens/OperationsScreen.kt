package com.samuel.retodivisas.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samuel.retodivisas.data.AuditRule
import com.samuel.retodivisas.data.TYPE_BUY
import com.samuel.retodivisas.data.TYPE_SELL
import com.samuel.retodivisas.hardware.CameraHelper
import com.samuel.retodivisas.ui.theme.AuditRed
import com.samuel.retodivisas.ui.theme.AuditRedBg
import com.samuel.retodivisas.ui.theme.BluePrimary
import com.samuel.retodivisas.ui.theme.BorderSubtle
import com.samuel.retodivisas.ui.theme.SuccessGreen
import com.samuel.retodivisas.ui.theme.SuccessGreenBg
import com.samuel.retodivisas.ui.theme.SurfaceCard
import com.samuel.retodivisas.ui.theme.SurfaceElevated
import com.samuel.retodivisas.ui.theme.TextPrimary
import com.samuel.retodivisas.ui.theme.TextSecondary
import com.samuel.retodivisas.viewmodel.CURRENCIES
import com.samuel.retodivisas.viewmodel.OperationsViewModel
import java.util.Locale

@Composable
fun BuyCurrencyScreen(
    onSwitchToSell: () -> Unit,
    onProfile: () -> Unit,
    onAudit: () -> Unit,
    onHardware: () -> Unit,
    onLogout: () -> Unit,
    vm: OperationsViewModel = viewModel()
) {
    CurrencyTradeScreen(
        operationType = TYPE_BUY,
        onSwitchOperation = onSwitchToSell,
        onProfile = onProfile,
        onAudit = onAudit,
        onHardware = onHardware,
        onLogout = onLogout,
        vm = vm
    )
}

@Composable
fun SellCurrencyScreen(
    onSwitchToBuy: () -> Unit,
    onProfile: () -> Unit,
    onAudit: () -> Unit,
    onHardware: () -> Unit,
    onLogout: () -> Unit,
    vm: OperationsViewModel = viewModel()
) {
    CurrencyTradeScreen(
        operationType = TYPE_SELL,
        onSwitchOperation = onSwitchToBuy,
        onProfile = onProfile,
        onAudit = onAudit,
        onHardware = onHardware,
        onLogout = onLogout,
        vm = vm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyTradeScreen(
    operationType: String,
    onSwitchOperation: () -> Unit,
    onProfile: () -> Unit,
    onAudit: () -> Unit,
    onHardware: () -> Unit,
    onLogout: () -> Unit,
    vm: OperationsViewModel
) {
    val isBuy = operationType == TYPE_BUY
    val accentColor = if (isBuy) SuccessGreen else BluePrimary
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val allHistory by vm.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val balance by vm.balance.collectAsStateWithLifecycle(initialValue = 0.0)

    val parsedAmount = state.amount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val marketRate = state.rate
    val effectiveRate = state.agreedRate.trim().replace(',', '.').toDoubleOrNull() ?: marketRate
    val convertedForeignAmount = if (effectiveRate != null && parsedAmount > 0.0) {
        parsedAmount * effectiveRate
    } else {
        0.0
    }
    val projectedBalance = if (isBuy) balance + parsedAmount else balance - parsedAmount
    val insufficientBalance = !isBuy && parsedAmount > balance

    val liveAuditPreview = if (marketRate != null && marketRate > 0.0 && effectiveRate != null && effectiveRate > 0.0) {
        AuditRule.evaluate(effectiveRate, marketRate)
    } else {
        null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.onPhotoSelectedFromGallery(uri)
            Toast.makeText(context, "Comprobante seleccionado", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vm.onPhotoCaptured(bitmap)
            Toast.makeText(context, "Comprobante capturado", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    if (state.auditAlert) {
        AlertDialog(
            onDismissRequest = vm::dismissAuditAlert,
            title = { Text("Operación enviada a Auditoría (> 2.5%)") },
            text = {
                Text(
                    "La tasa pactada presenta una desviación mayor al 2.5% frente a la cotización de Finnhub en el instante de la transacción. Se ha registrado y marcado automáticamente para revisión del Administrador."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::dismissAuditAlert) { Text("Entendido") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DiviXpress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isBuy) "Mesa de Compra · Divisas" else "Mesa de Venta · Divisas",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (state.isAdmin) {
                        IconButton(onClick = onAudit) {
                            Icon(Icons.Default.AdminPanelSettings, "Panel de Auditoría", tint = BluePrimary)
                        }
                    }
                    IconButton(onClick = onHardware) {
                        Icon(Icons.Default.Build, "Diagnóstico Hardware")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceCard) {
                NavigationBarItem(
                    selected = isBuy,
                    onClick = { if (!isBuy) onSwitchOperation() },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Comprar") },
                    label = { Text("Comprar") }
                )
                NavigationBarItem(
                    selected = !isBuy,
                    onClick = { if (isBuy) onSwitchOperation() },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "Vender") },
                    label = { Text("Vender") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Tarjeta de Portafolio y Cotización Finnhub
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Saldo disponible",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$${money(balance)} USD",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Finnhub · 1 USD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    when {
                                        state.loadingRate -> CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(18.dp)
                                        )
                                        state.rate != null -> Text(
                                            text = "${String.format(Locale.US, "%,.2f", state.rate)} ${state.currency}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        else -> Text(
                                            text = "—",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = vm::loadRate,
                                    enabled = !state.loadingRate,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar cotización",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Par de divisa objetivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CURRENCIES.forEach { c ->
                                    FilterChip(
                                        selected = state.currency == c,
                                        onClick = { vm.selectCurrency(c) },
                                        label = { Text("USD/$c") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Tarjeta de Orden de Operación
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBuy) "Orden de Compra (+USD)" else "Orden de Venta (-USD)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "USD/${state.currency}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.amount,
                            onValueChange = vm::onAmountChange,
                            label = { Text(if (isBuy) "Monto a comprar (USD)" else "Monto a vender (USD)") },
                            prefix = { Text("$ ") },
                            suffix = { Text("USD") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = state.agreedRate,
                            onValueChange = vm::onAgreedRateChange,
                            label = { Text("Tasa pactada (opcional)") },
                            placeholder = {
                                Text(
                                    if (state.rate != null) {
                                        "Mercado: ${String.format(Locale.US, "%,.2f", state.rate)} ${state.currency}"
                                    } else {
                                        "Vacío usa tasa Finnhub"
                                    }
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                if (state.rate != null) {
                                    TextButton(onClick = vm::useMarketRateAsAgreed) {
                                        Text("Usar mercado", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )

                        // Resumen en vivo de la orden
                        if (parsedAmount > 0.0 || (liveAuditPreview != null && state.agreedRate.isNotBlank())) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (parsedAmount > 0.0) {
                                    if (effectiveRate != null && effectiveRate > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Equivalente en ${state.currency}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${money(convertedForeignAmount)} ${state.currency}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isBuy) "Nuevo saldo proyectado" else "Saldo restante proyectado",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "$${money(projectedBalance)} USD",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (insufficientBalance) AuditRed else accentColor
                                        )
                                    }
                                    if (insufficientBalance) {
                                        Text(
                                            text = "Saldo insuficiente para cubrir esta venta.",
                                            color = AuditRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (liveAuditPreview != null && state.agreedRate.isNotBlank()) {
                                    val flagged = liveAuditPreview.requiresAudit
                                    Surface(
                                        color = if (flagged) AuditRedBg else SuccessGreenBg,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (flagged) Icons.Default.Warning else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (flagged) AuditRed else SuccessGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (flagged) {
                                                    "Desfase: ${String.format(Locale.US, "%.2f", liveAuditPreview.deviationPercent)}% · Supera 2.5% (Requiere Auditoría)"
                                                } else {
                                                    "Desfase: ${String.format(Locale.US, "%.2f", liveAuditPreview.deviationPercent)}% · En norma (≤ 2.5%)"
                                                },
                                                color = if (flagged) AuditRed else SuccessGreen,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderSubtle)

                        // Sección Comprobante
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Comprobante de soporte (opcional)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (CameraHelper.hasCameraPermission(context)) {
                                            cameraLauncher.launch(null)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cámara")
                                }

                                OutlinedButton(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Galería")
                                }

                                if (state.photoBitmap != null) {
                                    IconButton(onClick = vm::removePhoto) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Quitar comprobante",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (state.photoBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = state.photoBitmap!!.asImageBitmap(),
                                        contentDescription = "Comprobante de operación",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        state.error?.let { err ->
                            Surface(
                                color = AuditRedBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    color = AuditRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        state.message?.let { msg ->
                            Surface(
                                color = SuccessGreenBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { vm.execute(operationType) },
                            enabled = !state.executing && !insufficientBalance,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF042F2E)
                            )
                        ) {
                            if (state.executing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF042F2E)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Verificando en Finnhub...", fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    text = if (isBuy) {
                                        "Confirmar Compra · USD/${state.currency}"
                                    } else {
                                        "Confirmar Venta · USD/${state.currency}"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Historial de Operaciones
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis operaciones recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${allHistory.size} registros",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (allHistory.isEmpty()) {
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
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no tienes operaciones registradas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(allHistory, key = { it.id }) { tx ->
                    TransactionCard(tx)
                }
            }
        }
    }
}
