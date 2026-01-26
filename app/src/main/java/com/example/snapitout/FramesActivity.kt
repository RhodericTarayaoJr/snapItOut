package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FramesActivity : AppCompatActivity() {

    private val frameIds = listOf(
        R.id.frameContainer1, R.id.frameContainer2, R.id.frameContainer3,
        R.id.frameContainer4, R.id.frameContainer5, R.id.frameContainer6,
        R.id.frameContainer7, R.id.frameContainer8, R.id.frameContainer9,
        R.id.frameContainer10, R.id.frameContainer11, R.id.frameContainer12,
        R.id.frameContainer13, R.id.frameContainer14, R.id.frameContainer15,
        R.id.frameContainer16, R.id.frameContainer17, R.id.frameContainer18,
        R.id.frameContainer19, R.id.frameContainer20, R.id.frameContainer21,
        R.id.frameContainer22, R.id.frameContainer23, R.id.frameContainer24,
        R.id.frameContainer25, R.id.frameContainer26, R.id.frameContainer27,
        R.id.frameContainer28, R.id.frameContainer29, R.id.frameContainer30
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frames)

        frameIds.forEach { id ->
            val frameImageView = findViewById<ImageView>(id)
            frameImageView.setOnClickListener {
                showFrameDialog(frameImageView)
            }
        }

        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.imageView8).setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.profileIcon).setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }



    private fun showFrameDialog(frameImageView: ImageView) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_frame_preview, null)
        val framePreview = dialogView.findViewById<ImageView>(R.id.framePreview)
        val useFrameButton = dialogView.findViewById<Button>(R.id.useFrameButton)

        // Ipakita ang napiling frame sa dialog
        framePreview.setImageDrawable(frameImageView.drawable)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        useFrameButton.setOnClickListener {
            val selectedFrameId = getDrawableIdFromImageView(frameImageView)

            // DIRETSA sa CameraActivity at ipasa ang napiling frame
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("selectedFrame", selectedFrameId) // ipapasa ang frame
            startActivity(intent)

            dialog.dismiss()
        }

        dialog.show()
    }


    private fun getDrawableIdFromImageView(imageView: ImageView): Int {
        return when (imageView.id) {
            R.id.frameContainer1 -> R.drawable.frame01
            R.id.frameContainer2 -> R.drawable.frame02
            R.id.frameContainer3 -> R.drawable.frame03
            R.id.frameContainer4 -> R.drawable.frame04
            R.id.frameContainer5 -> R.drawable.frame05
            R.id.frameContainer6 -> R.drawable.frame06
            R.id.frameContainer7 -> R.drawable.frame07
            R.id.frameContainer8 -> R.drawable.frame08
            R.id.frameContainer9 -> R.drawable.frame09
            R.id.frameContainer10 -> R.drawable.frame10
            R.id.frameContainer11 -> R.drawable.frame11
            R.id.frameContainer12 -> R.drawable.frame12
            R.id.frameContainer13 -> R.drawable.frame13
            R.id.frameContainer14 -> R.drawable.frame14
            R.id.frameContainer15 -> R.drawable.frame15
            R.id.frameContainer16 -> R.drawable.frame16
            R.id.frameContainer17 -> R.drawable.frame17
            R.id.frameContainer18 -> R.drawable.frame18
            R.id.frameContainer19 -> R.drawable.frame19
            R.id.frameContainer20 -> R.drawable.frame20
            R.id.frameContainer21 -> R.drawable.frame21
            R.id.frameContainer22 -> R.drawable.frame22
            R.id.frameContainer23 -> R.drawable.frame23
            R.id.frameContainer24 -> R.drawable.frame24
            R.id.frameContainer25 -> R.drawable.frame25
            R.id.frameContainer26 -> R.drawable.frame26
            R.id.frameContainer27 -> R.drawable.frame27
            R.id.frameContainer28 -> R.drawable.frame28
            R.id.frameContainer29 -> R.drawable.frame29
            R.id.frameContainer30 -> R.drawable.frame30
            else -> R.drawable.frame01
        }
    }

}
