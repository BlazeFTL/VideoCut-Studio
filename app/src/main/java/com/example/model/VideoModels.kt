package com.example.model

import android.graphics.Bitmap
import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int = 1280,
    val height: Int = 720,
    val path: String? = null,
    val thumbnail: Bitmap? = null
)

data class CutSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val isKeep: Boolean = true // true = keep segment, false = cut out / remove
)

enum class MultiPartCutMode {
    KEEP_SELECTED,   // Merge and keep only selected segments
    REMOVE_SELECTED  // Cut out / remove selected segments and merge remaining
}

enum class CompressionPreset(val label: String, val scaleFactor: Float, val bitrateMultiplier: Float, val description: String) {
    LOSSLESS("Lossless Quality", 0.95f, 1.0f, "Original video & audio quality with stream structure optimization"),
    HIGH_QUALITY("High Quality", 0.85f, 0.75f, "15-25% size reduction, preserves maximum clarity"),
    BALANCED("Balanced", 0.65f, 0.5f, "40-50% size reduction with great HD quality"),
    ULTRA_FAST("Ultra Fast", 0.45f, 0.3f, "70-80% size reduction, ideal for fast sharing")
}

enum class CompressionMethod(
    val title: String,
    val badge: String,
    val description: String,
    val defaultCrf: Int
) {
    H264_CRF("H.264 Standard (CRF)", "RECOMMENDED", "Universal compatibility & fast encoding", 23),
    HEVC_H265("HEVC / H.265 (High Efficiency)", "BEST RATIO", "Up to 50% smaller size at identical visual quality", 28),
    TARGET_SIZE("Target File Size", "CUSTOM SIZE", "Set custom target megabytes or percentage", 23),
    STREAM_COPY("Lossless / Fast Seek (Stream Copy)", "ULTRA SPEED", "Instant remux stream copy (~0-5% loss)", 0)
}

data class RotatePart(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

data class CompressionSettings(
    val preset: CompressionPreset = CompressionPreset.BALANCED,
    val method: CompressionMethod = CompressionMethod.H264_CRF,
    val targetWidth: Int = 1280,
    val targetHeight: Int = 720,
    val targetBitrate: Int = 2_000_000, // 2 Mbps
    val removeAudio: Boolean = false,
    val fastMode: Boolean = true
)
