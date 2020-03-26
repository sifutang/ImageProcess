package com.example.imageprocess.utils

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Util {
    fun clamp(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else if (amount > high) high else amount
    }

    fun fetchRgbaFromBitmap(bitmap: Bitmap): IntArray {
        val buffer = ByteBuffer.allocate(bitmap.byteCount).order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(buffer)
        val rgbaArr = buffer.array()
        val rgba = IntArray(rgbaArr.size)
        val count = rgbaArr.size / 4
        for (i in 0 until count) {
            rgba[i * 4] = rgbaArr[i * 4].toInt() and 0xff           // R
            rgba[i * 4 + 1] = rgbaArr[i * 4 + 1].toInt() and 0xff   // G
            rgba[i * 4 + 2] = rgbaArr[i * 4 + 2].toInt() and 0xff   // B
            rgba[i * 4 + 3] = rgbaArr[i * 4 + 3].toInt() and 0xff   // A
        }
        return rgba
    }
}