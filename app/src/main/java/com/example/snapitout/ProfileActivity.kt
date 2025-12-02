package com.example.snapitout

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var txtUsername: EditText
    private lateinit var txtEmail: EditText
    private lateinit var txtPassword: EditText
    private lateinit var imgBack: ImageView
    private lateinit var imgEdit: ImageView
    private lateinit var imgHome: ImageView
    private lateinit var imgAlbum: ImageView
    private lateinit var userLogo: ImageView

    private var isEditing = false
    private var selectedImageUri: Uri? = null
    private val prefs by lazy { getSharedPreferences("user_prefs", MODE_PRIVATE) }
    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser }
    private val uid by lazy { currentUser?.uid ?: "default" }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                saveAndSetProfileImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        hideSystemUI()

        // ---------------- Init Views ----------------
        txtUsername = findViewById(R.id.editTextText2)
        txtEmail = findViewById(R.id.editTextText3)
        txtPassword = findViewById(R.id.editTextTextPassword)
        imgBack = findViewById(R.id.imageView20)
        imgEdit = findViewById(R.id.editpencil)
        imgHome = findViewById(R.id.imageView26)
        imgAlbum = findViewById(R.id.imageView27)
        userLogo = findViewById(R.id.imageView14)

        txtEmail.isEnabled = false
        txtEmail.isFocusable = false

        txtPassword.setText("********")
        txtPassword.isEnabled = false
        txtPassword.isFocusable = false

        setEditable(false)
        loadUserData()

        // ---------------- Navigation ----------------
        imgHome.setOnClickListener { startActivity(Intent(this, HomePageActivity::class.java)) }
        imgAlbum.setOnClickListener { startActivity(Intent(this, AlbumActivity::class.java)) }
        imgBack.setOnClickListener { finish() }

        // ---------------- Edit / Save Toggle ----------------
        imgEdit.setOnClickListener {
            if (!isEditing) {
                isEditing = true
                setEditable(true)
                Toast.makeText(this, "Edit Mode Enabled", Toast.LENGTH_SHORT).show()
            } else {
                saveProfileChanges()
            }
        }

        userLogo.setOnClickListener {
            if (isEditing) pickImage()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }

    private fun loadUserData() {
        // Get username from local storage or fallback to Google/Facebook name
        val savedName = prefs.getString("username_$uid", currentUser?.displayName ?: "")
        txtUsername.setText(savedName)

        txtEmail.setText(currentUser?.email)

        // Load profile image: first check local, else Firebase
        val imagePath = prefs.getString("profile_image_path_$uid", null)
        if (!imagePath.isNullOrEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                userLogo.setImageBitmap(bitmap)
            } else {
                // fallback to Firebase profile photo if available
                val photoUrl = currentUser?.photoUrl
                if (photoUrl != null) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .into(userLogo)
                } else {
                    userLogo.setImageResource(R.drawable.userlogow)
                }
            }
        } else {
            val photoUrl = currentUser?.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(userLogo)
            } else {
                userLogo.setImageResource(R.drawable.userlogow)
            }
        }
    }

    private fun setEditable(editable: Boolean) {
        txtUsername.isEnabled = editable
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfileChanges() {
        val newName = txtUsername.text.toString().trim()
        prefs.edit().putString("username_$uid", newName).apply()
        selectedImageUri?.let { saveAndSetProfileImage(it) }

        // Notify UserActivity to update immediately
        sendBroadcast(Intent("update_user_ui"))

        isEditing = false
        setEditable(false)
        Toast.makeText(this, "Profile Updated Locally!", Toast.LENGTH_SHORT).show()
    }

    private fun saveAndSetProfileImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val file = File(filesDir, "profile_image_$uid.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            prefs.edit().putString("profile_image_path_$uid", file.absolutePath).apply()
            userLogo.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }
}
