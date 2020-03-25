package com.example.imageprocess

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val UPDATE_IMAGE_VIEW = 1000
    }

    private lateinit var imageView: ImageView
    private lateinit var saturationBtn: Button
    private lateinit var resetBtn: Button

    private var workThread: HandlerThread? = null
    private var handler: Handler? = null

    private var originBitmapRgba: IntArray? = null
    private var tmpBitmapPixels: IntArray? = null
    private var bitmapWidth = -1
    private var bitmapHeight = -1

    private var working = false

    private val mainHandler = MainHandler()
    private class MainHandler: Handler(Looper.getMainLooper()) {

        private var weakRef: WeakReference<ImageView?>? = null
        fun setImageView(imageView:ImageView?) {
            weakRef = WeakReference(imageView)
        }

        override fun dispatchMessage(msg: Message) {
            when(msg.what) {
                UPDATE_IMAGE_VIEW -> {
                    val imageView = weakRef?.get()
                    imageView?.setImageBitmap(msg.obj as Bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        saturationBtn = findViewById(R.id.saturationBtn)
        resetBtn = findViewById(R.id.resetBtn)
        setupActionListener()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.setImageView(imageView)
        workThread = HandlerThread("work-thread")
        workThread!!.start()
        handler = Handler(workThread!!.looper)
        fetchOriginBitmapRgbaData()
    }

    override fun onPause() {
        super.onPause()
        handler?.removeCallbacksAndMessages(null)
        workThread?.quitSafely()
        workThread = null
        handler = null
        mainHandler.setImageView(null)
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun fetchOriginBitmapRgbaData() {
        handler?.post {
            if (originBitmapRgba == null) {
                val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                bitmapWidth = bitmap.width
                bitmapHeight = bitmap.height
                val buffer = ByteBuffer.allocate(bitmap.byteCount).order(ByteOrder.nativeOrder())
                bitmap.copyPixelsToBuffer(buffer)
                val rgba = buffer.array()
                originBitmapRgba = IntArray(rgba.size)
                val count = rgba.size / 4
                for (i in 0 until count) {
                    originBitmapRgba!![i * 4] = rgba[i * 4].toInt() and 0xff           // R
                    originBitmapRgba!![i * 4 + 1] = rgba[i * 4 + 1].toInt() and 0xff   // G
                    originBitmapRgba!![i * 4 + 2] = rgba[i * 4 + 2].toInt() and 0xff   // B
                    originBitmapRgba!![i * 4 + 3] = rgba[i * 4 + 3].toInt() and 0xff   // A
                }
            }
        }
    }

    private fun setupActionListener() {
        resetBtn.setOnClickListener {
            if (tmpBitmapPixels == null) {
                Log.w(TAG, "setupActionListener: not modify origin bitmap")
                return@setOnClickListener
            }

            val count = originBitmapRgba!!.size / 4
            for (i in 0 until count) {
                val r = originBitmapRgba!![i * 4]
                val g = originBitmapRgba!![i * 4 + 1]
                val b = originBitmapRgba!![i * 4 + 2]
                val newColor = Color.rgb(r, g, b)
                tmpBitmapPixels!![i] = newColor
            }
            val bitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
            mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
        }

        saturationBtn.setOnClickListener {
            if (working) {
                Log.d(TAG, "setupActionListener: working now")
                return@setOnClickListener
            }

            handler?.post {
                working = true
                val count = originBitmapRgba!!.size / 4
                val hsl = FloatArray(3)
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4) // rgb
                }
                for (i in 0 until count) {
                    val r = originBitmapRgba!![i * 4]
                    val g = originBitmapRgba!![i * 4 + 1]
                    val b = originBitmapRgba!![i * 4 + 2]
                    ColorUtils.RGBToHSL(r, g, b, hsl)
                    hsl[0] = hsl[0] + 127f * 0.5f
                    val newColor = ColorUtils.HSLToColor(hsl)
                    tmpBitmapPixels!![i] = newColor
                }

                val bitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                        0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
                mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
                working = false
            }
        }
    }
}