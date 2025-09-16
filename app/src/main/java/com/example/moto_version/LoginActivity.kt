package com.example.moto_version

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moto_version.cliente.ClienteMainActivity
import com.example.moto_version.gimi.GimiMainActivity1
import com.example.moto_version.moto.MainActivity
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
                obtenerRolDeFirestore()  // Verifica el rol y redirige a MainActivity si es necesario
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
                                user?.email?.let { obtenerRolDeFirestore() }
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

    private fun obtenerRolDeFirestore() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    SessionManager.rol = document.getString("rol") ?: ""
                    SessionManager.ruta = document.getString("ruta") ?: ""
                    SessionManager.nombre = document.getString("nombre") ?: ""
                    SessionManager.phone = document.getString("phone") ?: ""
                    SessionManager.nombreEmpresa = document.getString("nombreEmpresa") ?: ""

                    when (SessionManager.rol) {
                        "Motorizado" -> startActivity(Intent(this, MainActivity::class.java))
                        "Proveedor" -> startActivity(Intent(this, ClienteMainActivity::class.java))
                        "Admin" -> startActivity(Intent(this, GimiMainActivity1::class.java))
                    }
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error obteniendo rol", e)
            }
    }





}
