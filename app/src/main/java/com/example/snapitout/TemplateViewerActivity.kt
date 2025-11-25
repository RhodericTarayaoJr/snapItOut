package com.example.snapitout

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class TemplateViewerActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private lateinit var nameView: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnUseTemplate: Button
    private lateinit var btnBack: ImageButton

    private var templateId: Long = 0L
    private var templateName: String = ""
    private var templateSlots = arrayListOf<String>()
    private var frameColor: Int = Color.WHITE

    // Launcher para sa Edit Template
    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!
                templateName = data.getStringExtra("TEMPLATE_NAME") ?: templateName
                templateSlots = data.getStringArrayListExtra("TEMPLATE_SLOTS") ?: templateSlots
                frameColor = data.getIntExtra("FRAME_COLOR", frameColor)

                nameView.text = templateName
                loadTemplateGrid(templateSlots)

                val returnIntent = Intent().apply {
                    putExtra("TEMPLATE_ID", templateId)
                    putExtra("TEMPLATE_NAME", templateName)
                    putStringArrayListExtra("TEMPLATE_SLOTS", templateSlots)
                    putExtra("FRAME_COLOR", frameColor)
                }
                setResult(Activity.RESULT_OK, returnIntent)
            }
        }

    // Launcher para sa Use Template
    private val useTemplateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val capturedUris = result.data!!.getParcelableArrayListExtra<Uri>("CAPTURED_IMAGES") ?: arrayListOf()
                val templateSlotsFromCamera = result.data!!.getStringArrayListExtra("TEMPLATE_SLOTS") ?: arrayListOf()

                // Papunta sa EventEditActivity
                val editIntent = Intent(this, EventEditActivity::class.java).apply {
                    putStringArrayListExtra("TEMPLATE_SLOTS", templateSlotsFromCamera)
                    putParcelableArrayListExtra("CAPTURED_IMAGES", capturedUris)
                }
                startActivity(editIntent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_viewer)

        nameView = findViewById(R.id.templateNameView)
        grid = findViewById(R.id.gridViewer)
        btnEdit = findViewById(R.id.btnEditTemplate)
        btnUseTemplate = findViewById(R.id.btnUseTemplate)
        btnBack = findViewById(R.id.btnBack)

        templateId = intent.getLongExtra("TEMPLATE_ID", System.currentTimeMillis())
        templateName = intent.getStringExtra("TEMPLATE_NAME") ?: "Untitled Template"
        templateSlots = intent.getStringArrayListExtra("TEMPLATE_SLOTS") ?: arrayListOf()
        frameColor = intent.getIntExtra("FRAME_COLOR", Color.WHITE)

        nameView.text = templateName
        loadTemplateGrid(templateSlots)

        btnBack.setOnClickListener { finish() }

        // Edit Template button
        btnEdit.setOnClickListener {
            val editIntent = Intent(this, EditTemplateActivity::class.java).apply {
                putExtra("TEMPLATE_ID", templateId)
                putExtra("TEMPLATE_NAME", templateName)
                putStringArrayListExtra("TEMPLATE_SLOTS", templateSlots)
                putExtra("FRAME_COLOR", frameColor)
                putExtra("IS_EDIT_MODE", true)
            }
            editLauncher.launch(editIntent)
        }

        // Use Template button
        btnUseTemplate.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity2::class.java)
            cameraIntent.putStringArrayListExtra("TEMPLATE_SLOTS", templateSlots)
            useTemplateLauncher.launch(cameraIntent)
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

    private fun loadTemplateGrid(slots: List<String>) {
        grid.removeAllViews()
        grid.columnCount = 2

        val screenWidth = resources.displayMetrics.widthPixels
        val itemMargin = (6 * resources.displayMetrics.density).toInt()
        val totalMargins = itemMargin * (grid.columnCount + 1)
        val itemWidth = (screenWidth - totalMargins) / grid.columnCount

        for (uriStr in slots) {
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = itemWidth
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                }
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(frameColor)
                setImageURI(if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null)
            }
            grid.addView(imageView)
        }
    }
}
