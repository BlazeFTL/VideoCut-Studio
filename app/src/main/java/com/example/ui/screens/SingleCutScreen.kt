package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.VideoItem
import com.example.util.VideoProcessor

import com.example.ui.components.FullscreenProcessingDialog
import com.example.ui.components.VideoPlayerView

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SingleCutScreen(
    selectedVideo: VideoItem?,
    timelineThumbnails: List<Bitmap>,
    startMs: Long,
    endMs: Long,
    isProcessing: Boolean,
    processingProgress: Float,
    statusMessage: String,
    onRangeChanged: (startMs: Long, endMs: Long) -> Unit,
    onSelectVideoUri: (Uri) -> Unit,
    onOpenPicker: (() -> Unit)? = null,
    onProcessCut: () -> Unit,
    onPlayVideo: (Uri) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSelectVideoUri(uri)
    }

    val totalDurationMs = if (selectedVideo != null && selectedVideo.durationMs > 0L) selectedVideo.durationMs else 180_000L
    val cutDurationMs = (endMs - startMs).coerceAtLeast(0L)

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackMs by remember { mutableStateOf(0L) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var activeCutTab by remember { mutableStateOf(0) } // 0: Cut/Trim video, 1: Equal Parts

    fun seekToPreview(ms: Long) {
        val safeMs = ms.coerceIn(0L, totalDurationMs).toInt()
        videoViewRef?.let { vv ->
            vv.seekTo(safeMs)
            currentPlaybackMs = safeMs.toLong()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (onOpenPicker != null) {
                            onOpenPicker()
                        } else {
                            pickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                )
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back / Select Video",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Crop",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedVideo?.title?.substringAfterLast('.')?.uppercase() ?: "MP4",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Button(
                onClick = onProcessCut,
                enabled = !isProcessing && selectedVideo != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.testTag("single_cut_save_button")
            ) {
                Text(
                    text = if (isProcessing) "Saving..." else "Cut & Save",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Live Video Player Screen View (Takes full majority of available screen space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (selectedVideo != null) {
                VideoPlayerView(
                    videoUri = selectedVideo.uri,
                    durationMs = totalDurationMs,
                    startMs = startMs,
                    endMs = endMs,
                    videoWidth = selectedVideo.width,
                    videoHeight = selectedVideo.height
                )
            } else {
                // Empty state pick video card
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No video selected", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (onOpenPicker != null) {
                                    onOpenPicker()
                                } else {
                                    pickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.VideoOnly
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Video from Gallery")
                        }
                    }
                }
            }
        }

        // Unified Stepper Controls & Video Frame Timeline Strip Below Video Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        val newS = (startMs - 1000L).coerceAtLeast(0L)
                                        onRangeChanged(newS, endMs)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = VideoProcessor.formatDurationPrecise(startMs),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { showStartPicker = true }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        val newS = (startMs + 1000L).coerceAtMost((endMs - 100L).coerceAtLeast(0L))
                                        onRangeChanged(newS, endMs)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = VideoProcessor.formatDurationPrecise(cutDurationMs),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        val newE = (endMs - 1000L).coerceAtLeast((startMs + 100L).coerceAtMost(totalDurationMs))
                                        onRangeChanged(startMs, newE)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = VideoProcessor.formatDurationPrecise(endMs),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { showEndPicker = true }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        val newE = (endMs + 1000L).coerceAtMost(totalDurationMs)
                                        onRangeChanged(startMs, newE)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Integrated Filmstrip Frame Row with RangeSlider and Red Handles Overlaid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                    ) {
                        // Background Frame Thumbnails
                        if (timelineThumbnails.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                for (thumb in timelineThumbnails) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }

                        val maxDurationFloat = totalDurationMs.coerceAtLeast(1000L).toFloat()
                        val safeStartFloat = startMs.coerceIn(0L, (maxDurationFloat - 100f).toLong().coerceAtLeast(0L)).toFloat()
                        val safeEndFloat = endMs.coerceIn((safeStartFloat + 100f).toLong().coerceAtLeast(100L), maxDurationFloat.toLong()).toFloat()

                        val startRatio = (safeStartFloat / maxDurationFloat).coerceIn(0f, 1f)
                        val endRatio = (safeEndFloat / maxDurationFloat).coerceIn(0f, 1f)

                        Row(modifier = Modifier.fillMaxSize()) {
                            // Dimmed Left
                            if (startRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(startRatio.coerceAtLeast(0.001f))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                )
                            }

                            // Highlighted Selected Range
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight((endRatio - startRatio).coerceAtLeast(0.01f))
                                    .border(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                // Left Primary Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxHeight()
                                        .width(18.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Right Primary Handle
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .width(18.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Dimmed Right
                            if (endRatio < 1f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((1f - endRatio).coerceAtLeast(0.001f))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                )
                            }
                        }

                        // Range Slider directly over filmstrip container
                        RangeSlider(
                            value = safeStartFloat..safeEndFloat,
                            onValueChange = { range ->
                                val newStart = range.start.toLong()
                                val newEnd = range.endInclusive.toLong()
                                onRangeChanged(newStart, newEnd)
                            },
                            valueRange = 0f..maxDurationFloat,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

        // Processing Progress State
        Column {
            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${(processingProgress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearWavyProgressIndicator(
                    progress = { processingProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }

    // Precise Millisecond Input Dialog for Start Time
    if (showStartPicker) {
        PreciseTimePickerDialog(
            title = "Set Start Time",
            initialMs = startMs,
            maxMs = (endMs - 100L).coerceAtLeast(0L),
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                onRangeChanged(ms, endMs)
                seekToPreview(ms)
                showStartPicker = false
            }
        )
    }

    // Precise Millisecond Input Dialog for End Time
    if (showEndPicker) {
        PreciseTimePickerDialog(
            title = "Set End Time",
            initialMs = endMs,
            maxMs = totalDurationMs,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms ->
                onRangeChanged(startMs, ms)
                seekToPreview(ms)
                showEndPicker = false
            }
        )
    }
}

@Composable
private fun TimeStepperBox(
    title: String,
    timeString: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onClickTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onMinus),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onClickTime),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timeString, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onPlus),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PreciseTimePickerDialog(
    title: String,
    initialMs: Long,
    maxMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val totalSec = initialMs / 1000
    val initMin = (totalSec / 60).toString()
    val initSec = (totalSec % 60).toString()
    val initMs = (initialMs % 1000).toString()

    var minStr by remember { mutableStateOf(initMin) }
    var secStr by remember { mutableStateOf(initSec) }
    var msStr by remember { mutableStateOf(initMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter exact timestamp:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minStr,
                        onValueChange = { minStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = secStr,
                        onValueChange = { secStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = msStr,
                        onValueChange = { msStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Ms") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = minStr.toLongOrNull() ?: 0L
                    val s = secStr.toLongOrNull() ?: 0L
                    val ms = msStr.toLongOrNull() ?: 0L
                    val computedMs = ((m * 60 + s) * 1000 + ms).coerceIn(0L, maxMs.coerceAtLeast(0L))
                    onConfirm(computedMs)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
