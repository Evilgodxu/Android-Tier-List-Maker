package com.tdds.jh.screens.tierlist.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors
import kotlin.math.roundToInt

@Composable
fun ProgramSettingsDialog(
    onDismiss: () -> Unit,
    disableClickAdd: Boolean,
    onToggleDisableClickAdd: (Boolean) -> Unit,
    floatOffsetX: Float,
    onFloatOffsetXChange: (Float) -> Unit,
    floatOffsetY: Float,
    onFloatOffsetYChange: (Float) -> Unit,
    externalBadgeEnabled: Boolean,
    onToggleExternalBadge: (Boolean) -> Unit,
    followSystemTheme: Boolean,
    onToggleFollowSystemTheme: (Boolean) -> Unit,
    onShowLanguageDialog: () -> Unit,
    disableCustomFont: Boolean,
    onToggleDisableCustomFont: (Boolean) -> Unit,
    nameBelowImage: Boolean,
    onToggleNameBelowImage: (Boolean) -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight().heightIn(max = 580.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.program_settings), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 20.dp))
                SettingsClickableItem(text = stringResource(R.string.change_language), onClick = onShowLanguageDialog)
                SettingsSwitchItem(text = stringResource(R.string.follow_system_theme), checked = followSystemTheme, onCheckedChange = onToggleFollowSystemTheme)
                SettingsSwitchItem(text = stringResource(R.string.disable_custom_font), checked = disableCustomFont, onCheckedChange = onToggleDisableCustomFont)
                SettingsSwitchItem(text = stringResource(R.string.disable_click_add), checked = disableClickAdd, onCheckedChange = onToggleDisableClickAdd)
                SettingsSwitchItem(text = stringResource(R.string.external_badge), checked = externalBadgeEnabled, onCheckedChange = onToggleExternalBadge)
                SettingsSwitchItem(text = stringResource(R.string.name_below_image), checked = nameBelowImage, onCheckedChange = onToggleNameBelowImage)
                val extColors2 = LocalExtendedColors.current
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = extColors2.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(text = stringResource(R.string.adjust_float_display), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center)
                        SettingsSliderItem(label = stringResource(R.string.horizontal_offset), value = floatOffsetX, onValueChange = onFloatOffsetXChange, valueRange = 0f..300f, unit = "dp")
                        SettingsSliderItem(label = stringResource(R.string.vertical_offset), value = floatOffsetY, onValueChange = onFloatOffsetYChange, valueRange = 0f..150f, unit = "dp")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val extendedColors = LocalExtendedColors.current
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp), maxLines = 2, softWrap = true)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = extendedColors.accentColor, checkedTrackColor = extendedColors.accentColor.copy(alpha = 0.5f), uncheckedThumbColor = MaterialTheme.colorScheme.outline, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun SettingsClickableItem(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, softWrap = true)
    }
}

@Composable
private fun SettingsSliderItem(label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, unit: String) {
    val extendedColors = LocalExtendedColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "${value.toInt()} $unit", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        Slider(value = value, onValueChange = { val sv = (it / 5f).roundToInt() * 5f; onValueChange(sv) }, valueRange = valueRange, modifier = Modifier.padding(horizontal = 0.dp), colors = SliderDefaults.colors(thumbColor = extendedColors.accentColor, activeTrackColor = extendedColors.accentColor, inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
fun LanguageSelectionDialog(currentLanguage: String, onDismiss: () -> Unit, onLanguageSelected: (String) -> Unit) {
    val languages = listOf(
        "zh" to stringResource(R.string.language_zh),
        "en" to "English",
        "ja" to "日本語",
        "ko" to "한국어",
        "ru" to "Русский",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "ar" to "العربية",
        "pt" to "Português"
    )
    val extendedColors = LocalExtendedColors.current
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 500.dp), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)) {
            Column(modifier = Modifier.padding(vertical = 16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.select_language), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                languages.forEach { (code, name) ->
                    val isSelected = code == currentLanguage
                    Box(modifier = Modifier.fillMaxWidth().clickable { onLanguageSelected(code); onDismiss() }.padding(horizontal = 16.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(text = name, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(text = name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
