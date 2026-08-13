package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProcessedVideoEntity
import com.example.model.MultiPartCutMode
import com.example.model.VideoItem
import com.example.ui.NavigationTab
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted

import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.theme.PrimaryIndigo

private val wittyGreetings = listOf(
    "Welcome, Master" to "What can I do for you today?",
    "Ready for Magic, Boss?" to "Which video tool shall we use today?",
    "At Your Service, Captain!" to "Select your tool and let's craft some cuts.",
    "Greetings, Video Wizard!" to "What master stroke are we creating today?",
    "What's the Master Plan?" to "Choose a video tool to get started.",
    "Command Center Ready!" to "Your studio suite is primed and ready.",
    "Ready to Slice & Dice?" to "Let's trim, join, or compress your media.",
    "Your Wish is My Export!" to "Which video project are we tackling today?",
    "Studio Master at Work!" to "Your arsenal is loaded and ready."
)

@Composable
fun OverviewScreen(
    selectedVideo: VideoItem?,
    history: List<ProcessedVideoEntity>,
    totalBytesSaved: Long,
    onSelectVideoUri: (Uri) -> Unit,
    onNavigateTab: (NavigationTab) -> Unit,
    onOpenPickerForTool: (NavigationTab) -> Unit,
    onPlayVideo: (Uri) -> Unit,
    onSetMultiPartMode: ((MultiPartCutMode) -> Unit)? = null
) {
    val greetingIndex = rememberSaveable { (0 until wittyGreetings.size).random() }
    val greeting = wittyGreetings[greetingIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Section (Theme Primary Accent Color)
        Text(
            text = greeting.first,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = greeting.second,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Centered "Your Arsenal" Header Title (Theme Primary Accent Color)
        Text(
            text = "Your Arsenal",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        val primary = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val secondary = MaterialTheme.colorScheme.secondary
        val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

        // 6 Tool Cards Grid Layout with theme responsive accents
        val buttons = listOf(
            ArsenalButtonItem(
                title = "Cut Video",
                description = "Trim single range",
                icon = Icons.Outlined.ContentCut,
                badge = "SINGLE",
                iconBg = primaryContainer,
                iconTint = primary,
                badgeBg = primaryContainer,
                badgeText = primary,
                onClick = { onOpenPickerForTool(NavigationTab.SINGLE_CUT) }
            ),
            ArsenalButtonItem(
                title = "Remove Multiple Parts",
                description = "Cut out selected segments",
                icon = Icons.Default.FolderOpen,
                badge = "MULTI-CUT",
                iconBg = secondaryContainer,
                iconTint = secondary,
                badgeBg = secondaryContainer,
                badgeText = secondary,
                onClick = {
                    onSetMultiPartMode?.invoke(MultiPartCutMode.REMOVE_SELECTED)
                    onOpenPickerForTool(NavigationTab.MULTI_CUT)
                }
            ),
            ArsenalButtonItem(
                title = "Keep Multiple Parts",
                description = "Merge selected segments",
                icon = Icons.Filled.FolderSpecial,
                badge = "MULTI-CUT",
                iconBg = primaryContainer.copy(alpha = 0.7f),
                iconTint = primary,
                badgeBg = primaryContainer.copy(alpha = 0.7f),
                badgeText = primary,
                onClick = {
                    onSetMultiPartMode?.invoke(MultiPartCutMode.KEEP_SELECTED)
                    onOpenPickerForTool(NavigationTab.MULTI_CUT)
                }
            ),
            ArsenalButtonItem(
                title = "Video Compressor",
                description = "Reduce file size",
                icon = Icons.Outlined.Compress,
                badge = "FAST",
                iconBg = primaryContainer,
                iconTint = primary,
                badgeBg = primaryContainer,
                badgeText = primary,
                onClick = { onOpenPickerForTool(NavigationTab.COMPRESSOR) }
            ),
            ArsenalButtonItem(
                title = "Join Video",
                description = "Combine multiple videos",
                icon = Icons.Default.CallMerge,
                badge = "MERGER",
                iconBg = secondaryContainer,
                iconTint = secondary,
                badgeBg = secondaryContainer,
                badgeText = secondary,
                onClick = { onOpenPickerForTool(NavigationTab.JOIN_VIDEO) }
            ),
            ArsenalButtonItem(
                title = "Rotate Video",
                description = "Rotate, mirror & timeline",
                icon = Icons.Outlined.RotateRight,
                badge = "ROTATE",
                iconBg = secondaryContainer.copy(alpha = 0.7f),
                iconTint = secondary,
                badgeBg = secondaryContainer.copy(alpha = 0.7f),
                badgeText = secondary,
                onClick = { onOpenPickerForTool(NavigationTab.ROTATE_VIDEO) }
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(buttons.size) { index ->
                val btn = buttons[index]
                ArsenalCard(item = btn)
            }
        }
    }
}

private data class ArsenalButtonItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val iconBg: Color,
    val iconTint: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val onClick: () -> Unit
)

@Composable
private fun ArsenalCard(item: ArsenalButtonItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clickable(onClick = item.onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon Box on Left, Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = item.badgeText,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bottom Section: Title, Description, and Bottom-Right Chevron Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

