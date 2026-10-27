package com.spworld.gallery

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SlideshowActivity : AppCompatActivity() {
    private lateinit var image: ImageView
    private lateinit var video: VideoView
    private lateinit var label: TextView
    private lateinit var counter: TextView
    private lateinit var playButton: Button
    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var playing = true
    private lateinit var uris: Array<String>
    private lateinit var videos: BooleanArray
    private var place = "અજ્ઞાત સ્થળ"
    private val advance = object : Runnable {
        override fun run() {
            if (!playing) return
            index++
            if (index >= uris.size) { finish(); return }
            showCurrent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uris = intent.getStringArrayExtra("uris") ?: emptyArray()
        videos = intent.getBooleanArrayExtra("videos") ?: booleanArrayOf()
        place = intent.getStringExtra("place") ?: "અજ્ઞાત સ્થળ"
        buildUi()
        if (uris.isNotEmpty()) showCurrent()
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        image = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        video = VideoView(this)
        root.addView(image, FrameLayout.LayoutParams(-1, -1))
        root.addView(video, FrameLayout.LayoutParams(-1, -1))
        label = TextView(this).apply {
            text = "📍 $place"
            textSize = 16f
            setTextColor(-1)
            setBackgroundColor(0x99000000.toInt())
            setPadding(12, 10, 12, 10)
        }
        val labelParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        labelParams.setMargins(8, 12, 8, 8)
        root.addView(label, labelParams)
        counter = TextView(this).apply {
            setTextColor(-1); textSize = 14f; setPadding(12, 8, 12, 8)
            setBackgroundColor(0x77000000)
        }
        val countParams = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        root.addView(counter, countParams)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(0x66000000)
        }
        controls.addView(Button(this).apply { text = "પાછળ"; setOnClickListener { go(-1) } })
        playButton = Button(this).apply {
            text = "Pause"
            setOnClickListener {
                playing = !playing
                text = if (playing) "Pause" else "ચાલુ કરો"
                if (playing) showCurrent() else handler.removeCallbacks(advance)
                if (video.isPlaying && !playing) video.pause()
                else if (playing && videos.getOrNull(index) == true) video.start()
            }
        }
        controls.addView(playButton)
        controls.addView(Button(this).apply { text = "આગળ"; setOnClickListener { go(1) } })
        val controlParams = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)
        root.addView(controls, controlParams)
        setContentView(root)
    }

    private fun showCurrent() {
        handler.removeCallbacks(advance)
        if (index !in uris.indices) { finish(); return }
        val uri = Uri.parse(uris[index])
        counter.text = "${index + 1} / ${uris.size}"
        label.text = "📍 $place"
        if (videos.getOrNull(index) == true) {
            image.visibility = View.GONE
            video.visibility = View.VISIBLE
            video.setVideoURI(uri)
            video.setOnCompletionListener { if (playing) { index++; showCurrent() } }
            video.setOnErrorListener { _, _, _ -> if (playing) { index++; showCurrent() }; true }
            video.start()
        } else {
            video.stopPlayback()
            video.visibility = View.GONE
            image.visibility = View.VISIBLE
            try { image.setImageURI(uri) } catch (_: Exception) { image.setImageDrawable(null) }
            if (playing) handler.postDelayed(advance, 6000)
        }
    }

    private fun go(delta: Int) {
        handler.removeCallbacks(advance)
        if (::video.isInitialized) video.stopPlayback()
        index = (index + delta).coerceIn(0, (uris.size - 1).coerceAtLeast(0))
        showCurrent()
    }

    override fun onDestroy() { handler.removeCallbacks(advance); if (::video.isInitialized) video.stopPlayback(); super.onDestroy() }
}
