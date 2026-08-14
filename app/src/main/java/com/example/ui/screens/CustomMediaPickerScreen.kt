package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MediaSortType
import com.example.ui.NavigationTab
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CustomMediaPickerScreen(
    videos: List<VideoProcessor.DeviceMediaVideo>,
    targetTab: NavigationTab,
    initialFolderFilter: String? = null,
    initialTabIndex: Int = 0,
    onFolderFilterChanged: (String?) -> Unit = {},
    onTabIndexChanged: (Int) -> Unit = {},
    onClose: () -> Unit,
    onSelectVideoForTool: (Uri, NavigationTab) -> Unit,
    onSelectMultipleForJoin: (List<Uri>) -> Unit,
    onPlayVideo: (Uri) -> Unit,
    onDeleteVideo: (VideoProcessor.DeviceMediaVideo) -> Unit,
    onRefreshStorage: () -> Unit
) {
    // Automatically trigger storage scan/refresh when picker opens
    LaunchedEffect(Unit) {
        onRefreshStorage()
    }

    var isMultiSelectMode by remember(targetTab) { mutableStateOf(targetTab == NavigationTab.JOIN_VIDEO) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    var selectedTabIndex by remember { mutableStateOf(initialTabIndex) } // 0: Media, 1: Folders
    var isMediaGridView by remember { mutableStateOf(false) }
    var isFolderGridView by remember { mutableStateOf(false) } // Grid vs List view for Folders

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(MediaSortType.DATE_DESC) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    var videoToDelete by remember { mutableStateOf<VideoProcessor.DeviceMediaVideo?>(null) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(initialFolderFilter) }

    LaunchedEffect(selectedFolderFilter) {
        onFolderFilterChanged(selectedFolderFilter)
    }

    LaunchedEffect(selectedTabIndex) {
        onTabIndexChanged(selectedTabIndex)
    }

    // External System File Manager Launcher (Document / Files)
    val externalDocPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (isMultiSelectMode) {
                onSelectMultipleForJoin(listOf(uri))
            } else {
                onSelectVideoForTool(uri, targetTab)
            }
            onClose()
        }
    }

    val toolTitle = when (targetTab) {
        NavigationTab.SINGLE_CUT -> "Select Video to Cut"
        NavigationTab.MULTI_CUT -> "Select Video for Multi-Cut"
        NavigationTab.COMPRESSOR -> "Select Video to Compress"
        NavigationTab.JOIN_VIDEO -> "Select Videos to Join"
        NavigationTab.ROTATE_VIDEO -> "Select Video to Rotate"
        else -> "Select Video File"
    }

    // Filtered and Sorted list
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

    // Folder groups with first video thumbnail
    val folderGroups by remember(videos) {
        derivedStateOf {
            videos.groupBy { it.folderName }.map { (folderName, folderVideos) ->
                val firstVideo = folderVideos.firstOrNull()
                PickerFolderGroup(
                    name = folderName,
                    videoCount = folderVideos.size,
                    totalSizeBytes = folderVideos.sumOf { it.sizeBytes },
                    firstVideoUri = firstVideo?.uri,
                    firstVideoPath = firstVideo?.path
                )
            }.sortedByDescending { it.videoCount }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Light aesthetic background
    ) {
        // Top Action Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = toolTitle,
                            color = TextPrimaryDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${videos.size} videos in storage",
                            color = TextSecondaryMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Open Files / Storage Document Picker Icon
                    IconButton(onClick = { externalDocPickerLauncher.launch("video/*") }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open Files",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // View Mode Toggle (Grid/List)
                    IconButton(onClick = {
                        if (selectedTabIndex == 0) {
                            isMediaGridView = !isMediaGridView
                        } else {
                            isFolderGridView = !isFolderGridView
                        }
                    }) {
                        val isGrid = if (selectedTabIndex == 0) isMediaGridView else isFolderGridView
                        Icon(
                            imageVector = if (isGrid) Icons.Default.ViewList else Icons.Default.ViewModule,
                            contentDescription = "Toggle Grid/List",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Sort Menu
                    Box {
                        IconButton(onClick = { isSortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            MediaSortType.values().forEach { st ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = st.label,
                                            color = if (st == sortType) MaterialTheme.colorScheme.primary else TextPrimaryDark,
                                            fontSize = 13.sp,
                                            fontWeight = if (st == sortType) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        sortType = st
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = onRefreshStorage) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Clean Full-Width Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search video files...", color = TextSecondaryMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search", tint = TextSecondaryMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Tabs: Media | Folders
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Media (${processedVideos.size})",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else TextSecondaryMuted
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Folders (${folderGroups.size})",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else TextSecondaryMuted
                        )
                    }
                )
            }
        }

        // Active Folder Filter Indicator
        if (selectedFolderFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtered by Folder: $selectedFolderFilter",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Clear Filter ✕",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { selectedFolderFilter = null }
                )
            }
        }

        // Main Body List Content
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTabIndex == 1) {
                // Folders View Mode: Grid or List
                if (folderGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No video folders found", color = TextSecondaryMuted, fontSize = 14.sp)
                    }
                } else if (isFolderGridView) {
                    // Folders Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folderGroups) { folder ->
                            FolderGridCardItem(
                                folder = folder,
                                onClick = {
                                    selectedFolderFilter = folder.name
                                    selectedTabIndex = 0
                                }
                            )
                        }
                    }
                } else {
                    // Folders List View (One by One)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(folderGroups) { folder ->
                            FolderListRowItem(
                                folder = folder,
                                onClick = {
                                    selectedFolderFilter = folder.name
                                    selectedTabIndex = 0
                                }
                            )
                        }
                    }
                }
            } else {
                // Media Tab: Grid or List
                if (processedVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found", color = TextSecondaryMuted, fontSize = 14.sp)
                    }
                } else if (isMediaGridView) {
                    // Media Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(processedVideos) { item ->
                            val isSelected = selectedUris.contains(item.uri)
                            VideoGridCardItem(
                                item = item,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) selectedUris.remove(item.uri) else selectedUris.add(item.uri)
                                    } else {
                                        onSelectVideoForTool(item.uri, targetTab)
                                    }
                                },
                                onPlay = { onPlayVideo(item.uri) },
                                onDelete = { videoToDelete = item }
                            )
                        }
                    }
                } else {
                    // Media List View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(processedVideos) { item ->
                            val isSelected = selectedUris.contains(item.uri)
                            VideoListRowItem(
                                item = item,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        if (isSelected) selectedUris.remove(item.uri) else selectedUris.add(item.uri)
                                    } else {
                                        onSelectVideoForTool(item.uri, targetTab)
                                    }
                                },
                                onPlay = { onPlayVideo(item.uri) },
                                onSelectTool = { tab -> onSelectVideoForTool(item.uri, tab) },
                                onDelete = { videoToDelete = item }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Join Action Button
        if (isMultiSelectMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedUris.isNotEmpty()) {
                                onSelectMultipleForJoin(selectedUris.toList())
                            }
                        },
                        enabled = selectedUris.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedUris.isEmpty()) "Select videos to join" else "Add ${selectedUris.size} selected videos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
            text = { Text("Are you sure you want to delete '${target.title}' from your device storage?") },
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

