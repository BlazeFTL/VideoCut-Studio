package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CompressionMethod
import com.example.model.VideoItem
import com.example.ui.components.FullscreenProcessingDialog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
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

    val origBytes = if (selectedVideo != null && selectedVideo.sizeBytes > 0) selectedVideo.sizeBytes else 25_000_000L

    // Calculate Estimated Size based on method and parameters
    val estBytes: Long = when (currentMethod) {
        CompressionMethod.H264_CRF -> {
            val ratio = (1.0 - (crfValue - 18) * 0.038).coerceIn(0.12, 0.95)
            (origBytes * ratio).toLong()
        }
        CompressionMethod.HEVC_H265 -> {
            val ratio = (1.0 - (crfValue - 18) * 0.048).coerceIn(0.08, 0.85)
            (origBytes * ratio).toLong()
        }
        CompressionMethod.TARGET_SIZE -> {
            if (targetSizeBytes > 0) targetSizeBytes.coerceIn((origBytes * 0.05).toLong(), origBytes) else (origBytes * 0.50).toLong()
        }
        CompressionMethod.STREAM_COPY -> {
            (origBytes * 0.98).toLong()
        }
    }

    val savedBytes = (origBytes - estBytes).coerceAtLeast(0L)
    val savedPercent = if (origBytes > 0) {
        ((1.0 - (estBytes.toDouble() / origBytes.toDouble())) * 100).toInt().coerceAtLeast(0)
    } else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Section
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Video Compressor",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "High performance video size reduction with CRF, H.265 & custom target size",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // 2. Selected Source Video Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedVideo?.title ?: "No video selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark,
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Size: ${VideoProcessor.formatFileSize(origBytes)}",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted
                            )
                            if (selectedVideo != null) {
                                Text(
                                    text = "• ${selectedVideo.width}x${selectedVideo.height}",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            pickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pick", fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. Compression Engine Selector Card (MOVED UP)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SecondaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Compression Engine Method",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CompressionMethod.entries.forEach { method ->
                        val isSelected = currentMethod == method
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSetMethod(method) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
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
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .padding(2.dp),
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = method.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextPrimaryDark
                                        )
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = method.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondaryMuted,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

        // 4. Dynamic Engine Settings Panel (CRF / Speed / Target Size / Lossless Banner)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (currentMethod) {
                        CompressionMethod.H264_CRF, CompressionMethod.HEVC_H265 -> {
                            // CRF Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "CRF Quality Level",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimaryDark
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "CRF $crfValue",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (currentMethod == CompressionMethod.H264_CRF)
                                    "Recommended: CRF 23 (Lower = Higher Quality / Larger Size, Higher = Smaller File)"
                                else
                                    "Recommended: CRF 28 for HEVC (Superior visual clarity at smaller sizes)",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )

                            Slider(
                                value = crfValue.toFloat(),
                                onValueChange = { onSetCrfValue(it.toInt()) },
                                valueRange = 18f..36f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Encoding Speed / Preset Selector
                            Text(
                                text = "Encoding Speed / Preset",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("ultrafast", "fast", "medium", "slow").forEach { speed ->
                                    val isSpeedSelected = encodingSpeed.equals(speed, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSetEncodingSpeed(speed) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSpeedSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, if (isSpeedSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (speed) {
                                                    "medium" -> "Medium ★"
                                                    else -> speed.replaceFirstChar { it.uppercase() }
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSpeedSelected) Color.White else TextPrimaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CompressionMethod.TARGET_SIZE -> {
                            // Target File Size Controls
                            val currentTargetBytes = if (targetSizeBytes > 0) targetSizeBytes else (origBytes * 0.50).toLong()

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
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Original size: ${VideoProcessor.formatFileSize(origBytes)}",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )

                            Slider(
                                value = currentTargetBytes.toFloat(),
                                onValueChange = { onSetTargetSizeBytes(it.toLong()) },
                                valueRange = (origBytes * 0.10f)..(origBytes * 0.95f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Quick Percent Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(0.25f to "25% Size", 0.50f to "50% Size", 0.75f to "75% Size").forEach { (pct, label) ->
                                    val calcBytes = (origBytes * pct).toLong()
                                    OutlinedButton(
                                        onClick = { onSetTargetSizeBytes(calcBytes) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                    }
                                }
                            }
                        }

                        CompressionMethod.STREAM_COPY -> {
                            // Lossless Info Card
                            Surface(
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "✨ Lossless Stream Optimization",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = EmeraldSuccess
                                        )
                                        Text(
                                            text = "Re-muxes original audio & video streams without re-encoding. 100% full visual fidelity.",
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Size Preview Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Possible Output Size",
                                    fontSize = 11.sp,
                                    color = TextSecondaryMuted
                                )
                                Text(
                                    text = "~${VideoProcessor.formatFileSize(estBytes)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Surface(
                                color = EmeraldSuccess,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "-$savedPercent% (~${VideoProcessor.formatFileSize(savedBytes)} saved)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Target Resolution Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SurfaceBorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Resolution",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Select output video dimensions (Default is Original Resolution)",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted,
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
                                .padding(vertical = 4.dp)
                                .clickable { onSetResolution(res) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else SurfaceBorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        contentDescription = null,
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

        // 6. Primary Compress Action Button
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onProcessCompress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = selectedVideo != null && !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compress & Save Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedVideo != null) {
                    OutlinedButton(
                        onClick = { onPlayVideo(selectedVideo.uri) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
