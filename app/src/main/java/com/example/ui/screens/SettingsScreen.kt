package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    enableForegroundService: Boolean,
    enableWakeLock: Boolean,
    onSelectTheme: (AppTheme) -> Unit,
    onToggleForegroundService: (Boolean) -> Unit,
    onToggleWakeLock: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedThemeCategoryTab by remember { mutableIntStateOf(if (currentTheme.isMixed) 1 else 0) }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkIsIgnoringBatteryOptimizations(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = checkIsIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: App Theme & Accents
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SurfaceBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "App Theme & Accents",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Select from 6 Static or 6 Mixed Accent Themes",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedThemeCategoryTab,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedThemeCategoryTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.5.dp
                        )
                    },
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedThemeCategoryTab == 0,
                        onClick = { selectedThemeCategoryTab = 0 },
                        text = {
                            Text(
                                "6 Static Colors",
                                fontSize = 12.sp,
                                fontWeight = if (selectedThemeCategoryTab == 0) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedThemeCategoryTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )
                    Tab(
                        selected = selectedThemeCategoryTab == 1,
                        onClick = { selectedThemeCategoryTab = 1 },
                        text = {
                            Text(
                                "6 Mixed Accents",
                                fontSize = 12.sp,
                                fontWeight = if (selectedThemeCategoryTab == 1) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (selectedThemeCategoryTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val displayedThemes = if (selectedThemeCategoryTab == 0) {
                    AppTheme.values().filter { !it.isMixed }
                } else {
                    AppTheme.values().filter { it.isMixed }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayedThemes.chunked(2).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowThemes.forEach { theme ->
                                ThemeOptionCard(
                                    theme = theme,
                                    isSelected = currentTheme == theme,
                                    onClick = { onSelectTheme(theme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Battery Optimization
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SurfaceBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isIgnoringBatteryOptimizations) EmeraldContainer else Color(0xFFFFEDD5)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIgnoringBatteryOptimizations) Icons.Default.BatterySaver else Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = if (isIgnoringBatteryOptimizations) EmeraldSuccess else Color(0xFFEA580C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "Unrestricted Background Processing Enabled" else "Optimized (App may be killed in background)",
                            fontSize = 12.sp,
                            color = if (isIgnoringBatteryOptimizations) EmeraldSuccess else Color(0xFFEA580C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "To ensure video cutting, compression, join, and rotation tasks complete reliably when the app is in the background or screen turns off, grant Battery Optimization Exemption.",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!isIgnoringBatteryOptimizations) {
                    Button(
                        onClick = { requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ignore_battery_optimization_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ignore Battery Optimization", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { openBatterySettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Optimization Disabled", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                }
            }
        }

        // Section 3: Background Execution Priority
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SurfaceBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Background Execution Priority",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Keep tasks alive without getting killed",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Foreground Processing Service",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Shows ongoing progress notification to give process high system priority",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }

                    Switch(
                        checked = enableForegroundService,
                        onCheckedChange = onToggleForegroundService,
                        modifier = Modifier.testTag("foreground_service_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Keep CPU Awake (WakeLock)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Prevents CPU from entering deep sleep during video transcoding",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }

                    Switch(
                        checked = enableWakeLock,
                        onCheckedChange = onToggleWakeLock,
                        modifier = Modifier.testTag("wakelock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Provided free RAM is available, tasks will finish seamlessly.",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        }

        // Section 4: Output Directory & Engine Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SurfaceBorderLight),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Output Directory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Internal Storage / Download / VideoCut",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VideoCut Studio v1.0",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                    Text(
                        text = "FFmpeg 8.1 GPL (x264/x265) & Media3",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("theme_option_${theme.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) theme.primaryColor.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) theme.primaryColor else SurfaceBorderLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(theme.primaryColor)
                )

                if (theme.isMixed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(theme.secondaryColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = theme.title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected theme",
                        tint = theme.primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun checkIsIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
    return true
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openBatterySettings(context)
        }
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Settings.ACTION_SETTINGS)
        context.startActivity(intent)
    }
}