private data class PickerFolderGroup(
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val firstVideoUri: Uri?,
    val firstVideoPath: String?
)

@Composable
private fun VideoThumbnailView(
    uri: Uri?,
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(uri, path) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri, path) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                bitmap = VideoProcessor.loadVideoThumbnail(context, uri, path)
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FolderGridCardItem(
    folder: PickerFolderGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                VideoThumbnailView(
                    uri = folder.firstVideoUri,
                    path = folder.firstVideoPath,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = folder.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${folder.videoCount} Videos • ${VideoProcessor.formatFileSize(folder.totalSizeBytes)}",
                fontSize = 11.sp,
                color = TextSecondaryMuted
            )
        }
    }
}

@Composable
private fun FolderListRowItem(
    folder: PickerFolderGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp, 56.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                VideoThumbnailView(
                    uri = folder.firstVideoUri,
                    path = folder.firstVideoPath,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${folder.videoCount} Videos • ${VideoProcessor.formatFileSize(folder.totalSizeBytes)}",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VideoListRowItem(
    item: VideoProcessor.DeviceMediaVideo,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onSelectTool: (NavigationTab) -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Thumbnail Box with Duration & Play button
            Box(
                modifier = Modifier
                    .size(72.dp, 58.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                VideoThumbnailView(
                    uri = item.uri,
                    path = item.path,
                    modifier = Modifier.fillMaxSize()
                )

                // Duration Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = VideoProcessor.formatDuration(item.durationMs),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onPlay),
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

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.width}x${item.height} • ${VideoProcessor.formatFileSize(item.sizeBytes)}",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = "Folder: ${item.folderName}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Options 3-dot dropdown
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Cut / Trim Video", color = TextPrimaryDark, fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectTool(NavigationTab.SINGLE_CUT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Multi-Part Cut", color = TextPrimaryDark, fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectTool(NavigationTab.MULTI_CUT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rotate Video", color = TextPrimaryDark, fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectTool(NavigationTab.ROTATE_VIDEO)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Compress Video", color = TextPrimaryDark, fontSize = 13.sp) },
                        onClick = {
                            isMenuExpanded = false
                            onSelectTool(NavigationTab.COMPRESSOR)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete from Storage", color = Color(0xFFDC2626), fontSize = 13.sp) },
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

@Composable
private fun VideoGridCardItem(
    item: VideoProcessor.DeviceMediaVideo,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    VideoThumbnailView(
                        uri = item.uri,
                        path = item.path,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = VideoProcessor.formatDuration(item.durationMs),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = item.title,
                    color = TextPrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = VideoProcessor.formatFileSize(item.sizeBytes),
                    color = TextSecondaryMuted,
                    fontSize = 9.sp
                )
            }

            if (isMultiSelectMode && isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                )
            }
        }
    }
}
