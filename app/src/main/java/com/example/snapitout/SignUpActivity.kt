package com.example.snapitout

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameSignUp: EditText
    private lateinit var emailSignUp: EditText
    private lateinit var passwordSignUp: EditText
    private lateinit var signUpBtn: Button
    private lateinit var goToLogin: TextView
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var mAuth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        nameSignUp = findViewById(R.id.nameSignUp)
        emailSignUp = findViewById(R.id.emailSignUp)
        passwordSignUp = findViewById(R.id.passwordSignUp)
        signUpBtn = findViewById(R.id.signUpBtn)
        goToLogin = findViewById(R.id.goToLogin)
        togglePasswordIcon = findViewById(R.id.togglePasswordVisibility)
        mAuth = FirebaseAuth.getInstance()

        // Toggle password visibility
        togglePasswordIcon.setOnClickListener {
            if (isPasswordVisible) {
                passwordSignUp.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_closed)
            } else {
                passwordSignUp.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_open)
            }
            isPasswordVisible = !isPasswordVisible
            passwordSignUp.setSelection(passwordSignUp.text.length)
        }

        signUpBtn.setOnClickListener {
            val name = nameSignUp.text.toString().trim()
            val email = emailSignUp.text.toString().trim()
            val password = passwordSignUp.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Get the UID of the newly created user
                        val uid = mAuth.currentUser?.uid ?: "default"

                        // Save user info locally in user_prefs
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("username_$uid", name)
                            .putString("email_$uid", email)
                            .putBoolean("eventMode", false)
                            .apply()

                        // Also update Firebase displayName
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        mAuth.currentUser?.updateProfile(profileUpdates)

                        Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }


        goToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Close the SignUpActivity
        }
    }
}
