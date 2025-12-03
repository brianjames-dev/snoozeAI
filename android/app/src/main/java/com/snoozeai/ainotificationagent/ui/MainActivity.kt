package com.snoozeai.ainotificationagent.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.snoozeai.ainotificationagent.BuildConfig
import com.snoozeai.ainotificationagent.backend.ApiClient
import com.snoozeai.ainotificationagent.data.Settings
import com.snoozeai.ainotificationagent.data.SettingsRepository
import com.snoozeai.ainotificationagent.data.SnoozeDatabase
import com.snoozeai.ainotificationagent.data.SnoozeRepository
import com.snoozeai.ainotificationagent.data.SnoozedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class NavItem(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val api = ApiClient.create(BuildConfig.BASE_URL)
        val db = SnoozeDatabase.build(applicationContext)
        SnoozeRepository(api, db.snoozeDao())
    }

    private val settingsRepository by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false
        window.statusBarColor = android.graphics.Color.parseColor("#2E386D")
        window.navigationBarColor = android.graphics.Color.parseColor("#1C2349")
        enableEdgeToEdge()
        setContent {
            SnoozeApp(repository = repository, settingsRepository = settingsRepository)
        }
    }
}

@Composable
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
    val error by vm.error.collectAsState()

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
        NavItem("snoozed", "Snoozed", Icons.AutoMirrored.Filled.List),
        NavItem("settings", "Settings", Icons.Default.Settings)
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    val title = navItems.find { it.route == currentRoute }?.label ?: "SnoozeAI"

    val backdrop = Brush.verticalGradient(
        listOf(Color(0xFF2E386D), Color(0xFF1C2349))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {},
            bottomBar = {
                val navContainer = Color(0xE6F4F1FF)
                val navContent = Color(0xFF1E1B2C)
                NavigationBar(containerColor = navContainer) {
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
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = navContent,
                                selectedTextColor = navContent,
                                unselectedIconColor = navContent,
                                unselectedTextColor = navContent,
                                indicatorColor = navContainer
                            )
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
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(it)
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
                        items = items,
                        settings = settings
                    )
                }
                composable("snoozed") {
                    SnoozedScreen(snoozedItems = items, error = error)
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
}

