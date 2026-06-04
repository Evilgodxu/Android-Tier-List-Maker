package com.tdds.jh.screens.tierlist.logic.utils

import android.graphics.Paint

/**
 * 文本处理工具类
 * 提供文本截断、计算、检测等纯文本处理功能
 */
object TextUtils {

    /**
     * 截断层级标签名称
     * 限制6个中文或12个大写字母,直接截断不使用省略号
     * @param label 原始标签
     * @return 截断后的标签
     */
    fun truncateTierLabel(label: String): String {
        var charCount = 0
        var byteCount = 0
        val maxBytes = 12 // 大写字母按1个字节算,中文按2个字节算,支持6个中文

        for (char in label) {
            val charBytes = if (char.code in 0x4E00..0x9FFF) 2 else 1
            if (byteCount + charBytes > maxBytes) {
                break
            }
            byteCount += charBytes
            charCount++
        }

        return label.take(charCount)
    }

    /**
     * 计算层级标签文字需要的行数
     * 用于在生成图片时自适应调整层级高度
     * @param label 标签文字
     * @param labelWidth 标签宽度
     * @param textSize 文字大小
     * @return 需要的行数
     */
    fun calculateLabelLineCount(label: String, labelWidth: Float, textSize: Float = 72f): Int {
        val textPaint = Paint().apply {
            this.textSize = textSize
            isFakeBoldText = true
        }

        // 计算每行最大宽度（标签宽度的80%）
        val maxLineWidth = labelWidth * 0.8f

        // 将文字分行
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (char in label) {
            val testLine = currentLine + char
            val testWidth = textPaint.measureText(testLine)
            if (testWidth > maxLineWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = char.toString()
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines.size
    }

    /**
     * 判断是否包含中文字符
     * @param text 待检测文本
     * @return 是否包含中文
     */
    fun containsChinese(text: String): Boolean {
        return text.any { it.code in 0x4E00..0x9FFF }
    }

    /**
     * 编辑页面显示名称（中文不超过6个字，非中文不超过12个字符，使用省略号）
     * @param name 原始名称
     * @return 截断后的名称
     */
    fun truncateImageNameForEdit(name: String): String {
        return if (containsChinese(name)) {
            // 中文名称：不超过6个字
            if (name.length > 6) name.take(6) + "..." else name
        } else {
            // 非中文名称：不超过12个字符
            if (name.length > 12) name.take(12) + "..." else name
        }
    }

    /**
     * 导出时显示名称（中文不超过10个字，非中文不超过18个字符，不使用省略号）
     * @param name 原始名称
     * @return 截断后的名称
     */
    fun truncateImageName(name: String): String {
        return if (containsChinese(name)) {
            // 中文名称：不超过10个字
            name.take(10)
        } else {
            // 非中文名称：不超过18个字符
            name.take(18)
        }
    }
}
