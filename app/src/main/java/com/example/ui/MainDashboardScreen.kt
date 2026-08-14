package com.example.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FullscreenProcessingDialog
import com.example.ui.components.NavigationDrawerContent
import com.example.ui.components.TopDashboardBar
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.CompressorScreen
import com.example.ui.screens.CustomMediaPickerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.JoinVideoScreen
import com.example.ui.screens.MediaSelectorScreen
import com.example.ui.screens.MultiPartCutScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.RotateVideoScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SingleCutScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val timelineThumbnails by viewModel.timelineThumbnails.collectAsStateWithLifecycle()

    val singleStartMs by viewModel.singleStartMs.collectAsStateWithLifecycle()
    val singleEndMs by viewModel.singleEndMs.collectAsStateWithLifecycle()

    val multiPartMode by viewModel.multiPartMode.collectAsStateWithLifecycle()
    val multiPartSegments by viewModel.multiPartSegments.collectAsStateWithLifecycle()

    val compressionPreset by viewModel.compressionPreset.collectAsStateWithLifecycle()
    val customResolution by viewModel.customResolution.collectAsStateWithLifecycle()

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingProgress by viewModel.processingProgress.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val history by viewModel.historyState.collectAsStateWithLifecycle()
    val totalBytesSaved by viewModel.totalBytesSavedState.collectAsStateWithLifecycle()

    val previewVideoUri by viewModel.previewVideoUri.collectAsStateWithLifecycle()

    val joinVideosList by viewModel.joinVideosList.collectAsStateWithLifecycle()
    val joinSortOption by viewModel.joinSortOption.collectAsStateWithLifecycle()

    val rotationDegrees by viewModel.rotationDegrees.collectAsStateWithLifecycle()
    val flipHorizontal by viewModel.flipHorizontal.collectAsStateWithLifecycle()
    val flipVertical by viewModel.flipVertical.collectAsStateWithLifecycle()
    val isTimelineRotateEnabled by viewModel.isTimelineRotateEnabled.collectAsStateWithLifecycle()
    val rotateStartMs by viewModel.rotateStartMs.collectAsStateWithLifecycle()
    val rotateEndMs by viewModel.rotateEndMs.collectAsStateWithLifecycle()
    val rotateParts by viewModel.rotateParts.collectAsStateWithLifecycle()
    val compressionMethod by viewModel.compressionMethod.collectAsStateWithLifecycle()
    val crfValue by viewModel.crfValue.collectAsStateWithLifecycle()
    val encodingSpeed by viewModel.encodingSpeed.collectAsStateWithLifecycle()
    val targetSizeBytes by viewModel.targetSizeBytes.collectAsStateWithLifecycle()

    val deviceVideos by viewModel.deviceVideos.collectAsStateWithLifecycle()
    val mediaViewMode by viewModel.mediaViewMode.collectAsStateWithLifecycle()
    val mediaSortType by viewModel.mediaSortType.collectAsStateWithLifecycle()
    val customPickerTargetTab by viewModel.customPickerTargetTab.collectAsStateWithLifecycle()
    val pickerFolderFilter by viewModel.pickerFolderFilter.collectAsStateWithLifecycle()
    val pickerTabIndex by viewModel.pickerTabIndex.collectAsStateWithLifecycle()

    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val enableForegroundService by viewModel.enableForegroundService.collectAsStateWithLifecycle()
    val enableWakeLock by viewModel.enableWakeLock.collectAsStateWithLifecycle()

    // Handle System Back Button so it navigates back to Overview instead of minimizing the app
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen || activeTab != NavigationTab.OVERVIEW) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (activeTab != NavigationTab.OVERVIEW) {
            viewModel.selectTab(NavigationTab.OVERVIEW)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            NavigationDrawerContent(
                activeTab = activeTab,
                onSelectTab = { tab ->
                    viewModel.selectTab(tab)
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopDashboardBar(
                    activeTab = activeTab,
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    },
                    onOpenSettings = {
                        viewModel.selectTab(NavigationTab.SETTINGS)
                    },
                    onNavigateBack = {
                        viewModel.selectTab(NavigationTab.OVERVIEW)
                    },
                    onGoHome = {
                        viewModel.selectTab(NavigationTab.OVERVIEW)
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    NavigationTab.OVERVIEW -> {
                        OverviewScreen(
                            selectedVideo = selectedVideo,
                            history = history,
                            totalBytesSaved = totalBytesSaved ?: 0L,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onNavigateTab = viewModel::selectTab,
                            onOpenPickerForTool = viewModel::openCustomPicker,
                            onPlayVideo = viewModel::setPreviewVideoUri,
                            onSetMultiPartMode = viewModel::setMultiPartMode
                        )
                    }

                    NavigationTab.SINGLE_CUT -> {
                        SingleCutScreen(
                            selectedVideo = selectedVideo,
                            timelineThumbnails = timelineThumbnails,
                            startMs = singleStartMs,
                            endMs = singleEndMs,
                            isProcessing = isProcessing,
                            processingProgress = processingProgress,
                            statusMessage = statusMessage,
                            onRangeChanged = viewModel::setSingleCutRange,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onOpenPicker = { viewModel.openCustomPicker(NavigationTab.SINGLE_CUT) },
                            onProcessCut = viewModel::processSingleCut,
                            onPlayVideo = viewModel::setPreviewVideoUri
                        )
                    }

                    NavigationTab.MULTI_CUT -> {
                        MultiPartCutScreen(
                            selectedVideo = selectedVideo,
                            timelineThumbnails = timelineThumbnails,
                            mode = multiPartMode,
                            segments = multiPartSegments,
                            isProcessing = isProcessing,
                            processingProgress = processingProgress,
                            statusMessage = statusMessage,
                            onSetMode = viewModel::setMultiPartMode,
                            onAddSegment = viewModel::addCutSegment,
                            onRemoveSegment = viewModel::removeCutSegment,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onProcessCut = viewModel::processMultiPartCut,
                            onPlayVideo = viewModel::setPreviewVideoUri
                        )
                    }

                    NavigationTab.COMPRESSOR -> {
                        CompressorScreen(
                            selectedVideo = selectedVideo,
                            currentMethod = compressionMethod,
                            crfValue = crfValue,
                            encodingSpeed = encodingSpeed,
                            targetSizeBytes = targetSizeBytes,
                            currentResolution = customResolution,
                            isProcessing = isProcessing,
                            processingProgress = processingProgress,
                            statusMessage = statusMessage,
                            onSetMethod = viewModel::setCompressionMethod,
                            onSetCrfValue = viewModel::setCrfValue,
                            onSetEncodingSpeed = viewModel::setEncodingSpeed,
                            onSetTargetSizeBytes = viewModel::setTargetSizeBytes,
                            onSetResolution = viewModel::setCustomResolution,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onProcessCompress = viewModel::processCompression,
                            onPlayVideo = viewModel::setPreviewVideoUri
                        )
                    }

                    NavigationTab.JOIN_VIDEO -> {
                        JoinVideoScreen(
                            videoList = joinVideosList,
                            sortOption = joinSortOption,
                            onAddVideoUris = viewModel::addJoinVideoUris,
                            onRemoveVideo = viewModel::removeJoinVideo,
                            onMoveVideo = viewModel::moveJoinVideo,
                            onSetSortOption = viewModel::setJoinSortOption,
                            onPlayVideo = viewModel::setPreviewVideoUri,
                            onProcessJoin = viewModel::processJoinVideos,
                            isProcessing = isProcessing,
                            onOpenCustomPicker = { viewModel.openCustomPicker(NavigationTab.JOIN_VIDEO) },
                            onRotateVideoInJoin = viewModel::updateJoinVideoRotation
                        )
                    }

                    NavigationTab.ROTATE_VIDEO -> {
                        RotateVideoScreen(
                            selectedVideo = selectedVideo,
                            rotationDegrees = rotationDegrees,
                            flipHorizontal = flipHorizontal,
                            flipVertical = flipVertical,
                            isTimelineRotateEnabled = isTimelineRotateEnabled,
                            rotateStartMs = rotateStartMs,
                            rotateEndMs = rotateEndMs,
                            rotateParts = rotateParts,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onRotate90CW = viewModel::rotate90CW,
                            onSetRotationDegrees = viewModel::setRotationDegrees,
                            onToggleFlipHorizontal = viewModel::toggleFlipHorizontal,
                            onToggleFlipVertical = viewModel::toggleFlipVertical,
                            onSetTimelineRotateEnabled = viewModel::setTimelineRotateEnabled,
                            onSetTimelineRange = viewModel::setRotateTimelineRange,
                            onAddRotatePart = viewModel::addRotatePart,
                            onRemoveRotatePart = viewModel::removeRotatePart,
                            onUpdateRotatePart = viewModel::updateRotatePart,
                            onPlayVideo = viewModel::setPreviewVideoUri,
                            onProcessRotate = viewModel::processRotateVideo,
                            isProcessing = isProcessing,
                            processingProgress = processingProgress,
                            statusMessage = statusMessage
                        )
                    }

                    NavigationTab.MEDIA_SELECTOR -> {
                        MediaSelectorScreen(
                            videos = deviceVideos,
                            viewMode = mediaViewMode,
                            sortType = mediaSortType,
                            onSetViewMode = viewModel::setMediaViewMode,
                            onSetSortType = viewModel::setMediaSortType,
                            onSelectVideoUri = viewModel::selectVideoUri,
                            onNavigateTab = viewModel::selectTab,
                            onPlayVideo = viewModel::setPreviewVideoUri,
                            onDeleteVideo = viewModel::deleteDeviceMediaVideo
                        )
                    }

                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            history = history,
                            totalBytesSaved = totalBytesSaved ?: 0L,
                            onDelete = viewModel::deleteHistoryItem,
                            onPlayVideo = viewModel::setPreviewVideoUri
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            currentTheme = currentTheme,
                            enableForegroundService = enableForegroundService,
                            enableWakeLock = enableWakeLock,
                            onSelectTheme = viewModel::setAppTheme,
                            onToggleForegroundService = viewModel::setEnableForegroundService,
                            onToggleWakeLock = viewModel::setEnableWakeLock,
                            onNavigateBack = { viewModel.selectTab(NavigationTab.OVERVIEW) }
                        )
                    }
                }
            }
        }
    }

    // Video Player Fullscreen Dialog Modal
    if (previewVideoUri != null) {
        Dialog(
            onDismissRequest = { viewModel.setPreviewVideoUri(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayerView(
                    videoUri = previewVideoUri!!,
                    autoPlay = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Overlay Bar with Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Preview",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { viewModel.setPreviewVideoUri(null) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Custom Media Picker Overlay
    if (customPickerTargetTab != null) {
        Dialog(
            onDismissRequest = { viewModel.closeCustomPicker() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CustomMediaPickerScreen(
                videos = deviceVideos,
                targetTab = customPickerTargetTab!!,
                initialFolderFilter = pickerFolderFilter,
                initialTabIndex = pickerTabIndex,
                onFolderFilterChanged = viewModel::setPickerFolderFilter,
                onTabIndexChanged = viewModel::setPickerTabIndex,
                onClose = { viewModel.closeCustomPicker() },
                onSelectVideoForTool = { uri, tab ->
                    viewModel.pickVideoForTarget(uri, tab)
                },
                onSelectMultipleForJoin = { uris ->
                    viewModel.addJoinVideoUris(uris)
                    viewModel.selectTab(NavigationTab.JOIN_VIDEO)
                    viewModel.closeCustomPicker()
                },
                onPlayVideo = { uri -> viewModel.setPreviewVideoUri(uri) },
                onDeleteVideo = { video -> viewModel.deleteDeviceMediaVideo(video) },
                onRefreshStorage = { viewModel.loadDeviceVideos() }
            )
        }
    }

    // Global Fullscreen Processing Dialog with Cancel action
    FullscreenProcessingDialog(
        isProcessing = isProcessing,
        progress = processingProgress,
        statusMessage = statusMessage,
        processName = when (activeTab) {
            NavigationTab.SINGLE_CUT -> "Cutting Video"
            NavigationTab.MULTI_CUT -> "Multi-Part Cut"
            NavigationTab.COMPRESSOR -> "Compressing Video"
            NavigationTab.JOIN_VIDEO -> "Merging Videos"
            NavigationTab.ROTATE_VIDEO -> "Rotating Video"
            else -> "Video Processing"
        },
        title = when (activeTab) {
            NavigationTab.JOIN_VIDEO -> "0/${joinVideosList.size.coerceAtLeast(1)} Files Converted"
            else -> "0/1 Files Converted"
        },
        videoItem = selectedVideo ?: joinVideosList.firstOrNull(),
        onCancel = { viewModel.cancelCurrentProcessing() }
    )
}
