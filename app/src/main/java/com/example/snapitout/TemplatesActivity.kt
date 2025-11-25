package com.example.snapitout

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TemplatesActivity : AppCompatActivity() {

    private lateinit var gridContainer: GridLayout
    private lateinit var tvNoTemplates: TextView
    private val templates = mutableListOf<Template>()
    private val gson = Gson()
    private var currentUserId = "default_user"

    private val prefsName = "templates_prefs"
    private val keyTemplates = "key_templates"

    private val viewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val updatedId = data.getLongExtra("TEMPLATE_ID", -1L)
            val updatedName = data.getStringExtra("TEMPLATE_NAME")
            val updatedSlots = data.getStringArrayListExtra("TEMPLATE_SLOTS")
            val updatedColor = data.getIntExtra("FRAME_COLOR", Color.WHITE)

            if (updatedId != -1L && updatedName != null && updatedSlots != null) {
                val index = templates.indexOfFirst { it.id == updatedId }
                if (index != -1) {
                    // Update existing template
                    templates[index] = Template(updatedId, updatedName, updatedSlots, updatedColor)
                    Toast.makeText(this, "Template updated", Toast.LENGTH_SHORT).show()
                } else {
                    // Add new template
                    templates.add(Template(updatedId, updatedName, updatedSlots, updatedColor))
                    Toast.makeText(this, "Template created", Toast.LENGTH_SHORT).show()
                }
                saveTemplatesToPrefs()
                refreshGridFromTemplates()
                updateNoTemplatesUi()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)

        gridContainer = findViewById(R.id.albumImageContainer)
        tvNoTemplates = findViewById(R.id.tvNoTemplates)

        // Get current user ID from SharedPreferences
        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("current_user_id", "default_user") ?: "default_user"

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navArchive)?.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        loadTemplatesFromPrefs()
        refreshGridFromTemplates()
        updateNoTemplatesUi()
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

    private fun refreshGridFromTemplates() {
        gridContainer.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val itemMargin = (8 * resources.displayMetrics.density).toInt()
        val columns = 3
        val totalMargins = itemMargin * 2 * columns
        val itemSize = (screenWidth - totalMargins) / columns

        // Add "Create Template" tile
        val createTile = ImageView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = itemSize
                height = itemSize
                setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
            }
            setImageResource(R.drawable.ic_add_black)
            setBackgroundResource(R.drawable.bg_create_template_tile)
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener {
                val intent = Intent(this@TemplatesActivity, EditTemplateActivity::class.java)
                viewLauncher.launch(intent)
            }
        }
        gridContainer.addView(createTile)

        // Add templates
        for ((index, template) in templates.withIndex()) {
            val uriString = template.slotUris.firstOrNull()
            val imageView = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = itemSize
                    height = itemSize
                    setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(template.frameColor)
                setImageURI(if (uriString != null) Uri.parse(uriString) else null)
                setOnClickListener {
                    val intent = Intent(this@TemplatesActivity, TemplateViewerActivity::class.java)
                    intent.putExtra("TEMPLATE_ID", template.id)
                    intent.putExtra("TEMPLATE_NAME", (index + 1).toString()) // show number only
                    intent.putStringArrayListExtra("TEMPLATE_SLOTS", ArrayList(template.slotUris))
                    intent.putExtra("FRAME_COLOR", template.frameColor)
                    viewLauncher.launch(intent)
                }
            }
            gridContainer.addView(imageView)
        }
    }

    private fun updateNoTemplatesUi() {
        tvNoTemplates.visibility = if (templates.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveTemplatesToPrefs() {
        val prefs = getSharedPreferences("${prefsName}_$currentUserId", Context.MODE_PRIVATE)
        val json = gson.toJson(templates)
        prefs.edit().putString(keyTemplates, json).apply()
    }

    private fun loadTemplatesFromPrefs() {
        val prefs = getSharedPreferences("${prefsName}_$currentUserId", Context.MODE_PRIVATE)
        val json = prefs.getString(keyTemplates, null) ?: return
        val type = object : TypeToken<List<Template>>() {}.type
        templates.clear()
        templates.addAll(Gson().fromJson(json, type))
    }

    fun switchUser(newUserId: String) {
        currentUserId = newUserId
        templates.clear()
        loadTemplatesFromPrefs()
        refreshGridFromTemplates()
        updateNoTemplatesUi()
    }
}
