package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MediaSortType
import com.example.ui.MediaViewMode
import com.example.ui.NavigationTab
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

@Composable
fun MediaSelectorScreen(
    videos: List<VideoProcessor.DeviceMediaVideo>,
    viewMode: MediaViewMode,
    sortType: MediaSortType,
    onSetViewMode: (MediaViewMode) -> Unit,
    onSetSortType: (MediaSortType) -> Unit,
    onSelectVideoUri: (Uri) -> Unit,
    onNavigateTab: (NavigationTab) -> Unit,
    onPlayVideo: (Uri) -> Unit,
    onDeleteVideo: (VideoProcessor.DeviceMediaVideo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoProcessor.DeviceMediaVideo?>(null) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }

    // Filtered & Sorted Video List
    val processedVideos by remember(videos, searchQuery, sortType, selectedFolderFilter) {
        derivedStateOf {
            var list = videos

            if (selectedFolderFilter != null) {
                list = list.filter { it.folderName.equals(selectedFolderFilter, ignoreCase = true) }
            }

            if (searchQuery.isNotBlank()) {
                list = list.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            when (sortType) {
                MediaSortType.DATE_DESC -> list.sortedByDescending { it.dateModifiedMs }
                MediaSortType.DATE_ASC -> list.sortedBy { it.dateModifiedMs }
                MediaSortType.NAME_ASC -> list.sortedBy { it.title.lowercase() }
                MediaSortType.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                MediaSortType.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            }
        }
    }

    // Folders Grouping
    val folderGroups by remember(videos) {
        derivedStateOf {
            videos.groupBy { it.folderName }.map { (folderName, folderVideos) ->
                FolderGroup(
                    name = folderName,
                    videoCount = folderVideos.size,
                    totalSizeBytes = folderVideos.sumOf { it.sizeBytes },
                    sampleUri = folderVideos.firstOrNull()?.uri
                )
            }.sortedByDescending { it.videoCount }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Media Selector",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${videos.size} Videos found on device",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // View Mode Toggle (Files vs Folders)
                IconButton(
                    onClick = {
                        val next = if (viewMode == MediaViewMode.FILES) MediaViewMode.FOLDERS else MediaViewMode.FILES
                        selectedFolderFilter = null
                        onSetViewMode(next)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PrimaryContainerLight)
                ) {
                    Icon(
                        imageVector = if (viewMode == MediaViewMode.FILES) Icons.Default.Folder else Icons.Default.ViewList,
                        contentDescription = "Toggle View Mode",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown Menu
                Box {
                    IconButton(
                        onClick = { isSortMenuExpanded = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryContainerLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Options",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        MediaSortType.values().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label, fontSize = 13.sp) },
                                onClick = {
                                    onSetSortType(st)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search videos by title...", fontSize = 13.sp, color = TextSecondaryMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Folder Filter Breadcrumb
        if (selectedFolderFilter != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryContainerLight)
                    .clickable { selectedFolderFilter = null }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Folder: $selectedFolderFilter  ✕ Clear Filter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (viewMode == MediaViewMode.FOLDERS && selectedFolderFilter == null) {
            // Folders View
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folderGroups) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFolderFilter = folder.name
                                onSetViewMode(MediaViewMode.FILES)
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SurfaceBorderLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryContainerLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = folder.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                maxLines = 1
                            )

                            Text(
                                text = "${folder.videoCount} Videos • ${VideoProcessor.formatFileSize(folder.totalSizeBytes)}",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                }
            }
        } else {
            // Files View (List of videos)
            if (processedVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No videos found", color = TextSecondaryMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(processedVideos) { item ->
                        DeviceVideoRowItem(
                            item = item,
                            onPlay = { onPlayVideo(item.uri) },
                            onSelectForTool = { tab ->
                                onSelectVideoUri(item.uri)
                                onNavigateTab(tab)
                            },
                            onDelete = { videoToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (videoToDelete != null) {
        val target = videoToDelete!!
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${target.title}' from your device?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVideo(target)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class FolderGroup(
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val sampleUri: Uri?
)

@Composable
private fun DeviceVideoRowItem(
    item: VideoProcessor.DeviceMediaVideo,
    onPlay: () -> Unit,
    onSelectForTool: (NavigationTab) -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(64.dp, 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(26.dp)
                )

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
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimaryDark,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${VideoProcessor.formatDuration(item.durationMs)} • ${item.width}x${item.height}",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Size: ${VideoProcessor.formatFileSize(item.sizeBytes)} • Folder: ${item.folderName}",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted
                )
            }

            // Options Action Menu
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.primary)
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Cut / Trim Video", fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectForTool(NavigationTab.SINGLE_CUT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Multi-Part Cut", fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectForTool(NavigationTab.MULTI_CUT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rotate Video", fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectForTool(NavigationTab.ROTATE_VIDEO)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress Video", fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectForTool(NavigationTab.COMPRESSOR)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete from Device", fontSize = 13.sp, color = Color(0xFFDC2626)) },
                        onClick = {
                            isMenuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
