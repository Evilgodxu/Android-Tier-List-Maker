package com.tdds.jh.ui.toast

import android.content.Context
import android.widget.Toast

/**
 * 显示Toast提示
 */
fun showToastWithoutIcon(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}
