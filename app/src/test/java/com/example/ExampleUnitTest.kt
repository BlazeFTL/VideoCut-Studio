package com.example

import com.example.model.CompressionMethod
import com.example.util.VideoProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local JVM unit tests for VideoCut Studio compression math and utilities.
 */
class ExampleUnitTest {
    @Test
    fun estimateCompressedSize_crf28Fast_producesSmallerOutputThanCrf23() {
        val originalBytes = 40L * 1024L * 1024L // 40 MB
        val durationMs = 60_000L

        val estCrf23 = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = durationMs,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.H264_CRF,
            crf = 23,
            speedPreset = "medium",
            targetSizeBytes = 0L,
            targetResolution = "Original Resolution"
        )

        val estCrf28Fast = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = durationMs,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.H264_CRF,
            crf = 28,
            speedPreset = "fast",
            targetSizeBytes = 0L,
            targetResolution = "Original Resolution"
        )

        assertTrue("CRF 28 Fast ($estCrf28Fast) should be smaller than CRF 23 Medium ($estCrf23)", estCrf28Fast < estCrf23)
        assertTrue("CRF 28 Fast ($estCrf28Fast) should be significantly below original size ($originalBytes)", estCrf28Fast < originalBytes * 0.65)
    }

    @Test
    fun estimateCompressedSize_hevcIsMoreEfficientThanH264() {
        val originalBytes = 50L * 1024L * 1024L
        val h264 = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = 45_000L,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.H264_CRF,
            crf = 26,
            speedPreset = "fast",
            targetSizeBytes = 0L,
            targetResolution = "Original Resolution"
        )
        val hevc = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = 45_000L,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.HEVC_H265,
            crf = 26,
            speedPreset = "fast",
            targetSizeBytes = 0L,
            targetResolution = "Original Resolution"
        )
        assertTrue("HEVC ($hevc) should estimate smaller than H.264 ($h264)", hevc < h264)
    }

    @Test
    fun estimateCompressedSize_downscalingReducesEstimate() {
        val originalBytes = 60L * 1024L * 1024L
        val est1080p = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = 60_000L,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.H264_CRF,
            crf = 28,
            speedPreset = "fast",
            targetSizeBytes = 0L,
            targetResolution = "Original Resolution"
        )
        val est720p = VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = originalBytes,
            durationMs = 60_000L,
            srcWidth = 1920,
            srcHeight = 1080,
            method = CompressionMethod.H264_CRF,
            crf = 28,
            speedPreset = "fast",
            targetSizeBytes = 0L,
            targetResolution = "720p"
        )
        assertTrue("720p ($est720p) should estimate smaller than 1080p ($est1080p)", est720p < est1080p)
    }

    @Test
    fun resolveTargetDimensions_preservesAspectRatioAndEvenDimensions() {
        // Landscape 1920x1080 -> 720p = 1280x720
        val landscape = VideoProcessor.resolveTargetDimensions(1920, 1080, "720p (HD)")
        assertEquals(1280 to 720, landscape)

        // Portrait 1080x1920 -> 720p = 720x1280
        val portrait = VideoProcessor.resolveTargetDimensions(1080, 1920, "720p (HD)")
        assertEquals(720 to 1280, portrait)

        // Never upscales smaller video
        val small = VideoProcessor.resolveTargetDimensions(641, 361, "1080p")
        assertEquals(640 to 360, small)
        assertEquals(0, small.first % 2)
        assertEquals(0, small.second % 2)
    }

    @Test
    fun normalizeEncodingPreset_handlesCaseAndInvalidInputs() {
        assertEquals("fast", VideoProcessor.normalizeEncodingPreset(" Fast "))
        assertEquals("veryslow", VideoProcessor.normalizeEncodingPreset("VERYSLOW"))
        assertEquals("medium", VideoProcessor.normalizeEncodingPreset("unknown_speed"))
    }
}

