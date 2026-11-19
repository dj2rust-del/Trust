package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConnectionState
import com.example.data.model.LogLevel
import com.example.data.model.UdpSettings
import com.example.data.model.VpnServer
import com.example.data.model.VpnTelemetry
import com.example.ui.components.CountryFlagBadge
import com.example.ui.components.LatencyBadge
import com.example.ui.components.MegaBrandTopBar
import com.example.ui.components.MegaPowerCoreButton
import com.example.ui.components.ThroughputSparkline
import com.example.ui.components.formatBytesHuman
import com.example.ui.components.formatDurationTimer
import com.example.ui.components.formatSpeedHuman
import com.example.ui.theme.LocalMegaColors

@Composable
fun DashboardScreen(
    telemetry: VpnTelemetry,
    selectedServer: VpnServer,
    settings: UdpSettings,
    onToggleConnection: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onToggleKillSwitch: () -> Unit,
    onToggleBadVpn: () -> Unit,
    onToggleObfuscation: () -> Unit,
    onToggleDnsGuard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    var showConsole by remember { mutableStateOf(false) }

    val activeProtocol = settings.protocolOverride ?: selectedServer.protocolMode
    val activePort = if (settings.customUdpPort in 1..65535) {
        settings.customUdpPort
    } else {
        selectedServer.port
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MegaBrandTopBar(
                    connectionState = telemetry.connectionState,
                    protocolTag = "${activeProtocol.shortCode}:$activePort",
                    onSupportClick = onNavigateToSupport
                )
            }

            // Hero Power Core + Session Status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when (telemetry.connectionState) {
                            ConnectionState.CONNECTED -> megaColors.emeraldContainer
                            ConnectionState.CONNECTING, ConnectionState.RECONNECTING ->
                                megaColors.orange.copy(alpha = 0.16f)
                            ConnectionState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            when (telemetry.connectionState) {
                                ConnectionState.CONNECTED -> megaColors.emerald
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> megaColors.orange
                                ConnectionState.ERROR -> megaColors.crimson
                                ConnectionState.DISCONNECTED -> megaColors.cardBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (telemetry.connectionState) {
                                            ConnectionState.CONNECTED -> megaColors.emerald
                                            ConnectionState.ERROR -> megaColors.crimson
                                            else -> megaColors.orange
                                        }
                                    )
                            )
                            Text(
                                text = if (telemetry.connectionState == ConnectionState.CONNECTED) {
                                    "PROTECTED • ${formatDurationTimer(telemetry.elapsedSeconds)}"
                                } else {
                                    telemetry.connectionState.label
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (telemetry.connectionState == ConnectionState.CONNECTED) {
                                    megaColors.emerald
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    MegaPowerCoreButton(
                        connectionState = telemetry.connectionState,
                        onClick = onToggleConnection
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (telemetry.connectionState == ConnectionState.CONNECTED) {
                            "TUN ${telemetry.virtualIp} • DNS ${settings.resolvedPrimaryDns()} • MTU ${settings.mtuSize}"
                        } else {
                            "Ready • ${activeProtocol.label} • MTU ${settings.mtuSize} • ${settings.udpBufferKb}KB Buf"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    telemetry.errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = megaColors.crimson
                        )
                    }
                }
            }

            // Active Country Flag Server Card + Generate Shortcut
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_server_card")
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onNavigateToServers),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, megaColors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountryFlagBadge(
                                flagEmoji = selectedServer.flagEmoji,
                                countryCode = selectedServer.countryCode
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedServer.countryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selectedServer.isCustomGenerated) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = megaColors.orange.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "GEN",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = megaColors.orange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${selectedServer.city} • ${activeProtocol.shortCode} :$activePort",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            LatencyBadge(
                                pingMs = if (telemetry.connectionState == ConnectionState.CONNECTED && telemetry.currentPingMs > 0) {
                                    telemetry.currentPingMs
                                } else {
                                    selectedServer.pingMs
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Switch server",
                                tint = megaColors.orange
                            )
                        }
                    }

                    // Action Row: Generate Custom UDP Server & Support Contact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_generate_server_btn")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateToGenerator),
                            shape = RoundedCornerShape(14.dp),
                            color = megaColors.orange,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Build,
                                    contentDescription = "Generate UDP server",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Generate Server",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_support_btn")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateToSupport),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, megaColors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Phone,
                                    contentDescription = "Support phone",
                                    tint = megaColors.orange,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+256742222056",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Live Speed & Throughput Sparkline
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = megaColors.orange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "LIVE UDP THROUGHPUT",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "PKTS ${telemetry.totalPacketsIn}/${telemetry.totalPacketsOut}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ThroughputSparkline(
                            downloadSamples = telemetry.downloadHistory,
                            uploadSamples = telemetry.uploadHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TrafficStatTile(
                                icon = Icons.Rounded.ArrowDownward,
                                label = "DOWNLOAD",
                                speed = formatSpeedHuman(telemetry.downloadBytesPerSec),
                                total = formatBytesHuman(telemetry.totalDownloadBytes),
                                accent = megaColors.orange,
                                modifier = Modifier.weight(1f)
                            )
                            TrafficStatTile(
                                icon = Icons.Rounded.ArrowUpward,
                                label = "UPLOAD",
                                speed = formatSpeedHuman(telemetry.uploadBytesPerSec),
                                total = formatBytesHuman(telemetry.totalUploadBytes),
                                accent = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick UDP VPN Feature Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "UDP ENGINE MODULES",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            UdpQuickToggleChip(
                                label = "Kill Switch",
                                selected = settings.killSwitchEnabled,
                                icon = Icons.Rounded.Security,
                                tag = "chip_kill_switch",
                                onClick = onToggleKillSwitch
                            )
                        }
                        item {
                            UdpQuickToggleChip(
                                label = "BadVPN ${settings.badVpnPort}",
                                selected = settings.badVpnEnabled,
                                icon = Icons.Rounded.Bolt,
                                tag = "chip_badvpn",
                                onClick = onToggleBadVpn
                            )
                        }
                        item {
                            UdpQuickToggleChip(
                                label = "UDP Obfuscation",
                                selected = settings.udpObfuscationEnabled,
                                icon = Icons.Rounded.Tune,
                                tag = "chip_obfuscation",
                                onClick = onToggleObfuscation
                            )
                        }
                        item {
                            UdpQuickToggleChip(
                                label = "DNS Leak Guard",
                                selected = settings.dnsLeakProtection,
                                icon = Icons.Rounded.Dns,
                                tag = "chip_dns_guard",
                                onClick = onToggleDnsGuard
                            )
                        }
                    }
                }
            }

            // Expandable Live Tunnel Console
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("live_console_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = megaColors.terminalBg),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showConsole = !showConsole }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = "Tunnel console",
                                    tint = megaColors.orange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "LIVE UDP TUNNEL LOGS (${telemetry.logs.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = if (showConsole) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (showConsole) "Collapse logs" else "Expand logs",
                                tint = megaColors.orange
                            )
                        }

                        AnimatedVisibility(visible = showConsole) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                telemetry.logs.takeLast(8).forEach { entry ->
                                    val levelColor = when (entry.level) {
                                        LogLevel.SUCCESS -> megaColors.emerald
                                        LogLevel.WARN -> megaColors.amber
                                        LogLevel.ERROR -> megaColors.crimson
                                        LogLevel.INFO -> megaColors.orangeBright
                                    }
                                    Text(
                                        text = "[${entry.timestamp}] ${entry.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = levelColor
                                    )
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
private fun TrafficStatTile(
    icon: ImageVector,
    label: String,
    speed: String,
    total: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, megaColors.cardBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = speed,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = "Total $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UdpQuickToggleChip(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val megaColors = LocalMegaColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = megaColors.orange,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}
