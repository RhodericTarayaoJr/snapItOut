package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aboutus)  // ← replace this with your XML file name

        val backButton = findViewById<ImageView>(R.id.imageView30)

        backButton.setOnClickListener {
            // ⭐ YOU_DECIDE: Replace 'MainActivity::class.java' with where you want to go
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
