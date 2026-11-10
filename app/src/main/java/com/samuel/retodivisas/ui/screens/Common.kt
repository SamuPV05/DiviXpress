package com.samuel.retodivisas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samuel.retodivisas.data.TYPE_BUY
import com.samuel.retodivisas.data.TransactionModel
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(ts))

fun money(v: Double): String = String.format(Locale.US, "%,.2f", v)

fun resolveImageModel(pathOrUrl: String?): Any? {
    if (pathOrUrl.isNullOrBlank()) return null
    return if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
        pathOrUrl
    } else {
        File(pathOrUrl)
    }
}

@Composable
fun TransactionCard(tx: TransactionModel, email: String? = null, adminView: Boolean = false) {
    val flagged = tx.requiresAudit
    val isBuy = tx.transactionType.equals(TYPE_BUY, ignoreCase = true)
    val imageModel = resolveImageModel(tx.photoPath)
    val effectiveAgreedRate = tx.agreedRate.takeIf { it > 0.0 } ?: tx.exchangeRate

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (flagged) AuditRed.copy(alpha = 0.6f) else BorderSubtle
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (adminView && flagged) AuditRedBg.copy(alpha = 0.65f) else SurfaceCard,
            contentColor = TextPrimary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isBuy) SuccessGreenBg else BluePrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isBuy) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isBuy) SuccessGreen else BluePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isBuy) "COMPRA" else "VENTA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isBuy) SuccessGreen else BluePrimary
                            )
                        }
                    }
                    Text(
                        text = tx.currencyPair,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (flagged) AuditRedBg else SuccessGreenBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (flagged) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (flagged) AuditRed else SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (flagged) "Auditoría (>2.5%)" else "Verificada",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (flagged) AuditRed else SuccessGreen
                        )
                    }
                }
            }

            if (!email.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Monto operado",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${if (isBuy) "+" else "-"}$${money(tx.amount)} USD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isBuy) SuccessGreen else TextPrimary
                    )
                }
                Text(
                    text = formatDate(tx.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RateMetricColumn(
                    label = "Tasa Pactada",
                    value = String.format(Locale.US, "%,.2f", effectiveAgreedRate)
                )
                RateMetricColumn(
                    label = "Mercado Finnhub",
                    value = if (tx.marketRate > 0) String.format(Locale.US, "%,.2f", tx.marketRate) else "—"
                )
                RateMetricColumn(
                    label = "Desfase",
                    value = "${String.format(Locale.US, "%.2f", tx.deviationPercent)}%",
                    valueColor = if (flagged) AuditRed else SuccessGreen
                )
            }

            if (imageModel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Comprobante adjunto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun RateMetricColumn(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
