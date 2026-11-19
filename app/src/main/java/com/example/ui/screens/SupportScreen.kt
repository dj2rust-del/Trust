package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LogLevel
import com.example.data.model.TunnelLogEntry
import com.example.ui.theme.LocalMegaColors

@Composable
fun SupportScreen(
    logs: List<TunnelLogEntry>,
    onClearLogs: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    val context = LocalContext.current
    val phoneRaw = stringResource(id = R.string.support_phone_raw)
    val phoneDisplay = stringResource(id = R.string.support_phone_display)

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
            // Branded DJ TRUST MEGA Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = megaColors.terminalBg),
                    border = BorderStroke(1.5.dp, megaColors.orange)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_mega_vpn_logo_1795125790731),
                            contentDescription = "DJ TRUST MEGA official logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DJ TRUST ",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "MEGA",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = megaColors.orange
                                )
                            }
                            Text(
                                text = "High-Speed UDP VPN Suite • v1.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = megaColors.orange.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "UDP • QUIC • WIREGUARD • PORT 53 • UDPGW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = megaColors.orangeBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Direct Support Contact Card (+256742222056)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_contact_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = megaColors.orange.copy(alpha = 0.18f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SupportAgent,
                                        contentDescription = "Customer support",
                                        tint = megaColors.orange
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "24/7 TECHNICAL SUPPORT",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = megaColors.orange
                                )
                                Text(
                                    text = phoneDisplay,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "Need custom UDP server ports, premium accounts, or carrier payload assistance? Reach DJ TRUST MEGA directly:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneRaw"))
                                    runCatching { context.startActivity(dialIntent) }
                                        .onFailure { onShowMessage("No dialer app available") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("support_call_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = megaColors.orange,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = "Call support", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call Now")
                            }

                            Button(
                                onClick = {
                                    val waIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/256742222056")
                                    )
                                    runCatching { context.startActivity(waIntent) }
                                        .onFailure { onShowMessage("Could not open WhatsApp") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("support_whatsapp_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = megaColors.emerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Message,
                                    contentDescription = "WhatsApp support",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneRaw"))
                                    runCatching { context.startActivity(smsIntent) }
                                        .onFailure { onShowMessage("No SMS app available") }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("support_sms_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Send SMS")
                            }

                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("DJ TRUST MEGA Contact", phoneRaw))
                                    onShowMessage("Copied $phoneRaw")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("support_copy_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Number")
                            }
                        }
                    }
                }
            }

            // System Always-On / Kill Switch VPN Settings shortcut
            item {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
                        runCatching { context.startActivity(intent) }
                            .onFailure { onShowMessage("Unable to open Android VPN settings") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("android_vpn_settings_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SettingsApplications,
                        contentDescription = null,
                        tint = megaColors.orange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Android Always-On & Lockdown VPN Settings")
                }
            }

            // Full Tunnel Diagnostics Log Viewer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = megaColors.terminalBg),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = megaColors.orange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "UDP DIAGNOSTIC LOGS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val dump = logs.joinToString("\n") { "[${it.timestamp}] ${it.message}" }
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("DJ TRUST MEGA Logs", dump))
                                        onShowMessage("Logs copied to clipboard")
                                    },
                                    modifier = Modifier.testTag("copy_logs_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy logs",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onClearLogs,
                                    modifier = Modifier.testTag("clear_logs_btn")
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteSweep,
                                        contentDescription = "Clear logs",
                                        tint = megaColors.orange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (logs.isEmpty()) {
                            Text(
                                text = "No tunnel events logged yet.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                logs.takeLast(25).forEach { entry ->
                                    val color = when (entry.level) {
                                        LogLevel.SUCCESS -> megaColors.emerald
                                        LogLevel.WARN -> megaColors.amber
                                        LogLevel.ERROR -> megaColors.crimson
                                        LogLevel.INFO -> megaColors.orangeBright
                                    }
                                    Text(
                                        text = "[${entry.timestamp}] ${entry.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = color
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
