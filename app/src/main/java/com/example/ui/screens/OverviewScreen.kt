package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProcessedVideoEntity
import com.example.model.MultiPartCutMode
import com.example.model.VideoItem
import com.example.ui.NavigationTab
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

private val wittyGreetings = listOf(
    "Welcome, Master" to "What masterpiece are we cutting today?",
    "Ready for Magic?" to "Select a studio tool below to begin.",
    "Time to Edit!" to "Fast trimming, merging, rotation & compression.",
    "Ready to Slice & Dice?" to "Lossless cuts or crisp x264/x265 transcodes.",
    "Studio Engine Ready" to "Pick a tool or browse your device videos."
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

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val buttons = listOf(
        ArsenalButtonItem(
            testTag = "tool_card_single_cut",
            title = "Cut Video",
            description = "Frame-accurate trim",
            icon = Icons.Outlined.ContentCut,
            badge = "SINGLE",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.SINGLE_CUT) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_remove_parts",
            title = "Remove Parts",
            description = "Cut out unwanted clips",
            icon = Icons.Default.FolderOpen,
            badge = "MULTI",
            iconBg = secondaryContainer,
            iconTint = secondary,
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.REMOVE_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_keep_parts",
            title = "Keep Parts",
            description = "Keep selected parts",
            icon = Icons.Filled.FolderSpecial,
            badge = "MULTI",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.KEEP_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_compressor",
            title = "Compressor",
            description = "CRF x264, H.265 & Target",
            icon = Icons.Outlined.Compress,
            badge = "COMPRESS",
            iconBg = secondaryContainer,
            iconTint = secondary,
            onClick = { onOpenPickerForTool(NavigationTab.COMPRESSOR) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_join",
            title = "Join Video",
            description = "Merge multiple videos",
            icon = Icons.Default.CallMerge,
            badge = "MERGE",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.JOIN_VIDEO) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_rotate",
            title = "Rotate Video",
            description = "Rotate, flip & segments",
            icon = Icons.Outlined.RotateRight,
            badge = "ROTATE",
            iconBg = secondaryContainer,
            iconTint = secondary,
            onClick = { onOpenPickerForTool(NavigationTab.ROTATE_VIDEO) }
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Greeting Banner Card
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = greeting.first,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = greeting.second,
                        fontSize = 13.sp,
                        color = TextSecondaryMuted
                    )

                    if (history.isNotEmpty() || totalBytesSaved > 0L) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryContainer.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${history.size} Saved Exports • ${VideoProcessor.formatFileSize(totalBytesSaved.coerceAtLeast(0L))} saved",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                    }

                    if (selectedVideo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loaded: ${selectedVideo.title}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick Library Buttons
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(NavigationTab.MEDIA_SELECTOR) }
                    .testTag("overview_media_browser_btn"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = "Media Browser",
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Media Browser",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(NavigationTab.HISTORY) }
                    .testTag("overview_saved_library_btn"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = "Saved Videos",
                        tint = primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved (${history.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Studio Tools",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimaryDark,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(buttons.size) { index ->
            ArsenalCard(item = buttons[index])
        }
    }
}

private data class ArsenalButtonItem(
    val testTag: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val iconBg: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

@Composable
private fun ArsenalCard(item: ArsenalButtonItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clickable(onClick = item.onClick)
            .testTag(item.testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                        .clip(RoundedCornerShape(6.dp))
                        .background(item.iconBg)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.iconTint
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = item.iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
