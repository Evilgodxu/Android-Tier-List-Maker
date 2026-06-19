package com.tdds.jh.data.tierlist.video

import android.graphics.Bitmap

/**
 * Bitmap 转 YUV420Planar 工具
 */
object BitmapToYuvConverter {

    /**
     * 将 ARGB Bitmap 转换为 YUV420Planar（I420）字节数组
     */
    fun argbToYuv420p(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val ySize = width * height
        val uvSize = ySize / 4
        val yuv = ByteArray(ySize + uvSize * 2)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uIndex = ySize
        var vIndex = ySize + uvSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = pixels[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val y = (0.257 * r + 0.504 * g + 0.098 * b + 16).toInt().coerceIn(0, 255)
                val u = (-0.148 * r - 0.291 * g + 0.439 * b + 128).toInt().coerceIn(0, 255)
                val v = (0.439 * r - 0.368 * g - 0.071 * b + 128).toInt().coerceIn(0, 255)

                yuv[yIndex++] = y.toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uIndex++] = u.toByte()
                    yuv[vIndex++] = v.toByte()
                }
            }
        }
        return yuv
    }
}
