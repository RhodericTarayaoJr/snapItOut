package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class UploadingActivity : AppCompatActivity() {

    private lateinit var homeIcon: ImageView
    private lateinit var albumIcon: ImageView
    private lateinit var addButton: ImageView
    private lateinit var backArrow: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uploading)

        // Initialize views
        homeIcon = findViewById(R.id.imageView20)
        albumIcon = findViewById(R.id.imageView21)
        addButton = findViewById(R.id.imageView28)
        backArrow = findViewById(R.id.imageView29)

        // Set click listeners
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
        }

        albumIcon.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
        }

        addButton.setOnClickListener {
            // You can trigger image upload, open a new screen, etc.
            // Example:
            // val intent = Intent(this, AddTemplateActivity::class.java)
            // startActivity(intent)
        }

        backArrow.setOnClickListener {
            finish() // Go back to the previous screen
        }
    }
}
