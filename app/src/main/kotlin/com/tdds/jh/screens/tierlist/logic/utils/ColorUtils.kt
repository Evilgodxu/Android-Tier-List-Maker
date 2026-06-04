package com.tdds.jh.screens.tierlist.logic.utils

import androidx.compose.ui.graphics.Color

/**
 * 颜色工具类
 * 提供颜色计算、生成等纯工具函数
 */
object ColorUtils {

    /**
     * 判断颜色是否为深色
     * 基于亮度公式: 0.299*R + 0.587*G + 0.114*B
     * @param color 待判断颜色
     * @return 是否为深色(亮度 < 0.5)
     */
    fun isDarkColor(color: Color): Boolean {
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        return luminance < 0.5f
    }

    /**
     * 生成随机颜色
     * 从预设的12种颜色中随机选择
     * @return 随机颜色
     */
    fun generateRandomColor(): Color {
        val colors = listOf(
            Color(0xFFFF6B6B), Color(0xFFFFB347), Color(0xFFFFFACD),
            Color(0xFFB8E6B8), Color(0xFF87CEEB), Color(0xFFDDA0DD),
            Color(0xFFFF69B4), Color(0xFF98D8C8), Color(0xFFF7DC6F),
            Color(0xFFBB8FCE), Color(0xFF85C1E9), Color(0xFFF8B739)
        )
        return colors.random()
    }

    /**
     * 生成下一个层级标签
     * 从A开始,跳过已存在的标签
     * @param existingLabels 现有标签列表
     * @return 下一个可用标签
     */
    fun generateNextLabel(existingLabels: List<String>): String {
        var char = 'A'
        while (existingLabels.contains(char.toString())) {
            char++
        }
        return char.toString()
    }
}
