package com.tdds.jh.model.tierlist

import androidx.compose.ui.graphics.Color

/**
 * 梯度表默认配置
 */
object TierListConfig {

    /**
     * 获取默认梯度模板
     * @param isChinese 是否为中文环境
     */
    fun getDefaultTiers(isChinese: Boolean): List<TierItem> {
        return if (isChinese) {
            // 中文本地化模板
            listOf(
                TierItem("夯", Color(0xFFFF6B6B)),
                TierItem("顶级", Color(0xFFFFB347)),
                TierItem("人上人", Color(0xFFFFFACD)),
                TierItem("NPC", Color(0xFFB8E6B8)),
                TierItem("拉完了", Color(0xFF87CEEB))
            )
        } else {
            // 其他语言使用标准模板
            listOf(
                TierItem("S", Color(0xFFFF6B6B)),
                TierItem("A", Color(0xFFFFB347)),
                TierItem("B", Color(0xFFFFFACD)),
                TierItem("C", Color(0xFFB8E6B8)),
                TierItem("D", Color(0xFF87CEEB))
            )
        }
    }

    // 默认标题（从字符串资源获取）
    // 实际使用时通过 context.getString(R.string.default_title) 获取
}
