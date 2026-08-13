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
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Section (Accent Color)
        Text(
            text = greeting.first,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5338D5),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = greeting.second,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF6B6E7B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Centered "Your Arsenal" Header Title (Accent Color)
        Text(
            text = "Your Arsenal",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5338D5),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6 Tool Cards Grid Layout (Matches Screenshot 3)
        val buttons = listOf(
            ArsenalButtonItem(
                title = "Cut Video",
                description = "Trim single range",
                icon = Icons.Outlined.ContentCut,
                badge = "SINGLE",
                iconBg = Color(0xFFEBE6FF),
                iconTint = Color(0xFF5338D5),
                badgeBg = Color(0xFFEBE6FF),
                badgeText = Color(0xFF5338D5),
                onClick = { onOpenPickerForTool(NavigationTab.SINGLE_CUT) }
            ),
            ArsenalButtonItem(
                title = "Remove Multiple Parts",
                description = "Cut out selected segments",
                icon = Icons.Default.FolderOpen,
                badge = "MULTI-CUT",
                iconBg = Color(0xFFD3F5E4),
                iconTint = Color(0xFF0C7A5E),
                badgeBg = Color(0xFFD3F5E4),
                badgeText = Color(0xFF0C7A5E),
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
                iconBg = Color(0xFFFEF0C7),
                iconTint = Color(0xFF9A6800),
                badgeBg = Color(0xFFFEF0C7),
                badgeText = Color(0xFF9A6800),
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
                iconBg = Color(0xFFEBE6FF),
                iconTint = Color(0xFF5338D5),
                badgeBg = Color(0xFFEBE6FF),
                badgeText = Color(0xFF5338D5),
                onClick = { onOpenPickerForTool(NavigationTab.COMPRESSOR) }
            ),
            ArsenalButtonItem(
                title = "Join Video",
                description = "Combine multiple videos",
                icon = Icons.Default.CallMerge,
                badge = "MERGER",
                iconBg = Color(0xFFD3F5E4),
                iconTint = Color(0xFF0C7A5E),
                badgeBg = Color(0xFFD3F5E4),
                badgeText = Color(0xFF0C7A5E),
                onClick = { onOpenPickerForTool(NavigationTab.JOIN_VIDEO) }
            ),
            ArsenalButtonItem(
                title = "Rotate Video",
                description = "Rotate, mirror & timeline",
                icon = Icons.Outlined.RotateRight,
                badge = "ROTATE",
                iconBg = Color(0xFFFEF0C7),
                iconTint = Color(0xFF9A6800),
                badgeBg = Color(0xFFFEF0C7),
                badgeText = Color(0xFF9A6800),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAEAF0)),
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
                        color = Color(0xFF1E1F24),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 10.sp,
                        color = Color(0xFF757885),
                        maxLines = 2,
                        lineHeight = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F4F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = Color(0xFF555866),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

