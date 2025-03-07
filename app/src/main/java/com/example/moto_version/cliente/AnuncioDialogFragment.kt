package com.example.moto_version.cliente

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.moto_version.R
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AnuncioDialogFragment : DialogFragment() {

    private val TAG = "AnuncioDialog"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_anuncio)

        // Configurar para pantalla completa
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnCerrar = dialog.findViewById<Button>(R.id.btnCerrar)
        val imgAnuncio = dialog.findViewById<ImageView>(R.id.imgAnuncio)

        // Inicialmente ocultar el botón
        btnCerrar.visibility = android.view.View.INVISIBLE

        Log.d(TAG, "Iniciando verificación de imagen para el modal")

        // Listar contenido de Storage
        listarContenidoStorage(imgAnuncio, dialog)

        // Configurar el botón para cerrar el diálogo
        btnCerrar.setOnClickListener {
            Log.d(TAG, "Botón cerrar presionado")
            dismiss()
        }

        return dialog
    }

    private fun listarContenidoStorage(imageView: ImageView, dialog: Dialog) {
        // Referencia al directorio raíz de imagenPublicidad
        val storageReference = FirebaseStorage.getInstance().reference.child("imagenPublicidad")

        Log.d(TAG, "Listando contenido de 'imagenPublicidad'...")

        // Listar todo el contenido para diagnóstico
        storageReference.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) {
                    Log.e(TAG, "El directorio 'imagenPublicidad' está vacío")
                    Toast.makeText(context, "No hay imágenes disponibles", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                // Registrar todos los archivos encontrados para diagnóstico
                Log.d(TAG, "Archivos encontrados en 'imagenPublicidad': ${result.items.size}")
                for (item in result.items) {
                    Log.d(TAG, "Archivo encontrado: ${item.name}")
                }

                // Intentar cargar el archivo específico primero (06-03-2025)
                val fechaHoy = "06-03-2025"

                // Probar con diferentes extensiones comunes
                probarCargaConExtensiones(imageView, fechaHoy, dialog) { exito ->
                    if (!exito) {
                        // Si no funciona, cargar el archivo más reciente
                        cargarImagenMasReciente(result.items, imageView, dialog)
                    }

                    // Mostrar el botón después de 3 segundos
                    mostrarBotonConRetraso(dialog)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al listar contenido de Storage: ${e.message}")
                Toast.makeText(context, "Error al cargar imágenes", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
    }

    private fun mostrarBotonConRetraso(dialog: Dialog) {
        val btnCerrar = dialog.findViewById<Button>(R.id.btnCerrar)
        Handler(Looper.getMainLooper()).postDelayed({
            btnCerrar.visibility = android.view.View.VISIBLE
        }, 3000) // 3 segundos de retraso
    }

    private fun probarCargaConExtensiones(imageView: ImageView, nombreBase: String, dialog: Dialog, callback: (Boolean) -> Unit) {
        // Lista de extensiones comunes para probar
        val extensiones = listOf("", ".jpg", ".jpeg", ".png", ".webp")
        var intentosRestantes = extensiones.size
        var exitoso = false

        for (extension in extensiones) {
            val nombreCompleto = nombreBase + extension
            val ref = FirebaseStorage.getInstance().reference.child("imagenPublicidad/$nombreCompleto")

            Log.d(TAG, "Intentando cargar: imagenPublicidad/$nombreCompleto")

            ref.downloadUrl
                .addOnSuccessListener { uri ->
                    if (!exitoso) {
                        exitoso = true
                        Log.d(TAG, "Éxito al cargar: $nombreCompleto con URL: $uri")

                        // Configurar opciones para mantener proporción 3:4
                        val requestOptions = RequestOptions()
                            .centerInside()
                            .override(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                        Glide.with(requireContext())
                            .load(uri)
                            .apply(requestOptions)
                            .placeholder(R.drawable.fondo_gris_nanpi)
                            .error(R.drawable.fondo_gris_nanpi)
                            .into(imageView)

                        callback(true)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Falló la carga de $nombreCompleto: ${e.message}")
                    intentosRestantes--

                    if (intentosRestantes == 0 && !exitoso) {
                        Log.d(TAG, "Todas las extensiones fallaron. Pasando a la siguiente opción.")
                        callback(false)
                    }
                }
        }
    }

    private fun cargarImagenMasReciente(items: List<com.google.firebase.storage.StorageReference>,
                                        imageView: ImageView, dialog: Dialog) {
        Log.d(TAG, "Buscando la imagen más reciente por metadatos...")

        if (items.isEmpty()) {
            dialog.dismiss()
            return
        }

        // Primero intentamos usar la imagen más reciente según el último archivo modificado
        var procesados = 0
        var masRecienteRef: com.google.firebase.storage.StorageReference? = null
        var timestampMasReciente: Long = 0

        for (item in items) {
            item.metadata
                .addOnSuccessListener { metadata ->
                    val updateTime = metadata.updatedTimeMillis
                    Log.d(TAG, "Archivo: ${item.name}, actualizado: $updateTime")

                    if (updateTime > timestampMasReciente) {
                        timestampMasReciente = updateTime
                        masRecienteRef = item
                    }

                    procesados++

                    // Cuando ya procesamos todos los archivos
                    if (procesados == items.size) {
                        masRecienteRef?.let { ref ->
                            Log.d(TAG, "Archivo más reciente: ${ref.name}")

                            ref.downloadUrl
                                .addOnSuccessListener { uri ->
                                    Log.d(TAG, "Cargando imagen más reciente: $uri")

                                    // Configurar opciones para mantener proporción 3:4
                                    val requestOptions = RequestOptions()
                                        .centerInside()
                                        .override(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                                    Glide.with(requireContext())
                                        .load(uri)
                                        .apply(requestOptions)
                                        .placeholder(R.drawable.fondo_gris_nanpi)
                                        .error(R.drawable.fondo_gris_nanpi)
                                        .into(imageView)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error al cargar la imagen más reciente: ${e.message}")
                                    dialog.dismiss()
                                }
                        } ?: run {
                            Log.e(TAG, "No se pudo determinar la imagen más reciente")
                            dialog.dismiss()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al obtener metadatos de ${item.name}: ${e.message}")
                    procesados++

                    if (procesados == items.size && masRecienteRef == null) {
                        Log.e(TAG, "No se pudo determinar la imagen más reciente")
                        dialog.dismiss()
                    }
                }
        }
    }
}