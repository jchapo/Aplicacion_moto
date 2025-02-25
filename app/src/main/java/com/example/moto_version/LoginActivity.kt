package com.example.moto_version

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private val db = FirebaseFirestore.getInstance()
    private var isSigningIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)

        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        val signInButton = findViewById<SignInButton>(R.id.btnGoogleSignIn)

        signInButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener  // Evita múltiples clics

            isSigningIn = true
            signInButton.isEnabled = false  // Deshabilita el botón

            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, 100,
                        null, 0, 0, 0, null
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("LoginActivity", "Error al iniciar sesión", e)
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                    isSigningIn = false
                    signInButton.isEnabled = true  // Rehabilita el botón en caso de error
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                user?.email?.let { obtenerRolDeFirestore(it) }
                            } else {
                                Toast.makeText(this, "Error en autenticación", Toast.LENGTH_SHORT)
                                    .show()
                                resetSignInState()  // Restablece estado si falla la autenticación
                            }
                        }
                } else {
                    resetSignInState()  // Restablece estado si no hay token
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error en onActivityResult", e)
                resetSignInState()
            }
        }
    }

    private fun resetSignInState() {
        isSigningIn = false
        findViewById<SignInButton>(R.id.btnGoogleSignIn).isEnabled = true
    }

    private fun obtenerRolDeFirestore(email: String) {
        db.collection("usuarios").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val rol = document.getString("rol") ?: "Usuario"
                    val ruta = document.getString("ruta") ?: ""  // Obtén la ruta desde Firestore

                    if (rol == "Motorizado") {
                        Toast.makeText(this, "Bienvenido Motorizado", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("ruta", ruta)  // Pasa la variable "ruta"
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "No eres motorizado", Toast.LENGTH_LONG).show()
                        auth.signOut()  // ❌ Cerrar sesión
                    }
                } else {
                    Toast.makeText(this, "Usuario no registrado", Toast.LENGTH_LONG).show()
                    auth.signOut()  // ❌ Cerrar sesión
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error obteniendo rol", exception)
            }
    }



}
