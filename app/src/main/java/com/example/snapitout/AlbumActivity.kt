package com.example.snapitout

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.snapitout.utils.CollageUtils
import com.google.android.material.button.MaterialButton
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt

class AlbumActivity : AppCompatActivity() {

    private lateinit var albumGrid: GridLayout
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var albumFolder: File
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        // Hide navigation + status bars
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        albumGrid = findViewById(R.id.albumImageContainer)
        albumGrid.columnCount = 3

        // Load current user
        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("current_user_id", "default_user") ?: "default_user"

        // Prepare user-specific album folder
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        albumFolder = File(picturesDir, "SnapItOut_$currentUserId")
        if (!albumFolder.exists()) albumFolder.mkdirs()

        loadAlbumImages()

        // Home button
        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        // Auto Collage
        findViewById<MaterialButton>(R.id.autoCollageButton).setOnClickListener {
            createAutoCollage()
        }

        // Shape Collage
        findViewById<MaterialButton>(R.id.shapeCollageButton)?.setOnClickListener {
            createShapeCollage()
        }

        // PRINT BUTTON → Only 1 image allowed
        findViewById<ImageView>(R.id.printButton2)?.setOnClickListener {
            handlePrintRequest()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    /** Load all images for current user */
    private fun loadAlbumImages() {
        albumGrid.removeAllViews()
        val imageUris = getAllImages().sortedByDescending { getFileModifiedTime(it) }

        imageUris.forEach { uri ->
            val imageView = ImageView(this)

            val sizeInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 110f, resources.displayMetrics
            ).toInt()

            val marginInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics
            ).toInt()

            val params = GridLayout.LayoutParams().apply {
                width = sizeInDp
                height = sizeInDp
                setMargins(marginInDp, marginInDp, marginInDp, marginInDp)
            }

            imageView.layoutParams = params
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.alpha = if (selectedImages.contains(uri)) 0.5f else 1.0f

            Glide.with(this).load(uri).into(imageView)

            // Tap → view full screen
            imageView.setOnClickListener {
                val allImageUris = getAllImages().map { it.toString() }
                val currentIndex = allImageUris.indexOf(uri.toString())

                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putStringArrayListExtra("imageUris", ArrayList(allImageUris))
                intent.putExtra("currentIndex", currentIndex)
                startActivity(intent)
            }

            // Long press → select/deselect
            imageView.setOnLongClickListener {
                if (selectedImages.contains(uri)) selectedImages.remove(uri)
                else selectedImages.add(uri)
                loadAlbumImages()
                true
            }

            albumGrid.addView(imageView)
        }
    }

    /** Get all images in the user folder */
    private fun getAllImages(): List<Uri> {
        return albumFolder.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
    }

    private fun getFileModifiedTime(uri: Uri): Long {
        return File(uri.path ?: "").lastModified()
    }

    /** Save a regular image */
    fun saveImageToAlbum(sourceUri: Uri, fileName: String? = null) {
        try {
            val inputStream = contentResolver.openInputStream(sourceUri)
            val outputFile = File(albumFolder, fileName ?: "image_${System.currentTimeMillis()}.png")
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Image saved to album!", Toast.LENGTH_SHORT).show()
            loadAlbumImages()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Auto Collage */
    private fun createAutoCollage() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmaps = selectedImages.mapNotNull { uri ->
            BitmapFactory.decodeFile(File(uri.path ?: "").absolutePath)
        }

        if (bitmaps.isEmpty()) {
            Toast.makeText(this, "Failed to load images!", Toast.LENGTH_SHORT).show()
            return
        }

        val count = bitmaps.size
        val gridSize = ceil(sqrt(count.toDouble())).toInt()
        val cellSize = 400
        val collageWidth = gridSize * cellSize
        val collageHeight = gridSize * cellSize

        val collageBitmap = Bitmap.createBitmap(collageWidth, collageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collageBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var x = 0
        var y = 0
        bitmaps.forEachIndexed { index, bmp ->
            val scaled = Bitmap.createScaledBitmap(bmp, cellSize, cellSize, true)
            canvas.drawBitmap(scaled, x.toFloat(), y.toFloat(), paint)

            x += cellSize
            if ((index + 1) % gridSize == 0) {
                x = 0
                y += cellSize
            }
        }

        CollageUtils.saveBitmapToAlbum(collageBitmap, albumFolder)
        loadAlbumImages()
        Toast.makeText(this, "✅ Auto Collage created successfully!", Toast.LENGTH_SHORT).show()
    }

    /** Shape Collage */
    private fun createShapeCollage() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
            return
        }

        val shapes = arrayOf("❤️ Heart", "⭐ Star", "🔺 Triangle", "🔵 Circle", "⬜ Square")
        val shapeResIds = arrayOf(
            R.drawable.shape_heart,
            R.drawable.shape_star,
            R.drawable.shape_triangle,
            R.drawable.shape_circle,
            R.drawable.shape_square
        )

        AlertDialog.Builder(this)
            .setTitle("Choose Shape")
            .setItems(shapes) { _, which ->
                val chosenShape = shapeResIds[which]
                generateShapeCollage(chosenShape)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateShapeCollage(shapeResId: Int) {
        val bitmaps = selectedImages.mapNotNull { uri ->
            BitmapFactory.decodeFile(File(uri.path ?: "").absolutePath)
        }

        try {
            val maskDrawable = ContextCompat.getDrawable(this, shapeResId)
            if (maskDrawable == null) {
                Toast.makeText(this, "Shape not found!", Toast.LENGTH_SHORT).show()
                return
            }

            val maskBitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            maskDrawable.setBounds(0, 0, maskCanvas.width, maskCanvas.height)
            maskDrawable.draw(maskCanvas)

            val collage = CollageUtils.createShapeCollage(bitmaps, maskBitmap)
            if (collage != null) {
                CollageUtils.saveBitmapToAlbum(collage, albumFolder)
                loadAlbumImages()
                Toast.makeText(this, "✅ Shape Collage created!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to create collage", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // -------------------------------
    // 🔥 PRINTING RULES HERE
    // -------------------------------
    private fun handlePrintRequest() {
        when {
            selectedImages.isEmpty() -> {
                Toast.makeText(this, "Select a photo to print", Toast.LENGTH_SHORT).show()
            }

            selectedImages.size > 1 -> {
                Toast.makeText(this, "Only 1 image can be printed at a time", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val selectedUri = selectedImages[0].toString()
                val intent = Intent(this, PrintPreviewActivity::class.java)
                intent.putExtra("imageUri", selectedUri)
                startActivity(intent)
            }
        }
    }
}
