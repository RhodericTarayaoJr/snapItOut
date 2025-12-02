package com.example.snapitout

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class GuestBookActivity : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var albumIcon: ImageView
    private lateinit var backArrow: ImageView

    private lateinit var guestbookGrid: GridLayout
    private lateinit var sp: SharedPreferences
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guestbook)

        // Bottom nav buttons
        homeIcon = findViewById(R.id.imageView5)
        albumIcon = findViewById(R.id.imageView21)
        backArrow = findViewById(R.id.imageView29)

        // Guestbook GridLayout inside ScrollView
        guestbookGrid = findViewById(R.id.guestbookContainer)
        guestbookGrid.columnCount = 2

        // Get current logged-in user ID
        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("current_user_id", "default_user") ?: "default_user"

        // Use user-specific SharedPreferences for guestbook
        sp = getSharedPreferences("GUESTBOOK_DATA_$currentUserId", MODE_PRIVATE)

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

    override fun onResume() {
        super.onResume()
        loadGuestbookImages()
    }

    private fun loadGuestbookImages() {
        guestbookGrid.removeAllViews()

        val paths = sp.getStringSet("IMAGES", emptySet())?.toList() ?: emptyList()
        if (paths.isEmpty()) {
            Toast.makeText(this, "No guestbook entries yet!", Toast.LENGTH_SHORT).show()
            return
        }

        val sortedPaths = paths.sortedDescending()

        val marginInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()

        for (path in sortedPaths) {
            val file = File(path)
            if (!file.exists()) continue

            val bmp = BitmapFactory.decodeFile(path) ?: continue

            val img = ImageView(this).apply {
                setImageBitmap(bmp)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER

                // **Use weight for equal column width**
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
                }
            }

            guestbookGrid.addView(img)
        }
    }


    /** Save an image to the current user's guestbook */
    fun saveImageToGuestBook(imageFile: File) {
        if (!imageFile.exists()) return

        try {
            // Get current set and make mutable copy
            val savedPaths = sp.getStringSet("IMAGES", emptySet())?.toMutableSet() ?: mutableSetOf()

            // Add new image
            savedPaths.add(imageFile.absolutePath)

            // Save back to SharedPreferences
            sp.edit().putStringSet("IMAGES", savedPaths).apply()

            // Reload UI
            loadGuestbookImages()

            Toast.makeText(this, "Image added to guestbook!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
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
