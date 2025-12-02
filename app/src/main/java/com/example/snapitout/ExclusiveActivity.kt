package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ExclusiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exclusivefeatures)

        // Top and bottom toolbars
        val topToolbar: MaterialToolbar = findViewById(R.id.materialToolbar4)
        val bottomToolbar: MaterialToolbar = findViewById(R.id.materialToolbar10)

        // Toolbar Cards
        val toolbar12: MaterialToolbar = findViewById(R.id.materialToolbar12)
        val toolbar13: MaterialToolbar = findViewById(R.id.materialToolbar13)

        // Images
        val logoImage: ImageView = findViewById(R.id.imageView9)
        val profileImage: ImageView = findViewById(R.id.imageView12)  // User profile logo
        val homeIcon: ImageView = findViewById(R.id.imageView)        // Home icon
        val albumIcon: ImageView = findViewById(R.id.imageView4)
        val featureIcon: ImageView = findViewById(R.id.imageView18)

        val frameLeft: ImageView = findViewById(R.id.imageView15)
        val frameRight: ImageView = findViewById(R.id.imageView16)
        val guestbookImage: ImageView = findViewById(R.id.imageView17)

        // Texts
        val titleUploadFrame: TextView = findViewById(R.id.textView5)
        val titleGuestBook: TextView = findViewById(R.id.textView6)
        val titleExclusive: TextView = findViewById(R.id.textView8)


        // Click on Home icon navigates to HomePageActivity
        homeIcon.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Click on User logo navigates to UserActivity
        profileImage.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
            finish()
        }

        albumIcon.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
            finish()
        }

        titleGuestBook.setOnClickListener {
            val intent = Intent(this, GuestBookActivity::class.java)
            startActivity(intent)
            finish()
        }

        titleUploadFrame.setOnClickListener {
            val intent = Intent(this, TemplatesActivity::class.java)
            startActivity(intent)
            finish()
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
