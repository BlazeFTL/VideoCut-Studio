package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.VideoPlayerView
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoItem
import com.example.ui.JoinSortOption
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

@Composable
fun JoinVideoScreen(
    videoList: List<VideoItem>,
    sortOption: JoinSortOption,
    onAddVideoUris: (List<Uri>) -> Unit,
    onRemoveVideo: (Int) -> Unit,
    onMoveVideo: (fromIndex: Int, toIndex: Int) -> Unit,
    onSetSortOption: (JoinSortOption) -> Unit,
    onPlayVideo: (Uri) -> Unit,
    onProcessJoin: () -> Unit,
    isProcessing: Boolean,
    onOpenCustomPicker: () -> Unit = {},
    onRotateVideoInJoin: (videoItem: VideoItem, rotationDegrees: Int, flipH: Boolean, flipV: Boolean, onSuccess: (VideoItem) -> Unit) -> Unit = { _, _, _, _, _ -> }
) {
    var isSortDropdownExpanded by remember { mutableStateOf(false) }
    var videoToRotate by remember { mutableStateOf<VideoItem?>(null) }

    // Check if all resolutions match
    val resolutionsMatch = remember(videoList) {
        if (videoList.isEmpty()) true
        else {
            val firstW = videoList.first().width
            val firstH = videoList.first().height
            videoList.all { it.width == firstW && it.height == firstH }
        }
    }

    val totalDurationMs = remember(videoList) { videoList.sumOf { it.durationMs } }
    val totalSizeBytes = remember(videoList) { videoList.sumOf { it.sizeBytes } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Join Videos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Merge multiple videos seamlessly into one",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            Button(
                onClick = onOpenCustomPicker,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (videoList.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onOpenCustomPicker() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallMerge,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Videos Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tap here or 'Add Videos' to pick 2 or more videos to merge",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            // Control bar: Sorting & Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${videoList.size} Videos • ${VideoProcessor.formatDuration(totalDurationMs)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Total Size: ${VideoProcessor.formatFileSize(totalSizeBytes)}",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }

                    // Sort Selector
                    Box {
                        OutlinedButton(
                            onClick = { isSortDropdownExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(sortOption.label, fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = isSortDropdownExpanded,
                            onDismissRequest = { isSortDropdownExpanded = false }
                        ) {
                            JoinSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label, fontSize = 13.sp) },
                                    onClick = {
                                        onSetSortOption(option)
                                        isSortDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reorderable / Sortable Video List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(videoList, key = { _, item -> item.uri.toString() }) { index, item ->
                    var isDragging by remember { mutableStateOf(false) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }

                    Card(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = dragOffsetY
                                shadowElevation = if (isDragging) 12f else 2f
                            }
                            .clickable { onPlayVideo(item.uri) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDragging) Color(0xFFF5F3FF) else Color.White
                        ),
                        border = BorderStroke(
                            width = if (isDragging) 2.dp else 1.dp,
                            color = if (isDragging) PrimaryIndigo else SurfaceBorderLight
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDragging) 8.dp else 1.dp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle Icon on the left for reordering
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .pointerInput(videoList.size, index) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                isDragging = true
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount
                                                val stepPx = 160f
                                                val delta = kotlin.math.round(dragOffsetY / stepPx).toInt()
                                                if (delta != 0) {
                                                    val targetIndex = (index + delta).coerceIn(0, videoList.size - 1)
                                                    if (targetIndex != index) {
                                                        onMoveVideo(index, targetIndex)
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = if (isDragging) PrimaryIndigo else TextSecondaryMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Thumbnail with Play Icon overlay
                            Box(
                                modifier = Modifier
                                    .size(60.dp, 60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryContainerLight),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.thumbnail != null) {
                                    Image(
                                        bitmap = item.thumbnail.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Movie,
                                        contentDescription = null,
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Index Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .background(PrimaryIndigo, RoundedCornerShape(bottomEnd = 6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Details with Long-Press Drag Anywhere Support
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(videoList.size, index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                isDragging = true
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val stepPx = 160f
                                                val delta = kotlin.math.round(dragOffsetY / stepPx).toInt()
                                                if (delta != 0) {
                                                    val targetIndex = (index + delta).coerceIn(0, videoList.size - 1)
                                                    if (targetIndex != index) {
                                                        onMoveVideo(index, targetIndex)
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimaryDark,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${VideoProcessor.formatDuration(item.durationMs)} • ${item.width}x${item.height} • ${VideoProcessor.formatFileSize(item.sizeBytes)}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                            }

                            // Quick Move Up / Move Down Actions
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveVideo(index, index - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextSecondaryMuted, modifier = Modifier.size(16.dp))
                                    }
                                }

                                if (index < videoList.size - 1) {
                                    IconButton(
                                        onClick = { onMoveVideo(index, index + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextSecondaryMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Rotate Video Option Button
                            IconButton(
                                onClick = { videoToRotate = item },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate Video",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Clean Trash Icon Button to Remove
                            IconButton(
                                onClick = { onRemoveVideo(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove Video",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Fast Merge vs Re-encode Needed
            Button(
                onClick = onProcessJoin,
                enabled = videoList.size >= 2 && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resolutionsMatch) EmeraldSuccess else PrimaryIndigo
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallMerge,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (resolutionsMatch) "Fast Merge (${videoList.size} Videos)" else "Re-encode Needed (${videoList.size} Videos)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // Join Video Rotate Dialog Overlay
    if (videoToRotate != null) {
        JoinRotateVideoDialog(
            videoItem = videoToRotate!!,
            onDismiss = { videoToRotate = null },
            onSaveRotation = { degrees, flipH, flipV ->
                val targetVideo = videoToRotate!!
                onRotateVideoInJoin(targetVideo, degrees, flipH, flipV) { updatedItem ->
                    videoToRotate = null
                }
            }
        )
    }
}

@Composable
fun JoinRotateVideoDialog(
    videoItem: VideoItem,
    onDismiss: () -> Unit,
    onSaveRotation: (rotationDegrees: Int, flipHorizontal: Boolean, flipVertical: Boolean) -> Unit
) {
    var rotationDegrees by remember { mutableStateOf(0) }
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, SurfaceBorderLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rotate Video",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = videoItem.title,
                            fontSize = 12.sp,
                            color = TextSecondaryMuted,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryMuted)
                    }
                }

                // Video Player Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        VideoPlayerView(
                            videoUri = videoItem.uri,
                            durationMs = videoItem.durationMs,
                            startMs = 0L,
                            endMs = videoItem.durationMs,
                            autoPlay = false,
                            rotationDegrees = rotationDegrees,
                            flipHorizontal = flipHorizontal,
                            flipVertical = flipVertical,
                            heightDp = null,
                            videoWidth = videoItem.width,
                            videoHeight = videoItem.height
                        )
                    }
                }

                // Rotation Controls (NO time ranges shown)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF18181B),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // CCW (-90)
                        IconButton(
                            onClick = { rotationDegrees = ((rotationDegrees - 90) % 360 + 360) % 360 },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "CCW", tint = Color.White)
                        }

                        // CW (+90)
                        IconButton(
                            onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "CW", tint = Color.White)
                        }

                        // Flip H
                        IconButton(
                            onClick = { flipHorizontal = !flipHorizontal },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (flipHorizontal) PrimaryIndigo else Color.Transparent)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Flip H", tint = Color.White)
                        }

                        // Flip V
                        IconButton(
                            onClick = { flipVertical = !flipVertical },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (flipVertical) PrimaryIndigo else Color.Transparent)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Flip V", tint = Color.White)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onSaveRotation(rotationDegrees, flipHorizontal, flipVertical)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save & Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
