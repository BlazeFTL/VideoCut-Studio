package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.VideoItem
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.delay

@Composable
fun FullscreenProcessingDialog(
    isProcessing: Boolean,
    progress: Float,
    statusMessage: String,
    processName: String = "",
    title: String = "0/1 Files Converted",
    videoItem: VideoItem? = null,
    onCancel: (() -> Unit)? = null
) {
    if (!isProcessing) return

    var startTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isProcessing) {
        startTimeMs = System.currentTimeMillis()
        while (isProcessing) {
            elapsedMs = System.currentTimeMillis() - startTimeMs
            delay(250)
        }
    }

    val safeProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 250),
        label = "processing_progress"
    )
    val percentageInt = (safeProgress * 100).toInt().coerceIn(0, 100)

    fun formatSecondsMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0L)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(java.util.Locale.US, "%02d:%02d", min, sec)
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "--"
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = processName.ifEmpty { "Video Processing" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = title.ifEmpty { "0/1 Files Converted" },
                    fontSize = 13.sp,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier.size(144.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentageInt%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimaryDark,
                            modifier = Modifier.testTag("processing_percentage_text")
                        )
                        Text(
                            text = formatSecondsMs(elapsedMs),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                val itemTitle = videoItem?.title ?: statusMessage.ifEmpty { "Video Processing..." }
                val itemSizeStr = formatFileSize(videoItem?.sizeBytes ?: 0L)
                val durationStr = if ((videoItem?.durationMs ?: 0L) > 0) {
                    "${formatSecondsMs(elapsedMs)} / ${formatSecondsMs(videoItem?.durationMs ?: 0L)}"
                } else {
                    formatSecondsMs(elapsedMs)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SurfaceBorderLight)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (videoItem?.thumbnail != null) {
                                    Image(
                                        bitmap = videoItem.thumbnail.asImageBitmap(),
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (videoItem?.uri != null) {
                                    AsyncImage(
                                        model = videoItem.uri,
                                        contentDescription = "Video Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = "Video icon",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = itemTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MP4",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(14.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MP4",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "$itemSizeStr • $durationStr • ($percentageInt%)",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                            }

                            if (onCancel != null) {
                                IconButton(
                                    onClick = onCancel,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("dialog_close_cancel_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Processing",
                                        tint = TextSecondaryMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (onCancel != null) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cancel_processing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancel Processing",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
