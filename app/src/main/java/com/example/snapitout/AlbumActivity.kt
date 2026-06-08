package com.example.snapitout

import android.app.Activity
import androidx.lifecycle.lifecycleScope
import com.example.snapitout.repo.PhotoRepository
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
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

    private val photoRepo by lazy { PhotoRepository.get(this) }

    private var isCollageModeOpen = false
    private lateinit var collageContainer: LinearLayout
    private lateinit var collageScrollView: View
    private lateinit var albumGrid: GridLayout
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var albumFolder: File
    private lateinit var currentUserId: String

    private val REQUEST_MAIN_IMAGE = 2001

    private var selectedTemplateRects: List<RectF>? = null

    data class Slot(
        val rect: RectF,
        val imageView: ImageView,
        var imageIndex: Int
    )

    private var selectedSlot: Slot? = null

    data class Template(
        val name: String,
        val rects: List<RectF>,
        val preview: () -> Bitmap
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        albumGrid = findViewById(R.id.albumImageContainer)
        albumGrid.columnCount = 3

        collageScrollView = findViewById(R.id.collageScrollView)
        collageContainer = findViewById(R.id.collageContainer)

        fun hideCollageBar() {
            collageScrollView.visibility = View.GONE
        }

        findViewById<MaterialButton>(R.id.collageButton).setOnClickListener {

            if (!isCollageModeOpen) {

                if (selectedImages.isEmpty()) {
                    Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                collageScrollView.visibility = View.VISIBLE
                populateTemplates(collageContainer)

                isCollageModeOpen = true

            } else {

                collageScrollView.visibility = View.GONE
                isCollageModeOpen = false
            }
        }

        findViewById<MaterialButton>(R.id.autoCollageButton).setOnClickListener {
            hideCollageBar()
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "Select at least 1 image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createAutoCollage()
        }

        findViewById<MaterialButton>(R.id.deleteButton).setOnClickListener {
            hideCollageBar()
            deleteSelectedImages()
        }

        findViewById<ImageView>(R.id.printButton2).setOnClickListener {
            hideCollageBar()
            handlePrintRequest()
        }

        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            hideCollageBar()
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.profileIcon).setOnClickListener {
            hideCollageBar()
            startActivity(Intent(this, UserActivity::class.java))
            finish()
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please sign in to view your album", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java)) // 🔧 your sign-in screen
            finish()
            return
        }
        currentUserId = uid

        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        albumFolder = File(picturesDir, "SnapItOut_$currentUserId")
        if (!albumFolder.exists()) albumFolder.mkdirs()

        // show local images immediately (offline-first)
        loadAlbumImages()

// then sync with the cloud in the background
        lifecycleScope.launch {
            val changed = photoRepo.syncWithCloud(currentUserId, albumFolder)
            if (changed) loadAlbumImages()
        }
    }

    // =========================
    // TEMPLATE SYSTEM (FIXED WITH RECT ACCESS)
    // =========================
    private fun getTemplates(): List<Template> {
        return when (selectedImages.size) {

            1 -> listOf(
                Template("Single", rectSingle(), ::templateSingle)
            )

            2 -> listOf(
                Template("Vertical", rectTwoVertical(), ::templateTwoVertical),
                Template("Horizontal", rectTwoHorizontal(), ::templateTwoHorizontal),
                Template("Split", rectTwoSplitLargeSmall(), ::templateTwoSplitLargeSmall)
            )

            3 -> listOf(
                Template("Grid", rectThreeGrid(), ::templateThreeGrid),
                Template("Strip", rectThreeStrip(), ::templateThreeStrip),
                Template("Stack", rectThreeStacked(), ::templateThreeStacked)
            )

            4 -> listOf(
                Template("Grid4", rectFourGrid(), ::templateFourGrid),
                Template("WideTop", rectFourWideTop(), ::templateFourWideTop),
                Template("Split", rectFourVerticalSplit(), ::templateFourVerticalSplit)
            )

            else -> listOf(
                Template("Mosaic", rectFiveMosaic(), ::templateFiveMosaic)
            )
        }
    }

    private fun populateTemplates(container: LinearLayout) {
        container.removeAllViews()

        getTemplates().forEach { template ->

            val preview = ImageView(this)

            val size = dp(45)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(dp(4), dp(2), dp(4), dp(2))

            preview.layoutParams = params
            preview.setImageBitmap(template.preview())
            preview.scaleType = ImageView.ScaleType.FIT_XY

            preview.setOnClickListener {
                selectedTemplateRects = template.rects
                showTemplatePreview(template.rects)
            }

            container.addView(preview)
        }
    }

    // =========================
    // TEMPLATE PREVIEW (NEW FEATURE)
    // =========================
    private fun showTemplatePreview(rects: List<RectF>) {

        val containerSize = dp(320)

        val frame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(containerSize, containerSize)
            setBackgroundColor(Color.WHITE)
        }

        val bitmaps = selectedImages.mapNotNull {
            val stream = contentResolver.openInputStream(it)
            BitmapFactory.decodeStream(stream)
        }.toMutableList()

        val slots = mutableListOf<Slot>()

        rects.forEachIndexed { index, rectF ->

            if (index >= bitmaps.size) return@forEachIndexed

            val left = (rectF.left * containerSize).toInt()
            val top = (rectF.top * containerSize).toInt()
            val width = ((rectF.right - rectF.left) * containerSize).toInt()
            val height = ((rectF.bottom - rectF.top) * containerSize).toInt()

            // FRAME HOLDER
            val slotContainer = FrameLayout(this).apply {
                clipChildren = true
                clipToPadding = true
                setBackgroundColor(Color.WHITE)
            }

            val slotParams = FrameLayout.LayoutParams(width, height)
            slotParams.leftMargin = left
            slotParams.topMargin = top

            slotContainer.layoutParams = slotParams

            // IMAGE
            val imgView = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmaps[index])
            }

            // make image larger for dragging room
            val imageParams = FrameLayout.LayoutParams(
                (width * 1.5f).toInt(),
                (height * 1.5f).toInt()
            )

            imageParams.gravity = Gravity.CENTER
            imgView.layoutParams = imageParams

            // ENABLE DRAGGING
            enableImageDragging(imgView, width, height)

            slotContainer.addView(imgView)

            val slot = Slot(rectF, imgView, index)
            slots.add(slot)

            enableSlotTap(slot, slots, bitmaps)

            frame.addView(slotContainer)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("SnapItOut Collage")
            .setView(frame)
            .setPositiveButton("Save", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            val bitmap = viewToBitmap(frame)

            saveCollageToAlbum(bitmap)

            Toast.makeText(this, "Saved to Album", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }
    }

    private fun saveCollageToAlbum(bitmap: Bitmap) {

        val fileName = "collage_${System.currentTimeMillis()}.jpg"
        val file = File(albumFolder, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        photoRepo.enqueueSave(file, currentUserId)   // ☁️ NEW
        loadAlbumImages()
    }


    // =========================
    // SWAP FUNCTION
    // =========================

    private fun enableSlotTap(
        slot: Slot,
        slots: MutableList<Slot>,
        bitmaps: List<Bitmap>
    ) {
        slot.imageView.setOnClickListener {

            // 🔥 FIRST CLICK (select)
            if (selectedSlot == null) {
                selectedSlot = slot
                slot.imageView.alpha = 0.5f  // highlight
                return@setOnClickListener
            }

            // 🔥 SECOND CLICK (swap)
            val first = selectedSlot!!

            if (first == slot) {
                // same slot → deselect
                slot.imageView.alpha = 1f
                selectedSlot = null
                return@setOnClickListener
            }

            // 🔄 SWAP INDEX
            val temp = first.imageIndex
            first.imageIndex = slot.imageIndex
            slot.imageIndex = temp

            // 🔄 UPDATE IMAGES
            first.imageView.setImageBitmap(bitmaps[first.imageIndex])
            slot.imageView.setImageBitmap(bitmaps[slot.imageIndex])

            // 🔄 RESET UI
            first.imageView.alpha = 1f
            slot.imageView.alpha = 1f
            selectedSlot = null
        }
    }

    private fun viewToBitmap(view: View): Bitmap {

        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun enableImageDragging(
        imageView: ImageView,
        frameWidth: Int,
        frameHeight: Int
    ) {

        var startRawX = 0f
        var startRawY = 0f

        var startViewX = 0f
        var startViewY = 0f

        var isDragging = false

        imageView.setOnTouchListener { view, event ->

            when (event.action) {

                android.view.MotionEvent.ACTION_DOWN -> {

                    startRawX = event.rawX
                    startRawY = event.rawY

                    startViewX = view.x
                    startViewY = view.y

                    isDragging = false
                }

                android.view.MotionEvent.ACTION_MOVE -> {

                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY

                    // only start dragging if moved enough
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                    }

                    if (isDragging) {

                        var newX = startViewX + dx
                        var newY = startViewY + dy

                        val minX = frameWidth - view.width.toFloat()
                        val minY = frameHeight - view.height.toFloat()

                        val maxX = 0f
                        val maxY = 0f

                        if (newX < minX) newX = minX
                        if (newX > maxX) newX = maxX

                        if (newY < minY) newY = minY
                        if (newY > maxY) newY = maxY

                        view.x = newX
                        view.y = newY
                    }
                }

                android.view.MotionEvent.ACTION_UP -> {

                    // if user did NOT drag → treat as normal click
                    if (!isDragging) {
                        view.performClick()
                    }
                }
            }

            true
        }
    }


    // =========================
    // TEMPLATE RECT DEFINITIONS
    // =========================

    private fun rectSingle() = listOf(
        RectF(0f, 0f, 1f, 1f)
    )
    private fun rectTwoVertical() = listOf(
        RectF(0f, 0f, 0.5f, 1f),
        RectF(0.5f, 0f, 1f, 1f)
    )

    private fun rectTwoHorizontal() = listOf(
        RectF(0f, 0f, 1f, 0.5f),
        RectF(0f, 0.5f, 1f, 1f)
    )

    private fun rectTwoSplitLargeSmall() = listOf(
        RectF(0f, 0f, 0.7f, 1f),
        RectF(0.7f, 0f, 1f, 1f)
    )

    private fun rectThreeGrid() = listOf(
        RectF(0f, 0f, 0.5f, 0.5f),
        RectF(0.5f, 0f, 1f, 0.5f),
        RectF(0f, 0.5f, 1f, 1f)
    )

    private fun rectThreeStrip() = listOf(
        RectF(0f, 0f, 1f, 0.33f),
        RectF(0f, 0.33f, 1f, 0.66f),
        RectF(0f, 0.66f, 1f, 1f)
    )

    private fun rectThreeStacked() = listOf(
        RectF(0f, 0f, 1f, 0.4f),
        RectF(0f, 0.4f, 1f, 0.7f),
        RectF(0f, 0.7f, 1f, 1f)
    )

    private fun rectFourGrid() = listOf(
        RectF(0f, 0f, 0.5f, 0.5f),
        RectF(0.5f, 0f, 1f, 0.5f),
        RectF(0f, 0.5f, 0.5f, 1f),
        RectF(0.5f, 0.5f, 1f, 1f)
    )

    private fun rectFourWideTop() = listOf(
        RectF(0f, 0f, 1f, 0.4f),
        RectF(0f, 0.4f, 0.33f, 1f),
        RectF(0.33f, 0.4f, 0.66f, 1f),
        RectF(0.66f, 0.4f, 1f, 1f)
    )

    private fun rectFourVerticalSplit() = listOf(
        RectF(0f, 0f, 0.4f, 0.5f),
        RectF(0.4f, 0f, 1f, 0.5f),
        RectF(0f, 0.5f, 0.4f, 1f),
        RectF(0.4f, 0.5f, 1f, 1f)
    )

    private fun rectFiveMosaic() = listOf(
        RectF(0f, 0f, 0.33f, 0.33f),
        RectF(0.33f, 0f, 0.66f, 0.33f),
        RectF(0.66f, 0f, 1f, 0.33f),
        RectF(0f, 0.33f, 0.5f, 1f),
        RectF(0.5f, 0.33f, 1f, 1f)
    )

    // =========================
    // FRAME PREVIEWS (FOR UI)
    // =========================
    private fun templateSingle() = createFrame(rectSingle())
    private fun templateTwoVertical() = createFrame(rectTwoVertical())
    private fun templateTwoHorizontal() = createFrame(rectTwoHorizontal())
    private fun templateTwoSplitLargeSmall() = createFrame(rectTwoSplitLargeSmall())
    private fun templateThreeGrid() = createFrame(rectThreeGrid())
    private fun templateThreeStrip() = createFrame(rectThreeStrip())
    private fun templateThreeStacked() = createFrame(rectThreeStacked())
    private fun templateFourGrid() = createFrame(rectFourGrid())
    private fun templateFourWideTop() = createFrame(rectFourWideTop())
    private fun templateFourVerticalSplit() = createFrame(rectFourVerticalSplit())
    private fun templateFiveMosaic() = createFrame(rectFiveMosaic())

    // =========================
    // FRAME DRAWER
    // =========================
    private fun createFrame(rects: List<RectF>): Bitmap {
        val size = 300
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.BLACK
        }

        rects.forEach {
            canvas.drawRect(
                it.left * size,
                it.top * size,
                it.right * size,
                it.bottom * size,
                paint
            )
        }

        return bmp
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // =========================
    // IMAGE LOADING (UNCHANGED FULL)
    // =========================
    private fun loadAlbumImages() {
        albumGrid.removeAllViews()

        val images = getAllImages().sortedByDescending { getFileModifiedTime(it) }

        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        var lastMonth = ""

        images.forEach { uri ->

            val month = format.format(Date(getFileModifiedTime(uri)))

            if (month != lastMonth) {
                val header = TextView(this).apply {
                    text = month
                    setTypeface(null, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    gravity = Gravity.CENTER
                }

                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.MATCH_PARENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(0, 3)
                    setMargins(0, 24, 0, 12)
                }

                header.layoutParams = params
                albumGrid.addView(header)

                lastMonth = month
            }

            val img = ImageView(this)

            val size = dp(110)
            val margin = dp(6)

            val params = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(margin, margin, margin, margin)
            }

            img.layoutParams = params
            img.scaleType = ImageView.ScaleType.CENTER_CROP

            Glide.with(this).load(uri).into(img)

            img.setOnClickListener {
                val list = getAllImages().map { it.toString() }
                val index = list.indexOf(uri.toString())

                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putStringArrayListExtra("imageUris", ArrayList(list))
                intent.putExtra("currentIndex", index)
                startActivity(intent)
            }

            img.setOnLongClickListener {

                if (selectedImages.contains(uri)) {
                    selectedImages.remove(uri)
                } else {
                    selectedImages.add(uri)
                }

                loadAlbumImages()

                // AUTO UPDATE ONLY IF COLLAGE BAR IS OPEN
                if (isCollageModeOpen) {
                    populateTemplates(collageContainer)
                }

                true
            }

            img.alpha = if (selectedImages.contains(uri)) 0.5f else 1.0f

            albumGrid.addView(img)
        }
    }

    private fun getAllImages(): List<Uri> {
        return albumFolder.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
    }

    private fun getFileModifiedTime(uri: Uri): Long {
        return File(uri.path ?: "").lastModified()
    }

    // =========================
    // PRINT (UNCHANGED FULL)
    // =========================
    private fun handlePrintRequest() {
        if (selectedImages.size != 1) {
            Toast.makeText(this, "Select 1 image to print", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = selectedImages[0]

        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val pdf = PdfDocument()
        val page = pdf.startPage(
            PdfDocument.PageInfo.Builder(3600, 1800, 1).create()
        )

        page.canvas.drawColor(Color.WHITE)
        page.canvas.drawBitmap(bitmap, 0f, 0f, Paint())

        pdf.finishPage(page)

        val file = File(getExternalFilesDir(null), "print.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()

        startActivity(Intent(this, PdfPreviewActivity::class.java).apply {
            putExtra("PDF_PATH", file.absolutePath)
        })
    }

    // =========================
    // DELETE
    // =========================
    private fun deleteSelectedImages() {
        selectedImages.forEach { uri ->
            val f = File(uri.path ?: "")
            val name = f.name
            f.delete()
            photoRepo.enqueueDelete(currentUserId, name)   // ☁️ NEW
        }
        selectedImages.clear()
        loadAlbumImages()
    }

    // =========================
    // AUTO COLLAGE
    // =========================
    private fun createAutoCollage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_MAIN_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MAIN_IMAGE && resultCode == Activity.RESULT_OK) {

            val mainUri = data?.data ?: return

            try {
                val mainBitmap =
                    MediaStore.Images.Media.getBitmap(contentResolver, mainUri)

                val tiles = selectedImages.mapNotNull {
                    val stream = contentResolver.openInputStream(it)
                    BitmapFactory.decodeStream(stream)
                }

                val mosaic = CollageUtils.createPhotoMosaic(
                    this,
                    mainBitmap,
                    tiles
                )

                mosaic?.let {
                    CollageUtils.saveBitmapToAlbum(it, albumFolder)
                    loadAlbumImages()
                    Toast.makeText(this, "Mosaic created!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Failed to create mosaic", Toast.LENGTH_SHORT).show()
            }
        }
    }
}