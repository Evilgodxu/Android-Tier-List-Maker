package com.tdds.jh.ui.toast

import android.content.Context
import android.widget.Toast

/**
 * Toast扩展函数
 * 提供便捷的原生Toast调用接口
 */

/**
 * 显示Toast提示
 * @param context 上下文
 * @param message 提示消息
 * @param duration 显示时长
 */
fun showToastWithoutIcon(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, message, duration).show()
}

/**
 * 显示成功Toast的扩展函数
 * @receiver Context对象
 * @param message 成功消息
 */
fun Context.showSuccessToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * 显示警告Toast的扩展函数
 * @receiver Context对象
 * @param message 警告消息
 */
fun Context.showWarningToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * 显示错误Toast的扩展函数
 * @receiver Context对象
 * @param message 错误消息
 */
fun Context.showErrorToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * 显示信息Toast的扩展函数
 * @receiver Context对象
 * @param message 信息消息
 */
fun Context.showInfoToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
