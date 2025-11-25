package com.example.snapitout

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var emailLogin: EditText
    private lateinit var passwordLogin: EditText
    private lateinit var loginBtn: Button
    private lateinit var goToSignUp: TextView
    private lateinit var togglePasswordIcon: ImageView
    private lateinit var googleSignInCustomBtn: ImageView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize UI elements
        emailLogin = findViewById(R.id.emailLogin)
        passwordLogin = findViewById(R.id.passwordLogin)
        loginBtn = findViewById(R.id.loginBtn)
        goToSignUp = findViewById(R.id.goToSignUp)
        togglePasswordIcon = findViewById(R.id.togglePasswordVisibility)
        googleSignInCustomBtn = findViewById(R.id.googleSignInCustomBtn)
        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Force sign out to avoid auto-login
        googleSignInClient.signOut()
        mAuth.signOut()

        // Toggle password visibility
        togglePasswordIcon.setOnClickListener {
            if (isPasswordVisible) {
                passwordLogin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_closed)
            } else {
                passwordLogin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordIcon.setImageResource(R.drawable.ic_eye_open)
            }
            isPasswordVisible = !isPasswordVisible
            passwordLogin.setSelection(passwordLogin.text.length)
        }

        // Email/password login
        loginBtn.setOnClickListener {
            val email = emailLogin.text.toString()
            val password = passwordLogin.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = mAuth.currentUser
                            user?.let {
                                saveCurrentUser(it.uid, it.displayName ?: "SnapItUser", it.email ?: "noemail@snapitout.com")
                            }

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomePageActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Navigate to SignUpActivity
        goToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Google Sign-In button
        googleSignInCustomBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    user?.let {
                        saveCurrentUser(it.uid, it.displayName ?: "SnapItUser", it.email ?: "noemail@snapitout.com")
                    }

                    Toast.makeText(this, "Google Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomePageActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveCurrentUser(uid: String, displayName: String, email: String) {
        val prefs = getSharedPreferences("SnapItOutPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("current_user_id", uid) // unique key for TemplatesActivity
            putString("username", displayName)
            putString("email", email)
            apply()
        }
    }
}
