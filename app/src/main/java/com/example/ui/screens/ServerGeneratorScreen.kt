package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BuiltInServers
import com.example.data.model.UdpProtocolMode
import com.example.data.model.VpnServer
import com.example.domain.ServerGenerator
import com.example.ui.components.CountryFlagBadge
import com.example.ui.theme.LocalMegaColors

@Composable
fun ServerGeneratorScreen(
    lastGeneratedServer: VpnServer?,
    onGenerateServer: (
        country: com.example.data.model.CountryTemplate,
        protocol: UdpProtocolMode,
        customPort: Int?,
        mtu: Int,
        customName: String?,
        sniOrBugHost: String
    ) -> Unit,
    onConnectGenerated: (VpnServer) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val megaColors = LocalMegaColors.current
    val context = LocalContext.current

    var selectedCountry by remember { mutableStateOf(BuiltInServers.countryTemplates.first()) }
    var selectedProtocol by remember { mutableStateOf(UdpProtocolMode.RAW_UDP) }
    var customPortText by remember { mutableStateOf(UdpProtocolMode.RAW_UDP.defaultPort.toString()) }
    var customNameText by remember { mutableStateOf("") }
    var sniHostText by remember { mutableStateOf("edge.djtrustmega.net") }
    var mtuSlider by remember { mutableFloatStateOf(1420f) }

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
            // Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, megaColors.orange.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vpn_hero_banner_1795125805988),
                            contentDescription = "DJ TRUST MEGA Server Generator banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {}
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = "UDP SERVER GENERATOR",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "Mint dedicated country UDP endpoints, keys & payloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Step 1: Country Flag Picker
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. SELECT COUNTRY NODE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = megaColors.orange
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BuiltInServers.countryTemplates) { country ->
                            val isSelected = selectedCountry == country
                            Surface(
                                modifier = Modifier
                                    .testTag("gen_country_${country.countryCode.lowercase()}")
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedCountry = country },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) megaColors.orange else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White else megaColors.cardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = country.flagEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = country.countryName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = country.city,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: UDP Protocol Mode
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. UDP PROTOCOL & PORT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = megaColors.orange
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UdpProtocolMode.entries) { mode ->
                            val selected = selectedProtocol == mode
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedProtocol = mode
                                    customPortText = mode.defaultPort.toString()
                                },
                                modifier = Modifier.testTag("gen_proto_${mode.name.lowercase()}"),
                                label = { Text("${mode.shortCode} (${mode.defaultPort})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = megaColors.orange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = selectedProtocol.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Step 3: Custom Port, MTU & Payload Host
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, megaColors.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customPortText,
                                onValueChange = { input ->
                                    if (input.length <= 5 && input.all { it.isDigit() }) {
                                        customPortText = input
                                    }
                                },
                                label = { Text("UDP Port") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.4f)
                                    .testTag("gen_port_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = customNameText,
                                onValueChange = { customNameText = it },
                                label = { Text("Profile Label (optional)") },
                                placeholder = { Text("My Mega Node") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.6f)
                                    .testTag("gen_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = sniHostText,
                            onValueChange = { sniHostText = it },
                            label = { Text("SNI / UDP Payload Host") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gen_sni_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MTU Packet Size",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${mtuSlider.toInt()} bytes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = megaColors.orange
                                )
                            }
                            Slider(
                                value = mtuSlider,
                                onValueChange = { mtuSlider = it },
                                valueRange = 1280f..1500f,
                                steps = 21,
                                colors = SliderDefaults.colors(
                                    thumbColor = megaColors.orange,
                                    activeTrackColor = megaColors.orange
                                )
                            )
                        }

                        Button(
                            onClick = {
                                val parsedPort = customPortText.toIntOrNull()?.takeIf { it in 1..65535 }
                                onGenerateServer(
                                    selectedCountry,
                                    selectedProtocol,
                                    parsedPort,
                                    mtuSlider.toInt(),
                                    customNameText,
                                    sniHostText
                                )
                                onShowMessage("Generated ${selectedCountry.flagEmoji} ${selectedCountry.countryName} UDP server!")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("generate_server_cta"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = megaColors.orange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GENERATE UDP SERVER",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }

            // Generated Output Card
            lastGeneratedServer?.let { server ->
                item {
                    val shareUri = remember(server.id) { ServerGenerator.exportShareUri(server) }
                    val configBlock = remember(server.id) { ServerGenerator.exportConfigBlock(server) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_server_result_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = megaColors.terminalBg),
                        border = BorderStroke(1.5.dp, megaColors.orange)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CountryFlagBadge(
                                    flagEmoji = server.flagEmoji,
                                    countryCode = server.countryCode
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${server.host}:${server.port} • MTU ${server.mtu}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = megaColors.orangeBright
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f)
                            ) {
                                Text(
                                    text = configBlock.trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("DJ TRUST MEGA URI", shareUri))
                                        onShowMessage("Copied djtrust:// server URI")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("copy_generated_uri_btn"),
                                    border = BorderStroke(1.dp, megaColors.orange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy URI",
                                        tint = megaColors.orange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy URI", color = Color.White)
                                }

                                Button(
                                    onClick = { onConnectGenerated(server) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("connect_generated_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = megaColors.emerald,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = "Connect generated server",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connect Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
