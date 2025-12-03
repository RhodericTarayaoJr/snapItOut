package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)   // ← Replace with your XML file name

        val backButton = findViewById<ImageView>(R.id.imageView30)

        backButton.setOnClickListener {
            // ⭐ YOU decide where it goes. Replace 'MainActivity::class.java'
            val intent = Intent(this, UserActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
