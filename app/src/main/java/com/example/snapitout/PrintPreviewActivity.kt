package com.example.snapitout

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper

class PrintPreviewActivity : AppCompatActivity() {

    private lateinit var previewImageView: ImageView
    private lateinit var printButton: Button

    // Fixed print size same as EventEditActivity
    private val targetWidth = 3600
    private val targetHeight = 1800

    private var finalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_preview)

        previewImageView = findViewById(R.id.pdfImageView)
        printButton = findViewById(R.id.printButton)

        val imageUriString = intent.getStringExtra("imageUri")

        if (imageUriString == null) {
            Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)

        // Load selected image
        val originalBitmap = loadBitmapFromUri(imageUri)
        if (originalBitmap == null) {
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Create 3600x1800 bitmap with white background
        finalBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap!!)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Scale proportionally to fit within 3600x1800
        val scale = minOf(
            targetWidth.toFloat() / originalBitmap.width,
            targetHeight.toFloat() / originalBitmap.height
        )

        val drawWidth = (originalBitmap.width * scale).toInt()
        val drawHeight = (originalBitmap.height * scale).toInt()

        // Draw at top-left (0,0) same as EventEditActivity
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(originalBitmap, drawWidth, drawHeight, true),
            0f, 0f, paint
        )

        // Preview
        previewImageView.setImageBitmap(finalBitmap)

        printButton.setOnClickListener {
            printImage()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun printImage() {
        if (finalBitmap == null) {
            Toast.makeText(this, "No image to print.", Toast.LENGTH_SHORT).show()
            return
        }

        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap("SnapItOut_Print", finalBitmap!!)
    }
}
