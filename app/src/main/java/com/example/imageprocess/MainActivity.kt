package com.example.imageprocess

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.example.imageprocess.utils.ImageProcessHelper
import com.example.imageprocess.utils.RotateAnimation
import com.example.imageprocess.utils.Util
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val UPDATE_IMAGE_VIEW = 1000
        private const val SATURATION_RATIO = 1.5f
        private const val LIGHTNESS_RATIO = 1.5f
        private const val CONTRACT_RATIO = 2f
    }

    private lateinit var imageView: ImageView
    private lateinit var resetBtn: Button
    private lateinit var saturationBtn: Button
    private lateinit var lightnessBtn: Button
    private lateinit var contrastBtn: Button
    private lateinit var blurBtn: Button

    private var sourceBitmap: Bitmap? = null

    private var hue = 0f
    private var saturation = 0f
    private var lum = 0f

    private var workThread: HandlerThread? = null
    private var handler: Handler? = null

    private var originBitmapRgba: IntArray? = null
    private var tmpBitmapPixels: IntArray? = null
    private var bitmapWidth = -1
    private var bitmapHeight = -1

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
        resetBtn = findViewById(R.id.resetBtn)
        saturationBtn = findViewById(R.id.saturationBtn)
        lightnessBtn = findViewById(R.id.lightnessBtn)
        contrastBtn = findViewById(R.id.contrastBtn)
        blurBtn = findViewById(R.id.blurBtn)

        findViewById<SeekBar>(R.id.hue_seek_bar).setOnSeekBarChangeListener(this)
        findViewById<SeekBar>(R.id.saturation_seek_bar).setOnSeekBarChangeListener(this)
        findViewById<SeekBar>(R.id.lum_seek_bar).setOnSeekBarChangeListener(this)

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
                originBitmapRgba = Util.fetchRgbaFromBitmap(bitmap)
            }
        }
    }

    private fun setupActionListener() {
        imageView.setOnClickListener {
            val centerX = imageView.width / 2
            val centerY = imageView.height / 2
            val animation = RotateAnimation(centerX, centerY, 0f, 180f, 50f)
            animation.duration = 3000
            animation.fillAfter = true
            imageView.startAnimation(animation)
        }

        // reset
        resetBtn.setOnClickListener {
            if (tmpBitmapPixels == null) {
                Log.w(TAG, "setupActionListener: not modify origin bitmap")
                return@setOnClickListener
            }

            handler?.post {
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
        }

        // adjust brightness
        lightnessBtn.setOnClickListener {
            handler?.post {
                val count = originBitmapRgba!!.size / 4
                val hsl = FloatArray(3)
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4)
                }
                for (i in 0 until count) {
                    val r = originBitmapRgba!![i * 4]
                    val g = originBitmapRgba!![i * 4 + 1]
                    val b = originBitmapRgba!![i * 4 + 2]
                    val a = originBitmapRgba!![i * 4 + 3]
                    ColorUtils.RGBToHSL(r, g, b, hsl)
                    hsl[2] = hsl[2] * LIGHTNESS_RATIO
                    val newColor = ColorUtils.HSLToColor(hsl)
                    ColorUtils.setAlphaComponent(newColor, a)
                    tmpBitmapPixels!![i] = newColor
                }

                val bitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                    0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
                mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
            }
        }

        // adjust saturation
        saturationBtn.setOnClickListener {
            handler?.post {
                val count = originBitmapRgba!!.size / 4
                val hsl = FloatArray(3)
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4)
                }
                for (i in 0 until count) {
                    val r = originBitmapRgba!![i * 4]
                    val g = originBitmapRgba!![i * 4 + 1]
                    val b = originBitmapRgba!![i * 4 + 2]
                    val a = originBitmapRgba!![i * 4 + 3]
                    ColorUtils.RGBToHSL(r, g, b, hsl)
                    val s = hsl[1]  * SATURATION_RATIO
                    hsl[1] = Util.clamp(s,0f, 1f)
                    val newColor = ColorUtils.HSLToColor(hsl)
                    ColorUtils.setAlphaComponent(newColor, a)
                    tmpBitmapPixels!![i] = newColor
                }

                val bitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                        0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
                mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
            }
        }

        // adjust contrast
        contrastBtn.setOnClickListener {
            handler?.post {
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4)
                }

                val count = originBitmapRgba!!.size / 4
                for (i in 0 until count) {
                    var r = originBitmapRgba!![i * 4]
                    var g = originBitmapRgba!![i * 4 + 1]
                    var b = originBitmapRgba!![i * 4 + 2]
                    val a = originBitmapRgba!![i * 4 + 3]

                    val cr = ((r / 255f) - 0.5f) * CONTRACT_RATIO
                    val cg = ((g / 255f) - 0.5f) * CONTRACT_RATIO
                    val cb = ((b / 255f) - 0.5f) * CONTRACT_RATIO

                    r = ((cr + 0.5f) * 255f).toInt()
                    g = ((cg + 0.5f) * 255f).toInt()
                    b = ((cb + 0.5f) * 255f).toInt()

                    val newColor = Color.rgb(
                        Util.clamp(r, 0, 255),
                        Util.clamp(g, 0, 255),
                        Util.clamp(b, 0, 255)
                    )
                    ColorUtils.setAlphaComponent(newColor, a)
                    tmpBitmapPixels!![i] = newColor
                }
                val bitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                    0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
                mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
            }
        }

        // blur btn
        blurBtn.setOnClickListener {
            handler?.post {
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4)
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

                val start = System.currentTimeMillis()
                val renderScript = RenderScript.create(applicationContext)
                val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                val allocation = Allocation.createFromBitmap(renderScript, bitmap)

                blurScript.setRadius(25f)
                blurScript.setInput(allocation)
                blurScript.forEach(allocation)
                allocation.copyTo(bitmap)
                Log.d(TAG, "setupActionListener: blur consume = ${System.currentTimeMillis() - start}")
                blurScript.destroy()
                mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
                mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when(seekBar?.id) {
            R.id.hue_seek_bar -> {
                hue = (progress - 127) * 1f / 127 * 180f
            }
            R.id.saturation_seek_bar -> {
                saturation = 1f * progress / 127
            }
            R.id.lum_seek_bar -> {
                lum = 1f * progress / 127
            }
        }

        handler?.post {
            if (sourceBitmap == null) {
                if (tmpBitmapPixels == null) {
                    tmpBitmapPixels = IntArray(originBitmapRgba!!.size / 4)
                }

                val count = originBitmapRgba!!.size / 4
                for (i in 0 until count) {
                    val r = originBitmapRgba!![i * 4]
                    val g = originBitmapRgba!![i * 4 + 1]
                    val b = originBitmapRgba!![i * 4 + 2]
                    val newColor = Color.rgb(r, g, b)
                    tmpBitmapPixels!![i] = newColor
                }
                val originBitmap = Bitmap.createBitmap(tmpBitmapPixels!!,
                    0, bitmapWidth, bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                sourceBitmap = originBitmap
            }

            val bitmap = ImageProcessHelper.adjustImage(sourceBitmap!!, hue, saturation, lum)
            mainHandler.removeMessages(UPDATE_IMAGE_VIEW)
            mainHandler.sendMessage(mainHandler.obtainMessage(UPDATE_IMAGE_VIEW, bitmap))
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}