@Composable
fun PermissionSection(
    pending: List<Pair<String, () -> Unit>>
) {
    val context = LocalContext.current

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            if (pending.isEmpty()) {
                Text("All required permissions are granted.", style = MaterialTheme.typography.bodySmall)
            } else {
                pending.forEach { (label, onClick) ->
                    PermissionRow(label, false, onClick)
                }
            }
            Text(text = context.getString(com.snoozeai.ainotificationagent.R.string.privacy_copy), style = MaterialTheme.typography.bodySmall)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietTimeField(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayFormatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        QuietTimePickerDialog(
            initial = time,
            onDismiss = { showPicker = false },
            onConfirm = { newTime ->
                showPicker = false
                onTimeChange(newTime)
            }
        )
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { showPicker = true }
    ) {
        OutlinedTextField(
            value = time.format(displayFormatter),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = false, // visual-only; clicks handled by Box
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledTextColor = LocalContentColor.current,
                disabledContainerColor = Color.Transparent,
                disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false
    )

    val accent = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val newTime = LocalTime.of(pickerState.hour, pickerState.minute)
                    onConfirm(newTime)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(
                state = pickerState,
                colors = TimePickerDefaults.colors(
                    periodSelectorSelectedContainerColor = accent,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectorColor = accent,
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}

@Composable
fun SettingsSection(
    settings: Settings?,
    onSaveQuietHours: (Boolean, LocalTime, LocalTime) -> Unit,
    onSetDefaultSnooze: (Long) -> Unit,
    onSaveHints: (List<String>) -> Unit
) {
    var quietEnabled by remember(settings?.quietHours?.enabled) { mutableStateOf(settings?.quietHours?.enabled ?: false) }
    var quietStart by remember(settings?.quietHours?.start) { mutableStateOf(settings?.quietHours?.start ?: LocalTime.of(9, 0)) }
    var quietEnd by remember(settings?.quietHours?.end) { mutableStateOf(settings?.quietHours?.end ?: LocalTime.of(17, 0)) }
    val defaultSnooze = settings?.defaultSnoozeMinutes ?: 60
    var hintsText by remember(settings?.hints) { mutableStateOf(settings?.hints?.joinToString(", ") ?: "") }

    fun persistQuietHours() {
        onSaveQuietHours(quietEnabled, quietStart, quietEnd)
    }
    fun persistHints(input: String) {
        val list = input.split(",").map { it.trim() }.filter { it.isNotBlank() }
        onSaveHints(list)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Quiet hours & snooze", style = MaterialTheme.typography.titleMedium)
                    val alwaysOn = !quietEnabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Always on (smart resurfacing)")
                        Switch(
                            checked = alwaysOn,
                            onCheckedChange = { isOn ->
                                quietEnabled = !isOn
                                persistQuietHours()
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable quiet hours")
                        Switch(
                            checked = quietEnabled,
                            onCheckedChange = {
                                quietEnabled = it
                                persistQuietHours()
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuietTimeField(
                            label = "Start (h:mm a)",
                            time = quietStart,
                            onTimeChange = { newTime ->
                                quietStart = newTime
                                persistQuietHours()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuietTimeField(
                            label = "End (h:mm a)",
                            time = quietEnd,
                            onTimeChange = { newTime ->
                                quietEnd = newTime
                                persistQuietHours()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Default snooze", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15L, 60L, 180L).forEach { mins ->
                            val selected = mins == defaultSnooze
                            val activeBg = MaterialTheme.colorScheme.primary
                            val activeContent = MaterialTheme.colorScheme.onPrimary
                            val colors = if (selected) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = activeBg,
                                    contentColor = activeContent
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                            FilledTonalButton(
                                onClick = { onSetDefaultSnooze(mins) },
                                colors = colors,
                                border = if (selected) BorderStroke(1.dp, activeBg) else null
                            ) {
                                Text("${mins}m")
                            }
                        }
                    }

                    Text("Classifier hints (comma-separated)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = hintsText,
                        onValueChange = {
                            hintsText = it
                            persistHints(it)
                        },
                        label = { Text("e.g., PagerDuty, Slack, Zendesk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onRequestNotifications: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    items: List<SnoozedItem>,
    settings: Settings?
) {
    val context = LocalContext.current
    val pkg = context.packageName
    val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(pkg)
    val pendingPermissions = listOfNotNull(
        if (!notificationEnabled) "Notifications" to onRequestNotifications else null,
        if (!listenerEnabled) "Notification Listener" to onOpenListenerSettings else null
    )

    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val quietStart = settings?.quietHours?.start?.format(formatter) ?: "9:00 AM"
    val quietEnd = settings?.quietHours?.end?.format(formatter) ?: "5:00 PM"
    val modeValue = if (settings?.quietHours?.enabled == true) {
        "$quietStart – $quietEnd"
    } else {
        "Always on"
    }
    val defaultSnoozeMins = settings?.defaultSnoozeMinutes ?: 60
    val total = items.size
    val upcoming = items.count { it.snoozeUntil.isAfter(Instant.now()) }
    val nextAt = items.filter { it.snoozeUntil.isAfter(Instant.now()) }
        .minByOrNull { it.snoozeUntil }
        ?.snoozeUntil
        ?.atZone(ZoneId.systemDefault())
        ?.format(formatter)
        ?: "6:23 PM"
    val urgencyBuckets = remember(items) { bucketUrgency(items) }
    val pillPalette = listOf(
        Color(0xFF8FB9FF),
        Color(0xFF9AD5C0),
        Color(0xFFF3B0C3),
        Color(0xFFB8A5FF)
    )
    val chartPagerState = rememberPagerState(pageCount = { 4 })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Hey there,", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Here’s your Snooze recap", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Window: $modeValue", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDEE3FF))
            }
        }
        item {
            StatGrid(
                stats = listOf(
                    "Total snoozed" to total.toString(),
                    "Upcoming" to upcoming.toString(),
                    "Next resurfacing" to nextAt,
                    "Default snooze" to "${defaultSnoozeMins}m"
                ),
                palette = pillPalette
            )
        }
        if (pendingPermissions.isNotEmpty()) {
            item {
                PermissionSection(pending = pendingPermissions)
            }
        }
        item {
            UrgencySummary(urgencyBuckets)
        }
        item {
            ChartPager(chartPagerState, items)
        }
    }
}

private data class UrgencyBuckets(
    val high: Int,
    val medium: Int,
    val low: Int,
    val unspecified: Int,
    val total: Int
)

private fun bucketUrgency(items: List<SnoozedItem>): UrgencyBuckets {
    var high = 0
    var medium = 0
    var low = 0
    var unspecified = 0
    items.forEach { item ->
        val u = item.urgency
        if (u == null) {
            unspecified++
        } else if (u >= 0.7) {
            high++
        } else if (u >= 0.4) {
            medium++
        } else if (u >= 0.0) {
            low++
        } else {
            unspecified++
        }
    }
    return UrgencyBuckets(high, medium, low, unspecified, items.size)
}

@Composable
private fun UrgencySummary(buckets: UrgencyBuckets) {
    val entries = buildList {
        add("Low" to buckets.low)
        add("Medium" to buckets.medium)
        add("High" to buckets.high)
        if (buckets.unspecified > 0) add("Unspecified" to buckets.unspecified)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Urgency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { (label, value) ->
                    UrgencyPill(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>, palette: List<Color>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        itemsIndexed(stats) { index, (label, value) ->
            val tone = palette.getOrElse(index % palette.size) { MaterialTheme.colorScheme.primaryContainer }
            StatCard(label = label, value = value, containerColor = tone)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UrgencyPill(label: String, value: Int, modifier: Modifier = Modifier) {
    val tone = when (label.lowercase(Locale.getDefault())) {
        "low" -> Color(0xFFC7F2D4)
        "medium" -> Color(0xFFFFF2C2)
        "high" -> Color(0xFFF9C2C2)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tone),
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartPager(pagerState: androidx.compose.foundation.pager.PagerState, items: List<SnoozedItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.height(220.dp)) { page ->
                when (page) {
                    0 -> DailySnoozesChart(items)
                    1 -> UrgencyStackedChart(items)
                    2 -> SourceBreakdownChart(items)
                    3 -> OutcomesChart()
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (active) Color.White else Color(0x66FFFFFF))
                )
            }
        }
    }
}

@Composable
private fun DailySnoozesChart(items: List<SnoozedItem>) {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    androidx.compose.runtime.LaunchedEffect(items) {
        val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()
        val counts = last7Days.map { day ->
            items.count {
                val itemDate = it.snoozeUntil.atZone(ZoneId.systemDefault()).toLocalDate()
                itemDate == day
            }
        }

        modelProducer.tryRunTransaction {
            lineSeries {
                series(counts)
            }
        }
    }

    Column {
        Text("Daily Snoozes (Last 7 Days)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun UrgencyStackedChart(items: List<SnoozedItem>) {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    androidx.compose.runtime.LaunchedEffect(items) {
        val last7Days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }.reversed()

        val lowCounts = mutableListOf<Int>()
        val mediumCounts = mutableListOf<Int>()
        val highCounts = mutableListOf<Int>()

        last7Days.forEach { day ->
            val daysItems = items.filter {
                it.snoozeUntil.atZone(ZoneId.systemDefault()).toLocalDate() == day
            }
            lowCounts.add(daysItems.count { (it.urgency ?: 0.0) < 0.4 })
            mediumCounts.add(daysItems.count { (it.urgency ?: 0.0) >= 0.4 && (it.urgency ?: 0.0) < 0.7 })
            highCounts.add(daysItems.count { (it.urgency ?: 0.0) >= 0.7 })
        }

        modelProducer.tryRunTransaction {
            columnSeries {
                series(lowCounts)
                series(mediumCounts)
                series(highCounts)
            }
        }
    }

    Column {
        Text("Urgency Distribution (Last 7 Days)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun SourceBreakdownChart(items: List<SnoozedItem>) {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    androidx.compose.runtime.LaunchedEffect(items) {
        // Simple heuristic: guess source from title/body keywords
        // Real app would track package name
        val sources = items.groupingBy { item ->
            val text = (item.title + " " + item.body).lowercase()
            when {
                "slack" in text -> "Slack"
                "email" in text || "gmail" in text -> "Email"
                "calendar" in text -> "Calendar"
                else -> "Other"
            }
        }.eachCount()

        // Map to fixed categories for chart
        val counts = listOf(
            sources["Slack"] ?: 0,
            sources["Email"] ?: 0,
            sources["Calendar"] ?: 0,
            sources["Other"] ?: 0
        )

        modelProducer.tryRunTransaction {
            columnSeries {
                series(counts)
            }
        }
    }

    Column {
        Text("Source Breakdown (Estimated)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun OutcomesChart() {
    val modelProducer = remember { CartesianChartModelProducer.build() }

    // Placeholder data since we don't track outcomes yet
    androidx.compose.runtime.LaunchedEffect(Unit) {
        modelProducer.tryRunTransaction {
            columnSeries {
                series(5, 3, 2)
            }
        }
    }

    Column {
        Text("Outcomes (Placeholder)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = modelProducer,
        )
    }
}

@Composable
fun SnoozedScreen(snoozedItems: List<SnoozedItem>, error: String?) {
    val backdrop = Brush.verticalGradient(
        listOf(Color(0xFF2E386D), Color(0xFF1C2349))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
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
                error?.let { Text("Last error: $it", style = MaterialTheme.typography.bodySmall) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SnoozedHeader(count = snoozedItems.size)
                }
                if (error != null) {
                    item {
                        Text("Last error: $error", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                itemsIndexed(snoozedItems) { _, item ->
                    SnoozeCard(item)
                }
            }
        }
    }
}

@Composable
private fun SnoozedHeader(count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "$count Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(Icons.Default.Search, "Search")
                HeaderIconButton(Icons.Default.MoreVert, "More")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CapsuleButton(icon = Icons.AutoMirrored.Filled.List, selected = true)
            CapsuleButton(icon = Icons.Default.ViewModule, selected = false)
            CapsuleButton(icon = Icons.Default.Settings, selected = false)
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33FFFFFF))
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun CapsuleButton(icon: ImageVector, selected: Boolean) {
    val bg = if (selected) Color(0xFFB8A5FF) else Color(0xFFE8E0FF)
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(10.dp),
        tint = Color(0xFF1E1B2C)
    )
}

@Composable
private fun SnoozeCard(item: SnoozedItem) {
    val urgencyLabel = urgencyLabel(item.urgency)
    val urgencyColor = urgencyColor(item.urgency)
    val until = item.snoozeUntil.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3ECFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE1D7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.title.firstOrNull()?.uppercase() ?: "•", fontWeight = FontWeight.Bold)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(urgencyLabel, color = urgencyColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        Text("Until $until", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
        }
    }
}

private fun urgencyLabel(value: Double?): String = when {
    value == null -> "Unspecified"
    value >= 0.7 -> "High"
    value >= 0.4 -> "Medium"
    value >= 0.0 -> "Low"
    else -> "Low"
}

private fun urgencyColor(value: Double?): Color = when {
    value == null -> Color(0xFF6B7280)
    value >= 0.7 -> Color(0xFFD9534F)
    value >= 0.4 -> Color(0xFFF0AD4E)
    value >= 0.0 -> Color(0xFF5CB85C)
    else -> Color(0xFF5CB85C)
}

class SnoozeViewModel(
    private val repository: SnoozeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<SnoozedItem>>(emptyList())
    val items: StateFlow<List<SnoozedItem>> = _items
    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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
            _error.value = null
            runCatching { repository.syncLatest() }
                .onFailure { err -> _error.value = err.message }
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
}
