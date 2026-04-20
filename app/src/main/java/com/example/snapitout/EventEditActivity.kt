package com.example.snapitout

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

class EventEditActivity : AppCompatActivity() {

    private lateinit var outerFrameContainer: ConstraintLayout
    private lateinit var frameContainer: ConstraintLayout
    private lateinit var mainFrames: List<ImageView>

    private lateinit var normalBtn: Button
    private lateinit var bwBtn: Button
    private lateinit var vintageBtn: Button
    private lateinit var oldPhotoBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var printBtn: Button

    private var templateSlots = arrayListOf<String>()
    private var capturedImages = arrayListOf<Uri>()

    // ✅ ONLY ORIGINALS ARE STORED (NO STACKING BUG)
    private val originalBitmaps = arrayListOf<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)

        outerFrameContainer = findViewById(R.id.outerFrameContainer)
        frameContainer = findViewById(R.id.frameContainer1)

        mainFrames = listOf(
            findViewById(R.id.mainFrame1),
            findViewById(R.id.mainFrame2),
            findViewById(R.id.mainFrame3),
            findViewById(R.id.mainFrame4)
        )

        normalBtn = findViewById(R.id.NormalBtn)
        bwBtn = findViewById(R.id.BWBtn)
        vintageBtn = findViewById(R.id.VintageBtn)
        oldPhotoBtn = findViewById(R.id.OldPhotoBtn)
        saveBtn = findViewById(R.id.saveButton)
        printBtn = findViewById(R.id.printButton)

        templateSlots = intent.getStringArrayListExtra("TEMPLATE_SLOTS") ?: arrayListOf()
        capturedImages = intent.getParcelableArrayListExtra("CAPTURED_IMAGES") ?: arrayListOf()

        if (templateSlots.isNotEmpty()) {
            val templateUri = Uri.parse(templateSlots[0])
            val templateImageView = ImageView(this)
            templateImageView.setImageURI(templateUri)
            templateImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            frameContainer.addView(templateImageView, 0)
        }

        loadCapturedImages()

        normalBtn.setOnClickListener { applyFilter(FilterType.NORMAL) }
        bwBtn.setOnClickListener { applyFilter(FilterType.BW) }
        vintageBtn.setOnClickListener { applyFilter(FilterType.VINTAGE) }
        oldPhotoBtn.setOnClickListener { applyFilter(FilterType.OLD) }

        saveBtn.setOnClickListener { saveFinalImage() }
        printBtn.setOnClickListener { generateFixedPdfAndPreview() }

        setupEditTexts()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    // ================= LOAD IMAGES =================
    private fun loadCapturedImages() {
        originalBitmaps.clear()

        for (i in mainFrames.indices) {

            if (i < capturedImages.size) {

                val bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver,
                    capturedImages[i]
                )

                mainFrames[i].post {

                    mainFrames[i].scaleType = ImageView.ScaleType.FIT_XY

                    val stretched = Bitmap.createScaledBitmap(
                        bitmap,
                        mainFrames[i].width,
                        mainFrames[i].height,
                        true
                    )

                    originalBitmaps.add(stretched)
                    mainFrames[i].setImageBitmap(stretched)
                    mainFrames[i].visibility = View.VISIBLE
                }

            } else {
                mainFrames[i].visibility = View.INVISIBLE
            }
        }
    }

    enum class FilterType { NORMAL, BW, VINTAGE, OLD }

    // ================= FIXED FILTER SYSTEM =================
    private fun applyFilter(type: FilterType) {

        for (i in mainFrames.indices) {

            if (i >= originalBitmaps.size) continue

            val original = originalBitmaps[i]

            // ✅ NORMAL = RESTORE ORIGINAL IMAGE (FIXED)
            if (type == FilterType.NORMAL) {
                mainFrames[i].setImageBitmap(original)
                continue
            }

            val filtered = Bitmap.createBitmap(
                original.width,
                original.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(filtered)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            paint.colorFilter = when (type) {

                FilterType.BW -> ColorMatrixColorFilter(
                    ColorMatrix().apply { setSaturation(0f) }
                )

                FilterType.VINTAGE -> ColorMatrixColorFilter(
                    ColorMatrix(floatArrayOf(
                        0.9f,0f,0f,0f,20f,
                        0f,0.8f,0f,0f,20f,
                        0f,0f,0.7f,0f,20f,
                        0f,0f,0f,1f,0f
                    ))
                )

                FilterType.OLD -> ColorMatrixColorFilter(
                    ColorMatrix(floatArrayOf(
                        1f,0f,0f,0f,30f,
                        0f,0.9f,0f,0f,30f,
                        0f,0f,0.8f,0f,30f,
                        0f,0f,0f,1f,0f
                    ))
                )

                else -> null
            }

            canvas.drawBitmap(original, 0f, 0f, paint)
            mainFrames[i].setImageBitmap(filtered)
        }
    }

    // ================= PRINT (UNCHANGED LOGIC, CLEAN SIZE) =================
    private fun generateFixedPdfAndPreview() {

        val bitmapToPrint = createPrintBitmap()

        val pdf = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(3600, 1800, 1).create()
        val page = pdf.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val pageW = pageInfo.pageWidth
        val pageH = pageInfo.pageHeight

        val scale = min(
            pageW.toFloat() / bitmapToPrint.width,
            pageH.toFloat() / bitmapToPrint.height
        )

        val newW = (bitmapToPrint.width * scale).toInt()
        val newH = (bitmapToPrint.height * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmapToPrint, newW, newH, true)

        val left = (pageW - newW) / 2f
        val top = (pageH - newH) / 2f

        canvas.drawBitmap(scaledBitmap, left, top, Paint())

        pdf.finishPage(page)

        val file = File(getExternalFilesDir(null), "guestbook_print.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        startActivity(Intent(this, PdfPreviewActivity::class.java).apply {
            putExtra("PDF_PATH", file.absolutePath)
        })
    }

    // ================= CAPTURE FRAME (FILTER + FRAME INCLUDED) =================
    private fun createPrintBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(
            frameContainer.width,
            frameContainer.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bmp)
        frameContainer.draw(canvas)

        return bmp
    }

    // ================= SAVE =================
    private fun saveFinalImage() {
        val bitmap = createBitmapFromView(outerFrameContainer)

        val file = File(getExternalFilesDir(null), "guestbook_${System.currentTimeMillis()}.png")

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        saveImagePath(file.absolutePath)

        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, GuestBookActivity::class.java))
        finish()
    }

    private fun saveImagePath(path: String) {
        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("current_user_id", "default_user") ?: "default_user"

        val sp = getSharedPreferences("GUESTBOOK_DATA_$currentUserId", Context.MODE_PRIVATE)
        val list = sp.getStringSet("IMAGES", mutableSetOf())!!.toMutableSet()
        list.add(path)
        sp.edit().putStringSet("IMAGES", list).apply()
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        view.draw(c)
        return b
    }

    private fun setupEditTexts() {
        fun hideKeyboardAndCursor(editText: EditText) {
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    editText.isCursorVisible = false
                    editText.clearFocus()
                    editText.setSelection(0)
                    true
                } else false
            }

            editText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    editText.isCursorVisible = false
                    editText.clearFocus()
                    editText.setSelection(0)
                    true
                } else false
            }
        }

        for (i in 0 until frameContainer.childCount) {
            val child = frameContainer.getChildAt(i)
            if (child is EditText) hideKeyboardAndCursor(child)
        }
    }
}