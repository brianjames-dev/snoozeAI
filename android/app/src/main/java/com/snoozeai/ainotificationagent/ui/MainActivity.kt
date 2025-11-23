package com.snoozeai.ainotificationagent.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snoozeai.ainotificationagent.BuildConfig
import com.snoozeai.ainotificationagent.backend.ApiClient
import com.snoozeai.ainotificationagent.data.Settings
import com.snoozeai.ainotificationagent.data.SettingsRepository
import com.snoozeai.ainotificationagent.data.SnoozeDatabase
import com.snoozeai.ainotificationagent.data.SnoozeRepository
import com.snoozeai.ainotificationagent.data.SnoozedItem
import com.snoozeai.ainotificationagent.notifications.NotificationPublisher
import com.snoozeai.ainotificationagent.notifications.SnoozeScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.util.Locale

@Composable
private fun rememberBatteryStatus(): Boolean {
    val context = LocalContext.current
    val pkg = context.packageName
    var batteryOk by remember { mutableStateOf(false) }
    LaunchedEffect(pkg, context) {
        batteryOk = runCatching {
            context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(pkg) == true
        }.getOrDefault(false)
    }
    return batteryOk
}

data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val api = ApiClient.create(BuildConfig.BASE_URL)
        val db = SnoozeDatabase.build(applicationContext)
        SnoozeRepository(api, db.snoozeDao())
    }

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnoozeApp(repository = repository, settingsRepository = settingsRepository)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SnoozeApp(repository: SnoozeRepository, settingsRepository: SettingsRepository) {
    val vm: SnoozeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SnoozeViewModel(repository, settingsRepository) as T
            }
        }
    )
    val items by vm.items.collectAsState()
    val settings by vm.settings.collectAsState(initial = null)

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) Toast.makeText(context, "Notifications remain off", Toast.LENGTH_SHORT).show()
        }
    )

    val navController = rememberNavController()
    val navItems = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("snoozed", "Snoozed", Icons.Default.List),
        NavItem("settings", "Settings", Icons.Default.Settings)
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val title = navItems.find { it.route == currentRoute }?.label ?: "SnoozeAI"

    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "snoozed") {
                FloatingActionButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(inner)
        ) {
            composable("home") {
                HomeScreen(
                    onRequestNotifications = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onOpenListenerSettings = {
                        context.startActivity(
                            Intent(SystemSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    onRequestBattery = {
                        val power = context.getSystemService(PowerManager::class.java)
                        val pkg = context.packageName
                        if (power?.isIgnoringBatteryOptimizations(pkg) != true) {
                            val intent = Intent(SystemSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$pkg")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    },
                    onSendSample = { vm.sendSample(context) },
                    items = items,
                    settings = settings
                )
            }
            composable("snoozed") {
                SnoozedScreen(snoozedItems = items)
            }
            composable("settings") {
                SettingsSection(
                    settings = settings,
                    onSaveQuietHours = { enabled, start, end -> vm.updateQuietHours(enabled, start, end) },
                    onSetDefaultSnooze = { minutes -> vm.updateDefaultSnooze(minutes) },
                    onSaveHints = { hints -> vm.updateHints(hints) }
                )
            }
        }
    }
}

@Composable
fun SnoozeCard(item: SnoozedItem) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    val localTime = item.snoozeUntil.atZone(ZoneId.systemDefault()).format(formatter)
    ElevatedCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.summary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            Text("Snoozed until $localTime", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            item.urgency?.let { urgency ->
                Text("Urgency: $urgency", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun PermissionSection(
    onRequestNotifications: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestBattery: () -> Unit,
    batteryGranted: Boolean
) {
    val context = LocalContext.current
    val pkg = context.packageName
    val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(pkg)

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            PermissionRow("Notifications", notificationEnabled, onRequestNotifications)
            PermissionRow("Notification Listener", listenerEnabled, onOpenListenerSettings)
            PermissionRow("Battery Optimization", batteryGranted, onRequestBattery)
            Text(
                text = context.getString(com.snoozeai.ainotificationagent.R.string.privacy_copy),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(if (granted) "Granted" else "Tap to grant", style = MaterialTheme.typography.bodySmall)
        }
        FilledTonalButton(onClick = onClick, enabled = !granted) {
            Text(if (granted) "Granted" else "Grant")
        }
    }
}

@Composable
fun SettingsSection(
    settings: Settings?,
    onSaveQuietHours: (Boolean, LocalTime, LocalTime) -> Unit,
    onSetDefaultSnooze: (Long) -> Unit,
    onSaveHints: (List<String>) -> Unit
) {
    val displayFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    fun formatTime(value: LocalTime?): String = value?.format(displayFormatter) ?: ""
    fun parseTime(input: String): LocalTime? {
        val trimmed = input.trim()
        return runCatching { LocalTime.parse(trimmed, displayFormatter) }.getOrNull()
            ?: runCatching { LocalTime.parse(trimmed) }.getOrNull()
    }

    var quietEnabled by remember(settings?.quietHours?.enabled) { mutableStateOf(settings?.quietHours?.enabled ?: false) }
    var quietStart by remember(settings?.quietHours?.start) { mutableStateOf(formatTime(settings?.quietHours?.start).ifBlank { "9:00 AM" }) }
    var quietEnd by remember(settings?.quietHours?.end) { mutableStateOf(formatTime(settings?.quietHours?.end).ifBlank { "5:00 PM" }) }
    val defaultSnooze = settings?.defaultSnoozeMinutes ?: 60
    var hintsText by remember(settings?.hints) { mutableStateOf(settings?.hints?.joinToString(", ") ?: "") }

    ElevatedCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quiet hours & snooze", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable quiet hours")
                Switch(checked = quietEnabled, onCheckedChange = { quietEnabled = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quietStart,
                    onValueChange = { quietStart = it },
                    label = { Text("Start (h:mm a)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quietEnd,
                    onValueChange = { quietEnd = it },
                    label = { Text("End (h:mm a)") },
                    modifier = Modifier.weight(1f)
                )
            }
            Button(onClick = {
                val parsedStart = parseTime(quietStart)
                val parsedEnd = parseTime(quietEnd)
                if (parsedStart != null && parsedEnd != null) {
                    onSaveQuietHours(quietEnabled, parsedStart, parsedEnd)
                }
            }) {
                Text("Save quiet hours")
            }

            Text("Default snooze: $defaultSnooze min")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15L, 60L, 180L).forEach { mins ->
                    FilledTonalButton(onClick = { onSetDefaultSnooze(mins) }) {
                        Text("${mins}m")
                    }
                }
            }

            Text("Classifier hints (comma-separated)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = hintsText,
                onValueChange = { hintsText = it },
                label = { Text("e.g., PagerDuty, Slack, Zendesk") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val list = hintsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                onSaveHints(list)
            }) {
                Text("Save hints")
            }
        }
    }
}

@Composable
fun HomeScreen(
    onRequestNotifications: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestBattery: () -> Unit,
    onSendSample: () -> Unit,
    items: List<SnoozedItem>,
    settings: Settings?
) {
    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val quietStart = settings?.quietHours?.start?.format(formatter) ?: "9:00 AM"
    val quietEnd = settings?.quietHours?.end?.format(formatter) ?: "5:00 PM"
    val quietLabel = if (settings?.quietHours?.enabled == true) {
        "$quietStart – $quietEnd (quiet)"
    } else {
        "$quietStart – $quietEnd (always on)"
    }
    val total = items.size
    val upcoming = items.count { it.snoozeUntil.isAfter(Instant.now()) }
    val nextAt = items.filter { it.snoozeUntil.isAfter(Instant.now()) }
        .minByOrNull { it.snoozeUntil }
        ?.snoozeUntil
        ?.atZone(ZoneId.systemDefault())
        ?.format(formatter)
        ?: "—"
    val batteryGranted = rememberBatteryStatus()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatGrid(
                stats = listOf(
                    "Total snoozed" to total.toString(),
                    "Upcoming" to upcoming.toString(),
                    "Next resurfacing" to nextAt,
                    "Window" to quietLabel
                )
            )
        }
        item {
            PermissionSection(
                onRequestNotifications = onRequestNotifications,
                onOpenListenerSettings = onOpenListenerSettings,
                onRequestBattery = onRequestBattery,
                batteryGranted = batteryGranted
            )
        }
        item {
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Try it", style = MaterialTheme.typography.titleMedium)
                    Text("Send a sample notification to verify summaries are working.")
                    Button(onClick = onSendSample) {
                        Text("Send sample notification")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        items(stats) { (label, value) ->
            ElevatedCard {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SnoozedScreen(snoozedItems: List<SnoozedItem>) {
    if (snoozedItems.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No snoozes yet.")
            Text("Grant notification access to start summarizing.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(snoozedItems) { item ->
                SnoozeCard(item)
            }
        }
    }
}

class SnoozeViewModel(
    private val repository: SnoozeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<SnoozedItem>>(emptyList())
    val items: StateFlow<List<SnoozedItem>> = _items
    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings

    init {
        viewModelScope.launch {
            repository.items.collect { list -> _items.update { list } }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { prefs -> _settings.value = prefs }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.syncLatest() }
        }
    }

    fun updateQuietHours(enabled: Boolean, start: LocalTime, end: LocalTime) {
        viewModelScope.launch {
            settingsRepository.setQuietHours(enabled, start, end)
        }
    }

    fun updateDefaultSnooze(minutes: Long) {
        viewModelScope.launch { settingsRepository.setDefaultSnooze(minutes) }
    }

    fun updateHints(hints: List<String>) {
        viewModelScope.launch { settingsRepository.setHints(hints) }
    }

    fun sendSample(context: Context) {
        viewModelScope.launch {
            runCatching {
                val prefs = _settings.value
                val item = repository.ingestNotification(
                    title = "Sample notification",
                    body = "This is a SnoozeAI sample to verify summaries and snoozes.",
                    defaultSnoozeMinutes = prefs?.defaultSnoozeMinutes ?: 60,
                    hints = prefs?.hints
                )
                val publisher = NotificationPublisher(context)
                publisher.postSummary(item)
                SnoozeScheduler(context, publisher).schedule(item, prefs?.quietHours)
            }
        }
    }
}
