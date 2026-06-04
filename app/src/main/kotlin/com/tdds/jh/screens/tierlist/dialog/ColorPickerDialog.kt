package com.tdds.jh.screens.tierlist.dialog

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

/**
 * HSV颜色选择器对话框
 * 使用双滑块设计,提供色相饱和度面板和亮度滑块
 *
 * @param currentColor 当前选中的颜色
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认选择回调，返回选中的颜色
 */
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    // 将当前颜色转换为HSV
    val currentHsv = remember(currentColor) { rgbToHsv(currentColor) }

    // 状态管理
    var hue by remember { mutableFloatStateOf(currentHsv[0]) }
    var saturation by remember { mutableFloatStateOf(currentHsv[1]) }
    var value by remember { mutableFloatStateOf(currentHsv[2]) }

    // 计算当前颜色
    val selectedColor = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value)
    }

    // 十六进制输入
    var hexInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val currentHex = remember(selectedColor) {
        String.format("%06X", selectedColor.toArgb() and 0xFFFFFF)
    }

    val extendedColors = LocalExtendedColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = extendedColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_color),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 色相饱和度选择面板
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    val width = maxWidth
                    val height = maxHeight

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                // 绘制饱和度/亮度渐变背景
                                val hueColor = hsvToColor(hue, 1f, 1f)

                                // 水平方向: 白色到色相色的渐变(饱和度)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, hueColor),
                                        startX = 0f,
                                        endX = size.width
                                    )
                                )

                                // 垂直方向: 透明到黑色的渐变(亮度)
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startY = 0f,
                                        endY = size.height
                                    )
                                )
                            }
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val y = change.position.y.coerceIn(0f, size.height.toFloat())

                                    val newSaturation = x / size.width
                                    val newValue = 1f - (y / size.height)

                                    saturation = newSaturation.coerceIn(0f, 1f)
                                    value = newValue.coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        // 绘制选择指示器
                        val indicatorX = saturation * width.value
                        val indicatorY = (1f - value) * height.value

                        Box(
                            modifier = Modifier
                                .offset(x = indicatorX.dp - 8.dp, y = indicatorY.dp - 8.dp)
                                .size(16.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(50))
                                .border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 色相滑块(彩虹条)
                val rainbowColors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val width = maxWidth

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.horizontalGradient(colors = rainbowColors),
                                    size = Size(size.width, size.height)
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val newHue = (x / size.width) * 360f
                                    hue = newHue.coerceIn(0f, 360f)
                                }
                            }
                    ) {
                        val thumbPosition = (hue / 360f) * width.value

                        Box(
                            modifier = Modifier
                                .offset(x = thumbPosition.dp - 12.dp)
                                .width(24.dp)
                                .fillMaxHeight()
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                .border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 亮度滑块
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val width = maxWidth
                    val startColor = hsvToColor(hue, saturation, 0f)
                    val endColor = hsvToColor(hue, saturation, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(startColor, endColor)
                                    ),
                                    size = Size(size.width, size.height)
                                )
                            }
                            .pointerInput(hue, saturation) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val x = change.position.x.coerceIn(0f, size.width.toFloat())
                                    val newValue = x / size.width
                                    value = newValue.coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        val thumbPosition = value * width.value

                        Box(
                            modifier = Modifier
                                .offset(x = thumbPosition.dp - 12.dp)
                                .width(24.dp)
                                .fillMaxHeight()
                                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                .border(4.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 颜色预览和输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 计算输入框背景色和文字色
                    // 优先使用输入的颜色,如果没有输入则使用滑条选择的颜色
                    val inputBackgroundColor = if (hexInput.isNotEmpty()) {
                        try {
                            // 将输入补齐为6位来预览颜色
                            val paddedHex = hexInput.padEnd(6, '0')
                            Color(android.graphics.Color.parseColor("#$paddedHex"))
                        } catch (_: Exception) {
                            selectedColor
                        }
                    } else {
                        selectedColor
                    }
                    val isInputDark = inputBackgroundColor.red * 0.299f +
                            inputBackgroundColor.green * 0.587f +
                            inputBackgroundColor.blue * 0.114f < 0.5f
                    val inputTextColor = if (isInputDark) Color.White else Color.Black

                    // 十六进制输入框
                    Row(
                        modifier = Modifier
                            .width(140.dp)
                            .height(40.dp)
                            .background(
                                inputBackgroundColor,
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = inputTextColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        BasicTextField(
                            value = hexInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                                hexInput = filtered
                                // 实时更新颜色
                                if (filtered.length == 6) {
                                    try {
                                        val newColor = Color(android.graphics.Color.parseColor("#$filtered"))
                                        val newHsv = rgbToHsv(newColor)
                                        hue = newHsv[0]
                                        saturation = newHsv[1]
                                        value = newHsv[2]
                                    } catch (_: Exception) {}
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = inputTextColor,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (hexInput.length == 6 || hexInput.isEmpty()) {
                                        onConfirm(selectedColor)
                                    } else {
                                        showError = true
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (hexInput.isEmpty()) {
                                        Text(
                                            text = currentHex,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = inputTextColor.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 颜色预览块(点击确认)
                    val isDarkColor = selectedColor.red * 0.299f + selectedColor.green * 0.587f + selectedColor.blue * 0.114f < 0.5f
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .background(selectedColor, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                if (hexInput.length == 6 || hexInput.isEmpty()) {
                                    onConfirm(selectedColor)
                                } else {
                                    showError = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✓",
                            color = if (isDarkColor) Color.White else Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 错误提示
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.color_code_length_error),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * RGB转HSV
 * @return FloatArray[0]=Hue(0-360), [1]=Saturation(0-1), [2]=Value(0-1)
 */
private fun rgbToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    // Value
    val v = max

    // Saturation
    val s = if (max == 0f) 0f else delta / max

    // Hue
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6
        max == g -> ((b - r) / delta) + 2
        else -> ((r - g) / delta) + 4
    } * 60f

    return floatArrayOf(if (h < 0) h + 360 else h, s, v)
}

/**
 * HSV转Color
 */
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
    val m = v - c

    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = r + m,
        green = g + m,
        blue = b + m
    )
}
