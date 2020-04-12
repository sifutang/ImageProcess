package com.example.imageprocess.utils

import android.graphics.*

object ImageProcessHelper {

    fun adjustImage(bitmap: Bitmap, hue:Float, saturation: Float, lum:Float): Bitmap {
        val tmpBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tmpBitmap)

        val hueMatrix = ColorMatrix()
        hueMatrix.setRotate(0, hue)
        hueMatrix.setRotate(1, hue)
        hueMatrix.setRotate(2, hue)

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)

        val lumMatrix = ColorMatrix()
        lumMatrix.setScale(lum, lum, lum, 1f)

        val colorMatrix = ColorMatrix()
        colorMatrix.postConcat(hueMatrix)
        colorMatrix.postConcat(saturationMatrix)
        colorMatrix.postConcat(lumMatrix)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return tmpBitmap
    }
}