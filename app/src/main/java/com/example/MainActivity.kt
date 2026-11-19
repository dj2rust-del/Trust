package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ConnectionState
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ServerGeneratorScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.UdpSettingsScreen
import com.example.ui.theme.DjTrustMegaTheme
import com.example.ui.theme.LocalMegaColors
import com.example.ui.viewmodel.VpnViewModel
import kotlinx.coroutines.launch

private data class MegaNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_SERVERS = "servers"
private const val ROUTE_GENERATOR = "generator"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SUPPORT = "support"

private val megaDestinations = listOf(
    MegaNavDestination(
        route = ROUTE_DASHBOARD,
        label = "Connect",
        selectedIcon = Icons.Rounded.Shield,
        unselectedIcon = Icons.Outlined.Shield
    ),
    MegaNavDestination(
        route = ROUTE_SERVERS,
        label = "Servers",
        selectedIcon = Icons.Rounded.Public,
        unselectedIcon = Icons.Outlined.Public
    ),
    MegaNavDestination(
        route = ROUTE_GENERATOR,
        label = "Generate",
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    MegaNavDestination(
        route = ROUTE_SETTINGS,
        label = "UDP",
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    MegaNavDestination(
        route = ROUTE_SUPPORT,
        label = "Support",
        selectedIcon = Icons.Rounded.SupportAgent,
        unselectedIcon = Icons.Outlined.SupportAgent
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DjTrustMegaApp()
        }
    }
}

@Composable
fun DjTrustMegaApp(viewModel: VpnViewModel = viewModel()) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val serversUiState by viewModel.serversUiState.collectAsStateWithLifecycle()
    val lastGeneratedServer by viewModel.lastGeneratedServer.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val appsLoading by viewModel.appsLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connectSelectedServer()
        } else {
            showMessage("VPN permission is required to start the UDP tunnel.")
        }
    }

    val startVpnWithConsent = {
        val prepareIntent = viewModel.prepareVpnIntent()
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            viewModel.connectSelectedServer()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        startVpnWithConsent()
    }

    val toggleVpnConnection = {
        val currentState = telemetry.connectionState
        if (currentState == ConnectionState.CONNECTED ||
            currentState == ConnectionState.CONNECTING ||
            currentState == ConnectionState.RECONNECTING
        ) {
            viewModel.disconnectTunnel()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startVpnWithConsent()
            }
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    DjTrustMegaTheme(darkTheme = settings.useDarkTheme) {
        val megaColors = LocalMegaColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            megaDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    modifier = Modifier.testTag("nav_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = megaColors.orange,
                                        indicatorColor = megaColors.orange,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            megaDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    modifier = Modifier.testTag("rail_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = megaColors.orange,
                                        indicatorColor = megaColors.orange
                                    )
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DASHBOARD,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                telemetry = telemetry,
                                selectedServer = selectedServer,
                                settings = settings,
                                onToggleConnection = toggleVpnConnection,
                                onNavigateToServers = { navigateTo(ROUTE_SERVERS) },
                                onNavigateToGenerator = { navigateTo(ROUTE_GENERATOR) },
                                onNavigateToSupport = { navigateTo(ROUTE_SUPPORT) },
                                onToggleKillSwitch = {
                                    viewModel.updateSettings {
                                        it.copy(killSwitchEnabled = !it.killSwitchEnabled)
                                    }
                                },
                                onToggleBadVpn = {
                                    viewModel.updateSettings {
                                        it.copy(badVpnEnabled = !it.badVpnEnabled)
                                    }
                                },
                                onToggleObfuscation = {
                                    viewModel.updateSettings {
                                        it.copy(udpObfuscationEnabled = !it.udpObfuscationEnabled)
                                    }
                                },
                                onToggleDnsGuard = {
                                    viewModel.updateSettings {
                                        it.copy(dnsLeakProtection = !it.dnsLeakProtection)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_SERVERS) {
                            ServersScreen(
                                uiState = serversUiState,
                                selectedServerId = selectedServer.id,
                                onSelectRegion = viewModel::setRegionFilter,
                                onSearchChange = viewModel::setSearchQuery,
                                onPingAll = viewModel::pingAllServers,
                                onSelectServer = { server ->
                                    viewModel.selectServer(server)
                                    showMessage("Selected ${server.flagEmoji} ${server.countryName}")
                                    navigateTo(ROUTE_DASHBOARD)
                                },
                                onDeleteServer = { id ->
                                    viewModel.deleteGeneratedServer(id)
                                    showMessage("Deleted generated server")
                                },
                                onNavigateToGenerator = { navigateTo(ROUTE_GENERATOR) }
                            )
                        }

                        composable(ROUTE_GENERATOR) {
                            ServerGeneratorScreen(
                                lastGeneratedServer = lastGeneratedServer,
                                onGenerateServer = { country, proto, port, mtu, name, sni ->
                                    viewModel.generateAndSaveServer(
                                        country = country,
                                        protocol = proto,
                                        customPort = port,
                                        mtu = mtu,
                                        customName = name,
                                        sniOrBugHost = sni
                                    )
                                },
                                onConnectGenerated = { server ->
                                    viewModel.selectServer(server, reconnectIfActive = false)
                                    startVpnWithConsent()
                                    navigateTo(ROUTE_DASHBOARD)
                                },
                                onShowMessage = showMessage
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            UdpSettingsScreen(
                                settings = settings,
                                installedApps = installedApps,
                                appsLoading = appsLoading,
                                onUpdateSettings = viewModel::updateSettings,
                                onOpenSplitTunneling = viewModel::loadInstalledAppsIfNeeded,
                                onToggleSplitTunnelApp = viewModel::toggleSplitTunnelApp
                            )
                        }

                        composable(ROUTE_SUPPORT) {
                            SupportScreen(
                                logs = telemetry.logs,
                                onClearLogs = {
                                    viewModel.clearLogs()
                                    showMessage("Tunnel logs cleared")
                                },
                                onShowMessage = showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
