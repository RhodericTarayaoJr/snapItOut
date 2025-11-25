package com.example.snapitout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.View
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

    private val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val capturedPhotoFiles = mutableListOf<File>()
    private lateinit var snapItOutFolder: File

    private var templateSlots = arrayListOf<String>()

    private lateinit var gestureDetector: GestureDetector

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.CAMERA, false)) startCamera()
        else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraPreview = findViewById(R.id.cameraPreview)
        shutterButton = findViewById(R.id.shutterButton)
        countdownText = findViewById(R.id.countdownText)

        templateSlots = intent.getStringArrayListExtra("TEMPLATE_SLOTS") ?: arrayListOf()

        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        snapItOutFolder = File(picturesDir, "SnapItOut")
        if (!snapItOutFolder.exists()) snapItOutFolder.mkdirs()

        // GestureDetector for double-tap to switch camera
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

        shutterButton.setOnClickListener { startCountdown() }

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

            val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraPreview.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
            }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(rotation)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCountdown() {
        countdownText.visibility = View.VISIBLE
        var countdown = 3
        countdownText.text = "$countdown"

        val countdownRunnable = object : Runnable {
            override fun run() {
                countdown--
                countdownText.text = "$countdown"

                if (countdown > 0) {
                    countdownText.postDelayed(this, 1000)
                } else {
                    countdownText.visibility = View.GONE
                    capturePhoto()
                }
            }
        }

        countdownText.postDelayed(countdownRunnable, 1000)
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(snapItOutFolder, "IMG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val rotatedBitmap = rotateImageIfRequired(
                        photoFile.absolutePath,
                        mirror = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    )

                    FileOutputStream(photoFile).use {
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }

                    capturedPhotoFiles.add(photoFile)

                    if (capturedPhotoFiles.size >= 4) {
                        goToEventEditActivity()
                    } else {
                        startCamera()
                        startCountdown()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity2, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            })
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
}
