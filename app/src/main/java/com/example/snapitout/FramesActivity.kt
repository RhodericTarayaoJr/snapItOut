package com.example.snapitout

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FramesActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frames)

        // 🏠 Home & Album Navigation
        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.imageView8).setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        // 👤 Navigate to UserActivity from profile icon
        findViewById<ImageView>(R.id.profileIcon).setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }

        // 🎨 Map frames according to XML visual order (left column top→bottom, then right column top→bottom)
        val frameMap = linkedMapOf(
            // Left Column
            R.id.frameContainer1 to R.drawable.frame01,
            R.id.frameContainer3 to R.drawable.frame03,
            R.id.frameContainer5 to R.drawable.frame05,
            R.id.frameContainer7 to R.drawable.frame07,
            R.id.frameContainer9 to R.drawable.frame09,
            R.id.frameContainer11 to R.drawable.frame11,
            R.id.frameContainer13 to R.drawable.frame13,
            R.id.frameContainer15 to R.drawable.frame15,
            R.id.frameContainer17 to R.drawable.frame17,
            R.id.frameContainer19 to R.drawable.frame19,
            R.id.frameContainer21 to R.drawable.frame21,
            R.id.frameContainer23 to R.drawable.frame23,
            R.id.frameContainer25 to R.drawable.frame25,
            R.id.frameContainer27 to R.drawable.frame27,
            R.id.frameContainer29 to R.drawable.frame29,

            // Right Column
            R.id.frameContainer2 to R.drawable.frame02,
            R.id.frameContainer4 to R.drawable.frame04,
            R.id.frameContainer6 to R.drawable.frame06,
            R.id.frameContainer8 to R.drawable.frame08,
            R.id.frameContainer10 to R.drawable.frame10,
            R.id.frameContainer12 to R.drawable.frame12,
            R.id.frameContainer14 to R.drawable.frame14,
            R.id.frameContainer16 to R.drawable.frame16,
            R.id.frameContainer18 to R.drawable.frame18,
            R.id.frameContainer20 to R.drawable.frame20,
            R.id.frameContainer22 to R.drawable.frame22,
            R.id.frameContainer24 to R.drawable.frame24,
            R.id.frameContainer26 to R.drawable.frame26,
            R.id.frameContainer28 to R.drawable.frame28,
            R.id.frameContainer30 to R.drawable.frame30
        )

        // Attach click listeners to each frame container
        frameMap.forEach { (id, drawable) ->
            findViewById<ImageView>(id).setOnClickListener {
                showFramePopup(drawable)
            }
        }
    }

    private fun showFramePopup(frameResId: Int) {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_frame_preview, null)
        val framePreview: ImageView = view.findViewById(R.id.framePreview)
        val useButton: Button = view.findViewById(R.id.useFrameButton)

        Glide.with(this).load(frameResId).into(framePreview)

        builder.setView(view)
        val dialog = builder.create()
        dialog.show()

        useButton.setOnClickListener {
            prefs.edit().putInt("selected_frame_res", frameResId).apply()
            dialog.dismiss()
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
