package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConnectionState
import com.example.ui.theme.LocalMegaColors

@Composable
fun MegaBrandTopBar(
    connectionState: ConnectionState,
    protocolTag: String,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_mega_vpn_logo_1795125790731),
                contentDescription = "DJ TRUST MEGA logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .border(1.dp, megaColors.orange, RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DJ TRUST ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "MEGA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = megaColors.orange
                    )
                }
                Text(
                    text = "UDP VPN • $protocolTag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier
                .testTag("top_support_chip")
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSupportClick),
            color = if (connectionState == ConnectionState.CONNECTED) {
                megaColors.emeraldContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(50),
            border = BorderStroke(
                1.dp,
                if (connectionState == ConnectionState.CONNECTED) megaColors.emerald else megaColors.cardBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (connectionState == ConnectionState.CONNECTED) megaColors.emerald
                            else megaColors.orange
                        )
                )
                Text(
                    text = if (connectionState == ConnectionState.CONNECTED) "ONLINE" else "+256 742...",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (connectionState == ConnectionState.CONNECTED) megaColors.emerald
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MegaPowerCoreButton(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "vpn_pulse")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connectionState == ConnectionState.CONNECTING) 1100 else 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_ring"
    )

    val coreColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CONNECTED -> megaColors.orange
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> megaColors.amber
            ConnectionState.ERROR -> megaColors.crimson
            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "core_color"
    )

    val accentRingColor = when (connectionState) {
        ConnectionState.CONNECTED -> megaColors.emerald
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> megaColors.orange
        ConnectionState.ERROR -> megaColors.crimson
        ConnectionState.DISCONNECTED -> megaColors.orange.copy(alpha = 0.45f)
    }

    val stateDesc = when (connectionState) {
        ConnectionState.CONNECTED -> "Disconnect VPN"
        ConnectionState.CONNECTING -> "Connecting VPN"
        ConnectionState.RECONNECTING -> "Reconnecting VPN"
        ConnectionState.DISCONNECTED, ConnectionState.ERROR -> "Connect VPN"
    }

    Box(
        modifier = modifier
            .size(210.dp)
            .semantics { contentDescription = stateDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.36f

            if (connectionState != ConnectionState.DISCONNECTED) {
                val maxExpand = size.minDimension * 0.13f
                val pulseRadius = baseRadius + (maxExpand * pulseProgress)
                drawCircle(
                    color = accentRingColor.copy(alpha = (1f - pulseProgress) * 0.55f),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Outer track ring
            drawCircle(
                color = megaColors.cardBorder.copy(alpha = 0.7f),
                radius = baseRadius + 14.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Active rotating or glowing arc
            val arcSweep = when (connectionState) {
                ConnectionState.CONNECTED -> 360f
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> 120f
                else -> 90f
            }
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        megaColors.orange,
                        Color.White,
                        megaColors.orange
                    )
                ),
                startAngle = sweepAngle,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - (baseRadius + 14.dp.toPx()),
                    center.y - (baseRadius + 14.dp.toPx())
                ),
                size = androidx.compose.ui.geometry.Size(
                    (baseRadius + 14.dp.toPx()) * 2,
                    (baseRadius + 14.dp.toPx()) * 2
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Surface(
            modifier = Modifier
                .size(142.dp)
                .testTag("vpn_power_button")
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = coreColor,
            shadowElevation = 14.dp,
            border = BorderStroke(
                width = 2.5.dp,
                color = if (connectionState == ConnectionState.CONNECTED) Color.White else megaColors.orange
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (connectionState) {
                        ConnectionState.CONNECTED -> Icons.Rounded.Shield
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Icons.Rounded.Bolt
                        else -> Icons.Rounded.PowerSettingsNew
                    },
                    contentDescription = stateDesc,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (connectionState) {
                        ConnectionState.DISCONNECTED -> "TAP TO\nCONNECT"
                        ConnectionState.CONNECTING -> "LINKING..."
                        ConnectionState.CONNECTED -> "CONNECTED"
                        ConnectionState.RECONNECTING -> "RETRYING"
                        ConnectionState.ERROR -> "RETRY"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ThroughputSparkline(
    downloadSamples: List<Float>,
    uploadSamples: List<Float>,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    val gridColor = megaColors.cardBorder.copy(alpha = 0.45f)
    val dlColor = megaColors.orange
    val ulColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Subtle horizontal grid lines
        for (i in 1..2) {
            val y = h * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val maxVal = maxOf(
            downloadSamples.maxOrNull() ?: 1f,
            uploadSamples.maxOrNull() ?: 1f,
            5f
        )

        fun buildSeriesPath(samples: List<Float>): Path {
            val path = Path()
            if (samples.isEmpty()) return path
            val stepX = if (samples.size > 1) w / (samples.size - 1) else w
            samples.forEachIndexed { idx, value ->
                val x = idx * stepX
                val normalized = (value / maxVal).coerceIn(0f, 1f)
                val y = h - (normalized * (h * 0.85f)) - (h * 0.05f)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        val ulPath = buildSeriesPath(uploadSamples)
        drawPath(
            path = ulPath,
            color = ulColor.copy(alpha = 0.65f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val dlPath = buildSeriesPath(downloadSamples)
        drawPath(
            path = dlPath,
            color = dlColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CountryFlagBadge(
    flagEmoji: String,
    countryCode: String,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    Surface(
        modifier = modifier.size(46.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, megaColors.cardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = flagEmoji.ifEmpty { countryCode },
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun LatencyBadge(pingMs: Int, modifier: Modifier = Modifier) {
    val megaColors = LocalMegaColors.current
    val badgeColor = when {
        pingMs <= 70 -> megaColors.emerald
        pingMs <= 145 -> megaColors.orange
        else -> megaColors.crimson
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Text(
            text = "${pingMs}ms",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatBytesHuman(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
    bytes >= 1024L * 1024L -> String.format("%.2f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

fun formatSpeedHuman(bytesPerSec: Long): String = when {
    bytesPerSec >= 1024L * 1024L -> String.format("%.2f MB/s", bytesPerSec / (1024f * 1024f))
    bytesPerSec >= 1024L -> String.format("%.1f KB/s", bytesPerSec / 1024f)
    else -> "$bytesPerSec B/s"
}

fun formatDurationTimer(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
