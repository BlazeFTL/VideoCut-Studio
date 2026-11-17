package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompressionMethod
import com.example.model.VideoItem
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

@Composable
fun CompressorScreen(
    selectedVideo: VideoItem?,
    currentMethod: CompressionMethod = CompressionMethod.H264_CRF,
    crfValue: Int = 23,
    encodingSpeed: String = "medium",
    targetSizeBytes: Long = 0L,
    currentResolution: String = "Original Resolution",
    isProcessing: Boolean,
    processingProgress: Float,
    statusMessage: String,
    onSetMethod: (CompressionMethod) -> Unit = {},
    onSetCrfValue: (Int) -> Unit = {},
    onSetEncodingSpeed: (String) -> Unit = {},
    onSetTargetSizeBytes: (Long) -> Unit = {},
    onSetResolution: (String) -> Unit = {},
    onSelectVideoUri: (Uri) -> Unit,
    onProcessCompress: () -> Unit,
    onPlayVideo: (Uri) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSelectVideoUri(uri)
    }

    val origBytes = (selectedVideo?.sizeBytes ?: 0L).coerceAtLeast(0L)
    val hasSourceVideo = selectedVideo != null && origBytes > 0L

    val estBytes: Long = if (hasSourceVideo) {
        VideoProcessor.estimateCompressedSizeBytes(
            originalSizeBytes = origBytes,
            durationMs = selectedVideo?.durationMs ?: 10_000L,
            srcWidth = selectedVideo?.width ?: 1280,
            srcHeight = selectedVideo?.height ?: 720,
            method = currentMethod,
            crf = crfValue,
            speedPreset = encodingSpeed,
            targetSizeBytes = targetSizeBytes,
            targetResolution = currentResolution
        )
    } else 0L

    val savedBytes = (origBytes - estBytes).coerceAtLeast(0L)
    val savedPercent = if (origBytes > 0L) {
        ((1.0 - (estBytes.toDouble() / origBytes.toDouble())) * 100).toInt().coerceIn(0, 99)
    } else 0

    val (outW, outH) = if (selectedVideo != null) {
        VideoProcessor.resolveTargetDimensions(selectedVideo.width, selectedVideo.height, currentResolution)
    } else (0 to 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Video Compressor",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Shrink file size with x264/x265 CRF, speed presets or target MB.",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 1. Source Video Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = "Video",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = selectedVideo?.title ?: "No Video Selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hasSourceVideo) {
                                    "${VideoProcessor.formatFileSize(origBytes)} • ${selectedVideo!!.width}x${selectedVideo.height}"
                                } else {
                                    "Pick a video to inspect compression estimate"
                                },
                                fontSize = 12.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("pick_video_button")
                    ) {
                        Text(
                            text = if (selectedVideo == null) "Pick Video" else "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Estimated Output Size Card (clean classic summary card with accurate math)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ESTIMATED OUTPUT SIZE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (hasSourceVideo) "~${VideoProcessor.formatFileSize(estBytes)}" else "--",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("compressor_estimated_size_text")
                        )
                        Text(
                            text = if (hasSourceVideo) {
                                "Output: ${outW}x${outH} • Save ~$savedPercent% (${VideoProcessor.formatFileSize(savedBytes)})"
                            } else {
                                "Select a source video above"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    if (hasSourceVideo) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "-$savedPercent%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. Compression Engine Method Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Compression Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CompressionMethod.entries.forEach { method ->
                        val isSelected = currentMethod == method
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSetMethod(method) }
                                .testTag("method_option_${method.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = method.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimaryDark
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = method.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextSecondaryMuted
                                            )
                                        }
                                    }
                                    Text(
                                        text = method.description,
                                        fontSize = 11.sp,
                                        color = TextSecondaryMuted,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Method Parameters Card (CRF / Preset / Target MB / Lossless)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (currentMethod) {
                        CompressionMethod.H264_CRF, CompressionMethod.HEVC_H265 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CRF Quality",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimaryDark
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "CRF $crfValue",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = if (currentMethod == CompressionMethod.H264_CRF)
                                    "Default CRF 23. Higher CRF (28-32) produces smaller files with libx264 rate control."
                                else
                                    "Default CRF 28. HEVC libx265 achieves high efficiency at ~25-35% smaller sizes.",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            Slider(
                                value = crfValue.toFloat(),
                                onValueChange = { onSetCrfValue(it.toInt()) },
                                valueRange = 18f..36f,
                                steps = 17,
                                modifier = Modifier.testTag("crf_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Encoding Speed / Preset",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val presets = listOf("ultrafast", "fast", "medium", "slow")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { speed ->
                                    val isSpeedSelected = encodingSpeed.equals(speed, ignoreCase = true)
                                    Surface(
                                        onClick = { onSetEncodingSpeed(speed) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSpeedSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSpeedSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .testTag("preset_${speed}_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = speed.replaceFirstChar { it.uppercase() },
                                                fontSize = 12.sp,
                                                fontWeight = if (isSpeedSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSpeedSelected) Color.White else TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CompressionMethod.TARGET_SIZE -> {
                            val sliderMin = if (origBytes > 0L) (origBytes * 0.10f).coerceAtLeast(100_000f) else 100_000f
                            val sliderMax = if (origBytes > 0L) (origBytes * 0.95f).coerceAtLeast(sliderMin + 100_000f) else 2_000_000f
                            val currentTargetBytes = (if (targetSizeBytes > 0L) targetSizeBytes else (origBytes * 0.50).toLong())
                                .coerceIn(sliderMin.toLong(), sliderMax.toLong())

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target File Size",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = VideoProcessor.formatFileSize(currentTargetBytes),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = if (hasSourceVideo) "Source: ${VideoProcessor.formatFileSize(origBytes)}" else "Pick a video first to set an exact target size",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            Slider(
                                value = currentTargetBytes.toFloat().coerceIn(sliderMin, sliderMax),
                                onValueChange = { onSetTargetSizeBytes(it.toLong()) },
                                valueRange = sliderMin..sliderMax,
                                enabled = hasSourceVideo,
                                modifier = Modifier.testTag("target_size_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%").forEach { (pct, label) ->
                                    val calcBytes = (origBytes * pct).toLong()
                                    OutlinedButton(
                                        onClick = { onSetTargetSizeBytes(calcBytes) },
                                        enabled = hasSourceVideo,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        CompressionMethod.STREAM_COPY -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldContainer.copy(alpha = 0.6f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Lossless Stream Optimization",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldSuccess
                                    )
                                    Text(
                                        text = "Re-muxes streams with +faststart without re-encoding. Preserves 100% original video quality.",
                                        fontSize = 11.sp,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Output Resolution Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Output Resolution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Downscaling lowers pixel count and reduces output file size",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    val resolutions = listOf(
                        "Original Resolution",
                        "1080p (1920x1080)",
                        "720p (1280x720)",
                        "480p (854x480)",
                        "360p (640x360)"
                    )

                    resolutions.forEach { res ->
                        val isSelected = currentResolution == res
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetResolution(res) }
                                .testTag("res_option_${res.substringBefore(" ").lowercase()}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = res,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimaryDark
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected resolution",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Compress & Save Action Buttons
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onProcessCompress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("compress_button"),
                    enabled = selectedVideo != null && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compress & Save Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (selectedVideo != null) {
                    OutlinedButton(
                        onClick = { onPlayVideo(selectedVideo.uri) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("play_source_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Source Video",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
