package com.example.snapitout

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class GuestBookActivity : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var albumIcon: ImageView
    private lateinit var backArrow: ImageView

    private lateinit var guestbookContainer: LinearLayout
    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guestbook)

        // Bottom nav buttons
        homeIcon = findViewById(R.id.imageView5)
        albumIcon = findViewById(R.id.imageView21)
        backArrow = findViewById(R.id.imageView29)

        // Guestbook container
        guestbookContainer = findViewById(R.id.guestbookContainer)
        sp = getSharedPreferences("GUESTBOOK_DATA", MODE_PRIVATE)

        // Load saved images
        loadGuestbookImages()

        homeIcon.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
        }

        albumIcon.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        backArrow.setOnClickListener {
            finish()
        }
    }

    // 🔥 PARA PAG BALIK MO SA GUESTBOOK, LALABAS AGAD ANG BAGONG IMAGE
    override fun onResume() {
        super.onResume()
        loadGuestbookImages()
    }

    private fun loadGuestbookImages() {
        guestbookContainer.removeAllViews()

        val paths = sp.getStringSet("IMAGES", emptySet()) ?: emptySet()

        Log.d("GUESTBOOK", "Loaded saved paths: $paths")

        if (paths.isEmpty()) {
            Log.d("GUESTBOOK", "NO IMAGES FOUND")
            return
        }

        for (path in paths.sorted()) {

            val file = File(path)
            if (!file.exists()) {
                Log.e("GUESTBOOK", "FILE NOT FOUND: $path")
                continue
            }

            val bmp = BitmapFactory.decodeFile(path)
            if (bmp != null) {

                val img = ImageView(this)
                img.setImageBitmap(bmp)
                img.adjustViewBounds = true
                img.scaleType = ImageView.ScaleType.FIT_CENTER

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(20, 30, 20, 30)

                guestbookContainer.addView(img, params)

            } else {
                Log.e("GUESTBOOK", "ERROR DECODING BITMAP: $path")
            }
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
