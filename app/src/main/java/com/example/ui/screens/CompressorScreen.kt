package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompressionMethod
import com.example.model.VideoItem
import com.example.ui.theme.AppMonoFontFamily
import com.example.ui.theme.EmeraldSuccess
import com.example.util.VideoProcessor

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val targetKeptRatio = if (origBytes > 0L) (estBytes.toFloat() / origBytes.toFloat()).coerceIn(0.05f, 1f) else 0f
    val animatedKeptRatio by animateFloatAsState(
        targetValue = targetKeptRatio,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "compress_ratio_ring"
    )

    val (outW, outH) = if (selectedVideo != null) {
        VideoProcessor.resolveTargetDimensions(selectedVideo.width, selectedVideo.height, currentResolution)
    } else (0 to 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Focal Expressive Hero Card (Projected Size & Savings Ring)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        if (hasSourceVideo) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ESTIMATED OUTPUT",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "~${VideoProcessor.formatFileSize(estBytes)}",
                                            style = MaterialTheme.typography.displayMedium.copy(fontFamily = AppMonoFontFamily),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("compressor_estimated_size_text")
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Original ${VideoProcessor.formatFileSize(origBytes)} • Save ~$savedPercent% (${VideoProcessor.formatFileSize(savedBytes)})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Box(
                                        modifier = Modifier.size(92.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularWavyProgressIndicator(
                                            progress = { animatedKeptRatio },
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "-$savedPercent%",
                                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedVideo?.title ?: "",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${outW}x${outH} • ${currentMethod.title}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            pickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("pick_video_button"),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Change Video")
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Movie,
                                                contentDescription = "Select video",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = "Select a Video",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pick a clip to inspect bitrate & CRF savings",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        pickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("pick_video_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Pick")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Compression Engine Method Selector (Soft Tonal Surface)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Compression Engine",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CompressionMethod.entries.forEach { method ->
                            val isSelected = currentMethod == method
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSetMethod(method) }
                                    .testTag("method_option_${method.name.lowercase()}"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onPrimary)
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
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = method.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = method.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Method Parameters Card (CRF / Preset / Target MB / Lossless)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        when (currentMethod) {
                            CompressionMethod.H264_CRF, CompressionMethod.HEVC_H265 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "CRF Quality",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "CRF $crfValue",
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = AppMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (currentMethod == CompressionMethod.H264_CRF)
                                        "Default CRF 23. Higher CRF (28–32) yields smaller files with libx264 rate control."
                                    else
                                        "Default CRF 28. HEVC libx265 delivers crisp quality at ~25–35% smaller sizes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
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

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Encoding Speed / Preset",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val presets = listOf("ultrafast", "fast", "medium", "slow")
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    presets.forEachIndexed { index, speed ->
                                        val isSpeedSelected = encodingSpeed.equals(speed, ignoreCase = true)
                                        SegmentedButton(
                                            selected = isSpeedSelected,
                                            onClick = { onSetEncodingSpeed(speed) },
                                            shape = SegmentedButtonDefaults.itemShape(index = index, count = presets.size),
                                            modifier = Modifier.testTag("preset_${speed}_btn")
                                        ) {
                                            Text(
                                                text = speed.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = VideoProcessor.formatFileSize(currentTargetBytes),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = AppMonoFontFamily),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = if (hasSourceVideo) "Source: ${VideoProcessor.formatFileSize(origBytes)}" else "Pick a video first to set an exact target size",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        FilledTonalButton(
                                            onClick = { onSetTargetSizeBytes(calcBytes) },
                                            enabled = hasSourceVideo,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }

                            CompressionMethod.STREAM_COPY -> {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Lossless Stream Optimization",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = EmeraldSuccess
                                            )
                                            Text(
                                                text = "Re-muxes streams with +faststart without re-encoding. Preserves 100% original video quality.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Target Resolution Selector
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Output Resolution",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Downscaling lowers pixel count and dramatically cuts file size",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSetResolution(res) }
                                    .testTag("res_option_${res.substringBefore(" ").lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = res,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected resolution",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Primary Compress Action Button
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onProcessCompress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("compress_button"),
                        enabled = selectedVideo != null && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compress & Save Video",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedVideo != null) {
                        OutlinedButton(
                            onClick = { onPlayVideo(selectedVideo.uri) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("play_source_button"),
                            shape = RoundedCornerShape(16.dp),
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
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
