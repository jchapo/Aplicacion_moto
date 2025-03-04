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

        auth = FirebaseAuth.getInstance()

        // Verificar si ya hay un usuario autenticado
        if (auth.currentUser != null) {
            val email = auth.currentUser?.email
            if (email != null) {
                obtenerRolDeFirestore(email)  // Verifica el rol y redirige a MainActivity si es necesario
            }
            return
        }

        setContentView(R.layout.activity_login)

        oneTapClient = Identity.getSignInClient(this)

        val signInButton = findViewById<SignInButton>(R.id.btnGoogleSignIn)

        signInButton.setOnClickListener {
            if (isSigningIn) return@setOnClickListener

            isSigningIn = true
            signInButton.isEnabled = false

            val signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                ).build()

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
                    signInButton.isEnabled = true
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
                    val nombre = document.getString("name") ?:""
                    val phone = document.getString("phone") ?:""
                    val nombreEmpresa = document.getString("nombreEmpresa") ?:""

                    if (rol == "Motorizado") {
                        Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("ruta", ruta)  // Pasa la variable "ruta"
                        }
                        startActivity(intent)
                        finish()
                    } else if (rol == "Cliente") {
                        Toast.makeText(this, "Bienvenido $nombre ", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, ClienteMainActivity::class.java).apply {
                            putExtra("phone", phone)  // Pasa la variable "phone"
                            putExtra("nombreEmpresa", nombreEmpresa)  // Pasa la variable "nombreEmpresa"
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Hubo un error", Toast.LENGTH_LONG).show()
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
