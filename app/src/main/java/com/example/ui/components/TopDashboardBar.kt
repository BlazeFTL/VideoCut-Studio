package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationTab
import com.example.ui.theme.TextPrimaryDark

@Composable
fun StaggeredMenuIcon(
    tint: Color = TextPrimaryDark,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.8.dp.toPx()
        val cap = StrokeCap.Round
        val w = size.width
        val h = size.height

        // Top line (long)
        drawLine(
            color = tint,
            start = Offset(0f, h * 0.2f),
            end = Offset(w * 0.95f, h * 0.2f),
            strokeWidth = strokeWidth,
            cap = cap
        )

        // Middle line (medium)
        drawLine(
            color = tint,
            start = Offset(0f, h * 0.5f),
            end = Offset(w * 0.70f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = cap
        )

        // Bottom line (shorter)
        drawLine(
            color = tint,
            start = Offset(0f, h * 0.8f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = strokeWidth,
            cap = cap
        )
    }
}

@Composable
fun TopDashboardBar(
    activeTab: NavigationTab = NavigationTab.OVERVIEW,
    onOpenDrawer: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onNavigateBack: () -> Unit = {},
    onGoHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSettings = activeTab == NavigationTab.SETTINGS

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSettings) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    StaggeredMenuIcon(
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = if (isSettings) "App Settings" else "VideoCut Studio",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isSettings) {
                IconButton(onClick = onGoHome) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (onOpenSettings != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Text(
                    text = "By BlazeFTL",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

