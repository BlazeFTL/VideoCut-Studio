package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RotatePart
import com.example.model.VideoItem
import com.example.ui.components.FullscreenProcessingDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

@Composable
fun RotateVideoScreen(
    selectedVideo: VideoItem?,
    rotationDegrees: Int,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    isTimelineRotateEnabled: Boolean,
    rotateStartMs: Long,
    rotateEndMs: Long,
    rotateParts: List<RotatePart> = emptyList(),
    onSelectVideoUri: (Uri) -> Unit,
    onRotate90CW: () -> Unit,
    onSetRotationDegrees: (Int) -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onToggleFlipVertical: () -> Unit,
    onSetTimelineRotateEnabled: (Boolean) -> Unit,
    onSetTimelineRange: (startMs: Long, endMs: Long) -> Unit,
    onAddRotatePart: () -> Unit,
    onRemoveRotatePart: (String) -> Unit,
    onUpdateRotatePart: (RotatePart) -> Unit,
    onPlayVideo: (Uri) -> Unit,
    onProcessRotate: () -> Unit,
    isProcessing: Boolean,
    processingProgress: Float = 0f,
    statusMessage: String = ""
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectVideoUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Smart Video Rotate & Flip",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Rotate full video or multi-part timeline segments",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            OutlinedButton(
                onClick = { pickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(if (selectedVideo == null) "Select Video" else "Change", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        if (selectedVideo == null) {
            // Select Video Card Prompt
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { pickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No Video Selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimaryDark
                    )

                    Text(
                        text = "Tap to choose a video to rotate or mirror",
                        fontSize = 12.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            // Video Player Container (Expanded height to 460.dp for generous viewport)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoPlayerView(
                            videoUri = selectedVideo.uri,
                            durationMs = selectedVideo.durationMs,
                            startMs = if (isTimelineRotateEnabled) rotateStartMs else 0L,
                            endMs = if (isTimelineRotateEnabled && rotateEndMs > 0L) rotateEndMs else selectedVideo.durationMs,
                            autoPlay = false,
                            rotationDegrees = rotationDegrees,
                            flipHorizontal = flipHorizontal,
                            flipVertical = flipVertical,
                            heightDp = null,
                            videoWidth = selectedVideo.width,
                            videoHeight = selectedVideo.height
                        )

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val flipText = buildString {
                                if (flipHorizontal) append(" Flip H")
                                if (flipVertical) append(" Flip V")
                            }
                            Text(
                                text = "${rotationDegrees}° Rotation${if (flipText.isNotEmpty()) " •$flipText" else ""}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Player Toolbar Controls (Directly updates rotation & active segment) - Clean Modern Style
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        border = BorderStroke(1.dp, SurfaceBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate CCW (-90)
                            Surface(
                                onClick = {
                                    val target = ((rotationDegrees - 90) % 360 + 360) % 360
                                    onSetRotationDegrees(target)
                                    if (isTimelineRotateEnabled && rotateParts.isNotEmpty()) {
                                        val lastPart = rotateParts.last()
                                        onUpdateRotatePart(lastPart.copy(rotationDegrees = target))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RotateLeft,
                                        contentDescription = "Rotate CCW (-90°)",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Rotate CW (+90)
                            Surface(
                                onClick = {
                                    val target = (rotationDegrees + 90) % 360
                                    onSetRotationDegrees(target)
                                    if (isTimelineRotateEnabled && rotateParts.isNotEmpty()) {
                                        val lastPart = rotateParts.last()
                                        onUpdateRotatePart(lastPart.copy(rotationDegrees = target))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RotateRight,
                                        contentDescription = "Rotate CW (+90°)",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Flip Horizontal
                            Surface(
                                onClick = onToggleFlipHorizontal,
                                shape = RoundedCornerShape(14.dp),
                                color = if (flipHorizontal) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (flipHorizontal) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Flip Horizontal",
                                        tint = if (flipHorizontal) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Flip Vertical
                            Surface(
                                onClick = onToggleFlipVertical,
                                shape = RoundedCornerShape(14.dp),
                                color = if (flipVertical) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (flipVertical) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Flip Vertical",
                                        tint = if (flipVertical) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explicit Rotation Mode Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Rotation Scope Mode",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Select whether to rotate the entire video or specific time ranges",
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Full Video Mode Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSetTimelineRotateEnabled(false)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isTimelineRotateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, if (!isTimelineRotateEnabled) MaterialTheme.colorScheme.primary else SurfaceBorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔄 Full Video",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isTimelineRotateEnabled) Color.White else TextPrimaryDark
                                )
                                Text(
                                    text = "Rotate 100% video",
                                    fontSize = 10.sp,
                                    color = if (!isTimelineRotateEnabled) Color.White.copy(alpha = 0.8f) else TextSecondaryMuted
                                )
                            }
                        }

                        // Custom Time Range Mode Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onSetTimelineRotateEnabled(true)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTimelineRotateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, if (isTimelineRotateEnabled) MaterialTheme.colorScheme.primary else SurfaceBorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⏱️ Time Ranges",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimelineRotateEnabled) Color.White else TextPrimaryDark
                                )
                                Text(
                                    text = "Multi-part ranges",
                                    fontSize = 10.sp,
                                    color = if (isTimelineRotateEnabled) Color.White.copy(alpha = 0.8f) else TextSecondaryMuted
                                )
                            }
                        }
                    }

                    if (!isTimelineRotateEnabled) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "💡 Full video mode active. Tap the 4 control buttons under the video player above to adjust angle (${rotationDegrees}°) or flip settings.",
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isTimelineRotateEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val durationMs = selectedVideo.durationMs.coerceAtLeast(1000L)

                            // Single Range Fallback or Default Range Controls
                            if (rotateParts.isEmpty()) {
                                val safeStartMs = rotateStartMs.coerceIn(0L, (durationMs - 100L).coerceAtLeast(0L))
                                val safeEndMs = rotateEndMs.coerceIn((safeStartMs + 100L).coerceAtLeast(100L), durationMs)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Start: ${VideoProcessor.formatDuration(safeStartMs)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("End: ${VideoProcessor.formatDuration(safeEndMs)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                RangeSlider(
                                    value = safeStartMs.toFloat()..safeEndMs.toFloat(),
                                    onValueChange = { range ->
                                        onSetTimelineRange(range.start.toLong(), range.endInclusive.toLong())
                                    },
                                    valueRange = 0f..durationMs.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }

                            // Multi-Part List
                            rotateParts.forEachIndexed { index, part ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Part ${index + 1}: ${VideoProcessor.formatDuration(part.startMs)} - ${VideoProcessor.formatDuration(part.endMs)} (${part.rotationDegrees}°${if (part.flipHorizontal) ", Flip H" else ""}${if (part.flipVertical) ", Flip V" else ""})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            if (rotateParts.size > 1) {
                                                IconButton(
                                                    onClick = { onRemoveRotatePart(part.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove Part", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Part Range Slider
                                        val pStart = part.startMs.coerceIn(0L, durationMs - 100L)
                                        val pEnd = part.endMs.coerceIn(pStart + 100L, durationMs)

                                        RangeSlider(
                                            value = pStart.toFloat()..pEnd.toFloat(),
                                            onValueChange = { range ->
                                                val nStart = range.start.toLong()
                                                val nEnd = range.endInclusive.toLong()
                                                onUpdateRotatePart(part.copy(startMs = nStart, endMs = nEnd))
                                                onSetTimelineRange(nStart, nEnd)
                                                onSetRotationDegrees(part.rotationDegrees)
                                            },
                                            valueRange = 0f..durationMs.toFloat(),
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )

                                        // Part Rotation & Flip Action Buttons (Same 4 controls as player above)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // 1. Rotate CCW (-90)
                                            OutlinedButton(
                                                onClick = {
                                                    val newDeg = ((part.rotationDegrees - 90) % 360 + 360) % 360
                                                    onUpdateRotatePart(part.copy(rotationDegrees = newDeg))
                                                    onSetRotationDegrees(newDeg)
                                                    onSetTimelineRange(part.startMs, part.endMs)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp)
                                            ) {
                                                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate CCW", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }

                                            // 2. Rotate CW (+90)
                                            OutlinedButton(
                                                onClick = {
                                                    val newDeg = (part.rotationDegrees + 90) % 360
                                                    onUpdateRotatePart(part.copy(rotationDegrees = newDeg))
                                                    onSetRotationDegrees(newDeg)
                                                    onSetTimelineRange(part.startMs, part.endMs)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp)
                                            ) {
                                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate CW", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }

                                            // 3. Flip Horizontal
                                            OutlinedButton(
                                                onClick = {
                                                    val newFlipH = !part.flipHorizontal
                                                    onUpdateRotatePart(part.copy(flipHorizontal = newFlipH))
                                                    if (flipHorizontal != newFlipH) onToggleFlipHorizontal()
                                                    onSetTimelineRange(part.startMs, part.endMs)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (part.flipHorizontal) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                ),
                                                border = BorderStroke(1.dp, if (part.flipHorizontal) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.SwapHoriz,
                                                    contentDescription = "Flip H",
                                                    tint = if (part.flipHorizontal) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // 4. Flip Vertical
                                            OutlinedButton(
                                                onClick = {
                                                    val newFlipV = !part.flipVertical
                                                    onUpdateRotatePart(part.copy(flipVertical = newFlipV))
                                                    if (flipVertical != newFlipV) onToggleFlipVertical()
                                                    onSetTimelineRange(part.startMs, part.endMs)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (part.flipVertical) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                ),
                                                border = BorderStroke(1.dp, if (part.flipVertical) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.SwapVert,
                                                    contentDescription = "Flip V",
                                                    tint = if (part.flipVertical) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add Part Button
                            OutlinedButton(
                                onClick = onAddRotatePart,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Rotation Part", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Save Rotated Video Button
            Button(
                onClick = onProcessRotate,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Rotated Video",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
