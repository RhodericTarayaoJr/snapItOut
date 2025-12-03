package com.example.snapitout

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class UserActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var username: TextView
    private lateinit var email: TextView
    private lateinit var btnMyProfile: Button
    private lateinit var switchEventMode: Switch
    private lateinit var toolbar: MaterialToolbar
    private lateinit var homeIcon: ImageView
    private lateinit var albumIcon: ImageView

    private lateinit var about: TextView

    private lateinit var helps: TextView
    private lateinit var moreText: TextView
    private lateinit var exclusiveFeaturesLayout: LinearLayout
    private lateinit var logOutBtn: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth

    private val currentUser by lazy { FirebaseAuth.getInstance().currentUser }
    private val uid by lazy { currentUser?.uid ?: "default" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile)

        // ---------------- Init Views ----------------
        profileImage = findViewById(R.id.profileImage)
        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        btnMyProfile = findViewById(R.id.btnMyProfile)
        switchEventMode = findViewById(R.id.switch3)
        toolbar = findViewById(R.id.materialToolbar8)
        homeIcon = findViewById(R.id.home2btn)
        albumIcon = findViewById(R.id.imageView10)
        moreText = findViewById(R.id.moretxt)
        exclusiveFeaturesLayout = findViewById(R.id.exclusiveFeaturesLayout)
        logOutBtn = findViewById(R.id.logOutBtn)
        about = findViewById(R.id.about)
        helps = findViewById(R.id.helps)

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()

        val isEventModeOn = sharedPreferences.getBoolean("eventMode", false)
        applyEventModeState(isEventModeOn)

        switchEventMode.setOnCheckedChangeListener { _, isChecked ->
            applyEventModeState(isChecked)
            sharedPreferences.edit().putBoolean("eventMode", isChecked).apply()
        }

        exclusiveFeaturesLayout.setOnClickListener {
            if (switchEventMode.isChecked) {
                startActivity(Intent(this, ExclusiveActivity::class.java))
            } else {
                Toast.makeText(this, "Please turn on Event Mode.", Toast.LENGTH_SHORT).show()
            }
        }

        btnMyProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        homeIcon.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
        }

        albumIcon.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }

        about.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }

        helps.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        logOutBtn.setOnClickListener { logOutUser() }
    }

    override fun onResume() {
        super.onResume()
        // Reload username and profile image when returning from ProfileActivity
        loadUserData()
        loadUserProfileImage()
    }

    private fun loadUserData() {
        // Try to get per-account saved username first
        val savedUsername = sharedPreferences.getString("username_$uid", null)
        // If none, fallback to Firebase displayName (Google/Facebook)
        val usernameText = savedUsername ?: currentUser?.displayName ?: "User"
        val emailText = currentUser?.email ?: "noemail@snapitout.com"

        username.text = usernameText
        email.text = emailText
    }

    private fun loadUserProfileImage() {
        // Get per-account saved profile image
        val localProfilePath = sharedPreferences.getString("profile_image_path_$uid", null)

        when {
            localProfilePath != null -> {
                Glide.with(this)
                    .load(localProfilePath)
                    .circleCrop()
                    .into(profileImage)
            }
            else -> {
                val userPhotoUrl = currentUser?.photoUrl
                if (userPhotoUrl != null) {
                    Glide.with(this)
                        .load(userPhotoUrl)
                        .circleCrop()
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.userlogow)
                }
            }
        }
    }

    private fun applyEventModeState(isEnabled: Boolean) {
        switchEventMode.isChecked = isEnabled
        exclusiveFeaturesLayout.alpha = if (isEnabled) 1.0f else 0.4f
        exclusiveFeaturesLayout.isClickable = isEnabled
    }

    private fun logOutUser() {
        mAuth.signOut()
        sharedPreferences.edit().putBoolean("eventMode", false).apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
