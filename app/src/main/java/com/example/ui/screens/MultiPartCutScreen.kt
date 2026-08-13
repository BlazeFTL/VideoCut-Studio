package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CutSegment
import com.example.model.MultiPartCutMode
import com.example.model.VideoItem
import com.example.ui.components.FullscreenProcessingDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.util.VideoProcessor

@Composable
fun MultiPartCutScreen(
    selectedVideo: VideoItem?,
    timelineThumbnails: List<Bitmap>,
    mode: MultiPartCutMode,
    segments: List<CutSegment>,
    isProcessing: Boolean,
    processingProgress: Float,
    statusMessage: String,
    onSetMode: (MultiPartCutMode) -> Unit,
    onAddSegment: (startMs: Long, endMs: Long) -> Unit,
    onRemoveSegment: (id: String) -> Unit,
    onSelectVideoUri: (Uri) -> Unit,
    onProcessCut: () -> Unit,
    onPlayVideo: (Uri) -> Unit
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSelectVideoUri(uri)
    }

    val maxDurationMs = if (selectedVideo != null && selectedVideo.durationMs > 0L) selectedVideo.durationMs else 180_000L

    // Current segment builder state & collapse toggle
    var isAddSegmentExpanded by remember { mutableStateOf(false) }
    var builderStartMs by remember(selectedVideo?.uri, maxDurationMs) { mutableStateOf(0L) }
    var builderEndMs by remember(selectedVideo?.uri, maxDurationMs) { mutableStateOf(maxDurationMs) }

    if (builderEndMs <= builderStartMs) {
        builderEndMs = (builderStartMs + 5_000L).coerceAtMost(maxDurationMs)
    }

    // Calculate projected final output duration
    val calculatedOutputDurationMs = remember(segments, mode, maxDurationMs) {
        if (segments.isEmpty()) {
            maxDurationMs
        } else if (mode == MultiPartCutMode.REMOVE_SELECTED) {
            val totalRemovedMs = segments.sumOf { it.endMs - it.startMs }
            (maxDurationMs - totalRemovedMs).coerceAtLeast(0L)
        } else {
            segments.sumOf { it.endMs - it.startMs }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header Title
        item {
            Column {
                Text(
                    text = "Multi-Part Video Cutter",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Select exact video frames, cut multiple parts, and keep or remove them precisely.",
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Cutting Mode Selector (Remove vs Keep Strategy) - Matching SS 3
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Cutting Mode Strategy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onSetMode(MultiPartCutMode.REMOVE_SELECTED) },
                            color = if (mode == MultiPartCutMode.REMOVE_SELECTED) PrimaryContainerLight else Color.White,
                            border = BorderStroke(1.5.dp, if (mode == MultiPartCutMode.REMOVE_SELECTED) PrimaryIndigo else SurfaceBorderLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mode == MultiPartCutMode.REMOVE_SELECTED,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Remove Parts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (mode == MultiPartCutMode.REMOVE_SELECTED) PrimaryIndigo else TextPrimaryDark
                                    )
                                    Text(
                                        text = "Delete selected ranges",
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { onSetMode(MultiPartCutMode.KEEP_SELECTED) },
                            color = if (mode == MultiPartCutMode.KEEP_SELECTED) PrimaryContainerLight else Color.White,
                            border = BorderStroke(1.5.dp, if (mode == MultiPartCutMode.KEEP_SELECTED) PrimaryIndigo else SurfaceBorderLight),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mode == MultiPartCutMode.KEEP_SELECTED,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryIndigo)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Keep Parts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (mode == MultiPartCutMode.KEEP_SELECTED) PrimaryIndigo else TextPrimaryDark
                                    )
                                    Text(
                                        text = "Merge selected ranges",
                                        fontSize = 10.sp,
                                        color = TextSecondaryMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Frame Video Player & Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SurfaceBorderLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frame Precision Video Preview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryDark
                        )

                        Button(
                            onClick = {
                                pickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.VideoOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerLight),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Change Video", color = PrimaryIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedVideo != null) {
                        // Embedded Video Player with majority screen height
                        VideoPlayerView(
                            videoUri = selectedVideo.uri,
                            durationMs = maxDurationMs,
                            startMs = builderStartMs,
                            endMs = builderEndMs,
                            heightDp = 360,
                            videoWidth = selectedVideo.width,
                            videoHeight = selectedVideo.height
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Range Stepper Header & Action Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Select Segment Range",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondaryMuted
                                )
                                Text(
                                    text = "${VideoProcessor.formatDurationPrecise(builderStartMs)} - ${VideoProcessor.formatDurationPrecise(builderEndMs)} (${VideoProcessor.formatDurationPrecise((builderEndMs - builderStartMs).coerceAtLeast(0L))})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryIndigo
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { isAddSegmentExpanded = !isAddSegmentExpanded },
                                    border = BorderStroke(1.dp, PrimaryIndigo),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Fine-Tune +/-", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = {
                                        if (builderEndMs > builderStartMs) {
                                            onAddSegment(builderStartMs, builderEndMs)
                                            builderStartMs = 0L
                                            builderEndMs = maxDurationMs
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Segment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Filmstrip Range Selection Slider on Video Frame Preview
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            val totalWidthPx = constraints.maxWidth.toFloat()
                            val safeMaxMs = maxDurationMs.coerceAtLeast(1L).toFloat()
                            val startRatio = (builderStartMs.toFloat() / safeMaxMs).coerceIn(0f, 1f)
                            val endRatio = (builderEndMs.toFloat() / safeMaxMs).coerceIn(0f, 1f)

                            val activeColor = PrimaryIndigo

                            // 1. Filmstrip Thumbnails
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

                            // 2. Existing Added Segments
                            for (seg in segments) {
                                val segStartRatio = (seg.startMs.toFloat() / safeMaxMs).coerceIn(0f, 1f)
                                val segEndRatio = (seg.endMs.toFloat() / safeMaxMs).coerceIn(0f, 1f)
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (segStartRatio > 0f) {
                                        Spacer(modifier = Modifier.weight(segStartRatio.coerceAtLeast(0.0001f)))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight((segEndRatio - segStartRatio).coerceAtLeast(0.0001f))
                                            .background(PrimaryIndigo.copy(alpha = 0.6f))
                                            .border(1.dp, Color.White.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (mode == MultiPartCutMode.REMOVE_SELECTED) "CUT" else "KEEP",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (segEndRatio < 1f) {
                                        Spacer(modifier = Modifier.weight((1f - segEndRatio).coerceAtLeast(0.0001f)))
                                    }
                                }
                            }

                            // 3. Highlighted Selected Range & Dimmed Unselected Regions
                            val handleWidthDp = 18.dp
                            val handleWidthPx = with(LocalDensity.current) { handleWidthDp.toPx() }

                            val startPx = startRatio * totalWidthPx
                            val endPx = endRatio * totalWidthPx

                            // Dimmed Left
                            if (startPx > 0f) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(0, 0) }
                                        .width(with(LocalDensity.current) { startPx.toDp() })
                                        .fillMaxHeight()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                )
                            }

                            // Dimmed Right
                            if (endPx < totalWidthPx) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(endPx.toInt(), 0) }
                                        .width(with(LocalDensity.current) { (totalWidthPx - endPx).coerceAtLeast(0f).toDp() })
                                        .fillMaxHeight()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                )
                            }

                            // Selected Range Border
                            val selectionWidthPx = (endPx - startPx).coerceAtLeast(0f)
                            if (selectionWidthPx > 0f) {
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(startPx.toInt(), 0) }
                                        .width(with(LocalDensity.current) { selectionWidthPx.toDp() })
                                        .fillMaxHeight()
                                        .border(2.dp, activeColor)
                                )
                            }

                            // 4. Left Bracket Trim Handle [
                            val leftHandleOffsetPx = (startPx - handleWidthPx / 2f).coerceIn(0f, (totalWidthPx - handleWidthPx * 2f).coerceAtLeast(0f))
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(leftHandleOffsetPx.toInt(), 0) }
                                    .width(handleWidthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .background(activeColor)
                                    .pointerInput(maxDurationMs, builderEndMs, totalWidthPx) {
                                        detectHorizontalDragGestures { change, dragAmount ->
                                            change.consume()
                                            if (totalWidthPx > 0f) {
                                                val deltaRatio = dragAmount / totalWidthPx
                                                val deltaMs = (deltaRatio * safeMaxMs).toLong()
                                                builderStartMs = (builderStartMs + deltaMs).coerceIn(0L, (builderEndMs - 500L).coerceAtLeast(0L))
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                }
                            }

                            // 5. Right Bracket Trim Handle ]
                            val rightHandleOffsetPx = (endPx - handleWidthPx / 2f).coerceIn(handleWidthPx, (totalWidthPx - handleWidthPx).coerceAtLeast(handleWidthPx))
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(rightHandleOffsetPx.toInt(), 0) }
                                    .width(handleWidthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .background(activeColor)
                                    .pointerInput(maxDurationMs, builderStartMs, totalWidthPx) {
                                        detectHorizontalDragGestures { change, dragAmount ->
                                            change.consume()
                                            if (totalWidthPx > 0f) {
                                                val deltaRatio = dragAmount / totalWidthPx
                                                val deltaMs = (deltaRatio * safeMaxMs).toLong()
                                                builderEndMs = (builderEndMs + deltaMs).coerceIn(builderStartMs + 500L, maxDurationMs)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                    Box(modifier = Modifier.width(2.dp).height(18.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                }
                            }

                            val safeStartFloat = builderStartMs.coerceIn(0L, (safeMaxMs - 500f).toLong().coerceAtLeast(0L)).toFloat()
                            val safeEndFloat = builderEndMs.coerceIn((safeStartFloat + 500f).toLong().coerceAtMost(safeMaxMs.toLong()), safeMaxMs.toLong()).toFloat()

                            // 6. RangeSlider overlay for precise touch drag
                            RangeSlider(
                                value = safeStartFloat..safeEndFloat,
                                onValueChange = { range ->
                                    builderStartMs = range.start.toLong()
                                    builderEndMs = range.endInclusive.toLong()
                                },
                                valueRange = 0f..safeMaxMs,
                                colors = SliderDefaults.colors(
                                    thumbColor = activeColor,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (isAddSegmentExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // START TIME STEPPER BOX
                            TimeStepperBox(
                                label = "START TIME",
                                timeMs = builderStartMs,
                                containerBg = PrimaryContainerLight,
                                labelColor = PrimaryIndigo,
                                onMinus5s = { builderStartMs = (builderStartMs - 5_000L).coerceAtLeast(0L) },
                                onMinus1s = { builderStartMs = (builderStartMs - 1_000L).coerceAtLeast(0L) },
                                onMinus100ms = { builderStartMs = (builderStartMs - 100L).coerceAtLeast(0L) },
                                onPlus100ms = { builderStartMs = (builderStartMs + 100L).coerceAtMost(builderEndMs - 100L) },
                                onPlus1s = { builderStartMs = (builderStartMs + 1_000L).coerceAtMost(builderEndMs - 500L) },
                                onPlus5s = { builderStartMs = (builderStartMs + 5_000L).coerceAtMost(builderEndMs - 500L) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // END TIME STEPPER BOX
                            TimeStepperBox(
                                label = "END TIME",
                                timeMs = builderEndMs,
                                containerBg = PrimaryContainerLight,
                                labelColor = PrimaryIndigo,
                                onMinus5s = { builderEndMs = (builderEndMs - 5_000L).coerceAtLeast(builderStartMs + 500L) },
                                onMinus1s = { builderEndMs = (builderEndMs - 1_000L).coerceAtLeast(builderStartMs + 500L) },
                                onMinus100ms = { builderEndMs = (builderEndMs - 100L).coerceAtLeast(builderStartMs + 100L) },
                                onPlus100ms = { builderEndMs = (builderEndMs + 100L).coerceAtMost(maxDurationMs) },
                                onPlus1s = { builderEndMs = (builderEndMs + 1_000L).coerceAtMost(maxDurationMs) },
                                onPlus5s = { builderEndMs = (builderEndMs + 5_000L).coerceAtMost(maxDurationMs) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Video Specs Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedVideo.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Total: ${VideoProcessor.formatDurationPrecise(maxDurationMs)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                        }
                    } else {
                        // Empty State Video Picker
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Movie, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No video selected for multi-cut", fontSize = 14.sp, color = TextSecondaryMuted)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    pickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.VideoOnly
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
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
        }

        // Filmstrip Thumbnails & Cut Overlay Timeline Strip
        if (selectedVideo != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceBorderLight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Frame Timeline Strip Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Visual preview of all cut segments overlaid on video frames",
                            fontSize = 12.sp,
                            color = TextSecondaryMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Horizontal Filmstrip Frame Row
                        if (timelineThumbnails.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                timelineThumbnails.forEach { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Video Frame",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color.White.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Timeline Map Bar showing active segments overlaid
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            val totalWidthPx = constraints.maxWidth.toFloat()
                            if (maxDurationMs > 0 && totalWidthPx > 0) {
                                for (seg in segments) {
                                    val startRatio = (seg.startMs.toFloat() / maxDurationMs).coerceIn(0f, 1f)
                                    val endRatio = (seg.endMs.toFloat() / maxDurationMs).coerceIn(0f, 1f)
                                    val startPx = startRatio * totalWidthPx
                                    val segWidthPx = ((endRatio - startRatio) * totalWidthPx).coerceAtLeast(2f)

                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(startPx.toInt(), 0) }
                                            .width(with(LocalDensity.current) { segWidthPx.toDp() })
                                            .fillMaxHeight()
                                            .background(if (mode == MultiPartCutMode.REMOVE_SELECTED) RoseError else EmeraldSuccess)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("00:00.000", fontSize = 10.sp, color = TextSecondaryMuted)
                            Text("Active Segments: ${segments.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            Text(VideoProcessor.formatDuration(maxDurationMs), fontSize = 10.sp, color = TextSecondaryMuted)
                        }
                    }
                }
            }
        }

        // Selected Segments List Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added Cut Segments (${segments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryDark
                )

                if (selectedVideo != null && segments.isNotEmpty()) {
                    Text(
                        text = "Final Duration: ${VideoProcessor.formatDuration(calculatedOutputDurationMs)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }
        }

        // List of Added Segments
        if (segments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceBorderLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No segments added yet. Select range and tap '+ Add Segment' above.",
                            color = TextSecondaryMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(segments) { index, seg ->
                SegmentCardItem(
                    index = index + 1,
                    segment = seg,
                    mode = mode,
                    onDelete = { onRemoveSegment(seg.id) }
                )
            }
        }

        // Process Cut Video Action Section
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
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
                            color = PrimaryIndigo
                        )
                        Text(
                            text = "${(processingProgress * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                    LinearProgressIndicator(
                        progress = { processingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryIndigo,
                        trackColor = PrimaryContainerLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onProcessCut,
                    enabled = !isProcessing && selectedVideo != null && segments.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessing) "Processing Cut..." else "Process & Save Cut Video (${segments.size} Parts)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModePillButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) selectedColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) selectedColor else SurfaceBorderLight
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) selectedColor else TextPrimaryDark
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondaryMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TimeStepperBox(
    label: String,
    timeMs: Long,
    containerBg: Color,
    labelColor: Color,
    onMinus5s: () -> Unit,
    onMinus1s: () -> Unit,
    onMinus100ms: () -> Unit,
    onPlus100ms: () -> Unit,
    onPlus1s: () -> Unit,
    onPlus5s: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = labelColor
            )
            Text(
                text = VideoProcessor.formatDurationPrecise(timeMs),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TextPrimaryDark
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onMinus5s,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("-5s", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onMinus1s,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("-1s", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onMinus100ms,
                modifier = Modifier.weight(1.1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("-0.1s", fontSize = 10.sp) }

            OutlinedButton(
                onClick = onPlus100ms,
                modifier = Modifier.weight(1.1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("+0.1s", fontSize = 10.sp) }

            OutlinedButton(
                onClick = onPlus1s,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("+1s", fontSize = 11.sp) }

            OutlinedButton(
                onClick = onPlus5s,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) { Text("+5s", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun SegmentCardItem(
    index: Int,
    segment: CutSegment,
    mode: MultiPartCutMode,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (mode == MultiPartCutMode.REMOVE_SELECTED) RoseContainer else EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$index",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (mode == MultiPartCutMode.REMOVE_SELECTED) RoseError else EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${VideoProcessor.formatDurationPrecise(segment.startMs)}  ➔  ${VideoProcessor.formatDurationPrecise(segment.endMs)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                )

                val durSec = String.format(java.util.Locale.US, "%.1f", (segment.endMs - segment.startMs) / 1000.0)
                Text(
                    text = "${if (mode == MultiPartCutMode.REMOVE_SELECTED) "Remove" else "Keep"} $durSec seconds",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Segment", tint = RoseError)
            }
        }
    }
}
