package com.example.snapitout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream

class CameraActivity2 : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private lateinit var shutterButton: ImageView
    private lateinit var countdownText: TextView
    private lateinit var backgroundOverlay: ImageView
    private lateinit var addBackgroundButton: Button
    private lateinit var segmentedCameraView: ImageView

    private val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val capturedPhotoFiles = mutableListOf<File>()
    private lateinit var snapItOutFolder: File
    private var templateSlots = arrayListOf<String>()

    private lateinit var gestureDetector: GestureDetector
    private lateinit var imageAnalyzer: ImageAnalysis
    private val countdownHandler = Handler(Looper.getMainLooper())

    private var photosToTake = 4

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.CAMERA, false)) startCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            backgroundOverlay.setImageURI(uri)
            backgroundOverlay.visibility = View.VISIBLE
            Toast.makeText(this, "Background applied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)

        cameraPreview = findViewById(R.id.cameraPreview)
        shutterButton = findViewById(R.id.shutterButton)
        countdownText = findViewById(R.id.countdownText)
        backgroundOverlay = findViewById(R.id.backgroundOverlay)
        addBackgroundButton = findViewById(R.id.addBackgroundButton)
        segmentedCameraView = findViewById(R.id.segmentedCameraView)

        segmentedCameraView.visibility = View.VISIBLE
        segmentedCameraView.bringToFront()

        templateSlots = intent.getStringArrayListExtra("TEMPLATE_SLOTS") ?: arrayListOf()

        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        snapItOutFolder = File(picturesDir, "SnapItOut")
        if (!snapItOutFolder.exists()) snapItOutFolder.mkdirs()

        findViewById<View>(R.id.homeButton).setOnClickListener {
            startActivity(Intent(this, ExclusiveActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.imageView13).setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
            finish()
        }

        addBackgroundButton.setOnClickListener { pickImageLauncher.launch("image/*") }
        addBackgroundButton.setOnLongClickListener {
            backgroundOverlay.setImageDrawable(null)
            backgroundOverlay.visibility = View.GONE
            Toast.makeText(this, "Background removed", Toast.LENGTH_SHORT).show()
            true
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
                startCamera()
                Toast.makeText(this@CameraActivity2, "Camera switched", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        cameraPreview.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        shutterButton.setOnClickListener {
            if (capturedPhotoFiles.isEmpty()) {
                startPhotoSequence()
            }
        }

        if (isCameraPermissionGranted()) startCamera()
        else permissionRequest.launch(cameraPermissions)
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

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(cameraPreview.display.rotation)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(cameraPreview.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ---------------- AUTOMATIC PHOTO SEQUENCE ----------------
    private fun startPhotoSequence() {
        capturedPhotoFiles.clear()
        takeNextPhoto()
    }

    private fun takeNextPhoto() {
        if (capturedPhotoFiles.size >= photosToTake) {
            goToEventEditActivity()
            return
        }
        startCountdown {
            capturePhoto { takeNextPhoto() }
        }
    }

    // ---------------- COUNTDOWN ----------------
    private fun startCountdown(onComplete: () -> Unit) {
        countdownText.bringToFront()
        countdownText.visibility = View.VISIBLE

        var countdown = 3
        countdownHandler.removeCallbacksAndMessages(null)

        val countdownRunnable = object : Runnable {
            override fun run() {
                countdownText.text = countdown.toString() // update first

                if (countdown > 1) {
                    countdown--
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    // show "1" for 1 second before taking photo
                    countdownHandler.postDelayed({
                        countdownText.visibility = View.GONE
                        onComplete()
                    }, 1000)
                }
            }
        }

        countdownHandler.post(countdownRunnable)
    }

    private fun capturePhoto(onSaved: () -> Unit) {
        val imageCapture = imageCapture ?: return
        val photoFile = File(snapItOutFolder, "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    bitmap = rotateImageIfRequired(photoFile.absolutePath, mirror = (lensFacing == CameraSelector.LENS_FACING_FRONT))
                    val finalBitmap = mergeWithBackground(bitmap)

                    FileOutputStream(photoFile).use {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }

                    capturedPhotoFiles.add(photoFile)
                    onSaved()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity2, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun mergeWithBackground(foreground: Bitmap): Bitmap {
        val bgDrawable = backgroundOverlay.drawable as? BitmapDrawable ?: return foreground
        val background = Bitmap.createScaledBitmap(bgDrawable.bitmap, foreground.width, foreground.height, true)

        val fgSmall = Bitmap.createScaledBitmap(foreground, foreground.width / 4, foreground.height / 4, true)
        val bgSmall = Bitmap.createScaledBitmap(background, fgSmall.width, fgSmall.height, true)

        val mergedSmall = applyGreenScreenFast(fgSmall, bgSmall)
        return Bitmap.createScaledBitmap(mergedSmall, foreground.width, foreground.height, true)
    }

    private fun goToEventEditActivity() {
        val intent = Intent(this, EventEditActivity::class.java)
        intent.putStringArrayListExtra("TEMPLATE_SLOTS", templateSlots)
        intent.putParcelableArrayListExtra(
            "CAPTURED_IMAGES",
            ArrayList(capturedPhotoFiles.map { Uri.fromFile(it) })
        )
        startActivity(intent)
        finish()
    }

    private fun rotateImageIfRequired(filePath: String, mirror: Boolean): Bitmap {
        val bitmap = BitmapFactory.decodeFile(filePath)
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        if (mirror) {
            matrix.postScale(-1f, 1f)
            matrix.postTranslate(bitmap.width.toFloat(), 0f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ---------------- FAST GREEN SCREEN ----------------
    private fun processFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap() ?: run { imageProxy.close(); return }

        var rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())

        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            val matrix = Matrix()
            matrix.preScale(-1f, 1f)
            rotatedBitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height, matrix, true)
        }

        val bgDrawable = backgroundOverlay.drawable as? BitmapDrawable
        if (bgDrawable != null) {
            val smallFrame = Bitmap.createScaledBitmap(rotatedBitmap, rotatedBitmap.width / 4, rotatedBitmap.height / 4, true)
            val smallBg = Bitmap.createScaledBitmap(bgDrawable.bitmap, smallFrame.width, smallFrame.height, true)

            val resultSmall = applyGreenScreenFast(smallFrame, smallBg)
            val result = Bitmap.createScaledBitmap(resultSmall, rotatedBitmap.width, rotatedBitmap.height, true)
            runOnUiThread { segmentedCameraView.setImageBitmap(result) }
        } else {
            runOnUiThread { segmentedCameraView.setImageBitmap(rotatedBitmap) }
        }

        imageProxy.close()
    }

    private fun applyGreenScreenFast(frame: Bitmap, background: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val width = frame.width
        val height = frame.height

        val framePixels = IntArray(width * height)
        frame.getPixels(framePixels, 0, width, 0, 0, width, height)

        val bgPixels = IntArray(width * height)
        background.getPixels(bgPixels, 0, width, 0, 0, width, height)

        for (i in framePixels.indices) {
            val pixel = framePixels[i]
            val hsv = FloatArray(3)
            Color.colorToHSV(pixel, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val valBrightness = hsv[2]

            framePixels[i] = if (hue in 80f..160f && sat > 0.3f && valBrightness > 0.2f) bgPixels[i] else pixel
        }

        output.setPixels(framePixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
