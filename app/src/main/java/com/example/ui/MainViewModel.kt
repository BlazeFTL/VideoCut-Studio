package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ProcessedVideoEntity
import com.example.model.AppTheme
import com.example.model.CompressionMethod
import com.example.model.CompressionPreset
import com.example.model.CompressionSettings
import com.example.model.CutSegment
import com.example.model.MultiPartCutMode
import com.example.model.RotatePart
import com.example.model.VideoItem
import com.example.service.VideoProcessingService
import com.example.util.VideoProcessor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import android.content.Context

enum class NavigationTab(val title: String, val iconName: String) {
    OVERVIEW("Overview", "dashboard"),
    SINGLE_CUT("Cut Video", "content_cut"),
    MULTI_CUT("Multi-Part Cut", "cut"),
    COMPRESSOR("Video Compressor", "compress"),
    JOIN_VIDEO("Join Video", "merge"),
    ROTATE_VIDEO("Rotate Video", "rotate_right"),
    MEDIA_SELECTOR("Media Selector", "folder"),
    HISTORY("Saved Videos", "video_library"),
    SETTINGS("Settings", "settings")
}


enum class JoinSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DURATION("Duration"),
    SIZE("File Size"),
    CUSTOM("Custom Order")
}

enum class MediaViewMode {
    FILES,
    FOLDERS
}

