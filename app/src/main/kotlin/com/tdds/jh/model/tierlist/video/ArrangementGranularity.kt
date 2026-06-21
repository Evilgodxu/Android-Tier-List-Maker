package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 图片播放顺序模式
 */
enum class ArrangementGranularity(@StringRes val labelRes: Int) {
    /** 按图片顺序：按 tierImages 列表顺序逐张播放 */
    PER_IMAGE(R.string.granularity_per_image),

    /** 按层级顺序：按层级分组，每层内的图片按 tierImages 顺序播放 */
    PER_TYPE(R.string.granularity_per_type)
}
