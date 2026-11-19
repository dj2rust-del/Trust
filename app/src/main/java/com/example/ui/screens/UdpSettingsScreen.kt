package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DnsPreset
import com.example.data.model.InstalledAppInfo
import com.example.data.model.UdpProtocolMode
import com.example.data.model.UdpSettings
import com.example.ui.theme.LocalMegaColors

@Composable
fun UdpSettingsScreen(
    settings: UdpSettings,
    installedApps: List<InstalledAppInfo>,
    appsLoading: Boolean,
    onUpdateSettings: ((UdpSettings) -> UdpSettings) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onToggleSplitTunnelApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    var showSplitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "UDP VPN CONFIGURATION",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Full UDP tunnel tuning, DNS guard, kill switch & split routing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: UDP Protocol & Port Engine
            item {
                SettingsSectionCard(title = "UDP PROTOCOL & ENGINE") {
                    Text(
                        text = "Protocol Override (Auto uses server default)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = settings.protocolOverride == null,
                                onClick = { onUpdateSettings { it.copy(protocolOverride = null) } },
                                label = { Text("Auto") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(UdpProtocolMode.entries) { mode ->
                            FilterChip(
                                selected = settings.protocolOverride == mode,
                                onClick = { onUpdateSettings { it.copy(protocolOverride = mode) } },
                                label = { Text(mode.shortCode) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom UDP Port
                    var portInput by remember(settings.customUdpPort) {
                        mutableStateOf(
                            if (settings.customUdpPort > 0) settings.customUdpPort.toString() else ""
                        )
                    }
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(5)
                            portInput = clean
                            val parsed = clean.toIntOrNull() ?: 0
                            if (parsed in 0..65535) {
                                onUpdateSettings { it.copy(customUdpPort = parsed) }
                            }
                        },
                        label = { Text("Custom UDP Port Override (blank = server port)") },
                        placeholder = { Text("e.g. 1194, 443, 53, 51820") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_custom_port_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // MTU Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TUN MTU Size", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${settings.mtuSize} bytes",
                            style = MaterialTheme.typography.labelMedium,
                            color = megaColors.orange
                        )
                    }
                    Slider(
                        value = settings.mtuSize.toFloat(),
                        onValueChange = { v -> onUpdateSettings { it.copy(mtuSize = v.toInt()) } },
                        valueRange = 1280f..1500f,
                        steps = 21,
                        colors = SliderDefaults.colors(
                            thumbColor = megaColors.orange,
                            activeTrackColor = megaColors.orange
                        )
                    )

                    // UDP Buffer Size
                    Text("UDP Socket Buffer Size", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(64, 128, 256, 512, 1024)) { kb ->
                            FilterChip(
                                selected = settings.udpBufferKb == kb,
                                onClick = { onUpdateSettings { it.copy(udpBufferKb = kb) } },
                                label = { Text("${kb}KB") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Keep-Alive Interval
                    Text("UDP Keep-Alive Probe Interval", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(5, 15, 30, 60)) { sec ->
                            FilterChip(
                                selected = settings.keepAliveSeconds == sec,
                                onClick = { onUpdateSettings { it.copy(keepAliveSeconds = sec) } },
                                label = { Text("${sec}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        icon = Icons.Rounded.Bolt,
                        title = "BadVPN UDPGW (Port ${settings.badVpnPort})",
                        subtitle = "Accelerate UDP voice calls, gaming & streaming packets",
                        checked = settings.badVpnEnabled,
                        tag = "switch_badvpn",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(badVpnEnabled = enabled) }
                        }
                    )

                    SettingsSwitchRow(
                        icon = Icons.Rounded.Tune,
                        title = "UDP Header Obfuscation",
                        subtitle = "Scramble UDP handshakes with custom HTTP/SNI payload",
                        checked = settings.udpObfuscationEnabled,
                        tag = "switch_obfuscation",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(udpObfuscationEnabled = enabled) }
                        }
                    )

                    if (settings.udpObfuscationEnabled) {
                        OutlinedTextField(
                            value = settings.obfuscationPayload,
                            onValueChange = { txt ->
                                onUpdateSettings { it.copy(obfuscationPayload = txt) }
                            },
                            label = { Text("UDP Custom Payload") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .testTag("settings_payload_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Section 2: Security, Kill Switch & Routing
            item {
                SettingsSectionCard(title = "SECURITY & TUNNEL ROUTING") {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Security,
                        title = "UDP Kill Switch",
                        subtitle = "Arm tunnel guard against unencrypted drops",
                        checked = settings.killSwitchEnabled,
                        tag = "switch_kill_switch",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(killSwitchEnabled = v) } }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Refresh,
                        title = "Auto-Reconnect",
                        subtitle = "Automatically restore UDP tunnel on network handoff",
                        checked = settings.autoReconnect,
                        tag = "switch_auto_reconnect",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(autoReconnect = v) } }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lock,
                        title = "Strict Full Device Route (0.0.0.0/0)",
                        subtitle = "Route all IPv4 traffic via TUN (off = Smart DNS/UDP route)",
                        checked = settings.fullDeviceTunnel,
                        tag = "switch_full_tunnel",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(fullDeviceTunnel = v) } }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Lan,
                        title = "LAN Bypass",
                        subtitle = "Allow local network printers, cast devices & routers",
                        checked = settings.lanBypassEnabled,
                        tag = "switch_lan_bypass",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(lanBypassEnabled = v) } }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Security,
                        title = "Block IPv6 Leaks",
                        subtitle = "Prevent dual-stack IPv6 leaks outside UDP tunnel",
                        checked = settings.blockIpv6Leaks,
                        tag = "switch_block_ipv6",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(blockIpv6Leaks = v) } }
                    )
                }
            }

            // Section 3: DNS Guard & Custom DNS
            item {
                SettingsSectionCard(title = "DNS LEAK PROTECTION & RESOLVER") {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Dns,
                        title = "DNS Leak Protection",
                        subtitle = "Forward all port-53 DNS UDP queries through protected socket",
                        checked = settings.dnsLeakProtection,
                        tag = "switch_dns_leak",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(dnsLeakProtection = v) } }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DnsPreset.entries) { preset ->
                            FilterChip(
                                selected = settings.dnsPreset == preset,
                                onClick = { onUpdateSettings { it.copy(dnsPreset = preset) } },
                                modifier = Modifier.testTag("dns_preset_${preset.name.lowercase()}"),
                                label = {
                                    Text(
                                        if (preset == DnsPreset.CUSTOM) "Custom"
                                        else "${preset.label} (${preset.primary})"
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (settings.dnsPreset == DnsPreset.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = settings.customDnsPrimary,
                                onValueChange = { v -> onUpdateSettings { it.copy(customDnsPrimary = v) } },
                                label = { Text("Primary DNS") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = settings.customDnsSecondary,
                                onValueChange = { v -> onUpdateSettings { it.copy(customDnsSecondary = v) } },
                                label = { Text("Secondary DNS") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Section 4: Split Tunneling & Appearance
            item {
                SettingsSectionCard(title = "SPLIT TUNNELING & APPEARANCE") {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Apps,
                        title = "Split Tunneling (Per-App Bypass)",
                        subtitle = "Allow selected apps to bypass the UDP VPN tunnel",
                        checked = settings.splitTunnelingEnabled,
                        tag = "switch_split_tunnel",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(splitTunnelingEnabled = v) } }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onOpenSplitTunneling()
                                showSplitDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("manage_split_apps_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose Bypassed Apps",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${settings.splitTunnelDisallowedPackages.size} apps bypassing tunnel",
                                style = MaterialTheme.typography.bodySmall,
                                color = megaColors.orange
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Select apps",
                            tint = megaColors.orange
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsSwitchRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Obsidian Black Theme",
                        subtitle = "Signature DJ TRUST MEGA Orange, White & Black dark mode",
                        checked = settings.useDarkTheme,
                        tag = "switch_dark_theme",
                        onCheckedChange = { v -> onUpdateSettings { it.copy(useDarkTheme = v) } }
                    )
                }
            }
        }
    }

    if (showSplitDialog) {
        SplitTunnelAppsDialog(
            apps = installedApps,
            loading = appsLoading,
            disallowedPackages = settings.splitTunnelDisallowedPackages,
            onToggleApp = onToggleSplitTunnelApp,
            onDismiss = { showSplitDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val megaColors = LocalMegaColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, megaColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = megaColors.orange
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val megaColors = LocalMegaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = megaColors.orange,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = megaColors.orange
            )
        )
    }
}

@Composable
private fun SplitTunnelAppsDialog(
    apps: List<InstalledAppInfo>,
    loading: Boolean,
    disallowedPackages: Set<String>,
    onToggleApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val megaColors = LocalMegaColors.current
    var filterText by remember { mutableStateOf("") }
    val filtered = remember(apps, filterText) {
        val q = filterText.trim().lowercase()
        if (q.isEmpty()) apps
        else apps.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Tunneling Apps") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Filter installed apps...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = megaColors.orange)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isBypassed = disallowedPackages.contains(app.packageName)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleApp(app.packageName) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isBypassed,
                                    onCheckedChange = { onToggleApp(app.packageName) },
                                    colors = CheckboxDefaults.colors(checkedColor = megaColors.orange)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = megaColors.orange)
            }
        }
    )
}