enum class MediaSortType(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A-Z)"),
    SIZE_DESC("Largest Size"),
    DURATION_DESC("Longest Duration")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.processedVideoDao()

    val historyState: StateFlow<List<ProcessedVideoEntity>> = dao.getAllProcessedVideos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalBytesSavedState: StateFlow<Long?> = dao.getTotalBytesSaved().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    private val _activeTab = MutableStateFlow(NavigationTab.OVERVIEW)
    val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    private val _timelineThumbnails = MutableStateFlow<List<android.graphics.Bitmap>>(emptyList())
    val timelineThumbnails: StateFlow<List<android.graphics.Bitmap>> = _timelineThumbnails.asStateFlow()

    // Single Cut State
    private val _singleStartMs = MutableStateFlow(0L)
    val singleStartMs: StateFlow<Long> = _singleStartMs.asStateFlow()

    private val _singleEndMs = MutableStateFlow(0L)
    val singleEndMs: StateFlow<Long> = _singleEndMs.asStateFlow()

    // Multi-Part Cut State
    private val _multiPartMode = MutableStateFlow(MultiPartCutMode.REMOVE_SELECTED)
    val multiPartMode: StateFlow<MultiPartCutMode> = _multiPartMode.asStateFlow()

    private val _multiPartSegments = MutableStateFlow<List<CutSegment>>(emptyList())
    val multiPartSegments: StateFlow<List<CutSegment>> = _multiPartSegments.asStateFlow()

    // Compression State
    private val _compressionPreset = MutableStateFlow(CompressionPreset.BALANCED)
    val compressionPreset: StateFlow<CompressionPreset> = _compressionPreset.asStateFlow()

    private val _compressionMethod = MutableStateFlow(CompressionMethod.H264_CRF)
    val compressionMethod: StateFlow<CompressionMethod> = _compressionMethod.asStateFlow()

    private val _crfValue = MutableStateFlow(23)
    val crfValue: StateFlow<Int> = _crfValue.asStateFlow()

    private val _encodingSpeed = MutableStateFlow("medium")
    val encodingSpeed: StateFlow<String> = _encodingSpeed.asStateFlow()

    private val _targetSizeBytes = MutableStateFlow(0L)
    val targetSizeBytes: StateFlow<Long> = _targetSizeBytes.asStateFlow()

    private val _customResolution = MutableStateFlow("Original Resolution")
    val customResolution: StateFlow<String> = _customResolution.asStateFlow()

    // Join Video State
    private val _joinVideosList = MutableStateFlow<List<VideoItem>>(emptyList())
    val joinVideosList: StateFlow<List<VideoItem>> = _joinVideosList.asStateFlow()

    private val _joinSortOption = MutableStateFlow(JoinSortOption.NAME_ASC)
    val joinSortOption: StateFlow<JoinSortOption> = _joinSortOption.asStateFlow()

    // Rotate Video State
    private val _rotationDegrees = MutableStateFlow(0)
    val rotationDegrees: StateFlow<Int> = _rotationDegrees.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(false)
    val flipHorizontal: StateFlow<Boolean> = _flipHorizontal.asStateFlow()

    private val _flipVertical = MutableStateFlow(false)
    val flipVertical: StateFlow<Boolean> = _flipVertical.asStateFlow()

    private val _isTimelineRotateEnabled = MutableStateFlow(false)
    val isTimelineRotateEnabled: StateFlow<Boolean> = _isTimelineRotateEnabled.asStateFlow()

    private val _rotateStartMs = MutableStateFlow(0L)
    val rotateStartMs: StateFlow<Long> = _rotateStartMs.asStateFlow()

    private val _rotateEndMs = MutableStateFlow(0L)
    val rotateEndMs: StateFlow<Long> = _rotateEndMs.asStateFlow()

    private val _rotateParts = MutableStateFlow<List<RotatePart>>(emptyList())
    val rotateParts: StateFlow<List<RotatePart>> = _rotateParts.asStateFlow()

    // Media Selector State
    private val _deviceVideos = MutableStateFlow<List<VideoProcessor.DeviceMediaVideo>>(emptyList())
    val deviceVideos: StateFlow<List<VideoProcessor.DeviceMediaVideo>> = _deviceVideos.asStateFlow()

    private val _mediaViewMode = MutableStateFlow(MediaViewMode.FILES)
    val mediaViewMode: StateFlow<MediaViewMode> = _mediaViewMode.asStateFlow()

    private val _mediaSortType = MutableStateFlow(MediaSortType.DATE_DESC)
    val mediaSortType: StateFlow<MediaSortType> = _mediaSortType.asStateFlow()

    // Custom Fullscreen Media Picker Target & Persistent State
    private val _customPickerTargetTab = MutableStateFlow<NavigationTab?>(null)
    val customPickerTargetTab: StateFlow<NavigationTab?> = _customPickerTargetTab.asStateFlow()

    private val _pickerFolderFilter = MutableStateFlow<String?>(null)
    val pickerFolderFilter: StateFlow<String?> = _pickerFolderFilter.asStateFlow()

    private val _pickerTabIndex = MutableStateFlow(0)
    val pickerTabIndex: StateFlow<Int> = _pickerTabIndex.asStateFlow()

    private val _pickerScrollIndex = MutableStateFlow(0)
    val pickerScrollIndex: StateFlow<Int> = _pickerScrollIndex.asStateFlow()

    private val _pickerScrollOffset = MutableStateFlow(0)
    val pickerScrollOffset: StateFlow<Int> = _pickerScrollOffset.asStateFlow()

    fun setPickerFolderFilter(folder: String?) {
        _pickerFolderFilter.value = folder
    }

    fun setPickerTabIndex(index: Int) {
        _pickerTabIndex.value = index
    }

    fun setPickerScrollPosition(index: Int, offset: Int) {
        _pickerScrollIndex.value = index
        _pickerScrollOffset.value = offset
    }

    fun openCustomPicker(targetTab: NavigationTab) {
        _customPickerTargetTab.value = targetTab
    }

    fun closeCustomPicker() {
        _customPickerTargetTab.value = null
    }

    fun pickVideoForTarget(uri: Uri, targetTab: NavigationTab) {
        selectVideoUri(uri)
        _activeTab.value = targetTab
        _customPickerTargetTab.value = null
    }

    // Settings & Theme State
    private val prefs = getApplication<Application>().getSharedPreferences("videocut_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(
        AppTheme.fromId(prefs.getString("selected_theme", AppTheme.MODERN_INDIGO.id) ?: AppTheme.MODERN_INDIGO.id)
    )
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _enableForegroundService = MutableStateFlow(
        prefs.getBoolean("enable_fg_service", true)
    )
    val enableForegroundService: StateFlow<Boolean> = _enableForegroundService.asStateFlow()

    private val _enableWakeLock = MutableStateFlow(
        prefs.getBoolean("enable_wakelock", true)
    )
    val enableWakeLock: StateFlow<Boolean> = _enableWakeLock.asStateFlow()

    fun setAppTheme(theme: AppTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("selected_theme", theme.id).apply()
    }

    fun setEnableForegroundService(enabled: Boolean) {
        _enableForegroundService.value = enabled
        prefs.edit().putBoolean("enable_fg_service", enabled).apply()
    }

    fun setEnableWakeLock(enabled: Boolean) {
        _enableWakeLock.value = enabled
        prefs.edit().putBoolean("enable_wakelock", enabled).apply()
    }

    private fun startBackgroundServiceIfNeeded(title: String, status: String) {
        if (_enableForegroundService.value) {
            VideoProcessingService.startProcessing(
                context = getApplication(),
                title = title,
                status = status,
                progress = (_processingProgress.value * 100).toInt(),
                enableWakeLock = _enableWakeLock.value
            )
        }
    }

    private fun updateBackgroundServiceProgress(status: String, progress: Float) {
        if (_enableForegroundService.value && _isProcessing.value) {
            VideoProcessingService.updateProgress(
                context = getApplication(),
                status = status,
                progress = (progress * 100).toInt()
            )
        }
    }

    private fun stopBackgroundServiceIfNeeded() {
        if (_enableForegroundService.value) {
            VideoProcessingService.stopProcessing(getApplication())
        }
    }

    // Processing State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _previewVideoUri = MutableStateFlow<Uri?>(null)
    val previewVideoUri: StateFlow<Uri?> = _previewVideoUri.asStateFlow()

    private val mediaObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            loadDeviceVideos()
        }
    }

    init {
        loadDeviceVideos()
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
            )
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
        } catch (_: Exception) {}
    }

    fun loadDeviceVideos() {
        viewModelScope.launch {
            val list = VideoProcessor.queryAllMediaVideos(getApplication())
            _deviceVideos.value = list
        }
    }

    fun selectTab(tab: NavigationTab) {
        _activeTab.value = tab
    }

    fun setPreviewVideoUri(uri: Uri?) {
        _previewVideoUri.value = uri
    }

    fun selectVideoUri(uri: Uri) {
        _timelineThumbnails.value = emptyList()
        val titleFromUri = uri.lastPathSegment?.substringAfterLast('/') ?: "Video"
        _selectedVideo.value = VideoItem(
            uri = uri,
            title = titleFromUri,
            durationMs = 0L,
            sizeBytes = 0L,
            width = 1280,
            height = 720
        )
        viewModelScope.launch {
            _statusMessage.value = "Loading video details..."
            val item = VideoProcessor.getVideoMetadata(getApplication(), uri)
            _selectedVideo.value = item

            if (item.sizeBytes > 0) {
                _targetSizeBytes.value = (item.sizeBytes * 0.50f).toLong()
            }

            if (item.durationMs > 0) {
                _singleStartMs.value = 0L
                _singleEndMs.value = item.durationMs

                _timelineThumbnails.value = VideoProcessor.extractTimelineThumbnails(
                    getApplication(), uri, item.durationMs, count = 10
                )
            }
            _statusMessage.value = "Video loaded: ${item.title}"
        }
    }

    fun setSingleCutRange(startMs: Long, endMs: Long) {
        val maxDuration = _selectedVideo.value?.durationMs ?: 180_000L
        _singleStartMs.value = startMs.coerceIn(0L, maxDuration - 1000L)
        _singleEndMs.value = endMs.coerceIn(_singleStartMs.value + 1000L, maxDuration)
    }

    fun setMultiPartMode(mode: MultiPartCutMode) {
        _multiPartMode.value = mode
    }

    fun addCutSegment(startMs: Long, endMs: Long) {
        val current = _multiPartSegments.value.toMutableList()
        current.add(CutSegment(startMs = startMs, endMs = endMs, isKeep = (_multiPartMode.value == MultiPartCutMode.KEEP_SELECTED)))
        val merged = VideoProcessor.mergeOverlappingSegments(current)
        if (merged.size < current.size) {
            viewModelScope.launch {
                _userMessage.emit("Overlapping cut ranges automatically merged!")
            }
        }
        _multiPartSegments.value = merged
    }

    fun removeCutSegment(id: String) {
        _multiPartSegments.value = _multiPartSegments.value.filter { it.id != id }
    }

    fun setCompressionPreset(preset: CompressionPreset) {
        _compressionPreset.value = preset
    }

    fun setCompressionMethod(method: CompressionMethod) {
        _compressionMethod.value = method
        _crfValue.value = method.defaultCrf
    }

    fun setCrfValue(crf: Int) {
        _crfValue.value = crf
    }

    fun setEncodingSpeed(speed: String) {
        _encodingSpeed.value = speed
    }

    fun setTargetSizeBytes(bytes: Long) {
        _targetSizeBytes.value = bytes
    }

    fun setCustomResolution(res: String) {
        _customResolution.value = res
    }

    private var currentProcessingJob: kotlinx.coroutines.Job? = null

    fun cancelCurrentProcessing() {
        currentProcessingJob?.cancel()
        currentProcessingJob = null
        try {
            com.arthenica.ffmpegkit.FFmpegKit.cancel()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error cancelling FFmpeg: ${e.message}")
        }
        _isProcessing.value = false
        _processingProgress.value = 0f
        stopBackgroundServiceIfNeeded()
        viewModelScope.launch {
            _userMessage.emit("Operation cancelled")
            _statusMessage.value = "Operation cancelled by user"
        }
    }

    // Single Cut Action (e.g. 1 min to 2 min out of 3 min video)
    fun processSingleCut() {
        val video = _selectedVideo.value ?: return
        if (_isProcessing.value) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Fast cutting video stream..."
            startBackgroundServiceIfNeeded("Cutting Video", "Fast cutting video stream...")
            val startTimeMs = System.currentTimeMillis()

            val startSec = _singleStartMs.value / 1000
            val endSec = _singleEndMs.value / 1000
            val outputFileName = "Cut_${startSec}s-${endSec}s_${System.currentTimeMillis()}_Cut.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val success = VideoProcessor.cutSingleRangeFast(
                context = getApplication(),
                sourceUri = video.uri,
                startMs = _singleStartMs.value,
                endMs = _singleEndMs.value,
                outputFile = outputFile,
                onProgress = { p ->
                    _processingProgress.value = p
                    updateBackgroundServiceProgress("Fast cutting...", p)
                }
            )

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()
            val elapsedSecStr = String.format(java.util.Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMs) / 1000.0)

            if (success && outputFile.exists()) {
                val cutDurationMs = _singleEndMs.value - _singleStartMs.value
                val outputSize = outputFile.length()
                val origSize = if (video.sizeBytes > 0) video.sizeBytes else (outputSize * video.durationMs / maxOf(1L, cutDurationMs))
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)

                dao.insertProcessedVideo(
                    ProcessedVideoEntity(
                        title = outputFileName,
                        filePath = outputFile.absolutePath,
                        operationType = "SINGLE_CUT",
                        originalSizeBytes = origSize,
                        outputSizeBytes = outputSize,
                        durationMs = cutDurationMs,
                        resolution = "${video.width}x${video.height}",
                        details = "Cut in ${elapsedSecStr}s (${VideoProcessor.formatDuration(_singleStartMs.value)} - ${VideoProcessor.formatDuration(_singleEndMs.value)})"
                    )
                )

                _userMessage.emit("Cut finished in ${elapsedSecStr}s! Saved to Download/VideoCut")
                _statusMessage.value = "Done in ${elapsedSecStr}s. Saved to Download/VideoCut/${outputFile.name}"
                _activeTab.value = NavigationTab.HISTORY
            } else {
                _userMessage.emit("Failed to cut video")
                _statusMessage.value = "Error during video processing"
            }
        }
    }

    // Multi-Part Cut Action (Remove/Keep selected parts)
    fun processMultiPartCut() {
        val video = _selectedVideo.value ?: return
        if (_isProcessing.value) return
        if (_multiPartSegments.value.isEmpty()) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Processing multi-part video cut..."
            startBackgroundServiceIfNeeded("Multi-Part Cut", "Processing multi-part video cut...")
            val startTimeMs = System.currentTimeMillis()

            val modeName = if (_multiPartMode.value == MultiPartCutMode.REMOVE_SELECTED) "RemovedParts" else "KeptParts"
            val outputFileName = "MultiCut_${modeName}_${System.currentTimeMillis()}_Cut.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val success = VideoProcessor.processSegmentsFast(
                context = getApplication(),
                sourceUri = video.uri,
                rawSegments = _multiPartSegments.value,
                mode = _multiPartMode.value,
                outputFile = outputFile,
                onProgress = { p ->
                    _processingProgress.value = p
                    updateBackgroundServiceProgress("Processing segments...", p)
                }
            )

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()
            val elapsedSecStr = String.format(java.util.Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMs) / 1000.0)

            if (success && outputFile.exists()) {
                val outputSize = outputFile.length()
                val origSize = if (video.sizeBytes > 0) video.sizeBytes else (outputSize + 1024 * 1024)
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)

                val detailsStr = if (_multiPartMode.value == MultiPartCutMode.REMOVE_SELECTED) {
                    "Cut in ${elapsedSecStr}s (Removed ${_multiPartSegments.value.size} segments)"
                } else {
                    "Cut in ${elapsedSecStr}s (Joined ${_multiPartSegments.value.size} segments)"
                }

                dao.insertProcessedVideo(
                    ProcessedVideoEntity(
                        title = outputFileName,
                        filePath = outputFile.absolutePath,
                        operationType = "MULTI_CUT",
                        originalSizeBytes = origSize,
                        outputSizeBytes = outputSize,
                        durationMs = video.durationMs,
                        resolution = "${video.width}x${video.height}",
                        details = detailsStr
                    )
                )

                _userMessage.emit("Multi-cut finished in ${elapsedSecStr}s! Saved to Download/VideoCut")
                _statusMessage.value = "Done in ${elapsedSecStr}s. Saved to Download/VideoCut/${outputFile.name}"
                _activeTab.value = NavigationTab.HISTORY
            } else {
                _userMessage.emit("Failed to process multi-part cut")
                _statusMessage.value = "Error during multi-part cut"
            }
        }
    }

    // Video Compression Action
    fun processCompression() {
        val video = _selectedVideo.value ?: return
        if (_isProcessing.value) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Compressing video with ${_compressionMethod.value.title}..."
            startBackgroundServiceIfNeeded("Compressing Video", "Compressing with ${_compressionMethod.value.title}...")
            val startTimeMs = System.currentTimeMillis()

            val outputFileName = "Compressed_${_compressionMethod.value.name}_${System.currentTimeMillis()}.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val success = VideoProcessor.compressVideoWithEngine(
                context = getApplication(),
                sourceUri = video.uri,
                outputFile = outputFile,
                method = _compressionMethod.value,
                crf = _crfValue.value,
                speedPreset = _encodingSpeed.value,
                targetSizeBytes = _targetSizeBytes.value,
                targetResolution = _customResolution.value,
                onProgress = { p ->
                    _processingProgress.value = p
                    updateBackgroundServiceProgress("Compressing video...", p)
                }
            )

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()
            val elapsedSecStr = String.format(java.util.Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMs) / 1000.0)

            if (success && outputFile.exists()) {
                val outputSize = outputFile.length()
                val origSize = if (video.sizeBytes > 0) video.sizeBytes else (outputSize + 1024 * 1024)
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)

                val savedPercent = if (origSize > 0) {
                    ((1.0 - (outputSize.toDouble() / origSize)) * 100).toInt().coerceAtLeast(0)
                } else 0

                dao.insertProcessedVideo(
                    ProcessedVideoEntity(
                        title = outputFileName,
                        filePath = outputFile.absolutePath,
                        operationType = "COMPRESS",
                        originalSizeBytes = origSize,
                        outputSizeBytes = outputSize,
                        durationMs = video.durationMs,
                        resolution = _customResolution.value,
                        details = "Compressed in ${elapsedSecStr}s (${savedPercent}% saved)"
                    )
                )

                _userMessage.emit("Compression finished in ${elapsedSecStr}s! Saved to Download/VideoCut")
                _statusMessage.value = "Done in ${elapsedSecStr}s. Saved to Download/VideoCut/${outputFile.name}"
                _activeTab.value = NavigationTab.HISTORY
            } else {
                _userMessage.emit("Failed to compress video")
                _statusMessage.value = "Compression failed"
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            _userMessage.emit("Item removed from history")
        }
    }

    // Join Video Actions
    fun addJoinVideoUris(uris: List<Uri>) {
        viewModelScope.launch {
            val newList = _joinVideosList.value.toMutableList()
            for (uri in uris) {
                if (newList.none { it.uri == uri }) {
                    val metadata = VideoProcessor.getVideoMetadata(getApplication(), uri)
                    newList.add(metadata)
                }
            }
            _joinVideosList.value = newList
            applyJoinSort(_joinSortOption.value)
        }
    }

    fun removeJoinVideo(index: Int) {
        if (index in _joinVideosList.value.indices) {
            val current = _joinVideosList.value.toMutableList()
            current.removeAt(index)
            _joinVideosList.value = current
        }
    }

    fun moveJoinVideo(fromIndex: Int, toIndex: Int) {
        val list = _joinVideosList.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _joinVideosList.value = list
            _joinSortOption.value = JoinSortOption.CUSTOM
        }
    }

    fun setJoinSortOption(option: JoinSortOption) {
        _joinSortOption.value = option
        applyJoinSort(option)
    }

    private fun applyJoinSort(option: JoinSortOption) {
        val list = _joinVideosList.value
        val sorted = when (option) {
            JoinSortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            JoinSortOption.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            JoinSortOption.DURATION -> list.sortedByDescending { it.durationMs }
            JoinSortOption.SIZE -> list.sortedByDescending { it.sizeBytes }
            JoinSortOption.CUSTOM -> list
        }
        _joinVideosList.value = sorted
    }

    fun processJoinVideos() {
        val videos = _joinVideosList.value
        if (videos.size < 2 || _isProcessing.value) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Joining ${videos.size} videos..."
            startBackgroundServiceIfNeeded("Joining Videos", "Joining ${videos.size} videos...")
            val startTimeMs = System.currentTimeMillis()

            val firstW = videos.first().width
            val firstH = videos.first().height
            val isFastMerge = videos.all { it.width == firstW && it.height == firstH }

            val outputFileName = "Joined_${videos.size}v_${System.currentTimeMillis()}_Cut.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val success = VideoProcessor.joinVideosFastOrReencode(
                context = getApplication(),
                videoItems = videos,
                isFastMerge = isFastMerge,
                outputFile = outputFile,
                onProgress = { p ->
                    _processingProgress.value = p
                    updateBackgroundServiceProgress("Joining videos...", p)
                }
            )

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()
            val elapsedSecStr = String.format(java.util.Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMs) / 1000.0)

            if (success && outputFile.exists()) {
                val outputSize = outputFile.length()
                val totalOrigSize = videos.sumOf { it.sizeBytes }
                val totalDuration = videos.sumOf { it.durationMs }
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)

                val outMeta = try { VideoProcessor.getVideoMetadata(getApplication(), Uri.fromFile(outputFile)) } catch (_: Exception) { null }
                val resolvedRes = if (outMeta != null && outMeta.width > 0) "${outMeta.width}x${outMeta.height}" else "${firstW}x${firstH}"

                dao.insertProcessedVideo(
                    ProcessedVideoEntity(
                        title = outputFileName,
                        filePath = outputFile.absolutePath,
                        operationType = "JOIN",
                        originalSizeBytes = totalOrigSize,
                        outputSizeBytes = outputSize,
                        durationMs = totalDuration,
                        resolution = resolvedRes,
                        details = "Joined ${videos.size} videos in ${elapsedSecStr}s (${if (isFastMerge) "Fast Merge" else "Re-encode"})"
                    )
                )

                _userMessage.emit("Joined ${videos.size} videos in ${elapsedSecStr}s!")
                _statusMessage.value = "Done in ${elapsedSecStr}s. Saved to Download/VideoCut/${outputFile.name}"
                _activeTab.value = NavigationTab.HISTORY
            } else {
                _userMessage.emit("Failed to join videos")
                _statusMessage.value = "Error during video join"
            }
        }
    }

    // Rotate Video Actions
    fun setRotationDegrees(degrees: Int) {
        _rotationDegrees.value = degrees
    }

    fun rotate90CW() {
        _rotationDegrees.value = (_rotationDegrees.value + 90) % 360
    }

    fun toggleFlipHorizontal() {
        _flipHorizontal.value = !_flipHorizontal.value
    }

    fun toggleFlipVertical() {
        _flipVertical.value = !_flipVertical.value
    }

    fun setTimelineRotateEnabled(enabled: Boolean) {
        _isTimelineRotateEnabled.value = enabled
        val maxDur = _selectedVideo.value?.durationMs ?: 60000L
        if (enabled && _rotateEndMs.value <= 0L) {
            _rotateStartMs.value = 0L
            _rotateEndMs.value = maxDur
        }
        if (enabled && _rotateParts.value.isEmpty()) {
            _rotateParts.value = listOf(
                RotatePart(
                    id = "part_1",
                    startMs = 0L,
                    endMs = maxDur,
                    rotationDegrees = 0,
                    flipHorizontal = false,
                    flipVertical = false
                )
            )
        }
    }

    fun setRotateTimelineRange(startMs: Long, endMs: Long) {
        val maxDur = _selectedVideo.value?.durationMs ?: 60000L
        _rotateStartMs.value = startMs.coerceIn(0L, maxDur - 500L)
        _rotateEndMs.value = endMs.coerceIn(_rotateStartMs.value + 500L, maxDur)
    }

    fun addRotatePart() {
        val maxDur = _selectedVideo.value?.durationMs ?: 60000L
        val existing = _rotateParts.value
        val lastEnd = existing.lastOrNull()?.endMs ?: 0L
        val start = lastEnd.coerceIn(0L, (maxDur - 1000L).coerceAtLeast(0L))
        val end = (start + 10000L).coerceAtMost(maxDur)
        val newPart = RotatePart(
            startMs = start,
            endMs = if (end > start) end else maxDur,
            rotationDegrees = 0,
            flipHorizontal = false,
            flipVertical = false
        )
        _rotateParts.value = existing + newPart
    }

    fun removeRotatePart(id: String) {
        _rotateParts.value = _rotateParts.value.filter { it.id != id }
    }

    fun updateRotatePart(part: RotatePart) {
        _rotateParts.value = _rotateParts.value.map { if (it.id == part.id) part else it }
    }

    fun processRotateVideo() {
        val video = _selectedVideo.value ?: return
        if (_isProcessing.value) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Rotating video..."
            startBackgroundServiceIfNeeded("Rotating Video", "Rotating video stream...")
            val startTimeMs = System.currentTimeMillis()

            val deg = _rotationDegrees.value
            val outputFileName = "Rotated_${deg}deg_${System.currentTimeMillis()}_Cut.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val parts = _rotateParts.value
            val success = if (_isTimelineRotateEnabled.value) {
                val effectiveParts = if (parts.isNotEmpty()) parts else listOf(
                    RotatePart(
                        id = "default_range_1",
                        startMs = _rotateStartMs.value,
                        endMs = _rotateEndMs.value,
                        rotationDegrees = deg,
                        flipHorizontal = _flipHorizontal.value,
                        flipVertical = _flipVertical.value
                    )
                )
                VideoProcessor.rotateMultiPartVideo(
                    context = getApplication(),
                    sourceUri = video.uri,
                    parts = effectiveParts,
                    totalDurationMs = video.durationMs,
                    outputFile = outputFile,
                    onProgress = { p ->
                        _processingProgress.value = p
                        updateBackgroundServiceProgress("Rotating video...", p)
                    }
                )
            } else {
                VideoProcessor.rotateVideo(
                    context = getApplication(),
                    sourceUri = video.uri,
                    rotationDegrees = deg,
                    flipHorizontal = _flipHorizontal.value,
                    flipVertical = _flipVertical.value,
                    isTimelineRange = false,
                    startMs = 0L,
                    endMs = video.durationMs,
                    totalDurationMs = video.durationMs,
                    outputFile = outputFile,
                    onProgress = { p ->
                        _processingProgress.value = p
                        updateBackgroundServiceProgress("Rotating video...", p)
                    }
                )
            }

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()
            val elapsedSecStr = String.format(java.util.Locale.US, "%.2f", (System.currentTimeMillis() - startTimeMs) / 1000.0)

            if (success && outputFile.exists()) {
                val outputSize = outputFile.length()
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)

                dao.insertProcessedVideo(
                    ProcessedVideoEntity(
                        title = outputFileName,
                        filePath = outputFile.absolutePath,
                        operationType = "ROTATE",
                        originalSizeBytes = video.sizeBytes,
                        outputSizeBytes = outputSize,
                        durationMs = video.durationMs,
                        resolution = "${video.width}x${video.height}",
                        details = "Rotated ${deg}° in ${elapsedSecStr}s"
                    )
                )

                _userMessage.emit("Rotation finished in ${elapsedSecStr}s!")
                _statusMessage.value = "Done in ${elapsedSecStr}s. Saved to Download/VideoCut/${outputFile.name}"
                _activeTab.value = NavigationTab.HISTORY
            } else {
                _userMessage.emit("Failed to rotate video")
                _statusMessage.value = "Error during video rotation"
            }
        }
    }

    fun processRotateVideoForItem(
        videoItem: VideoItem,
        rotationDegrees: Int,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        onSuccess: (VideoItem) -> Unit
    ) {
        if (_isProcessing.value) return

        currentProcessingJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Rotating video..."
            startBackgroundServiceIfNeeded("Rotating Video", "Rotating video...")
            val startTimeMs = System.currentTimeMillis()

            val outputFileName = "Rotated_${rotationDegrees}deg_${System.currentTimeMillis()}.mp4"
            val outputDir = VideoProcessor.getVideoCutOutputDir(getApplication())
            val outputFile = File(outputDir, outputFileName)

            val success = VideoProcessor.rotateVideo(
                context = getApplication(),
                sourceUri = videoItem.uri,
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                isTimelineRange = false,
                startMs = 0L,
                endMs = videoItem.durationMs,
                totalDurationMs = videoItem.durationMs,
                outputFile = outputFile,
                onProgress = { p ->
                    _processingProgress.value = p
                    updateBackgroundServiceProgress("Rotating video...", p)
                }
            )

            _isProcessing.value = false
            stopBackgroundServiceIfNeeded()

            if (success && outputFile.exists()) {
                VideoProcessor.notifyMediaScanner(getApplication(), outputFile)
                val updatedMetadata = VideoProcessor.getVideoMetadata(getApplication(), Uri.fromFile(outputFile))

                // Replace item in Join Videos List if present
                val list = _joinVideosList.value.toMutableList()
                val index = list.indexOfFirst { it.uri == videoItem.uri || (!it.path.isNullOrEmpty() && it.path == videoItem.path) }
                if (index != -1) {
                    list[index] = updatedMetadata
                    _joinVideosList.value = list
                }

                onSuccess(updatedMetadata)
                _userMessage.emit("Video rotated and saved! Updated in Join list.")
            } else {
                _userMessage.emit("Failed to rotate video")
            }
        }
    }

    // Media Selector Actions
    fun setMediaViewMode(mode: MediaViewMode) {
        _mediaViewMode.value = mode
    }

    fun setMediaSortType(sortType: MediaSortType) {
        _mediaSortType.value = sortType
    }

    fun deleteDeviceMediaVideo(video: VideoProcessor.DeviceMediaVideo) {
        viewModelScope.launch {
            val deleted = VideoProcessor.deleteVideoFile(getApplication(), video)
            if (deleted) {
                _userMessage.emit("Deleted ${video.title}")
                loadDeviceVideos()
            } else {
                _userMessage.emit("Failed to delete ${video.title}")
            }
        }
    }
}
