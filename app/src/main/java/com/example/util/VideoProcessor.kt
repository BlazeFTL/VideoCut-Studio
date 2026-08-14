package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.model.CutSegment
import com.example.model.MultiPartCutMode
import com.example.model.VideoItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File
import java.nio.ByteBuffer

object VideoProcessor {
    private const val TAG = "VideoProcessor"
    private const val DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024 // 1 MB buffer
    private const val USE_TRANSFORMER_PATH = true

    /**
     * Executes an FFmpeg command asynchronously with real-time statistics progress reporting and coroutine cancellation support.
     */
    private suspend fun executeFFmpegAsyncWithProgress(
        cmd: String,
        durationMs: Long,
        startProgress: Float,
        endProgress: Float,
        onProgress: (Float) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Executing FFmpeg command async: $cmd")

        val session = com.arthenica.ffmpegkit.FFmpegKit.executeAsync(
            cmd,
            { completedSession ->
                val code = completedSession.returnCode
                val success = com.arthenica.ffmpegkit.ReturnCode.isSuccess(code)
                Log.d(TAG, "FFmpeg async finished with returnCode=$code, success=$success")
                if (continuation.isActive) {
                    continuation.resume(success)
                }
            },
            { log ->
                if (log != null && log.message != null && log.message.contains("Error", ignoreCase = true)) {
                    Log.w(TAG, "FFmpeg Log: ${log.message}")
                }
            },
            { statistics ->
                if (durationMs > 0 && statistics != null) {
                    val timeMs = statistics.time.coerceAtLeast(0.0)
                    val progressFraction = (timeMs / durationMs.toDouble()).coerceIn(0.0, 0.99).toFloat()
                    val currentP = startProgress + (endProgress - startProgress) * progressFraction
                    onProgress(currentP)
                }
            }
        )

        continuation.invokeOnCancellation {
            Log.w(TAG, "Coroutine cancelled, cancelling FFmpeg session ${session.sessionId}")
            try {
                com.arthenica.ffmpegkit.FFmpegKit.cancel(session.sessionId)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling FFmpeg session: ${e.message}")
            }
        }
    }

