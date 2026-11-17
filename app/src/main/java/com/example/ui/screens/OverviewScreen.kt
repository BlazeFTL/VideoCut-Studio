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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RotateRight
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
import androidx.compose.ui.text.style.TextAlign
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

private val wittyGreetings = listOf(
    "Greetings, Video Wizard!" to "What master stroke are we creating today?",
    "Ready for Magic?" to "Select a studio tool below to begin.",
    "Time to Edit!" to "Fast trimming, merging, rotation & compression.",
    "Ready to Slice & Dice?" to "Lossless cuts or crisp x264/x265 transcodes."
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
    val greetingIndex = rememberSaveable { 0 }
    val greeting = wittyGreetings[greetingIndex]

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val deeperTint = primary.copy(alpha = 0.22f)

    val buttons = listOf(
        ArsenalButtonItem(
            testTag = "tool_card_single_cut",
            title = "Cut Video",
            description = "Trim single range",
            icon = Icons.Outlined.ContentCut,
            badge = "SINGLE",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.SINGLE_CUT) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_remove_parts",
            title = "Remove Multiple Parts",
            description = "Cut out selected segments",
            icon = Icons.Outlined.Folder,
            badge = "MULTI-CUT",
            iconBg = deeperTint,
            iconTint = primary,
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.REMOVE_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_keep_parts",
            title = "Keep Multiple Parts",
            description = "Merge selected segments",
            icon = Icons.Filled.FolderSpecial,
            badge = "MULTI-CUT",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.KEEP_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_compressor",
            title = "Video Compressor",
            description = "Reduce file size",
            icon = Icons.Outlined.Compress,
            badge = "FAST",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.COMPRESSOR) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_join",
            title = "Join Video",
            description = "Combine multiple videos",
            icon = Icons.Default.CallMerge,
            badge = "MERGER",
            iconBg = deeperTint,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.JOIN_VIDEO) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_rotate",
            title = "Rotate Video",
            description = "Rotate, mirror & timeline",
            icon = Icons.Outlined.RotateRight,
            badge = "ROTATE",
            iconBg = primaryContainer,
            iconTint = primary,
            onClick = { onOpenPickerForTool(NavigationTab.ROTATE_VIDEO) }
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = greeting.first,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp,
                    color = primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = greeting.second,
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Your Arsenal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primary,
                    textAlign = TextAlign.Center
                )
            }
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
            .height(152.dp)
            .clickable(onClick = item.onClick)
            .testTag(item.testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(18.dp)
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
                        .clip(RoundedCornerShape(50))
                        .background(item.iconBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
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
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        color = TextPrimaryDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
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
