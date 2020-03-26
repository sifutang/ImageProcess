package com.example.imageprocess.utils

object Util {
    fun clamp(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else if (amount > high) high else amount
    }
}