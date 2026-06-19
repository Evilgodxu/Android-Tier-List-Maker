package com.tdds.jh.model.tierlist.video

import androidx.annotation.StringRes
import com.tdds.jh.R

/**
 * 编排粒度模式
 */
enum class ArrangementGranularity(@StringRes val labelRes: Int) {
    PER_IMAGE(R.string.granularity_per_image),
    PER_TYPE(R.string.granularity_per_type),
    MIXED(R.string.granularity_mixed)
}

/**
 * 混合模式下每个动作类型的执行方式
 */
enum class GranularityMode(@StringRes val labelRes: Int) {
    PER_IMAGE(R.string.granularity_mode_per_image),
    BATCH(R.string.granularity_mode_batch)
}
