package com.tdds.jh.screens.tierlist.components.video

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.model.tierlist.video.ArrangementGranularity
import com.tdds.jh.model.tierlist.video.AudioIntervalSource
import com.tdds.jh.model.tierlist.video.NameDisplayMode
import com.tdds.jh.model.tierlist.video.NarrationOrder
import com.tdds.jh.model.tierlist.video.VideoActionType
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.ui.theme.LocalExtendedColors

/**
 * 视频生成设置对话框
 */
@Composable
fun VideoGenerationConfigDialog(
    initialConfig: VideoGenerationConfig,
    onDismiss: () -> Unit,
    onConfigChange: (VideoGenerationConfig) -> Unit,
    onPreview: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    var config by remember(initialConfig) { mutableStateOf(initialConfig) }
    val extendedColors = LocalExtendedColors.current

    fun updateConfig(newConfig: VideoGenerationConfig) {
        config = newConfig
        onConfigChange(newConfig)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_generation_settings),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    ActionOrderSection(
                        actionOrder = config.actionOrder,
                        onMoveUp = { index ->
                            if (index > 0) {
                                val list = config.actionOrder.toMutableList()
                                list.add(index - 1, list.removeAt(index))
                                updateConfig(config.copy(actionOrder = list))
                            }
                        },
                        onMoveDown = { index ->
                            if (index < config.actionOrder.size - 1) {
                                val list = config.actionOrder.toMutableList()
                                list.add(index + 1, list.removeAt(index))
                                updateConfig(config.copy(actionOrder = list))
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    GranularitySection(
                        granularity = config.granularity,
                        onGranularityChange = { updateConfig(config.copy(granularity = it)) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    IntervalSection(
                        config = config,
                        onConfigChange = { updateConfig(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    AudioSection(
                        config = config,
                        onConfigChange = { updateConfig(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onPreview != null) {
                        TextButton(onClick = { onPreview.invoke() }) {
                            Text(stringResource(R.string.preview_video))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (onExport != null) {
                        TextButton(onClick = { onExport.invoke() }) {
                            Text(stringResource(R.string.export_video))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ActionOrderSection(
    actionOrder: List<VideoActionType>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    SectionTitle(stringResource(R.string.action_order))
    actionOrder.forEachIndexed { index, action ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${index + 1}. ${stringResource(action.labelRes)}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                IconButton(onClick = { onMoveUp(index) }, enabled = index > 0) {
                    Icon(Icons.Outlined.KeyboardArrowUp, stringResource(R.string.move_left))
                }
                IconButton(onClick = { onMoveDown(index) }, enabled = index < actionOrder.size - 1) {
                    Icon(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.move_right))
                }
            }
        }
    }
}

@Composable
private fun GranularitySection(
    granularity: ArrangementGranularity,
    onGranularityChange: (ArrangementGranularity) -> Unit
) {
    SectionTitle(stringResource(R.string.arrangement_granularity))
    ArrangementGranularity.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onGranularityChange(mode) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = granularity == mode, onClick = { onGranularityChange(mode) })
            Text(stringResource(mode.labelRes), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun IntervalSection(
    config: VideoGenerationConfig,
    onConfigChange: (VideoGenerationConfig) -> Unit
) {
    SectionTitle(stringResource(R.string.interval_settings))

    Text(stringResource(R.string.image_interval), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    AudioIntervalSource.entries.forEach { source ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onConfigChange(config.copy(imageIntervalSource = source)) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = config.imageIntervalSource == source, onClick = { onConfigChange(config.copy(imageIntervalSource = source)) })
            Text(stringResource(source.labelRes), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    if (config.imageIntervalSource == AudioIntervalSource.FIXED) {
        FloatSlider(
            label = stringResource(R.string.image_interval),
            value = config.fixedImageInterval,
            range = 0.1f..10f,
            onValueChange = { onConfigChange(config.copy(fixedImageInterval = it)) }
        )
    }

    FloatSlider(
        label = stringResource(R.string.badge_interval),
        value = config.badgeInterval,
        range = 0.1f..1f,
        onValueChange = { onConfigChange(config.copy(badgeInterval = it)) }
    )

    Text(stringResource(R.string.name_display), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
    NameDisplayMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onConfigChange(config.copy(nameDisplayMode = mode)) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = config.nameDisplayMode == mode, onClick = { onConfigChange(config.copy(nameDisplayMode = mode)) })
            Text(stringResource(mode.labelRes), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    if (config.nameDisplayMode == NameDisplayMode.PER_CHAR) {
        FloatSlider(
            label = stringResource(R.string.name_char_interval),
            value = config.nameCharInterval,
            range = 0.01f..1f,
            onValueChange = { onConfigChange(config.copy(nameCharInterval = it)) }
        )
    }

    FloatSlider(
        label = stringResource(R.string.cross_type_pause),
        value = config.crossTypePause,
        range = 0f..5f,
        onValueChange = { onConfigChange(config.copy(crossTypePause = it)) }
    )

    FloatSlider(
        label = stringResource(R.string.cross_image_pause),
        value = config.crossImagePause,
        range = 0f..5f,
        onValueChange = { onConfigChange(config.copy(crossImagePause = it)) }
    )

    if (config.imageIntervalSource == AudioIntervalSource.AUDIO_DURATION) {
        FloatSlider(
            label = stringResource(R.string.extra_audio_offset),
            value = config.extraAudioOffset,
            range = 0f..3f,
            onValueChange = { onConfigChange(config.copy(extraAudioOffset = it)) }
        )
    }
}

@Composable
private fun AudioSection(
    config: VideoGenerationConfig,
    onConfigChange: (VideoGenerationConfig) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            onConfigChange(config.copy(backgroundMusicUri = it.toString()))
        }
    }

    SectionTitle(stringResource(R.string.audio_settings))

    BackgroundMusicRow(
        uriString = config.backgroundMusicUri,
        onAdd = { musicPicker.launch("audio/*") },
        onReplace = { musicPicker.launch("audio/*") },
        onRemove = { onConfigChange(config.copy(backgroundMusicUri = null)) }
    )

    FloatSlider(
        label = stringResource(R.string.narration_volume),
        value = config.narrationVolume,
        range = 0f..1.5f,
        valueFormatter = { String.format("%.0f%%", it * 100) },
        onValueChange = { onConfigChange(config.copy(narrationVolume = it)) }
    )

    FloatSlider(
        label = stringResource(R.string.bgm_volume),
        value = config.backgroundMusicVolume,
        range = 0f..1.5f,
        valueFormatter = { String.format("%.0f%%", it * 100) },
        onValueChange = { onConfigChange(config.copy(backgroundMusicVolume = it)) }
    )

    Text(stringResource(R.string.narration_order), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
    NarrationOrder.entries.forEach { order ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onConfigChange(config.copy(narrationOrder = order)) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = config.narrationOrder == order, onClick = { onConfigChange(config.copy(narrationOrder = order)) })
            Text(stringResource(order.labelRes), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BackgroundMusicRow(
    uriString: String?,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uriString.isNullOrBlank()) {
            Text(stringResource(R.string.background_music), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onAdd) {
                Text(stringResource(R.string.add_background_music))
            }
        } else {
            val name = runCatching { Uri.parse(uriString).lastPathSegment?.substringAfterLast('/') }.getOrNull() ?: uriString
            Text(
                name,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            TextButton(onClick = onReplace) {
                Text(stringResource(R.string.replace))
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.remove))
            }
        }
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String = { String.format("%.1f s", it) },
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(valueFormatter(value), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = ((range.endInclusive - range.start) * 10).toInt() - 1
        )
    }
}
