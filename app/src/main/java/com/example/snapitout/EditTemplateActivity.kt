package com.example.snapitout

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditTemplateActivity : AppCompatActivity() {

    private lateinit var btnAddText: ImageButton
    private lateinit var btnBack2: ImageButton
    private lateinit var btnAddSticker: ImageButton
    private lateinit var btnAddImage: ImageButton
    private lateinit var btnColorPicker: ImageButton
    private lateinit var frameContainer: ConstraintLayout
    private lateinit var saveButton: Button
    private lateinit var navHome: ImageButton
    private lateinit var navAlbum: ImageButton

    private lateinit var mainFrame1: View
    private lateinit var mainFrame2: View
    private lateinit var mainFrame3: View
    private lateinit var mainFrame4: View

    private var isEditMode = false
    private var existingSlots: ArrayList<String>? = null
    private var templateId: Long = 0L
    private var templateName: String = "My Template"
    private var frameColor: Int = Color.WHITE

    private val REQUEST_IMAGE_PICK = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_template)

        // -------------------------
        // Initialize views
        // -------------------------
        btnAddText = findViewById(R.id.btnAddText)
        btnBack2 = findViewById(R.id.btnBack2)
        btnAddSticker = findViewById(R.id.btnAddSticker)
        btnAddImage = findViewById(R.id.btnAddImage)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        frameContainer = findViewById(R.id.frameContainer1)
        saveButton = findViewById(R.id.SaveButton)
        navHome = findViewById(R.id.navHome)
        navAlbum = findViewById(R.id.navAlbum)

        mainFrame1 = findViewById(R.id.mainFrame1)
        mainFrame2 = findViewById(R.id.mainFrame2)
        mainFrame3 = findViewById(R.id.mainFrame3)
        mainFrame4 = findViewById(R.id.mainFrame4)

        // -------------------------
        // Make main frames visible and always on top
        // -------------------------
        listOf(mainFrame1, mainFrame2, mainFrame3, mainFrame4).forEach { frame ->
            frame.setBackgroundColor(Color.WHITE) // visible
            frame.bringToFront() // always on top
        }

        frameContainer.viewTreeObserver.addOnGlobalLayoutListener {
            mainFrame1.bringToFront()
            mainFrame2.bringToFront()
            mainFrame3.bringToFront()
            mainFrame4.bringToFront()
        }

        // -------------------------
        // Load intent data
        // -------------------------
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        templateId = intent.getLongExtra("TEMPLATE_ID", System.currentTimeMillis())
        templateName = intent.getStringExtra("TEMPLATE_NAME") ?: "My Template"
        existingSlots = intent.getStringArrayListExtra("TEMPLATE_SLOTS")
        frameColor = intent.getIntExtra("FRAME_COLOR", Color.WHITE)
        frameContainer.setBackgroundColor(frameColor)

        if (isEditMode && !existingSlots.isNullOrEmpty()) {
            loadExistingTemplate(existingSlots!!)
        }

        // -------------------------
        // ADD TEXT
        // -------------------------
        btnAddText.setOnClickListener {
            val inputField = EditText(this).apply { hint = "Type your text"; setPadding(16) }
            AlertDialog.Builder(this)
                .setTitle("Enter Text")
                .setView(inputField)
                .setPositiveButton("Add") { _, _ ->
                    val userText = inputField.text.toString()
                    if (userText.isNotBlank()) {
                        val textView = TextView(this).apply {
                            text = userText
                            textSize = 16f
                            setTextColor(Color.BLACK)
                            setPadding(8)
                            x = 0f // Top-left
                            y = 0f // Top-left
                        }
                        makeViewDragZoomRotate(textView)
                        addViewBelowFrames(textView)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // -------------------------
        // ADD STICKER
        // -------------------------
        btnAddSticker.setOnClickListener {
            val categories = arrayOf("Faces", "Symbols", "Celebration")
            val emojiMap = mapOf(
                "Faces" to arrayOf("😊", "😎", "😂", "🥰", "😇", "🤔"),
                "Symbols" to arrayOf("❤️", "✨", "🔥", "💡", "👍", "💬"),
                "Celebration" to arrayOf("🎉", "🎈", "🎂", "🎨", "📸", "🎶")
            )

            AlertDialog.Builder(this)
                .setTitle("Choose Sticker Category")
                .setItems(categories) { _, categoryIndex ->
                    val selectedCategory = categories[categoryIndex]
                    val emojis = emojiMap[selectedCategory] ?: return@setItems
                    AlertDialog.Builder(this)
                        .setTitle("Choose a $selectedCategory Sticker")
                        .setItems(emojis) { _, emojiIndex ->
                            val emoji = TextView(this).apply {
                                text = emojis[emojiIndex]
                                textSize = 32f
                                setPadding(8)
                                x = 0f // Top-left
                                y = 0f // Top-left
                            }
                            makeViewDragZoomRotate(emoji)
                            addViewBelowFrames(emoji)
                        }
                        .show()
                }
                .show()
        }

        // -------------------------
        // ADD IMAGE FROM GALLERY
        // -------------------------
        btnAddImage.setOnClickListener { openGallery() }

        // -------------------------
        // BACKGROUND COLOR PICKER
        // -------------------------
        btnColorPicker.setOnClickListener {
            val colors = arrayOf("Black", "Light Pink", "Sky Blue", "Mint", "Gray")
            val colorValues = arrayOf("#000000", "#FFEBEE", "#E3F2FD", "#E0F7FA", "#EEEEEE")
            AlertDialog.Builder(this)
                .setTitle("Choose Background Color")
                .setItems(colors) { _, which ->
                    frameColor = Color.parseColor(colorValues[which])
                    frameContainer.setBackgroundColor(frameColor)
                }
                .show()
        }

        saveButton.setOnClickListener { saveTemplate() }

        navHome.setOnClickListener {
            startActivity(Intent(this, ExclusiveActivity::class.java))
            finish()
        }

        navAlbum.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
            finish()
        }

        btnBack2.setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
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

    // -------------------------
    // Keep main frames on top
    // -------------------------
    private fun keepFramesOnTop() {
        mainFrame1.bringToFront()
        mainFrame2.bringToFront()
        mainFrame3.bringToFront()
        mainFrame4.bringToFront()
    }

    // -------------------------
    // Add view below main frames
    // -------------------------
    private fun addViewBelowFrames(view: View) {
        frameContainer.addView(view)
        keepFramesOnTop()
    }

    // -------------------------
    // GALLERY IMAGE PICK
    // -------------------------
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { addImageFromGallery(it) }
        }
    }

    private fun addImageFromGallery(uri: Uri) {
        val imageView = ImageView(this).apply {
            setImageURI(uri)
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            x = 0f // Top-left
            y = 0f // Top-left
        }
        makeViewDragZoomRotate(imageView)
        addViewBelowFrames(imageView)
    }

    // -------------------------
    // DRAG & ZOOM
    // -------------------------
    private fun makeViewDragZoomRotate(view: View) {
        var dX = 0f
        var dY = 0f
        var scaleFactor = 1f

        val scaleDetector = ScaleGestureDetector(this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.3f, 3f)
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    return true
                }
            }
        )

        view.setOnTouchListener { v, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress) {
                        v.x = event.rawX + dX
                        v.y = event.rawY + dY
                    }
                }
            }
            true
        }
    }

    // -------------------------
    // LOAD EXISTING TEMPLATE
    // -------------------------
    private fun loadExistingTemplate(slots: ArrayList<String>) {
        for (uriStr in slots) {
            try {
                val imageView = ImageView(this).apply {
                    setImageURI(Uri.parse(uriStr))
                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                    )
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                addViewBelowFrames(imageView)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // -------------------------
    // SAVE TEMPLATE
    // -------------------------
    private fun saveTemplate() {
        val uri = saveFrameAsImage()
        val slots = ArrayList<String>()
        uri?.let { slots.add(it) }

        val result = Intent().apply {
            putExtra("TEMPLATE_ID", templateId)
            putExtra("TEMPLATE_NAME", templateName)
            putStringArrayListExtra("TEMPLATE_SLOTS", slots)
            putExtra("FRAME_COLOR", frameColor)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun saveFrameAsImage(): String? {
        val view = frameContainer
        if (view.width == 0 || view.height == 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "template_$timestamp.jpg"
        val relativePath = "Pictures/SnapItOut/Templates"

        val resolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.getContentUri(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"
        )

        val uri = try { resolver.insert(collection, values) } catch (e: Exception) { e.printStackTrace(); null }
        if (uri == null) return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                val compressed = bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                if (!compressed) throw IOException("Bitmap compress returned false")
            } ?: throw IOException("Failed to open output stream for uri: $uri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            null
        }
    }
}
