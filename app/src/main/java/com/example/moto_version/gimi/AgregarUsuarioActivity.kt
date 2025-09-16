package com.example.moto_version.gimi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moto_version.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.Locale

class AgregarUsuarioActivity : AppCompatActivity() {
    private var etNombre: TextInputEditText? = null
    private var etApellido: TextInputEditText? = null
    private var etEmail: TextInputEditText? = null
    private var etNombreEmpresa: TextInputEditText? = null
    private var etPhone: TextInputEditText? = null
    private lateinit var btnGuardar: Button
    private var progressBar: ProgressBar? = null
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var functions: FirebaseFunctions? = null // Nueva instancia de Functions
    private var userId: String? = null
    private var tipoUsuario: String? = null // "Proveedor" o "Motorizado"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_usuario)

        // Obtener parámetros
        userId = intent.getStringExtra("userId")
        tipoUsuario = intent.getStringExtra("tipoUsuario")

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance() // Inicializar Functions

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = if (userId.isNullOrEmpty()) "Agregar $tipoUsuario" else "Editar $tipoUsuario"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicialización de vistas
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etEmail = findViewById(R.id.etEmail)
        etNombreEmpresa = findViewById(R.id.etNombreEmpresa)

        if (tipoUsuario == "Motorizado") {
            etNombreEmpresa?.visibility = View.GONE
        }

        etPhone = findViewById(R.id.etPhone)
        btnGuardar = findViewById(R.id.btnAgregarUsuario)
        btnGuardar.text = if (userId.isNullOrEmpty()) "Guardar $tipoUsuario" else "Actualizar $tipoUsuario"
        progressBar = findViewById(R.id.progressBarAgregar)

        // Si es edición, cargar datos
        if (userId != null) {
            cargarDatosUsuario(userId!!)
        }

        setupTextWatchers()

        // Configuración del botón Guardar
        btnGuardar.setOnClickListener { guardarUsuario() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                validarCampos()
            }
        }

        etNombre?.addTextChangedListener(textWatcher)
        etApellido?.addTextChangedListener(textWatcher)
        etEmail?.addTextChangedListener(textWatcher)
        etNombreEmpresa?.addTextChangedListener(textWatcher)
        etPhone?.addTextChangedListener(textWatcher)

        // Formateo del número de teléfono
        etPhone?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                var cleaned = s.toString().replace("[^\\d]".toRegex(), "")
                if (cleaned.length > 9) {
                    cleaned = cleaned.substring(0, 9)
                }
                if (s.toString() != cleaned) {
                    etPhone?.removeTextChangedListener(this)
                    etPhone?.setText(cleaned)
                    etPhone?.setSelection(cleaned.length)
                    etPhone?.addTextChangedListener(this)
                }
            }
        })
    }

    private fun validarCampos() {
        val esProveedor = tipoUsuario == "Proveedor"
        val todosLosCamposLlenos =
            (!etNombre!!.text.isNullOrBlank() &&
                    !etApellido!!.text.isNullOrBlank() &&
                    !etEmail!!.text.isNullOrBlank() &&
                    (!esProveedor || !etNombreEmpresa!!.text.isNullOrBlank()) &&
                    !etPhone!!.text.isNullOrBlank() && etPhone!!.text!!.length == 9)

        btnGuardar.isEnabled = todosLosCamposLlenos
    }

    private fun guardarUsuario() {
        btnGuardar.visibility = View.INVISIBLE
        progressBar!!.visibility = View.VISIBLE

        val nombre = capitalizeFirstLetter(etNombre!!.text.toString().trim())
        val apellido = capitalizeFirstLetter(etApellido!!.text.toString().trim())
        val email = etEmail!!.text.toString().trim().lowercase(Locale.getDefault())
        val nombreEmpresa = if (tipoUsuario == "Proveedor") {
            etNombreEmpresa!!.text.toString().trim().uppercase(Locale.getDefault())
        } else {
            "TRABAJADOR_NANPI_COURIER"
        }
        val phone = etPhone!!.text.toString().trim()

        if (userId == null) {
            // Nuevo usuario - usar Firebase Function
            crearUsuarioConFunction(nombre, apellido, email, nombreEmpresa, phone)
        } else {
            // Actualizar usuario existente
            actualizarUsuarioExistente(userId!!, nombre, apellido, email, nombreEmpresa, phone)
        }
    }

    private fun crearUsuarioConFunction(nombre: String, apellido: String, email: String, nombreEmpresa: String, phone: String) {
        // Generar contraseña temporal
        val passwordTemporal = generarPasswordTemporal()

        // Crear el mapa de datos para enviar a la función
        val data = hashMapOf(
            "email" to email,
            "password" to passwordTemporal,
            "nombre" to nombre,
            "apellido" to apellido,
            "phone" to phone,
            "nombreEmpresa" to nombreEmpresa,
            "rol" to tipoUsuario
        )

        // Llamar a la función
        functions!!.getHttpsCallable("createUserWithRole")
            .call(data)
            .addOnSuccessListener { result ->
                // La función se ejecutó correctamente
                val resultData = result.data as? Map<String, Any>
                val message = resultData?.get("message") as? String ?: "Usuario creado correctamente"
                val uid = resultData?.get("uid") as? String

                Toast.makeText(this, "$tipoUsuario agregado con éxito", Toast.LENGTH_SHORT).show()

                // Opcional: Mostrar información de la contraseña temporal al administrador
                mostrarInfoPasswordTemporal(email, passwordTemporal)

                finish()
            }
            .addOnFailureListener { exception ->
                mostrarError("Error creando usuario: ${exception.message}")
            }
    }

    private fun actualizarUsuarioExistente(userId: String, nombre: String, apellido: String, email: String, nombreEmpresa: String, phone: String) {
        val usuario = Usuario(
            nombre,
            apellido,
            email,
            nombreEmpresa,
            phone,
            tipoUsuario,
            if (tipoUsuario == "Proveedor") "" else nombre
        )

        db!!.collection("usuarios").document(userId)
            .set(usuario)
            .addOnSuccessListener {
                Toast.makeText(this, "$tipoUsuario actualizado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                mostrarError("Error actualizando: ${exception.message}")
            }
    }

    private fun cargarDatosUsuario(userId: String) {
        db!!.collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etNombre?.setText(document.getString("nombre"))
                    etApellido?.setText(document.getString("apellido"))
                    etEmail?.setText(document.getString("email"))
                    etNombreEmpresa?.setText(document.getString("nombreEmpresa"))
                    etPhone?.setText(document.getString("phone"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarError(mensaje: String = "Error al guardar datos") {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        btnGuardar.visibility = View.VISIBLE
        progressBar!!.visibility = View.INVISIBLE
    }

    private fun capitalizeFirstLetter(text: String): String {
        return text.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }

    private fun generarPasswordTemporal(): String {
        // Genera una contraseña temporal segura
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..12)
            .map { chars.random() }
            .joinToString("")
    }

    private fun mostrarInfoPasswordTemporal(email: String, password: String) {
        // Aquí puedes mostrar la información de la contraseña temporal
        // Por ejemplo, en un dialog o enviarlo por otro medio al usuario
        Toast.makeText(
            this,
            "Usuario creado. Email: $email\nContraseña temporal: $password",
            Toast.LENGTH_LONG
        ).show()

        // También podrías copiar al portapapeles o enviar por email/SMS
    }
}