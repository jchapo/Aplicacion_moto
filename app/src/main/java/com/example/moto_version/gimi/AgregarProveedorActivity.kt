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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Double.min
import java.util.Locale


class AgregarProveedorActivity() : AppCompatActivity() {
    private var etNombre: TextInputEditText? = null
    private var etApellido: TextInputEditText? = null
    private var etEmail: TextInputEditText? = null
    private var etNombreEmpresa: TextInputEditText? = null
    private var etPhone: TextInputEditText? = null
    private lateinit var btnCrearProveedor: Button
    private var progressBar: ProgressBar? = null
    private var db: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_proveedor)

        // Inicialización de Firestore
        db = FirebaseFirestore.getInstance()

        // Configuración del Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // Inicialización de vistas
        etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        etApellido = findViewById<TextInputEditText>(R.id.etApellido)
        etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        etNombreEmpresa = findViewById<TextInputEditText>(R.id.etNombreEmpresa)
        etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        btnCrearProveedor = findViewById<Button>(R.id.btnCrearProveedor)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Validación de campos en tiempo real
        setupTextWatchers()

        // Configuración del botón Crear
        btnCrearProveedor.setOnClickListener(View.OnClickListener { v: View? -> guardarProveedor() })
    }

    private fun setupTextWatchers() {
        val textWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                validarCampos()
            }
        }

        etNombre!!.addTextChangedListener(textWatcher)
        etApellido!!.addTextChangedListener(textWatcher)
        etEmail!!.addTextChangedListener(textWatcher)
        etNombreEmpresa!!.addTextChangedListener(textWatcher)
        etPhone!!.addTextChangedListener(textWatcher)

        // Formateo especial para teléfono
        etPhone!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val phone = s.toString()
                // Eliminar cualquier carácter que no sea dígito
                var cleaned = phone.replace("[^\\d]".toRegex(), "")


                // Limitar a 9 dígitos
                if (cleaned.length > 9) {
                    cleaned = cleaned.substring(0, 9)
                }


                // Si el texto es diferente, actualizar
                if (phone != cleaned) {
                    etPhone!!.removeTextChangedListener(this)
                    etPhone!!.setText(cleaned)
                    etPhone!!.setSelection(cleaned.length)
                    etPhone!!.addTextChangedListener(this)
                }
            }
        })
    }

    private fun validarCampos() {
        val todosLosCamposLlenos =
            (!etNombre!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                    !etApellido!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                    !etEmail!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                    !etNombreEmpresa!!.text.toString().trim { it <= ' ' }.isEmpty() &&
                    !etPhone!!.text.toString().trim { it <= ' ' }
                        .isEmpty()) && etPhone!!.text.toString().length == 9 // Verificar que tenga 9 dígitos

        btnCrearProveedor!!.isEnabled = todosLosCamposLlenos
    }

    private fun guardarProveedor() {
        // Mostrar progress bar
        btnCrearProveedor!!.visibility = View.INVISIBLE
        progressBar!!.visibility = View.VISIBLE

        // Obtener valores formateados según los requisitos
        val nombre = capitalizeFirstLetter(etNombre!!.text.toString().trim { it <= ' ' })
        val apellido = capitalizeFirstLetter(etApellido!!.text.toString().trim { it <= ' ' })
        val email = etEmail!!.text.toString().trim { it <= ' ' }.lowercase(Locale.getDefault())
        val nombreEmpresa = etNombreEmpresa!!.text.toString().trim { it <= ' ' }
            .uppercase(Locale.getDefault())
        val phone = etPhone!!.text.toString().trim { it <= ' ' }

        // Crear objeto Proveedor
        val proveedor = Proveedor(
            nombre,
            apellido,
            email,
            nombreEmpresa,
            phone,
            "Cliente" // Rol fijo como "Cliente" según el requisito
        )

        // Guardar en Firestore
        db!!.collection("proveedores")
            .add(proveedor)
            .addOnSuccessListener({ documentReference: DocumentReference? ->
                Toast.makeText(this, "Proveedor agregado con éxito", Toast.LENGTH_SHORT)
                    .show()
                finish()
            })
            .addOnFailureListener({ e: Exception? ->
                Toast.makeText(this, "Error al agregar proveedor", Toast.LENGTH_SHORT)
                    .show()
                // Ocultar progress bar y mostrar botón nuevamente
                btnCrearProveedor!!.setVisibility(View.VISIBLE)
                progressBar!!.setVisibility(View.INVISIBLE)
            })
    }

    // Método para capitalizar la primera letra
    private fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) {
            return text
        }
        return text.substring(0, 1).uppercase(Locale.getDefault()) + text.substring(1).lowercase(
            Locale.getDefault()
        )
    }
}