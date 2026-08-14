package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.util.VideoProcessor
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    durationMs: Long = 0L,
    startMs: Long = 0L,
    endMs: Long = 0L,
    autoPlay: Boolean = true,
    heightDp: Int? = null,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
    rotationDegrees: Int = 0,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false
) {
    val context = LocalContext.current

    // Single ExoPlayer instance maintained across inline and fullscreen modes for seamless playback
    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = autoPlay
            repeatMode = Player.REPEAT_MODE_ALL
            if (startMs > 0) {
                seekTo(startMs)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayerContent(
                    exoPlayer = exoPlayer,
                    videoUri = videoUri,
                    durationMs = durationMs,
                    startMs = startMs,
                    endMs = endMs,
                    active = true,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    rotationDegrees = rotationDegrees,
                    flipHorizontal = flipHorizontal,
                    flipVertical = flipVertical,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        val boxModifier = if (heightDp != null && heightDp > 0) {
            modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        } else {
            modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        }

        Box(
            modifier = boxModifier
        ) {
            VideoPlayerContent(
                exoPlayer = exoPlayer,
                videoUri = videoUri,
                durationMs = durationMs,
                startMs = startMs,
                endMs = endMs,
                active = true,
                isFullscreen = false,
                onToggleFullscreen = { isFullscreen = true },
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerContent(
    exoPlayer: ExoPlayer,
    videoUri: Uri,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    active: Boolean,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    videoWidth: Int = 0,
    videoHeight: Int = 0,
    rotationDegrees: Int = 0,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPositionMs by remember { mutableLongStateOf(exoPlayer.currentPosition) }

    var detectedWidth by remember { mutableStateOf(videoWidth) }
    var detectedHeight by remember { mutableStateOf(videoHeight) }

    var isLandscapeRotated by remember { mutableStateOf(false) }

    // Auto-hide controls state
    var showControls by remember { mutableStateOf(true) }
    var userInteractionToken by remember { mutableIntStateOf(0) }

    fun resetAutoHider() {
        showControls = true
        userInteractionToken++
    }

    LaunchedEffect(showControls, isPlaying, userInteractionToken) {
        if (showControls && isPlaying) {
            delay(2500)
            showControls = false
        }
    }

    var isSeeking by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(exoPlayer.playbackState == Player.STATE_BUFFERING) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    detectedWidth = videoSize.width
                    detectedHeight = videoSize.height
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) resetAutoHider()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    isSeeking = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(active) {
        if (!active) {
            exoPlayer.pause()
            isPlaying = false
        }
    }

    val safeTotalDurationMs = if (exoPlayer.duration > 0) exoPlayer.duration else if (durationMs > 0) durationMs else 1000L
    val effectiveEndMs = if (endMs > startMs) endMs else safeTotalDurationMs

    var ignoreEndLoopUntilMs by remember { mutableLongStateOf(0L) }

    fun performManualSeek(targetMs: Long) {
        ignoreEndLoopUntilMs = System.currentTimeMillis() + 800L
        isSeeking = true
        currentPositionMs = targetMs
        exoPlayer.seekTo(targetMs)
    }

    var prevStartMs by remember { mutableStateOf(startMs) }
    var prevEndMs by remember { mutableStateOf(endMs) }

    LaunchedEffect(startMs) {
        if (startMs != prevStartMs) {
            prevStartMs = startMs
            performManualSeek(startMs)
        }
    }

    LaunchedEffect(endMs) {
        if (endMs != prevEndMs) {
            prevEndMs = endMs
            val target = endMs.coerceIn(startMs, safeTotalDurationMs)
            performManualSeek(target)
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(100)
            if (!isSeeking && exoPlayer.playbackState == Player.STATE_READY) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (effectiveEndMs > startMs && pos >= effectiveEndMs - 50L) {
                    if (System.currentTimeMillis() >= ignoreEndLoopUntilMs) {
                        performManualSeek(startMs)
                    }
                } else {
                    currentPositionMs = pos
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Box with ExoPlayer
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .clickable {
                    if (showControls) {
                        showControls = false
                    } else {
                        resetAutoHider()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val boxW = maxWidth.value
            val boxH = maxHeight.value

            val effectiveRotation = ((rotationDegrees + if (isLandscapeRotated) 90 else 0) % 360 + 360) % 360

            val vW = if (detectedWidth > 0) detectedWidth.toFloat() else if (videoWidth > 0) videoWidth.toFloat() else 16f
            val vH = if (detectedHeight > 0) detectedHeight.toFloat() else if (videoHeight > 0) videoHeight.toFloat() else 9f
            val videoAspect = vW / vH

            // Base aspect-fit size within container (unrotated)
            val containerAspect = if (boxH > 0) boxW / boxH else 1f
            val (fitW, fitH) = if (videoAspect > containerAspect) {
                Pair(boxW, if (videoAspect > 0) boxW / videoAspect else boxH)
            } else {
                Pair(boxH * videoAspect, boxH)
            }

            val isRotated90 = (effectiveRotation % 180 != 0)
            // Scale multiplier to fit container when rotated 90/270 degrees
            val rotScale = if (isRotated90 && fitW > 0f && fitH > 0f) {
                minOf(boxW / fitH, boxH / fitW)
            } else {
                1f
            }

            val scaleFlipX = (if (flipHorizontal) -1f else 1f) * rotScale
            val scaleFlipY = (if (flipVertical) -1f else 1f) * rotScale

            Box(
                modifier = Modifier
                    .size(fitW.dp, fitH.dp)
                    .graphicsLayer {
                        rotationZ = effectiveRotation.toFloat()
                        scaleX = scaleFlipX
                        scaleY = scaleFlipY
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.view.TextureView(ctx).apply {
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                    update = { _ -> },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Centered Playback Controls overlay (SS 4 styled, Auto-hiding)
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1. Skip to Start
                        IconButton(
                            onClick = {
                                performManualSeek(startMs)
                                resetAutoHider()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Skip to Start",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // 2. Replay 10s
                        IconButton(
                            onClick = {
                                val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(startMs)
                                performManualSeek(target)
                                resetAutoHider()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 3. Central Play/Pause Button (Clean White Circle)
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        isPlaying = true
                                    }
                                    resetAutoHider()
                                },
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // 4. Forward 10s
                        IconButton(
                            onClick = {
                                val maxLimit = if (endMs > startMs) endMs else safeTotalDurationMs
                                val target = (exoPlayer.currentPosition + 10000L).coerceAtMost(maxLimit)
                                performManualSeek(target)
                                resetAutoHider()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // 5. Skip to End
                        IconButton(
                            onClick = {
                                val maxLimit = if (endMs > startMs) endMs else safeTotalDurationMs
                                performManualSeek(maxLimit)
                                resetAutoHider()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip to End",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Separate Bottom Bar for Timestamp, Rotation & Fullscreen controls (Auto-hiding)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141414)
            ) {
                Column {
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, safeTotalDurationMs.toFloat()),
                        onValueChange = { newPos ->
                            val target = newPos.toLong()
                            performManualSeek(target)
                            resetAutoHider()
                        },
                        valueRange = 0f..safeTotalDurationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${VideoProcessor.formatDuration(currentPositionMs)}  •  ${VideoProcessor.formatDuration(safeTotalDurationMs)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Fullscreen Toggle Button
                            IconButton(
                                onClick = {
                                    onToggleFullscreen()
                                    resetAutoHider()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

