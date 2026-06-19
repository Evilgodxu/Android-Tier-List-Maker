package com.tdds.jh.screens.tierlist.components.video

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.data.tierlist.video.VideoPresetManager
import com.tdds.jh.model.tierlist.video.VideoGenerationConfig
import com.tdds.jh.model.tierlist.video.VideoPreset
import com.tdds.jh.ui.theme.LocalExtendedColors
import com.tdds.jh.ui.toast.showToastWithoutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 编排预设选择对话框
 */
@Composable
fun VideoPresetPickerDialog(
    currentConfig: VideoGenerationConfig,
    onDismiss: () -> Unit,
    onLoadPreset: (VideoGenerationConfig) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val scope = rememberCoroutineScope()
    val manager = remember { VideoPresetManager(context) }

    var presets by remember { mutableStateOf<List<VideoPreset>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetToRename by remember { mutableStateOf<VideoPreset?>(null) }
    var presetToExport by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val preset = manager.import(uri)
                withContext(Dispatchers.Main) {
                    if (preset != null) {
                        presets = manager.list()
                        showToastWithoutIcon(context, context.getString(R.string.preset_imported))
                    } else {
                        showToastWithoutIcon(context, context.getString(R.string.preset_import_failed, ""))
                    }
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val id = presetToExport ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val success = manager.export(id, uri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        showToastWithoutIcon(context, context.getString(R.string.preset_export_success))
                    } else {
                        showToastWithoutIcon(context, context.getString(R.string.preset_export_failed, ""))
                    }
                    presetToExport = null
                }
            }
        } else {
            presetToExport = null
        }
    }

    LaunchedEffect(presetToExport) {
        if (presetToExport != null) {
            exportLauncher.launch("$presetToExport${VideoPresetManager.EXTENSION}")
        }
    }

    LaunchedEffect(Unit) {
        presets = withContext(Dispatchers.IO) { manager.list() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_preset_manager),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showSaveDialog = true }) {
                        Text(stringResource(R.string.save_preset))
                    }
                    TextButton(onClick = { importLauncher.launch("*/*") }) {
                        Text(stringResource(R.string.import_preset))
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp)
                ) {
                    if (presets.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_presets),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(presets, key = { it.id }) { preset ->
                            PresetItem(
                                preset = preset,
                                onLoad = {
                                    onLoadPreset(preset.config)
                                    onDismiss()
                                },
                                onRename = { presetToRename = preset },
                                onToggleFavorite = {
                                    scope.launch(Dispatchers.IO) {
                                        manager.toggleFavorite(preset.id)
                                        presets = manager.list()
                                    }
                                },
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        manager.delete(preset.id)
                                        presets = manager.list()
                                    }
                                },
                                onExport = { presetToExport = preset.id }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveVideoPresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                scope.launch(Dispatchers.IO) {
                    val preset = VideoPreset(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        group = "",
                        isFavorite = false,
                        createTime = System.currentTimeMillis(),
                        config = currentConfig
                    )
                    manager.save(preset)
                    presets = manager.list()
                    withContext(Dispatchers.Main) {
                        showSaveDialog = false
                        showToastWithoutIcon(context, context.getString(R.string.preset_save_success))
                    }
                }
            }
        )
    }

    presetToRename?.let { preset ->
        SaveVideoPresetDialog(
            initialName = preset.name,
            onDismiss = { presetToRename = null },
            onSave = { newName ->
                scope.launch(Dispatchers.IO) {
                    manager.rename(preset.id, newName)
                    presets = manager.list()
                    withContext(Dispatchers.Main) { presetToRename = null }
                }
            }
        )
    }
}

@Composable
private fun PresetItem(
    preset: VideoPreset,
    onLoad: () -> Unit,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            if (preset.group.isNotBlank()) {
                Text(
                    text = preset.group,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (preset.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            }
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.export_preset)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

@Composable
private fun SaveVideoPresetDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_video_preset)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.preset_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (isValid) onSave(name) },
                enabled = isValid
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
