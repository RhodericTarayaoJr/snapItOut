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

    private val originalBitmaps = arrayListOf<Bitmap>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    private fun loadCapturedImages() {
        originalBitmaps.clear()
        for (i in mainFrames.indices) {
            if (i < capturedImages.size) {
                val fullBitmap = MediaStore.Images.Media.getBitmap(contentResolver, capturedImages[i])
                val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, 800, 800, true)
                originalBitmaps.add(scaledBitmap)
                mainFrames[i].setImageBitmap(scaledBitmap)
                mainFrames[i].visibility = View.VISIBLE
            } else {
                mainFrames[i].visibility = View.INVISIBLE
            }
        }
    }

    enum class FilterType { NORMAL, BW, VINTAGE, OLD }

    private fun applyFilter(type: FilterType) {
        for (i in mainFrames.indices) {
            if (i >= originalBitmaps.size) continue
            val src = originalBitmaps[i]
            val filtered = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(filtered)
            paint.colorFilter = when (type) {
                FilterType.NORMAL -> null
                FilterType.BW -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                FilterType.VINTAGE -> ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            0.9f, 0f, 0f, 0f, 20f,
                            0f, 0.8f, 0f, 0f, 20f,
                            0f, 0f, 0.7f, 0f, 20f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                FilterType.OLD -> ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, 30f,
                            0f, 0.9f, 0f, 0f, 30f,
                            0f, 0f, 0.8f, 0f, 30f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            canvas.drawBitmap(src, 0f, 0f, paint)
            mainFrames[i].setImageBitmap(filtered)
        }
    }

    private fun saveFinalImage() {
        val bitmap = createBitmapFromView(outerFrameContainer)
        val file = File(getExternalFilesDir(null), "guestbook_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        saveImagePath(file.absolutePath)

        Toast.makeText(this, "Saved to Guestbook!", Toast.LENGTH_SHORT).show()
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

    private fun createBitmapFromFrameContainer(): Bitmap {
        val bitmap = Bitmap.createBitmap(frameContainer.width, frameContainer.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        frameContainer.draw(canvas)
        return bitmap
    }

    // ================= PDF GENERATION AND PREVIEW =================
    private fun generateFixedPdfAndPreview() {
        val bitmapToPrint = createBitmapFromFrameContainer()
        val pdfWidth = 3600
        val pdfHeight = 1800

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create()
        val page = pdf.startPage(pageInfo)

        page.canvas.drawColor(Color.WHITE)

        val scale = minOf(
            pdfWidth.toFloat() / bitmapToPrint.width,
            pdfHeight.toFloat() / bitmapToPrint.height
        )
        val drawWidth = (bitmapToPrint.width * scale).toInt()
        val drawHeight = (bitmapToPrint.height * scale).toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        page.canvas.drawBitmap(
            Bitmap.createScaledBitmap(bitmapToPrint, drawWidth, drawHeight, true),
            0f, 0f, paint
        )

        pdf.finishPage(page)

        val file = File(getExternalFilesDir(null), "guestbook_print.pdf")
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        val previewIntent = Intent(this, PdfPreviewActivity::class.java)
        previewIntent.putExtra("PDF_PATH", file.absolutePath)
        startActivity(previewIntent)
    }

    // ================== EDITTEXT HANDLING ==================
    private fun setupEditTexts() {

        fun hideKeyboardAndCursor(editText: EditText) {

            // MAIN FIX → detect Enter key directly
            editText.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {

                    // REMOVE CURSOR COMPLETELY
                    editText.isCursorVisible = false
                    editText.clearFocus()
                    editText.setSelection(0)   // important: remove active selection highlight

                    true
                } else false
            }

            // Backup: also remove cursor on IME action
            editText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEND
                ) {

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
