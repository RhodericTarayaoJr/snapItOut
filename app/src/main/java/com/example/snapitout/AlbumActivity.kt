package com.example.snapitout

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.snapitout.utils.CollageUtils
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AlbumActivity : AppCompatActivity() {

    private lateinit var albumGrid: GridLayout
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var albumFolder: File
    private lateinit var currentUserId: String

    private val REQUEST_MAIN_IMAGE = 2001 // 🔥 NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

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

        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("current_user_id", "default_user") ?: "default_user"

        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        albumFolder = File(picturesDir, "SnapItOut_$currentUserId")
        if (!albumFolder.exists()) albumFolder.mkdirs()

        loadAlbumImages()

        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.profileIcon).setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.autoCollageButton).setOnClickListener {
            createAutoCollage()
        }

        findViewById<ImageView>(R.id.printButton2)?.setOnClickListener {
            handlePrintRequest()
        }

        findViewById<MaterialButton>(R.id.deleteButton)?.setOnClickListener {
            deleteSelectedImages()
        }
    }

    // 🔥 NEW: HANDLE GALLERY RESULT FOR MOSAIC
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MAIN_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            try {
                val mainBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                val tileBitmaps = selectedImages.mapNotNull {
                    BitmapFactory.decodeFile(File(it.path ?: "").absolutePath)
                }

                val mosaic = CollageUtils.createPhotoMosaic(
                    this,
                    mainBitmap,
                    tileBitmaps
                )

                mosaic?.let {
                    CollageUtils.saveBitmapToAlbum(it, albumFolder)
                    loadAlbumImages()
                    Toast.makeText(this, "✅ Mosaic created!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error creating mosaic", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ===============================
    // 🔥 UPDATED AUTO COLLAGE
    // ===============================
    private fun createAutoCollage() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔥 OPEN GALLERY AGAIN
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_MAIN_IMAGE)
    }

    // ===============================
    // EVERYTHING BELOW UNCHANGED
    // ===============================

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

    private fun loadAlbumImages() {
        albumGrid.removeAllViews()
        val imageUris = getAllImages().sortedByDescending { getFileModifiedTime(it) }

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        var lastMonth = ""

        imageUris.forEach { uri ->
            val monthLabel = monthFormat.format(Date(getFileModifiedTime(uri)))
            if (monthLabel != lastMonth) {
                val header = TextView(this).apply {
                    text = monthLabel
                    setTypeface(null, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    gravity = Gravity.CENTER
                }
                val headerParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.MATCH_PARENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(0, 3)
                    setMargins(0, 24, 0, 12)
                }
                header.layoutParams = headerParams
                albumGrid.addView(header)
                lastMonth = monthLabel
            }

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

            imageView.setOnClickListener {
                val allImageUris = getAllImages().map { it.toString() }
                val currentIndex = allImageUris.indexOf(uri.toString())

                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putStringArrayListExtra("imageUris", ArrayList(allImageUris))
                intent.putExtra("currentIndex", currentIndex)
                startActivity(intent)
            }

            imageView.setOnLongClickListener {
                if (selectedImages.contains(uri)) selectedImages.remove(uri)
                else selectedImages.add(uri)
                loadAlbumImages()
                true
            }

            albumGrid.addView(imageView)
        }
    }

    private fun getAllImages(): List<Uri> {
        return albumFolder.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
    }

    private fun getFileModifiedTime(uri: Uri): Long {
        return File(uri.path ?: "").lastModified()
    }

    private fun handlePrintRequest() {
        if (selectedImages.size != 1) {
            Toast.makeText(this, "Select 1 image to print", Toast.LENGTH_SHORT).show()
            return
        }
        generatePdfAndPreview(selectedImages[0])
    }

    private fun generatePdfAndPreview(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(3600, 1800, 1).create()
            val page = pdf.startPage(pageInfo)

            page.canvas.drawColor(android.graphics.Color.WHITE)
            page.canvas.drawBitmap(originalBitmap, 0f, 0f, Paint())

            pdf.finishPage(page)

            val file = File(getExternalFilesDir(null), "print.pdf")
            FileOutputStream(file).use { pdf.writeTo(it) }
            pdf.close()

            val intent = Intent(this, PdfPreviewActivity::class.java)
            intent.putExtra("PDF_PATH", file.absolutePath)
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteSelectedImages() {
        selectedImages.forEach { File(it.path ?: "").delete() }
        selectedImages.clear()
        loadAlbumImages()
    }
}