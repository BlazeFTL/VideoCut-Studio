package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.AppMonoFontFamily
import com.example.util.VideoProcessor

private val wittyGreetings = listOf(
    "Welcome, Master" to "Craft sharp cuts & studio exports.",
    "Ready for Magic?" to "Choose a studio tool to begin.",
    "Studio Command Ready" to "Precision trimming, merging & CRF compression.",
    "Ready to Slice & Dice?" to "Fast lossless cuts or high-efficiency transcodes.",
    "Your Studio Engine" to "Engineered for clean exports & real storage savings."
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val totalOrig = history.sumOf { it.originalSizeBytes.coerceAtLeast(0L) }
    val totalOut = history.sumOf { it.outputSizeBytes.coerceAtLeast(0L) }
    val overallSaveRatio = if (totalOrig > 0L) {
        ((totalOrig - totalOut).toFloat() / totalOrig.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedSaveRatio by animateFloatAsState(
        targetValue = if (history.isEmpty()) 0.18f else overallSaveRatio.coerceAtLeast(0.08f),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "overview_hero_ratio"
    )
    val savedPercentText = if (totalOrig > 0L) "${(overallSaveRatio * 100).toInt().coerceIn(0, 99)}%" else "0%"

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer

    val buttons = listOf(
        ArsenalButtonItem(
            testTag = "tool_card_single_cut",
            title = "Cut Video",
            description = "Frame-accurate or stream trim",
            icon = Icons.Outlined.ContentCut,
            badge = "SINGLE",
            iconBg = primaryContainer,
            iconTint = primary,
            badgeBg = primaryContainer,
            badgeText = onPrimaryContainer,
            cornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 28.dp),
            onClick = { onOpenPickerForTool(NavigationTab.SINGLE_CUT) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_remove_parts",
            title = "Remove Parts",
            description = "Cut out unwanted segments",
            icon = Icons.Default.FolderOpen,
            badge = "MULTI",
            iconBg = secondaryContainer,
            iconTint = secondary,
            badgeBg = secondaryContainer,
            badgeText = onSecondaryContainer,
            cornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 16.dp),
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.REMOVE_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_keep_parts",
            title = "Keep Parts",
            description = "Merge selected highlights",
            icon = Icons.Filled.FolderSpecial,
            badge = "MULTI",
            iconBg = primaryContainer,
            iconTint = primary,
            badgeBg = primaryContainer,
            badgeText = onPrimaryContainer,
            cornerShape = RoundedCornerShape(24.dp),
            onClick = {
                onSetMultiPartMode?.invoke(MultiPartCutMode.KEEP_SELECTED)
                onOpenPickerForTool(NavigationTab.MULTI_CUT)
            }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_compressor",
            title = "Compressor",
            description = "CRF x264, H.265 & Target MB",
            icon = Icons.Outlined.Compress,
            badge = "CRF ENGINE",
            iconBg = secondaryContainer,
            iconTint = secondary,
            badgeBg = secondaryContainer,
            badgeText = onSecondaryContainer,
            cornerShape = RoundedCornerShape(24.dp),
            onClick = { onOpenPickerForTool(NavigationTab.COMPRESSOR) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_join",
            title = "Join Video",
            description = "Merge clips cleanly",
            icon = Icons.Default.CallMerge,
            badge = "MERGE",
            iconBg = primaryContainer,
            iconTint = primary,
            badgeBg = primaryContainer,
            badgeText = onPrimaryContainer,
            cornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 16.dp),
            onClick = { onOpenPickerForTool(NavigationTab.JOIN_VIDEO) }
        ),
        ArsenalButtonItem(
            testTag = "tool_card_rotate",
            title = "Rotate Video",
            description = "Rotate, flip & timeline parts",
            icon = Icons.Outlined.RotateRight,
            badge = "TRANSFORM",
            iconBg = secondaryContainer,
            iconTint = secondary,
            badgeBg = secondaryContainer,
            badgeText = onSecondaryContainer,
            cornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 28.dp),
            onClick = { onOpenPickerForTool(NavigationTab.ROTATE_VIDEO) }
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Focal Visual Hero Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryContainer.copy(alpha = 0.72f),
                                        secondaryContainer.copy(alpha = 0.45f)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = greeting.first,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = VideoProcessor.formatFileSize(totalBytesSaved.coerceAtLeast(0L)),
                                        style = MaterialTheme.typography.displayMedium.copy(fontFamily = AppMonoFontFamily),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("overview_hero_saved_bytes")
                                    )
                                    Text(
                                        text = "Total storage reclaimed • $savedPercentText efficiency",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier.size(84.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularWavyProgressIndicator(
                                        progress = { animatedSaveRatio },
                                        modifier = Modifier.fillMaxSize(),
                                        color = primary,
                                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                    )
                                    Text(
                                        text = if (history.isEmpty()) "0" else "${history.size}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = AppMonoFontFamily),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = greeting.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { onNavigateTab(NavigationTab.MEDIA_SELECTOR) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("overview_media_browser_btn"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse Media", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                FilledTonalButton(
                                    onClick = { onNavigateTab(NavigationTab.HISTORY) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("overview_saved_library_btn"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.VideoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exports (${history.size})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Section Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Studio Tools",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedVideo != null) {
                        Text(
                            text = "Active: ${selectedVideo.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }
                }
            }

            // Expressive Soft-Tonal Tool Grid
            items(buttons.size) { index ->
                ArsenalCard(item = buttons[index])
            }
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
    val badgeBg: Color,
    val badgeText: Color,
    val cornerShape: RoundedCornerShape,
    val onClick: () -> Unit
)

@Composable
private fun ArsenalCard(item: ArsenalButtonItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(item.cornerShape)
            .clickable(onClick = item.onClick)
            .testTag(item.testTag),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = item.cornerShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
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
                        .background(item.badgeBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = item.badgeText,
                        letterSpacing = 0.4.sp
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${item.title}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
