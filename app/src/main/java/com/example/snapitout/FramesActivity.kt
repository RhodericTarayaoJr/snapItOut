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

    private val prefs by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frames)

        // 🏠 Home & Album Navigation
        val homeButton: ImageView = findViewById(R.id.imageView5)
        val albumButton: ImageView = findViewById(R.id.imageView8)

        homeButton.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        albumButton.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        // 👤 Navigate to UserActivity from profile icon
        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        profileIcon.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
        }

        // 🎨 Frame Containers
        val frameList = listOf(
            R.id.frameContainer1,
            R.id.frameContainer2,
            R.id.frameContainer3,
            R.id.frameContainer4,
            R.id.frameContainer5,
            R.id.frameContainer6,
            R.id.frameContainer7,
            R.id.frameContainer8,
            R.id.frameContainer9,
            R.id.frameContainer10,
            R.id.frameContainer11,
            R.id.frameContainer12,
            R.id.frameContainer13,
            R.id.frameContainer14,
            R.id.frameContainer15,
            R.id.frameContainer16,
            R.id.frameContainer17,
            R.id.frameContainer18,
            R.id.frameContainer19,
            R.id.frameContainer20,
            R.id.frameContainer21,
            R.id.frameContainer22,
            R.id.frameContainer23,
            R.id.frameContainer24,
            R.id.frameContainer25,
            R.id.frameContainer26,
            R.id.frameContainer27,
            R.id.frameContainer28,
            R.id.frameContainer29,
            R.id.frameContainer30

        )

        val frameImages = listOf(
            R.drawable.frame1,
            R.drawable.frame2,
            R.drawable.frame3,
            R.drawable.frame4,
            R.drawable.frame5,
            R.drawable.frame6,
            R.drawable.frame7,
            R.drawable.frame8,
            R.drawable.frame9,
            R.drawable.frame10,
            R.drawable.frame11,
            R.drawable.frame12,
            R.drawable.frame13,
            R.drawable.frame14,
            R.drawable.frame15,
            R.drawable.frame16,
            R.drawable.frame17,
            R.drawable.frame18,
            R.drawable.frame19,
            R.drawable.frame20,
            R.drawable.frame21,
            R.drawable.frame22,
            R.drawable.frame23,
            R.drawable.frame24,
            R.drawable.frame25,
            R.drawable.frame26,
            R.drawable.frame27,
            R.drawable.frame28,
            R.drawable.frame29,
            R.drawable.frame30
        )

        frameList.forEachIndexed { index, frameId ->
            val frameView: ImageView = findViewById(frameId)
            frameView.setOnClickListener {
                showFramePopup(frameImages[index])
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
            // Save selected frame in SharedPreferences
            prefs.edit().putInt("selected_frame_res", frameResId).apply()
            dialog.dismiss()

            // Launch CameraActivity
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
            finish() // Close FramesActivity
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
