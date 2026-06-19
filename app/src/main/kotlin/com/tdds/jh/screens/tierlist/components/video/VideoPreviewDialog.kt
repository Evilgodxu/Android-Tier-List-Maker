package com.tdds.jh.screens.tierlist.components.video

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.video.AudioDurationProvider
import com.tdds.jh.data.tierlist.video.AudioMixer
import com.tdds.jh.data.tierlist.video.BitmapFrameRenderer
import com.tdds.jh.model.tierlist.TierImage
import com.tdds.jh.model.tierlist.TierItem
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.frame.FrameSequenceGenerator
import com.tdds.jh.model.tierlist.video.frame.VideoFrame
import com.tdds.jh.model.tierlist.video.timeline.TimelineBuilder
import com.tdds.jh.ui.theme.LocalExtendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频预览对话框
 */
@Composable
fun VideoPreviewDialog(
    tiers: List<TierItem>,
    tierImages: List<TierImage>,
    config: VideoGenerationConfig,
    isDarkTheme: Boolean,
    disableCustomFont: Boolean,
    externalBadgeEnabled: Boolean,
    nameBelowImage: Boolean,
    title: String,
    authorName: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var keyFrames by remember { mutableStateOf<List<VideoFrame>>(emptyList()) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val previewConfig = remember(config) {
        config.copy(
            outputWidth = 854,
            outputHeight = 480
        )
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val timeline = TimelineBuilder(
                    previewConfig,
                    AudioDurationProvider(context)
                ).build(tiers, tierImages)

                val frames = FrameSequenceGenerator().generateKeyFrames(timeline, previewConfig)
                val renderer = BitmapFrameRenderer(context)
                val renderedFrames = frames.mapIndexed { index, frame ->
                    val bitmap = renderer.renderFrame(
                        tiers = tiers,
                        tierImages = tierImages,
                        config = previewConfig,
                        timeline = timeline,
                        currentTime = frame.timeSeconds,
                        isDarkTheme = isDarkTheme,
                        disableCustomFont = disableCustomFont,
                        externalBadgeEnabled = externalBadgeEnabled,
                        nameBelowImage = nameBelowImage,
                        title = title,
                        authorName = authorName
                    )
                    frame.bitmap = bitmap
                    loadingProgress = 0.6f * (index + 1) / frames.size
                    frame
                }

                val audioFile = File(context.cacheDir, "preview_audio_${System.currentTimeMillis()}.mp4")
                val mixed = AudioMixer(context).mix(
                    timeline = timeline,
                    config = previewConfig,
                    outputFile = audioFile,
                    progressCallback = { progress ->
                        loadingProgress = 0.6f + 0.3f * progress
                    }
                )

                if (mixed) {
                    audioUri = Uri.fromFile(audioFile)
                }

                keyFrames = renderedFrames
                isLoading = false
                loadingProgress = 1f
            } catch (e: Exception) {
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            keyFrames.forEach { it.bitmap?.recycle() }
            audioUri?.path?.let { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) {
                }
            }
        }
    }

    LaunchedEffect(audioUri) {
        if (audioUri == null) return@LaunchedEffect
        val player = MediaPlayer.create(context, audioUri)
        mediaPlayer = player
        player?.setOnCompletionListener {
            isPlaying = false
            currentFrameIndex = 0
        }
        if (isPlaying) player?.start()

        while (isActive) {
            if (isPlaying && player?.isPlaying == false) {
                player.start()
            } else if (!isPlaying && player?.isPlaying == true) {
                player.pause()
            }
            if (isPlaying && player != null) {
                val positionSec = player.currentPosition / 1000f
                val index = keyFrames.indexOfLast { it.timeSeconds <= positionSec }
                    .coerceAtLeast(0)
                    .coerceAtMost(keyFrames.size - 1)
                currentFrameIndex = index
            }
            delay(33)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.video_preview),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(androidx.compose.ui.graphics.Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = keyFrames.getOrNull(currentFrameIndex)?.bitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = errorMessage ?: stringResource(R.string.no_preview),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.preview_loading, (loadingProgress * 100).toInt()),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            enabled = audioUri != null
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${currentFrameIndex + 1} / ${keyFrames.size}",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

            }
        }
    }
}
