package com.example.imageprocess.utils

import android.graphics.Camera
import android.graphics.Matrix
import android.view.animation.Animation
import android.view.animation.Transformation

class RotateAnimation(
    private var centerX: Int,
    private var centerY: Int,
    private var fromDegree: Float,
    private var toDegree: Float,
    private var translateZ: Float
) : Animation() {

    private var currentDegree = 0f

    private var camera = Camera()
    private var matrix: Matrix? = null

    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        super.applyTransformation(interpolatedTime, t)
        currentDegree = (toDegree - fromDegree) * interpolatedTime + fromDegree
        matrix = t?.matrix

        camera.save()
        camera.translate(0f, 0f, translateZ * interpolatedTime)
        camera.rotateY(currentDegree)
        camera.getMatrix(matrix)
        camera.restore()

        matrix?.postTranslate(centerX.toFloat(), centerY.toFloat())
        matrix?.preTranslate(-centerX.toFloat(), -centerY.toFloat())
    }
}