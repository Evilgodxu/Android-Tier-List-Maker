package com.tdds.jh.screens.tierlist.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tdds.jh.R
import com.tdds.jh.ui.theme.LocalExtendedColors

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val currentHsv = remember(currentColor) { rgbToHsv(currentColor) }
    var hue by remember { mutableFloatStateOf(currentHsv[0]) }
    var saturation by remember { mutableFloatStateOf(currentHsv[1]) }
    var value by remember { mutableFloatStateOf(currentHsv[2]) }
    val selectedColor = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value)
    }
    var hexInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val currentHex = remember(selectedColor) {
        String.format("%06X", selectedColor.toArgb() and 0xFFFFFF)
    }
    val extendedColors = LocalExtendedColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.select_color), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val width = maxWidth; val height = maxHeight
                    Box(modifier = Modifier.fillMaxSize().drawBehind {
                        val hueColor = hsvToColor(hue, 1f, 1f)
                        drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.White, hueColor), startX = 0f, endX = size.width))
                        drawRect(brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black), startY = 0f, endY = size.height))
                    }.pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val x = change.position.x.coerceIn(0f, size.width.toFloat())
                            val y = change.position.y.coerceIn(0f, size.height.toFloat())
                            saturation = (x / size.width).coerceIn(0f, 1f)
                            value = (1f - (y / size.height)).coerceIn(0f, 1f)
                        }
                    }) {
                        val indicatorX = saturation * width.value; val indicatorY = (1f - value) * height.value
                        Box(modifier = Modifier.offset(x = indicatorX.dp - 8.dp, y = indicatorY.dp - 8.dp).size(16.dp).border(2.dp, Color.White, RoundedCornerShape(50)).border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50)))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 色相滑块
                val rainbowColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    val width = maxWidth
                    Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(brush = Brush.horizontalGradient(colors = rainbowColors), size = Size(size.width, size.height)) }.pointerInput(Unit) {
                        detectDragGestures { change, _ -> change.consume(); val x = change.position.x.coerceIn(0f, size.width.toFloat()); hue = ((x / size.width) * 360f).coerceIn(0f, 360f) }
                    }) {
                        val tb = (hue / 360f) * width.value
                        Box(modifier = Modifier.offset(x = tb.dp - 12.dp).width(24.dp).fillMaxHeight().border(2.dp, Color.White, RoundedCornerShape(12.dp)).border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 亮度滑块
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    val width = maxWidth; val startColor = hsvToColor(hue, saturation, 0f); val endColor = hsvToColor(hue, saturation, 1f)
                    Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(brush = Brush.horizontalGradient(colors = listOf(startColor, endColor)), size = Size(size.width, size.height)) }.pointerInput(hue, saturation) {
                        detectDragGestures { change, _ -> change.consume(); val x = change.position.x.coerceIn(0f, size.width.toFloat()); value = (x / size.width).coerceIn(0f, 1f) }
                    }) {
                        val tb = value * width.value
                        Box(modifier = Modifier.offset(x = tb.dp - 12.dp).width(24.dp).fillMaxHeight().border(2.dp, Color.White, RoundedCornerShape(12.dp)).border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    val inputBg = if (hexInput.isNotEmpty()) { try { val padded = hexInput.padEnd(6, '0'); Color(android.graphics.Color.parseColor("#$padded")) } catch (_: Exception) { selectedColor } } else selectedColor
                    val isInputDark = inputBg.red * 0.299f + inputBg.green * 0.587f + inputBg.blue * 0.114f < 0.5f
                    val inputTextColor = if (isInputDark) Color.White else Color.Black
                    Row(modifier = Modifier.width(140.dp).height(40.dp).background(inputBg, RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "#", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = inputTextColor, modifier = Modifier.padding(end = 4.dp))
                        BasicTextField(value = hexInput, onValueChange = { newValue ->
                            val filtered = newValue.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6); hexInput = filtered
                            if (filtered.length == 6) { try { val newColor = Color(android.graphics.Color.parseColor("#$filtered")); val newHsv = rgbToHsv(newColor); hue = newHsv[0]; saturation = newHsv[1]; value = newHsv[2] } catch (_: Exception) {} }
                        }, singleLine = true, textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = inputTextColor, textAlign = TextAlign.Start),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (hexInput.length == 6 || hexInput.isEmpty()) onConfirm(selectedColor) else showError = true }),
                            decorationBox = { innerTextField -> Box { if (hexInput.isEmpty()) Text(text = currentHex, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = inputTextColor.copy(alpha = 0.5f)); innerTextField() } },
                            modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val isDark = selectedColor.red * 0.299f + selectedColor.green * 0.587f + selectedColor.blue * 0.114f < 0.5f
                    Box(modifier = Modifier.width(40.dp).height(40.dp).background(selectedColor, RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { if (hexInput.length == 6 || hexInput.isEmpty()) onConfirm(selectedColor) else showError = true }, contentAlignment = Alignment.Center) {
                        Text("✓", color = if (isDark) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (showError) { Spacer(modifier = Modifier.height(8.dp)); Text(text = stringResource(R.string.color_code_length_error), fontSize = 14.sp, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun rgbToHsv(color: Color): FloatArray {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
    val v = max; val s = if (max == 0f) 0f else delta / max
    val h = when { delta == 0f -> 0f; max == r -> ((g - b) / delta) % 6; max == g -> ((b - r) / delta) + 2; else -> ((r - g) / delta) + 4 } * 60f
    return floatArrayOf(if (h < 0) h + 360 else h, s, v)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s; val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1)); val m = v - c
    val (r, g, b) = when { h < 60 -> Triple(c, x, 0f); h < 120 -> Triple(x, c, 0f); h < 180 -> Triple(0f, c, x); h < 240 -> Triple(0f, x, c); h < 300 -> Triple(x, 0f, c); else -> Triple(c, 0f, x) }
    return Color(red = r + m, green = g + m, blue = b + m)
}