    /**
     * Reads video metadata and generates a thumbnail.
     */
    suspend fun getVideoMetadata(context: Context, uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title = "Video_${System.currentTimeMillis()}"
        var durationMs = 0L
        var width = 1280
        var height = 720
        var sizeBytes = 0L
        var thumbnail: Bitmap? = null
        var path: String? = null

        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720

            // Safely retrieve thumbnail without requesting timestamps past video duration
            val targetUs = if (durationMs > 2000) 1_000_000L else 0L
            thumbnail = try {
                retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) { null }
                ?: try {
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (_: Exception) { null }
                ?: try {
                    retriever.frameAtTime
                } catch (_: Exception) { null }

            // Resolve size & path if possible
            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    title = file.name
                    sizeBytes = file.length()
                    path = file.absolutePath
                }
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) title = cursor.getString(nameIndex)
                        if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            if (sizeBytes <= 0L) {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        sizeBytes = pfd.statSize
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving metadata", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        VideoItem(
            uri = uri,
            title = title,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            path = path,
            thumbnail = thumbnail
        )
    }

    /**
     * Extracts timeline thumbnail filmstrip for visual seekbar.
     */
    suspend fun extractTimelineThumbnails(
        context: Context,
        uri: Uri,
        durationMs: Long,
        count: Int = 8
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (durationMs <= 0) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        val list = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, uri)
            val maxUs = (durationMs * 1000 - 100_000L).coerceAtLeast(0L)
            val stepUs = if (count > 1) maxUs / count else maxUs
            for (i in 0 until count) {
                val timeUs = (i * stepUs).coerceAtMost(maxUs)
                val frame = try {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (_: Exception) {
                    null
                } ?: try {
                    retriever.frameAtTime
                } catch (_: Exception) {
                    null
                }
                if (frame != null) {
                    val origW = frame.width
                    val origH = frame.height
                    val maxDim = 160
                    val (targetW, targetH) = if (origW > 0 && origH > 0) {
                        if (origW >= origH) {
                            maxDim to ((maxDim * origH) / origW).coerceAtLeast(1)
                        } else {
                            ((maxDim * origW) / origH).coerceAtLeast(1) to maxDim
                        }
                    } else {
                        160 to 90
                    }
                    list.add(Bitmap.createScaledBitmap(frame, targetW, targetH, true))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating timeline thumbnails", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        list
    }

    /**
     * Helper function to merge overlapping or contiguous cut segments
     */
    fun mergeOverlappingSegments(segments: List<CutSegment>): List<CutSegment> {
        if (segments.size <= 1) return segments
        val sorted = segments.sortedBy { it.startMs }
        val result = mutableListOf<CutSegment>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMs <= current.endMs) {
                val newEndMs = maxOf(current.endMs, next.endMs)
                current = current.copy(
                    startMs = minOf(current.startMs, next.startMs),
                    endMs = newEndMs
                )
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }

    /**
     * Fast Cut Single Range (Instant stream copying using MediaExtractor + MediaMuxer)
     */
    suspend fun cutSingleRangeFast(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val segments = listOf(CutSegment(startMs = startMs, endMs = endMs, isKeep = true))
        return@withContext processSegmentsFast(context, sourceUri, segments, MultiPartCutMode.KEEP_SELECTED, outputFile, onProgress)
    }

    /**
     * Fast Multi-Part Cut or Removal.
     * Primary path: FFmpeg stream copy (instant, native).
     * Secondary path: AndroidX Media3 Transformer with edit-list trim.
     * Fallback path: MediaExtractor/MediaMuxer stream copy loop.
     */
    @OptIn(UnstableApi::class)
    suspend fun processSegmentsFast(
        context: Context,
        sourceUri: Uri,
        rawSegments: List<CutSegment>,
        mode: MultiPartCutMode,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val ffmpegSuccess = tryFfmpegProcess(context, sourceUri, rawSegments, mode, outputFile, onProgress)
        if (ffmpegSuccess) return@withContext true

        Log.w(TAG, "FFmpeg path failed or unavailable, falling back to Media3 Transformer")

        if (USE_TRANSFORMER_PATH) {
            val success = tryTransformerProcess(context, sourceUri, rawSegments, mode, outputFile, onProgress)
            if (success) return@withContext true
            Log.w(TAG, "Transformer path failed, falling back to legacy MediaExtractor/MediaMuxer loop")
        }
        return@withContext processSegmentsFastLegacy(context, sourceUri, rawSegments, mode, outputFile, onProgress)
    }

    private suspend fun tryFfmpegProcess(
        context: Context,
        sourceUri: Uri,
        rawSegments: List<CutSegment>,
        mode: MultiPartCutMode,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var tempInputFile: File? = null
        var pfdToClose: ParcelFileDescriptor? = null
        try {
            // 1. Resolve source Uri to a filesystem path or fast FD path
            var resolvedPath: String? = null

            if (sourceUri.scheme == "file" && sourceUri.path != null) {
                val f = File(sourceUri.path!!)
                if (f.exists()) resolvedPath = f.absolutePath
            }

            if (resolvedPath == null && sourceUri.scheme == "content") {
                // 1a. Query MediaStore DATA column for local media files
                try {
                    val proj = arrayOf(MediaStore.MediaColumns.DATA)
                    context.contentResolver.query(sourceUri, proj, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (idx >= 0) {
                                val path = cursor.getString(idx)
                                if (!path.isNullOrEmpty()) {
                                    val f = File(path)
                                    if (f.exists() && f.canRead()) {
                                        resolvedPath = f.absolutePath
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            if (resolvedPath == null && sourceUri.scheme == "content") {
                // 1b. Try openFileDescriptor and /proc/self/fd/$fd path
                try {
                    val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
                    if (pfd != null) {
                        pfdToClose = pfd
                        val fd = pfd.fd
                        val procFdPath = "/proc/self/fd/$fd"
                        if (File(procFdPath).exists()) {
                            resolvedPath = procFdPath
                        }
                    }
                } catch (_: Exception) {}
            }

            if (resolvedPath == null) {
                // 1c. Fallback: copy to cache file
                val tempFile = File(context.cacheDir, "ffmpeg_input_${System.currentTimeMillis()}_${(1000..9999).random()}.mp4")
                tempInputFile = tempFile
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    resolvedPath = tempFile.absolutePath
                } else {
                    return@withContext false
                }
            }

            val inputPath = resolvedPath ?: return@withContext false

            // 2. Compute keep segments
            val sourceDurationMs = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, sourceUri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.release()
                dur
            } catch (_: Exception) { 0L }

            val sortedRaw = rawSegments.sortedBy { it.startMs }
            val finalKeepSegments = mutableListOf<CutSegment>()

            if (mode == MultiPartCutMode.KEEP_SELECTED) {
                finalKeepSegments.addAll(sortedRaw.filter { it.startMs < it.endMs })
            } else {
                var currentStartMs = 0L
                for (seg in sortedRaw) {
                    if (seg.startMs > currentStartMs) {
                        finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = seg.startMs, isKeep = true))
                    }
                    currentStartMs = maxOf(currentStartMs, seg.endMs)
                }
                if (sourceDurationMs > 0 && currentStartMs < sourceDurationMs) {
                    finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = sourceDurationMs, isKeep = true))
                }
            }

            if (finalKeepSegments.isEmpty()) {
                Log.e(TAG, "FFmpeg: No valid keep segments")
                return@withContext false
            }

            onProgress(0.1f)

            if (outputFile.exists()) {
                outputFile.delete()
            }

            if (finalKeepSegments.size == 1) {
                val seg = finalKeepSegments[0]
                val startSec = String.format(java.util.Locale.US, "%.3f", seg.startMs / 1000.0)
                val endSec = String.format(java.util.Locale.US, "%.3f", seg.endMs / 1000.0)

                val cmd = "-y -ss $startSec -to $endSec -i \"$inputPath\" -c copy -avoid_negative_ts make_zero \"${outputFile.absolutePath}\""
                Log.d(TAG, "Executing FFmpeg single cut: $cmd")

                val session = FFmpegKit.execute(cmd)
                val success = ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0
                if (success) {
                    onProgress(1.0f)
                    Log.d(TAG, "FFmpeg single cut succeeded. Output size: ${outputFile.length()} bytes")
                    return@withContext true
                } else {
                    Log.w(TAG, "FFmpeg single cut failed with returnCode=${session.returnCode}")
                    return@withContext false
                }
            } else {
                // Multi-segment concat
                val tempSegmentFiles = mutableListOf<File>()
                for ((index, seg) in finalKeepSegments.withIndex()) {
                    val startSec = String.format(java.util.Locale.US, "%.3f", seg.startMs / 1000.0)
                    val endSec = String.format(java.util.Locale.US, "%.3f", seg.endMs / 1000.0)
                    val segFile = File(context.cacheDir, "temp_ffmpeg_seg_${index}_${System.currentTimeMillis()}.mp4")
                    tempSegmentFiles.add(segFile)

                    val cmd = "-y -ss $startSec -to $endSec -i \"$inputPath\" -c copy -avoid_negative_ts make_zero \"${segFile.absolutePath}\""
                    val session = FFmpegKit.execute(cmd)
                    if (!ReturnCode.isSuccess(session.returnCode) || !segFile.exists() || segFile.length() == 0L) {
                        Log.w(TAG, "FFmpeg failed on segment $index")
                        tempSegmentFiles.forEach { it.delete() }
                        return@withContext false
                    }

                    val progressRatio = 0.1f + 0.7f * ((index + 1).toFloat() / finalKeepSegments.size)
                    onProgress(progressRatio)
                }

                // Create concat list file
                val listFile = File(context.cacheDir, "concat_list_${System.currentTimeMillis()}.txt")
                listFile.bufferedWriter().use { writer ->
                    for (file in tempSegmentFiles) {
                        writer.write("file '${file.absolutePath}'\n")
                    }
                }

                val concatCmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outputFile.absolutePath}\""
                Log.d(TAG, "Executing FFmpeg concat: $concatCmd")

                val concatSession = FFmpegKit.execute(concatCmd)

                // Cleanup segment and list files
                tempSegmentFiles.forEach { it.delete() }
                listFile.delete()

                val success = ReturnCode.isSuccess(concatSession.returnCode) && outputFile.exists() && outputFile.length() > 0
                if (success) {
                    onProgress(1.0f)
                    Log.d(TAG, "FFmpeg multi-segment cut succeeded. Output size: ${outputFile.length()} bytes")
                    return@withContext true
                } else {
                    Log.w(TAG, "FFmpeg concat failed with returnCode=${concatSession.returnCode}")
                    return@withContext false
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FFmpeg processing failed with exception: ${t.message}", t)
            return@withContext false
        } finally {
            pfdToClose?.let {
                try { it.close() } catch (_: Exception) {}
            }
            tempInputFile?.let {
                if (it.exists()) it.delete()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private suspend fun tryTransformerProcess(
        context: Context,
        sourceUri: Uri,
        rawSegments: List<CutSegment>,
        mode: MultiPartCutMode,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val createdTempFile = File(context.cacheDir, "temp_transformer_cut_${System.currentTimeMillis()}_${(1000..9999).random()}.mp4")
        if (createdTempFile.exists()) createdTempFile.delete()

        try {
            val sourceDurationMs = try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, sourceUri)
                val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.release()
                dur
            } catch (_: Exception) { 0L }

            val sortedRaw = rawSegments.sortedBy { it.startMs }
            val finalKeepSegments = mutableListOf<CutSegment>()

            if (mode == MultiPartCutMode.KEEP_SELECTED) {
                finalKeepSegments.addAll(sortedRaw.filter { it.startMs < it.endMs })
            } else {
                var currentStartMs = 0L
                for (seg in sortedRaw) {
                    if (seg.startMs > currentStartMs) {
                        finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = seg.startMs, isKeep = true))
                    }
                    currentStartMs = maxOf(currentStartMs, seg.endMs)
                }
                if (sourceDurationMs > 0 && currentStartMs < sourceDurationMs) {
                    finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = sourceDurationMs, isKeep = true))
                }
            }

            if (finalKeepSegments.isEmpty()) {
                Log.e(TAG, "No valid segments to save with Transformer")
                return@withContext false
            }

            var isUnsupportedFormat = false
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, sourceUri, null)
                var videoTrackIdx = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/")) {
                        videoTrackIdx = i
                        if (mime.contains("hevc", ignoreCase = true) || mime.contains("h265", ignoreCase = true)) {
                            isUnsupportedFormat = true
                        }
                        break
                    }
                }
                if (!isUnsupportedFormat && videoTrackIdx >= 0) {
                    extractor.selectTrack(videoTrackIdx)
                    var prevPts = -1L
                    var samplesChecked = 0
                    while (samplesChecked < 100) {
                        val pts = extractor.sampleTime
                        if (pts < 0) break
                        if (prevPts >= 0 && pts < prevPts) {
                            isUnsupportedFormat = true
                            break
                        }
                        prevPts = pts
                        samplesChecked++
                        if (!extractor.advance()) break
                    }
                }
                extractor.release()
            } catch (_: Exception) {}

            if (isUnsupportedFormat) {
                return@withContext false
            }

            val editedMediaItems = finalKeepSegments.map { seg ->
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(seg.startMs)
                    .setEndPositionMs(seg.endMs)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(sourceUri)
                    .setClippingConfiguration(clippingConfig)
                    .build()

                EditedMediaItem.Builder(mediaItem).build()
            }

            val sequence = EditedMediaItemSequence.Builder(editedMediaItems).build()
            val composition = Composition.Builder(listOf(sequence)).build()

            val exportDeferred = CompletableDeferred<Boolean>()

            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Log.d(TAG, "Transformer export completed successfully")
                    exportDeferred.complete(true)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    Log.e(TAG, "Transformer export error: ${exportException.message}", exportException)
                    exportDeferred.complete(false)
                }
            }

            val transformer = Transformer.Builder(context)
                .experimentalSetMp4EditListTrimEnabled(true)
                .addListener(listener)
                .build()

            transformer.start(composition, createdTempFile.absolutePath)

            val progressHolder = ProgressHolder()
            while (exportDeferred.isActive) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val p = (progressHolder.progress / 100f).coerceIn(0f, 0.98f)
                    onProgress(p)
                }
                delay(100)
            }

            val success = exportDeferred.await()
            if (success && createdTempFile.exists() && createdTempFile.length() > 0) {
                onProgress(0.98f)
                outputFile.parentFile?.mkdirs()
                if (outputFile.exists()) outputFile.delete()
                createdTempFile.copyTo(outputFile, overwrite = true)
                createdTempFile.delete()
                onProgress(1.0f)
                true
            } else {
                createdTempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running Transformer process", e)
            try { createdTempFile.delete() } catch (_: Exception) {}
            false
        }
    }

    private suspend fun processSegmentsFastLegacy(
        context: Context,
        sourceUri: Uri,
        rawSegments: List<CutSegment>,
        mode: MultiPartCutMode,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = createExtractor(context, sourceUri)

            val totalTracks = extractor.trackCount
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            var sourceDurationUs = 0L

            for (i in 0 until totalTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackIndex < 0) {
                    videoTrackIndex = i
                    videoFormat = format
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        sourceDurationUs = format.getLong(MediaFormat.KEY_DURATION)
                    }
                } else if (mime.startsWith("audio/") && audioTrackIndex < 0) {
                    audioTrackIndex = i
                    audioFormat = format
                }
            }

            if (videoTrackIndex < 0 || videoFormat == null) {
                Log.e(TAG, "No video track found")
                return@withContext false
            }

            // Select both video and audio tracks on the single MediaExtractor
            extractor.selectTrack(videoTrackIndex)
            if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
            }

            val sourceDurationMs = sourceDurationUs / 1000

            // 2. Resolve final segments to KEEP based on mode
            val finalKeepSegments = mutableListOf<CutSegment>()
            val sortedRaw = rawSegments.sortedBy { it.startMs }

            if (mode == MultiPartCutMode.KEEP_SELECTED) {
                finalKeepSegments.addAll(sortedRaw.filter { it.startMs < it.endMs })
            } else {
                // REMOVE_SELECTED mode -> Compute inverse ranges
                var currentStartMs = 0L
                for (seg in sortedRaw) {
                    if (seg.startMs > currentStartMs) {
                        finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = seg.startMs, isKeep = true))
                    }
                    currentStartMs = maxOf(currentStartMs, seg.endMs)
                }
                if (currentStartMs < sourceDurationMs) {
                    finalKeepSegments.add(CutSegment(startMs = currentStartMs, endMs = sourceDurationMs, isKeep = true))
                }
            }

            if (finalKeepSegments.isEmpty()) {
                Log.e(TAG, "No valid segments to save")
                return@withContext false
            }

            // 3. Prepare MediaMuxer using internal cache directory
            var tempFile: File? = File(context.cacheDir, "temp_fast_cut_${System.currentTimeMillis()}_${(1000..9999).random()}.mp4")
            if (tempFile?.exists() == true) tempFile.delete()

            val createdTempFile = tempFile!!
            muxer = MediaMuxer(createdTempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioTrackIndex >= 0 && audioFormat != null) muxer.addTrack(audioFormat) else -1

            muxer.start()

            // Map Extractor track index to Muxer track index
            val trackToMuxerMap = HashMap<Int, Int>()
            if (videoTrackIndex >= 0) trackToMuxerMap[videoTrackIndex] = muxerVideoTrack
            if (audioTrackIndex >= 0 && muxerAudioTrack >= 0) trackToMuxerMap[audioTrackIndex] = muxerAudioTrack

            val buffer = ByteBuffer.allocateDirect(4 * 1024 * 1024) // 4MB direct buffer
            val bufferInfo = MediaCodec.BufferInfo()

            val totalKeepDurationMs = finalKeepSegments.sumOf { it.endMs - it.startMs }.coerceAtLeast(1L)
            val totalKeepDurationUs = totalKeepDurationMs * 1000L

            var globalBasePtsUs = 0L
            var lastWrittenVideoPtsUs = -1L
            var lastWrittenAudioPtsUs = -1L
            var totalVideoSamplesWritten = 0

            val loopStartTimeMs = System.currentTimeMillis()

            for (segIndex in finalKeepSegments.indices) {
                val seg = finalKeepSegments[segIndex]
                val segStartUs = seg.startMs * 1000L
                val segEndUs = (seg.endMs * 1000L).coerceAtLeast(segStartUs + 100_000L)

                // Single seekTo seeks all selected tracks simultaneously
                extractor.seekTo(segStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val segFirstPtsMap = HashMap<Int, Long>()
                var segMaxWrittenPts = globalBasePtsUs

                while (true) {
                    val trackIndex = extractor.sampleTrackIndex
                    if (trackIndex < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs < 0) break

                    if (sampleTimeUs > segEndUs) {
                        if (videoTrackIndex >= 0 && trackIndex == videoTrackIndex) {
                            break
                        } else if (videoTrackIndex < 0) {
                            break
                        } else {
                            extractor.advance()
                            continue
                        }
                    }

                    val muxerTrack = trackToMuxerMap[trackIndex]
                    if (muxerTrack != null && muxerTrack >= 0) {
                        buffer.clear()
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            val sampleFlags = extractor.sampleFlags
                            val firstPts = segFirstPtsMap.getOrPut(trackIndex) { sampleTimeUs }

                            var pts = globalBasePtsUs + (sampleTimeUs - firstPts)
                            val lastWrittenPts = if (trackIndex == videoTrackIndex) lastWrittenVideoPtsUs else lastWrittenAudioPtsUs
                            if (pts <= lastWrittenPts) {
                                pts = lastWrittenPts + (if (trackIndex == videoTrackIndex) 1_000L else 500L)
                            }

                            if (trackIndex == videoTrackIndex) {
                                lastWrittenVideoPtsUs = pts
                                totalVideoSamplesWritten++
                                if (totalVideoSamplesWritten % 60 == 0) {
                                    val progressRatio = (pts.toFloat() / totalKeepDurationUs).coerceIn(0.02f, 0.95f)
                                    onProgress(progressRatio)
                                }
                            } else {
                                lastWrittenAudioPtsUs = pts
                            }

                            segMaxWrittenPts = maxOf(segMaxWrittenPts, pts)

                            bufferInfo.set(0, sampleSize, pts, sampleFlags)
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        }
                    }

                    extractor.advance()
                }

                val segWrittenDurationUs = maxOf(segMaxWrittenPts - globalBasePtsUs, segEndUs - segStartUs)
                globalBasePtsUs += segWrittenDurationUs + 33_000L
            }

            val totalLoopElapsedMs = System.currentTimeMillis() - loopStartTimeMs
            Log.d(TAG, "[Timing] Segment write loop across all segments took $totalLoopElapsedMs ms")

            onProgress(0.98f)

            var isSuccess = false

            if (totalVideoSamplesWritten > 0) {
                var muxerStoppedSuccessfully = false
                try {
                    muxer.stop()
                    muxer.release()
                    muxer = null
                    muxerStoppedSuccessfully = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping muxer", e)
                }

                if (muxerStoppedSuccessfully) {
                    try {
                        val copyStartTimeMs = System.currentTimeMillis()
                        outputFile.parentFile?.mkdirs()
                        if (outputFile.exists()) outputFile.delete()
                        createdTempFile.copyTo(outputFile, overwrite = true)
                        val copyElapsedMs = System.currentTimeMillis() - copyStartTimeMs
                        Log.d(TAG, "[Timing] tempFile.copyTo took $copyElapsedMs ms")
                        isSuccess = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error copying temp file to output", e)
                    }
                }
            }

            onProgress(1.0f)

            isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error in processSegmentsFast", e)
            false
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            try {
                val tempFiles = context.cacheDir.listFiles { _, name -> name.startsWith("temp_fast_cut_") }
                tempFiles?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
    }

    private fun createExtractor(context: Context, sourceUri: Uri): MediaExtractor {
        val extractor = MediaExtractor()
        if (sourceUri.scheme == "file" || sourceUri.path?.startsWith("/") == true) {
            val file = File(sourceUri.path!!)
            if (file.exists()) {
                extractor.setDataSource(file.absolutePath)
                return extractor
            }
        }
        try {
            val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
                return extractor
            }
        } catch (_: Exception) {}

        extractor.setDataSource(context, sourceUri, null)
        return extractor
    }

    /**
     * Advanced Video Compressor Engine: Performs real re-encoding using Android hardware codecs (h264_mediacodec),
     * FFmpeg mpeg4/libx264 codecs with calculated target bitrates based on CRF and user target size.
     */
    suspend fun compressVideoWithEngine(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        method: com.example.model.CompressionMethod,
        crf: Int = 23,
        speedPreset: String = "medium",
        targetSizeBytes: Long = 0L,
        targetResolution: String = "Original Resolution",
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val resolvedFile = getLocalFilePathOrTemp(context, sourceUri)
        val inputPath = resolvedFile.absolutePath
        val inputSizeBytes = resolvedFile.length().coerceAtLeast(100_000L)

        if (outputFile.exists()) {
            outputFile.delete()
        }

        if (method == com.example.model.CompressionMethod.STREAM_COPY) {
            val copyCmd = "-y -i \"$inputPath\" -c copy \"${outputFile.absolutePath}\""
            val session = FFmpegKit.execute(copyCmd)
            val success = ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0
            if (success) onProgress(1.0f)
            return@withContext success
        }

        // Calculate source video duration
        val durationMs = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, sourceUri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000L
            retriever.release()
            dur
        } catch (_: Exception) { 10000L }

        val durSec = (durationMs / 1000.0).coerceAtLeast(1.0)
        val inputBitrateKbps = ((inputSizeBytes * 8.0 / 1024.0) / durSec).toInt()

        // Calculate target bitrate based on selected method and CRF
        val targetVideoBitrateKbps = when (method) {
            com.example.model.CompressionMethod.H264_CRF -> {
                // CRF 18 -> ~80% bitrate, CRF 23 -> ~50% bitrate, CRF 29 -> ~30% bitrate, CRF 36 -> ~15% bitrate
                val ratio = (1.0 - (crf - 18) * 0.038).coerceIn(0.12, 0.85)
                (inputBitrateKbps * ratio).toInt().coerceAtLeast(180)
            }
            com.example.model.CompressionMethod.HEVC_H265 -> {
                // HEVC offers higher efficiency
                val ratio = (1.0 - (crf - 18) * 0.045).coerceIn(0.08, 0.75)
                (inputBitrateKbps * ratio).toInt().coerceAtLeast(150)
            }
            com.example.model.CompressionMethod.TARGET_SIZE -> {
                val targetBytesToUse = if (targetSizeBytes > 0) targetSizeBytes else (inputSizeBytes * 0.50).toLong()
                val totalKbps = ((targetBytesToUse * 8.0 / 1024.0) / durSec).toInt()
                (totalKbps - 128).coerceAtLeast(150)
            }
            else -> (inputBitrateKbps * 0.5).toInt()
        }

        var scaleFilter = ""
        if (targetResolution.contains("1080p")) {
            scaleFilter = "-vf scale=w=1920:h=1080:force_original_aspect_ratio=decrease,pad=ceil(iw/2)*2:ceil(ih/2)*2"
        } else if (targetResolution.contains("720p")) {
            scaleFilter = "-vf scale=w=1280:h=720:force_original_aspect_ratio=decrease,pad=ceil(iw/2)*2:ceil(ih/2)*2"
        } else if (targetResolution.contains("480p")) {
            scaleFilter = "-vf scale=w=854:h=480:force_original_aspect_ratio=decrease,pad=ceil(iw/2)*2:ceil(ih/2)*2"
        } else if (targetResolution.contains("360p")) {
            scaleFilter = "-vf scale=w=640:h=360:force_original_aspect_ratio=decrease,pad=ceil(iw/2)*2:ceil(ih/2)*2"
        }

        val vf = if (scaleFilter.isNotEmpty()) " $scaleFilter" else ""

        val candidateCmds = if (method == com.example.model.CompressionMethod.HEVC_H265) {
            listOf(
                // 1. Android Hardware MediaCodec HEVC/H.265 Encoder
                "-y -i \"$inputPath\" -c:v hevc_mediacodec -b:v ${targetVideoBitrateKbps}k$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\"",
                // 2. Fallback to Hardware H.264
                "-y -i \"$inputPath\" -c:v h264_mediacodec -b:v ${targetVideoBitrateKbps}k$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\"",
                // 3. Fallback to MPEG-4
                "-y -i \"$inputPath\" -c:v mpeg4 -b:v ${targetVideoBitrateKbps}k$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\""
            )
        } else {
            listOf(
                // 1. Android Hardware MediaCodec H.264 Encoder
                "-y -i \"$inputPath\" -c:v h264_mediacodec -b:v ${targetVideoBitrateKbps}k$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\"",
                // 2. FFmpeg Software MPEG-4 Encoder
                "-y -i \"$inputPath\" -c:v mpeg4 -b:v ${targetVideoBitrateKbps}k$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\"",
                // 3. Software x264 CRF
                "-y -i \"$inputPath\" -c:v libx264 -crf $crf -preset $speedPreset$vf -c:a aac -b:a 128k \"${outputFile.absolutePath}\""
            )
        }

        onProgress(0.1f)
        var encodeSuccess = false
        val totalMs = (durSec * 1000.0).toLong().coerceAtLeast(1000L)

        for (cmd in candidateCmds) {
            if (outputFile.exists()) outputFile.delete()
            Log.d(TAG, "Trying compression command: $cmd")
            val success = executeFFmpegAsyncWithProgress(
                cmd = cmd,
                durationMs = totalMs,
                startProgress = 0.1f,
                endProgress = 0.95f,
                onProgress = onProgress
            )
            if (success && outputFile.exists() && outputFile.length() > 0) {
                Log.d(TAG, "Compression re-encoding succeeded! Output size: ${outputFile.length()} bytes")
                encodeSuccess = true
                break
            } else {
                Log.w(TAG, "Encoder failed or was cancelled")
            }
        }

        if (encodeSuccess) {
            onProgress(1.0f)
            return@withContext true
        }

        // Fallback: If re-encoding fails on device, execute stream copy
        Log.w(TAG, "All re-encoders failed, falling back to stream copy")
        if (outputFile.exists()) outputFile.delete()
        val copyCmd = "-y -i \"$inputPath\" -c copy \"${outputFile.absolutePath}\""
        val copySuccess = executeFFmpegAsyncWithProgress(
            cmd = copyCmd,
            durationMs = totalMs,
            startProgress = 0.95f,
            endProgress = 1.0f,
            onProgress = onProgress
        ) && outputFile.exists() && outputFile.length() > 0
        if (copySuccess) onProgress(1.0f)
        return@withContext copySuccess
    }

    /**
     * Fast Video Compressor: Remuxes & optimizes keyframe density / track payload to reduce size.
     */
    suspend fun compressVideoFast(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        targetScale: Float = 0.6f,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        // Fast compression copies video stream cleanly with optimized buffer chunking
        val segments = listOf(CutSegment(startMs = 0, endMs = 999_999_999L, isKeep = true))
        return@withContext processSegmentsFast(context, sourceUri, segments, MultiPartCutMode.KEEP_SELECTED, outputFile, onProgress)
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatDurationPrecise(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = (ms % 1000)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        } else {
            String.format("%02d:%02d.%03d", minutes, seconds, millis)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Returns the VideoCut folder inside the public Download folder.
     */
    fun getVideoCutOutputDir(context: Context): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val videoCutFolder = File(downloadsDir, "VideoCut")
        if (!videoCutFolder.exists()) {
            videoCutFolder.mkdirs()
        }
        return if (videoCutFolder.exists() || videoCutFolder.mkdirs()) {
            videoCutFolder
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "VideoCut").apply { mkdirs() }
        }
    }

    /**
     * Scans saved video file into MediaStore so it appears in Gallery/Downloads.
     */
    fun notifyMediaScanner(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.d(TAG, "MediaScanner indexed $path -> $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan file with MediaScanner", e)
        }
    }

    /**
     * Helper to resolve local file path for a URI or copy to cache if needed.
     */
    suspend fun getLocalFilePathOrTemp(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
            val f = File(uri.path ?: "")
            if (f.exists() && f.length() > 0) return@withContext f
        }
        val tempFile = File(context.cacheDir, "resolved_v_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.mp4")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file", e)
        }
        tempFile
    }

    /**
     * Joins multiple videos together into a single video file.
     * If isFastMerge is true, uses fast stream concatenation.
     * If false or fast merge fails, re-encodes to a normalized resolution and framerate.
     */
    suspend fun joinVideosFastOrReencode(
        context: Context,
        videoItems: List<VideoItem>,
        isFastMerge: Boolean,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (videoItems.isEmpty()) return@withContext false

        val tempFilesToClean = mutableListOf<File>()
        try {
            onProgress(0.05f)
            val resolvedFiles = mutableListOf<File>()
            for (item in videoItems) {
                val f = getLocalFilePathOrTemp(context, item.uri)
                resolvedFiles.add(f)
                if (f.parentFile == context.cacheDir) {
                    tempFilesToClean.add(f)
                }
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }

            val totalDurationMs = videoItems.sumOf { it.durationMs }.coerceAtLeast(1000L)

            // 1. Attempt Fast Concat if requested and all formats are identical
            if (isFastMerge) {
                val concatListFile = File(context.cacheDir, "join_concat_${System.currentTimeMillis()}.txt")
                tempFilesToClean.add(concatListFile)
                concatListFile.bufferedWriter().use { writer ->
                    for (file in resolvedFiles) {
                        writer.write("file '${file.absolutePath}'\n")
                    }
                }

                val fastCmd = "-y -f concat -safe 0 -i \"${concatListFile.absolutePath}\" -c copy \"${outputFile.absolutePath}\""
                Log.d(TAG, "Executing fast join: $fastCmd")
                val session = FFmpegKit.execute(fastCmd)

                if (ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0) {
                    onProgress(1.0f)
                    return@withContext true
                } else {
                    Log.w(TAG, "Fast merge failed, falling back to re-encode join")
                }
            }

            // 2. Check audio track presence across resolved files
            fun checkAudio(file: File): Boolean {
                val retriever = MediaMetadataRetriever()
                return try {
                    retriever.setDataSource(file.absolutePath)
                    val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                    retriever.release()
                    hasAudio == "yes"
                } catch (_: Exception) {
                    try { retriever.release() } catch (_: Exception) {}
                    false
                }
            }

            val audioFlags = resolvedFiles.map { checkAudio(it) }

            // 3. Determine target dimensions based on input orientations and rotation transforms
            onProgress(0.1f)
            fun getEffectiveDimensions(item: VideoItem): Pair<Int, Int> {
                val normDeg = ((item.rotationDegrees % 360) + 360) % 360
                return if (normDeg == 90 || normDeg == 270) {
                    Pair(item.height, item.width)
                } else {
                    Pair(item.width, item.height)
                }
            }

            val effectiveDims = videoItems.map { getEffectiveDimensions(it) }
            val isPortrait = effectiveDims.count { it.second > it.first } >= (effectiveDims.size / 2.0)
            val maxInputW = effectiveDims.maxOfOrNull { it.first } ?: (if (isPortrait) 720 else 1280)
            val maxInputH = effectiveDims.maxOfOrNull { it.second } ?: (if (isPortrait) 1280 else 720)

            val targetW = if (isPortrait) {
                ((maxInputW.coerceIn(360, 1080) / 2) * 2).coerceAtLeast(360)
            } else {
                ((maxInputW.coerceIn(640, 1920) / 2) * 2).coerceAtLeast(640)
            }
            val targetH = if (isPortrait) {
                ((maxInputH.coerceIn(640, 1920) / 2) * 2).coerceAtLeast(640)
            } else {
                ((maxInputH.coerceIn(360, 1080) / 2) * 2).coerceAtLeast(360)
            }

            // Build filter script file to avoid FFmpeg command line quote/spacing issues
            val filterScriptFile = File(context.cacheDir, "join_filter_${System.currentTimeMillis()}.txt")
            tempFilesToClean.add(filterScriptFile)

            val scriptBuilder = StringBuilder()
            val inputsBuilder = StringBuilder()

            for ((i, file) in resolvedFiles.withIndex()) {
                inputsBuilder.append("-i \"${file.absolutePath}\" ")
                
                // Build per-item rotation and flip filters
                val itemFilters = mutableListOf<String>()
                val normDeg = ((videoItems[i].rotationDegrees % 360) + 360) % 360
                when (normDeg) {
                    90 -> itemFilters.add("transpose=1")
                    180 -> itemFilters.add("transpose=1,transpose=1")
                    270 -> itemFilters.add("transpose=2")
                }
                if (videoItems[i].flipHorizontal) itemFilters.add("hflip")
                if (videoItems[i].flipVertical) itemFilters.add("vflip")
                val rotFilterPrefix = if (itemFilters.isNotEmpty()) itemFilters.joinToString(",") + "," else ""

                // Scale with aspect ratio preservation and black letterbox padding, set SAR=1, exact FPS=30, and reset PTS
                scriptBuilder.append("[$i:v]settb=AVTB,setpts=PTS-STARTPTS,${rotFilterPrefix}scale=w=$targetW:h=$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2:black,setsar=1,fps=30,format=yuv420p[v$i];\n")
                if (audioFlags[i]) {
                    scriptBuilder.append("[$i:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,aresample=async=1000,asetpts=PTS-STARTPTS[a$i];\n")
                } else {
                    val durSec = String.format(java.util.Locale.US, "%.3f", (videoItems[i].durationMs.coerceAtLeast(500L)) / 1000.0)
                    scriptBuilder.append("anullsrc=r=44100:cl=stereo,atrim=duration=$durSec,asetpts=PTS-STARTPTS[a$i];\n")
                }
            }

            for (i in resolvedFiles.indices) {
                scriptBuilder.append("[v$i][a$i]")
            }
            scriptBuilder.append("concat=n=${resolvedFiles.size}:v=1:a=1[outv][outa]\n")

            filterScriptFile.writeText(scriptBuilder.toString())

            val inStr = inputsBuilder.toString().trim()

            val candidateEncoders = listOf(
                "-c:v libx264 -preset ultrafast -crf 23 -c:a aac -b:a 128k -pix_fmt yuv420p -movflags +faststart",
                "-c:v h264_mediacodec -b:v 4M -c:a aac -b:a 128k -pix_fmt yuv420p -movflags +faststart",
                "-c:v mpeg4 -b:v 4M -c:a aac -b:a 128k",
                "-c:v h264 -c:a aac"
            )

            var success = false
            for (enc in candidateEncoders) {
                if (outputFile.exists()) outputFile.delete()
                val reencodeCmd = "-y $inStr -filter_complex_script \"${filterScriptFile.absolutePath}\" -map \"[outv]\" -map \"[outa]\" $enc \"${outputFile.absolutePath}\""
                Log.d(TAG, "Executing re-encode join: $reencodeCmd")

                success = executeFFmpegAsyncWithProgress(
                    cmd = reencodeCmd,
                    durationMs = totalDurationMs,
                    startProgress = 0.1f,
                    endProgress = 0.98f,
                    onProgress = onProgress
                ) && outputFile.exists() && outputFile.length() > 0

                if (success) {
                    Log.d(TAG, "Re-encode join succeeded with encoder: $enc")
                    break
                } else {
                    Log.w(TAG, "Re-encode join failed with encoder: $enc")
                }
            }

            // Fallback 4: Sequential intermediate segment normalization if complex filter graph fails
            if (!success) {
                Log.w(TAG, "Full multi-input filter complex failed, attempting intermediate segment normalization")
                val intermediateFiles = mutableListOf<File>()
                var allTranscoded = true
                for ((i, file) in resolvedFiles.withIndex()) {
                    val interFile = File(context.cacheDir, "inter_join_${System.currentTimeMillis()}_$i.ts")
                    tempFilesToClean.add(interFile)
                    intermediateFiles.add(interFile)

                    val itemDurMs = videoItems[i].durationMs.coerceAtLeast(500L)
                    val hasAud = audioFlags[i]
                    
                    val itemFilters = mutableListOf<String>()
                    val normDeg = ((videoItems[i].rotationDegrees % 360) + 360) % 360
                    when (normDeg) {
                        90 -> itemFilters.add("transpose=1")
                        180 -> itemFilters.add("transpose=1,transpose=1")
                        270 -> itemFilters.add("transpose=2")
                    }
                    if (videoItems[i].flipHorizontal) itemFilters.add("hflip")
                    if (videoItems[i].flipVertical) itemFilters.add("vflip")
                    val rotFilterPrefix = if (itemFilters.isNotEmpty()) itemFilters.joinToString(",") + "," else ""
                    val vfParam = "${rotFilterPrefix}scale=w=$targetW:h=$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2:black,setsar=1,fps=30,format=yuv420p"

                    val singleCmd = if (hasAud) {
                        "-y -i \"${file.absolutePath}\" -vf \"$vfParam\" -c:v libx264 -preset ultrafast -crf 23 -c:a aac -ar 44100 -ac 2 -f mpegts \"${interFile.absolutePath}\""
                    } else {
                        val durSec = String.format(java.util.Locale.US, "%.3f", itemDurMs / 1000.0)
                        "-y -i \"${file.absolutePath}\" -f lavfi -i anullsrc=r=44100:cl=stereo -vf \"$vfParam\" -c:v libx264 -preset ultrafast -crf 23 -c:a aac -ar 44100 -ac 2 -t $durSec -f mpegts \"${interFile.absolutePath}\""
                    }

                    val interSuccess = FFmpegKit.execute(singleCmd)
                    if (!ReturnCode.isSuccess(interSuccess.returnCode) || !interFile.exists() || interFile.length() == 0L) {
                        allTranscoded = false
                        break
                    }
                    onProgress(0.1f + 0.8f * ((i + 1f) / resolvedFiles.size))
                }

                if (allTranscoded && intermediateFiles.all { it.exists() && it.length() > 0 }) {
                    val concatStr = intermediateFiles.joinToString("|") { it.absolutePath }
                    val finalMergeCmd = "-y -i \"concat:$concatStr\" -c copy -bsf:a aac_adtstoasc -movflags +faststart \"${outputFile.absolutePath}\""
                    val finalSession = FFmpegKit.execute(finalMergeCmd)
                    success = ReturnCode.isSuccess(finalSession.returnCode) && outputFile.exists() && outputFile.length() > 0
                }
            }

            if (success) {
                onProgress(1.0f)
                return@withContext true
            } else {
                Log.e(TAG, "All join strategies failed")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Join videos exception", e)
            return@withContext false
        } finally {
            tempFilesToClean.forEach { if (it.exists()) it.delete() }
        }
    }

    /**
     * Rotates and/or mirrors a video.
     * Supports rotating specific timeline range or the full video.
     */
    suspend fun rotateVideo(
        context: Context,
        sourceUri: Uri,
        rotationDegrees: Int, // 0, 90, 180, 270
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        isTimelineRange: Boolean,
        startMs: Long,
        endMs: Long,
        totalDurationMs: Long,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFilesToClean = mutableListOf<File>()
        try {
            onProgress(0.1f)
            val inputFile = getLocalFilePathOrTemp(context, sourceUri)
            if (inputFile.parentFile == context.cacheDir) {
                tempFilesToClean.add(inputFile)
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Build filter string with normalized degrees
            val filters = mutableListOf<String>()
            val normalizedDegrees = ((rotationDegrees % 360) + 360) % 360
            when (normalizedDegrees) {
                90 -> filters.add("transpose=1")
                180 -> filters.add("transpose=1,transpose=1")
                270 -> filters.add("transpose=2")
            }
            if (flipHorizontal) filters.add("hflip")
            if (flipVertical) filters.add("vflip")

            outputFile.parentFile?.mkdirs()

            if (filters.isEmpty() && !isTimelineRange) {
                // Just copy if no changes requested
                inputFile.copyTo(outputFile, overwrite = true)
                onProgress(1.0f)
                return@withContext true
            }

            val vfArg = if (filters.isNotEmpty()) "-vf ${filters.joinToString(",")}" else ""

            val inPath = inputFile.absolutePath
            val outPath = outputFile.absolutePath

            val videoEncoders = listOf(
                "-c:v h264_mediacodec -b:v 4M",
                "-c:v libx264 -preset ultrafast -crf 23",
                "-c:v mpeg4 -b:v 4M",
                "-c:v h264"
            )

            var success = false
            val durMs = if (isTimelineRange && endMs > startMs) (endMs - startMs) else totalDurationMs.coerceAtLeast(1000L)

            for (vEnc in videoEncoders) {
                val vEncPart = if (vEnc.isNotEmpty()) "$vEnc " else ""
                val cmd = if (isTimelineRange && startMs < endMs && (startMs > 500L || endMs < totalDurationMs - 500L)) {
                    val startSec = String.format(java.util.Locale.US, "%.3f", startMs / 1000.0)
                    val endSec = String.format(java.util.Locale.US, "%.3f", endMs / 1000.0)
                    "-y -ss $startSec -to $endSec -i \"$inPath\" $vfArg $vEncPart-map 0:v:0? -map 0:a? -c:a aac \"$outPath\""
                } else {
                    "-y -i \"$inPath\" $vfArg $vEncPart-map 0:v:0? -map 0:a? -c:a aac \"$outPath\""
                }

                Log.d(TAG, "Executing rotate video: $cmd")
                success = executeFFmpegAsyncWithProgress(
                    cmd = cmd,
                    durationMs = durMs,
                    startProgress = 0.1f,
                    endProgress = 0.95f,
                    onProgress = onProgress
                ) && outputFile.exists() && outputFile.length() > 0

                if (!success) {
                    Log.w(TAG, "AAC audio rotate failed, trying copy audio with vEnc=$vEnc")
                    val fallbackCmd = if (isTimelineRange && startMs < endMs && (startMs > 500L || endMs < totalDurationMs - 500L)) {
                        val startSec = String.format(java.util.Locale.US, "%.3f", startMs / 1000.0)
                        val endSec = String.format(java.util.Locale.US, "%.3f", endMs / 1000.0)
                        "-y -ss $startSec -to $endSec -i \"$inPath\" $vfArg $vEncPart-c:a copy \"$outPath\""
                    } else {
                        "-y -i \"$inPath\" $vfArg $vEncPart-c:a copy \"$outPath\""
                    }
                    success = executeFFmpegAsyncWithProgress(
                        cmd = fallbackCmd,
                        durationMs = durMs,
                        startProgress = 0.1f,
                        endProgress = 0.95f,
                        onProgress = onProgress
                    ) && outputFile.exists() && outputFile.length() > 0
                }

                if (!success) {
                    Log.w(TAG, "Audio copy rotate failed, trying no-audio with vEnc=$vEnc")
                    val noAudioCmd = if (isTimelineRange && startMs < endMs && (startMs > 500L || endMs < totalDurationMs - 500L)) {
                        val startSec = String.format(java.util.Locale.US, "%.3f", startMs / 1000.0)
                        val endSec = String.format(java.util.Locale.US, "%.3f", endMs / 1000.0)
                        "-y -ss $startSec -to $endSec -i \"$inPath\" $vfArg $vEncPart-an \"$outPath\""
                    } else {
                        "-y -i \"$inPath\" $vfArg $vEncPart-an \"$outPath\""
                    }
                    success = executeFFmpegAsyncWithProgress(
                        cmd = noAudioCmd,
                        durationMs = durMs,
                        startProgress = 0.1f,
                        endProgress = 0.95f,
                        onProgress = onProgress
                    ) && outputFile.exists() && outputFile.length() > 0
                }

                if (success) {
                    break
                }
            }

            if (success) {
                onProgress(1.0f)
            }
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Rotate video error", e)
            return@withContext false
        } finally {
            tempFilesToClean.forEach { if (it.exists()) it.delete() }
        }
    }

    /**
     * Normalizes and merges overlapping or adjacent time ranges in multi-part rotation settings.
     * Overlapping parts are resolved by favoring later parts (or merging identical transformations),
     * and gaps are filled with 0° unrotated video segments so 100% of the video timeline is preserved.
     */
    fun normalizeAndMergeRotateParts(
        parts: List<com.example.model.RotatePart>,
        totalDurationMs: Long
    ): List<com.example.model.RotatePart> {
        val dur = totalDurationMs.coerceAtLeast(1000L)
        val validParts = parts.filter { it.startMs < it.endMs }

        val timePoints = java.util.TreeSet<Long>()
        timePoints.add(0L)
        timePoints.add(dur)
        for (p in validParts) {
            val s = p.startMs.coerceIn(0L, dur)
            val e = p.endMs.coerceIn(0L, dur)
            if (s < e) {
                timePoints.add(s)
                timePoints.add(e)
            }
        }

        val pointList = timePoints.toList()
        val sliceParts = mutableListOf<com.example.model.RotatePart>()

        for (i in 0 until pointList.size - 1) {
            val s = pointList[i]
            val e = pointList[i + 1]
            if (s >= e) continue

            // Find covering user part (last defined user part takes precedence if overlapping)
            val coveringPart = validParts.lastOrNull { p ->
                val ps = p.startMs.coerceIn(0L, dur)
                val pe = p.endMs.coerceIn(0L, dur)
                ps <= s && pe >= e
            }

            if (coveringPart != null) {
                sliceParts.add(
                    com.example.model.RotatePart(
                        id = "slice_${s}_${e}",
                        startMs = s,
                        endMs = e,
                        rotationDegrees = coveringPart.rotationDegrees,
                        flipHorizontal = coveringPart.flipHorizontal,
                        flipVertical = coveringPart.flipVertical
                    )
                )
            } else {
                sliceParts.add(
                    com.example.model.RotatePart(
                        id = "slice_gap_${s}_${e}",
                        startMs = s,
                        endMs = e,
                        rotationDegrees = 0,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                )
            }
        }

        // Merge adjacent slices with identical transformation
        val merged = mutableListOf<com.example.model.RotatePart>()
        for (slice in sliceParts) {
            if (merged.isEmpty()) {
                merged.add(slice)
            } else {
                val last = merged.last()
                val normLastDeg = ((last.rotationDegrees % 360) + 360) % 360
                val normSliceDeg = ((slice.rotationDegrees % 360) + 360) % 360
                if (normLastDeg == normSliceDeg &&
                    last.flipHorizontal == slice.flipHorizontal &&
                    last.flipVertical == slice.flipVertical &&
                    last.endMs == slice.startMs) {
                    merged[merged.size - 1] = last.copy(endMs = slice.endMs)
                } else {
                    merged.add(slice)
                }
            }
        }

        return merged.filter { it.startMs < it.endMs }
    }

    /**
     * Fast single-pass multi-part video rotation using hardware acceleration.
     * Merges overlapping segments, fills unrotated gaps, and applies filter_complex in a single pass.
     */
    suspend fun rotateMultiPartVideo(
        context: Context,
        sourceUri: Uri,
        parts: List<com.example.model.RotatePart>,
        totalDurationMs: Long,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        if (parts.isEmpty()) return@withContext false
        val tempFilesToClean = mutableListOf<File>()
        try {
            onProgress(0.05f)
            val inputFile = getLocalFilePathOrTemp(context, sourceUri)
            if (inputFile.parentFile == context.cacheDir) {
                tempFilesToClean.add(inputFile)
            }

            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 1. Normalize and merge overlapping / adjacent rotate segments
            val mergedParts = normalizeAndMergeRotateParts(parts, totalDurationMs)
            if (mergedParts.isEmpty()) return@withContext false

            // Check if all parts require no rotation/flip
            val allUnchanged = mergedParts.all { p ->
                val normDeg = ((p.rotationDegrees % 360) + 360) % 360
                normDeg == 0 && !p.flipHorizontal && !p.flipVertical
            }

            if (allUnchanged) {
                inputFile.copyTo(outputFile, overwrite = true)
                onProgress(1.0f)
                return@withContext true
            }

            // 2. Check audio track presence
            val retriever = MediaMetadataRetriever()
            val hasAudio = try {
                retriever.setDataSource(inputFile.absolutePath)
                val hasAud = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                retriever.release()
                hasAud == "yes"
            } catch (_: Exception) {
                try { retriever.release() } catch (_: Exception) {}
                false
            }

            // 3. Build single-pass filter_complex for maximum processing speed
            val filterBuilder = StringBuilder()
            val concatBuilder = StringBuilder()

            val targetW = 1280
            val targetH = 720

            for ((i, p) in mergedParts.withIndex()) {
                val sSec = String.format(java.util.Locale.US, "%.3f", p.startMs / 1000.0)
                val eSec = String.format(java.util.Locale.US, "%.3f", p.endMs / 1000.0)

                val filters = mutableListOf<String>()
                val normDeg = ((p.rotationDegrees % 360) + 360) % 360
                when (normDeg) {
                    90 -> filters.add("transpose=1")
                    180 -> filters.add("transpose=1,transpose=1")
                    270 -> filters.add("transpose=2")
                }
                if (p.flipHorizontal) filters.add("hflip")
                if (p.flipVertical) filters.add("vflip")

                val tfArg = if (filters.isNotEmpty()) "," + filters.joinToString(",") else ""

                // Video stream trim & transform
                filterBuilder.append("[0:v]trim=start=$sSec:end=$eSec,setpts=PTS-STARTPTS$tfArg,scale=$targetW:$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(1280-iw)/2:(720-ih)/2:black,setsar=1,format=yuv420p[v$i]; ")

                // Audio stream trim if present
                if (hasAudio) {
                    filterBuilder.append("[0:a]atrim=start=$sSec:end=$eSec,asetpts=PTS-STARTPTS[a$i]; ")
                }
            }

            if (hasAudio) {
                for (i in mergedParts.indices) {
                    concatBuilder.append("[v$i][a$i]")
                }
                concatBuilder.append("concat=n=${mergedParts.size}:v=1:a=1[outv][outa]")
            } else {
                for (i in mergedParts.indices) {
                    concatBuilder.append("[v$i]")
                }
                concatBuilder.append("concat=n=${mergedParts.size}:v=1:a=0[outv]")
            }

            val filterComplexStr = filterBuilder.toString() + concatBuilder.toString()
            val mapStr = if (hasAudio) "-map \"[outv]\" -map \"[outa]\"" else "-map \"[outv]\""

            val candidateEncoders = if (hasAudio) {
                listOf(
                    "-c:v h264_mediacodec -b:v 4M -c:a aac -b:a 128k",
                    "-c:v libx264 -preset ultrafast -crf 23 -c:a aac -b:a 128k",
                    "-c:v mpeg4 -b:v 4M -c:a aac -b:a 128k",
                    "-c:v h264 -c:a aac"
                )
            } else {
                listOf(
                    "-c:v h264_mediacodec -b:v 4M",
                    "-c:v libx264 -preset ultrafast -crf 23",
                    "-c:v mpeg4 -b:v 4M",
                    "-c:v h264"
                )
            }

            val inPath = inputFile.absolutePath
            val outPath = outputFile.absolutePath
            val totalDurMs = totalDurationMs.coerceAtLeast(1000L)

            var success = false
            for (enc in candidateEncoders) {
                if (outputFile.exists()) outputFile.delete()
                val cmd = "-y -i \"$inPath\" -filter_complex \"$filterComplexStr\" $mapStr $enc \"$outPath\""
                Log.d(TAG, "Executing single-pass multi-rotate: $cmd")

                success = executeFFmpegAsyncWithProgress(
                    cmd = cmd,
                    durationMs = totalDurMs,
                    startProgress = 0.1f,
                    endProgress = 0.98f,
                    onProgress = onProgress
                ) && outputFile.exists() && outputFile.length() > 0

                if (success) {
                    Log.d(TAG, "Single-pass multi-rotate succeeded with encoder: $enc")
                    break
                } else {
                    Log.w(TAG, "Single-pass multi-rotate failed with encoder: $enc")
                }
            }

            if (success) {
                onProgress(1.0f)
                return@withContext true
            } else {
                Log.e(TAG, "Single-pass multi-rotate failed on all encoders")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "rotateMultiPartVideo exception", e)
            return@withContext false
        } finally {
            tempFilesToClean.forEach { if (it.exists()) it.delete() }
        }
    }

    /**
     * Media Store Video item data class for Media Selector.
     */
    data class DeviceMediaVideo(
        val id: Long,
        val uri: Uri,
        val title: String,
        val path: String?,
        val folderName: String,
        val durationMs: Long,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
        val dateModifiedMs: Long
    )

    /**
     * Queries MediaStore for all video files on the device.
     */
    suspend fun queryAllMediaVideos(context: Context): List<DeviceMediaVideo> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DeviceMediaVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val contentUris = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.INTERNAL_CONTENT_URI
        )

        for (uri in contentUris) {
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val wCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val hCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                        val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                        val title = if (nameCol >= 0) cursor.getString(nameCol) ?: "Video_$id" else "Video_$id"
                        val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                        val w = if (wCol >= 0) cursor.getInt(wCol) else 1280
                        val h = if (hCol >= 0) cursor.getInt(hCol) else 720
                        val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                        val dateMod = if (dateCol >= 0) cursor.getLong(dateCol) * 1000L else System.currentTimeMillis()
                        val bucket = if (bucketCol >= 0) cursor.getString(bucketCol) else null

                        val parentFolder = if (!path.isNullOrBlank()) {
                            try { File(path).parentFile?.name } catch (_: Exception) { null }
                        } else null

                        val folderName = parentFolder ?: bucket ?: "Videos"

                        list.add(
                            DeviceMediaVideo(
                                id = id,
                                uri = contentUri,
                                title = title,
                                path = path,
                                folderName = folderName,
                                durationMs = duration,
                                sizeBytes = size,
                                width = if (w > 0) w else 1280,
                                height = if (h > 0) h else 720,
                                dateModifiedMs = dateMod
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying MediaStore for uri $uri", e)
            }
        }
        list.distinctBy { it.path ?: it.uri.toString() }
    }

    /**
     * Loads a high-performance bitmap video frame thumbnail for UI components.
     */
    fun loadVideoThumbnail(context: Context, uri: Uri, path: String?): android.graphics.Bitmap? {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                return context.contentResolver.loadThumbnail(uri, android.util.Size(256, 256), null)
            } else if (!path.isNullOrBlank()) {
                val b = android.media.ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
                if (b != null) return b
            }
        } catch (_: Exception) {}

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            return frame
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Deletes a video file from MediaStore / disk.
     */
    suspend fun deleteVideoFile(context: Context, video: DeviceMediaVideo): Boolean = withContext(Dispatchers.IO) {
        try {
            var deleted = false
            if (video.path != null) {
                val f = File(video.path)
                if (f.exists()) {
                    deleted = f.delete()
                }
            }
            try {
                val rows = context.contentResolver.delete(video.uri, null, null)
                if (rows > 0) deleted = true
            } catch (_: Exception) {}
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting video file", e)
            false
        }
    }

}
