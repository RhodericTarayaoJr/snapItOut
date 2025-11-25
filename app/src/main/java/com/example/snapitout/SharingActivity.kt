package com.example.snapitout

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SharingActivity : AppCompatActivity() {

    private lateinit var mainFrames: List<ImageView>
    private lateinit var cameraButton: ImageView
    private lateinit var homeButton: ImageView
    private lateinit var albumButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sharing)

        // Reference ImageViews from layout
        mainFrames = listOf(
            findViewById(R.id.photo1)
        )

        // Buttons for navigation
        cameraButton = findViewById(R.id.camcam)
        homeButton = findViewById(R.id.homehome)
        albumButton = findViewById(R.id.albumBtn)

        // ✅ Try to receive new "saved_collage_uri" from EditingActivity
        val savedCollageUri = intent.getStringExtra("saved_collage_uri")

        if (!savedCollageUri.isNullOrEmpty()) {
            val uri = Uri.parse(savedCollageUri)

            // Check if it's a content URI (for scoped storage)
            if (savedCollageUri.startsWith("content://")) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    mainFrames[0].setImageBitmap(bitmap)
                    mainFrames[0].visibility = View.VISIBLE
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                    mainFrames[0].visibility = View.GONE
                }
            } else {
                // Handle file path (same as AlbumActivity)
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    mainFrames[0].setImageBitmap(bitmap)
                    mainFrames[0].visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "Image not found at: ${file.path}", Toast.LENGTH_SHORT).show()
                    mainFrames[0].visibility = View.GONE
                }
            }

        } else {
            Toast.makeText(this, "No saved image received", Toast.LENGTH_SHORT).show()
        }

        // 📸 Camera Button → Go back to camera
        cameraButton.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        // 🏠 Home Button → Go to home page
        homeButton.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
        }

        albumButton.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
}