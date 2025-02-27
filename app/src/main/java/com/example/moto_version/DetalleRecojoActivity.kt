package com.example.moto_version

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.moto_version.models.Item
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import com.google.firebase.Timestamp


class DetalleRecojoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var item: Item? = null  // Variable global
    private lateinit var btnCamara: ImageButton
    private lateinit var btnCheck: ImageButton
    private lateinit var imagenRecojo: ImageView
    private lateinit var imagenEntrega: ImageView
    private var seRecogioImagen: Boolean = false
    private var seEntregoImagen: Boolean = false
    private var seSubioRecogioImagen: Boolean = false
    private var seSubioEntregaImagen: Boolean = false

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_recojo)

        val id = intent.getStringExtra("id")
        val clienteNombre = intent.getStringExtra("clienteNombre")
        val proveedorNombre = intent.getStringExtra("proveedorNombre")
        val pedidoCantidadCobrar = intent.getStringExtra("pedidoCantidadCobrar")
        val fechaRecojoTimestamp = intent.getLongExtra("fechaRecojoPedidoMotorizado", -1)

        val fechaRecojoPedidoMotorizado: Timestamp? = if (fechaRecojoTimestamp != -1L) {
            Timestamp(fechaRecojoTimestamp, 0) // Convertimos de segundos a Timestamp
        } else {
            null
        }
        Log.d("DetalleRecojoActivity", "Fecha de recogida recibida: $fechaRecojoPedidoMotorizado")

        val fechaEntregaTimestamp = intent.getLongExtra("fechaEntregaPedidoMotorizado", -1)

        val fechaEntregaPedidoMotorizado: Timestamp? = if (fechaEntregaTimestamp != -1L) {
            Timestamp(fechaEntregaTimestamp, 0) // Convertimos de segundos a Timestamp
        } else {
            null
        }

        val tvCliente = findViewById<TextView>(R.id.tvDetalleCliente)
        val tvProveedor = findViewById<TextView>(R.id.tvDetalleProveedor)
        val tvPrecio = findViewById<TextView>(R.id.tvDetallePrecio)
        val tvCardCliente = findViewById<LinearLayout>(R.id.tvCardCliente)
        val tvCardProveedor = findViewById<LinearLayout>(R.id.tvCardProveedor)

        val btnTelefonoCliente = findViewById<ImageButton>(R.id.btnTelefonoCliente)
        val btnTelefonoProveedor = findViewById<ImageButton>(R.id.btnTelefonoProveedor)
        val btnWhatsappCliente = findViewById<ImageButton>(R.id.btnWhatsappCliente)
        val btnWhatsappProveedor = findViewById<ImageButton>(R.id.btnWhatsappProveedor)
        btnCamara = findViewById<ImageButton>(R.id.btnCamara)

        val btnMapsPedido = findViewById<ImageButton>(R.id.btnMapsCliente)
        val btnMapsRecojo = findViewById<ImageButton>(R.id.btnMapsProveedor)

        imagenRecojo = findViewById(R.id.imagenRecojo)
        imagenEntrega = findViewById(R.id.imagenEntrega)
        btnCheck = findViewById(R.id.btnCheck)

        btnCamara.setOnClickListener {
            abrirCamara()
        }

        btnCheck.setOnClickListener {
            if (item?.fechaRecojoPedidoMotorizado == null && seSubioRecogioImagen) {
                actualizarEstadoPedido(1)
            } else if (item?.fechaRecojoPedidoMotorizado != null && seSubioEntregaImagen) {
                actualizarEstadoPedido(2)
            }
        }


        // Inicializamos los textos con los valores del intent
        tvCliente.text = clienteNombre
        tvProveedor.text = proveedorNombre
        tvPrecio.text = "Cobrar: S/ $pedidoCantidadCobrar"

        // Ocultar el LinearLayout si fechaRecojoPedidoMotorizado es null o está vacío
        if (fechaRecojoPedidoMotorizado == null) {
            tvPrecio.text = "RECOJO PENDIENTE"
            tvCardCliente.visibility = View.GONE
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            btnCamara.setBackgroundColor(typedValue.data)
            btnCheck.isEnabled = false  // Deshabilita el botón
        } else if (fechaEntregaPedidoMotorizado == null) {
            tvPrecio.backgroundTintList = ContextCompat.getColorStateList(this, R.color.teal_200)
            tvPrecio.textSize = 20f
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            btnCamara.setBackgroundColor(typedValue.data)
            btnCheck.isEnabled = false  // Deshabilita el botón
        }

        db = FirebaseFirestore.getInstance()

        Log.d("Firestore", "Id recibido: $id")
        if (id.isNullOrEmpty()) {
            Log.e("DetalleRecojoActivity", "El ID recibido es nulo o vacío")
            return
        }

        // Obtener datos de Firebase y asignarlos a la variable global
        db.collection("recojos").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("Firestore", "Documento encontrado: ${document.data}")
                    item = document.toObject(Item::class.java)  // Se asigna a la variable global

                    // Se actualizan los botones cuando los datos ya están cargados
                    item?.let { it ->
                        // Se verifica si thumbnailFotoRecojo es null
                        it.thumbnailFotoRecojo?.let { imageUrl ->
                            // Si no es null, se carga la imagen en imagenRecojo
                            Glide.with(this)
                                .load(imageUrl)
                                .into(imagenRecojo)
                        }

                        // Se actualizan los botones cuando los datos ya están cargados
                        actualizarBotonesLlamada(
                            btnTelefonoCliente, btnTelefonoProveedor,
                            btnWhatsappCliente, btnWhatsappProveedor,
                            btnMapsPedido, btnMapsRecojo, it
                        )
                    }



                } else {
                    Log.e("Firestore", "No se encontró el documento")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener datos", e)
            }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && item?.fechaRecojoPedidoMotorizado == null) {
            val data: Intent? = result.data
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                seSubioRecogioImagen = false
                seRecogioImagen = true
                imagenRecojo.setImageBitmap(imageBitmap)
                subirFotosFirestore(imageBitmap,1)
            } else {
                Log.e("Cámara", "No se recibió imagen recojo desde la cámara.")
            }
        } else if (result.resultCode == RESULT_OK && item?.fechaRecojoPedidoMotorizado != null) {
            val data: Intent? = result.data
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                seSubioEntregaImagen = false
                seEntregoImagen = true
                imagenEntrega.setImageBitmap(imageBitmap)
                subirFotosFirestore(imageBitmap,2)
            } else {
                Log.e("Cámara", "No se recibió imagen entrega desde la cámara.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirCamara()
                } else {
                    Log.e("Permisos", "El usuario denegó el permiso para la cámara.")
                }
            }
            1 -> { // Este es el permiso para llamadas
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permisos", "Permiso de llamadas concedido.")
                } else {
                    Log.e("Permisos", "El usuario denegó el permiso para realizar llamadas.")
                }
            }
        }
    }



    private fun actualizarBotonesLlamada(
        btnCliente: ImageButton, btnProveedor: ImageButton,
        btnWhatsappCliente: ImageButton, btnWhatsappProveedor: ImageButton,
        btnMapsPedido: ImageButton, btnMapsRecojo: ImageButton,
        item: Item
    ) {
        val clienteTelefono = item.clienteTelefono ?: ""
        val proveedorTelefono = item.proveedorTelefono ?: ""

        val pedidoLat = item.pedidoCoordenadas?.get("lat") ?: 0.0
        val pedidoLong = item.pedidoCoordenadas?.get("lng") ?: 0.0
        val recojoLat = item.recojoCoordenadas?.get("lat") ?: 0.0
        val recojoLong = item.recojoCoordenadas?.get("lng") ?: 0.0

        // Log de coordenadas para depuración
        Log.d("Coordenadas", "Pedido: ($pedidoLat, $pedidoLong), Recojo: ($recojoLat, $recojoLong)")

        // Botón de llamada
        btnCliente.setOnClickListener { iniciarLlamada(clienteTelefono) }
        btnProveedor.setOnClickListener { iniciarLlamada(proveedorTelefono) }

        // Botón de WhatsApp
        btnWhatsappCliente.setOnClickListener { abrirWhatsApp(clienteTelefono) }
        btnWhatsappProveedor.setOnClickListener { abrirWhatsApp(proveedorTelefono) }

        // Botón de Google Maps (ahora en modo navegación)
        btnMapsPedido.setOnClickListener {
            Log.d("Maps", "Iniciando navegación a pedido: ($pedidoLat, $pedidoLong)")
            abrirGoogleMaps(pedidoLat, pedidoLong)
        }
        btnMapsRecojo.setOnClickListener {
            Log.d("Maps", "Iniciando navegación a recojo: ($recojoLat, $recojoLong)")
            abrirGoogleMaps(recojoLat, recojoLong)
        }

        // Agregar aquí los botones de Waze
        val btnWazeCliente = findViewById<ImageButton>(R.id.btnWazeCliente)
        val btnWazeProveedor = findViewById<ImageButton>(R.id.btnWazeProveedor)

        btnWazeCliente.setOnClickListener {
            Log.d("Waze", "Iniciando navegación a pedido: ($pedidoLat, $pedidoLong)")
            abrirWaze(pedidoLat, pedidoLong)
        }

        btnWazeProveedor.setOnClickListener {
            Log.d("Waze", "Iniciando navegación a recojo: ($recojoLat, $recojoLong)")
            abrirWaze(recojoLat, recojoLong)
        }
    }


    private fun abrirWaze(lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            try {
                // URI para abrir navegación en Waze
                val wazeUri = Uri.parse("waze://?ll=$lat,$lng&navigate=yes")
                val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)

                // Verificar si Waze está instalado
                if (wazeIntent.resolveActivity(packageManager) != null) {
                    startActivity(wazeIntent)
                } else {
                    // Si Waze no está instalado, abrir en navegador para descargar
                    val webWazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
                    val webIntent = Intent(Intent.ACTION_VIEW, webWazeUri)
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("Waze", "Error al abrir Waze", e)
                // Fallback - abrir en navegador si hay error
                val webWazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
                val webIntent = Intent(Intent.ACTION_VIEW, webWazeUri)
                startActivity(webIntent)
            }
        } else {
            Log.e("Waze", "Coordenadas no disponibles (lat=$lat, long=$lng)")
        }
    }

    private fun abrirWhatsApp(numero: String) {
        try {
            val uri = Uri.parse("https://wa.me/$numero")  // Enlace directo a WhatsApp
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp") // Asegura que se abra en WhatsApp
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WhatsApp", "No se pudo abrir WhatsApp", e)
        }
    }

    private fun abrirGoogleMaps(lat: Double, lng: Double) {
        if (lat != 0.0 && lng != 0.0) {
            try {
                // URI para abrir Google Maps en modo navegación
                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                // Verificar si Google Maps está instalado
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Si Google Maps no está instalado, abrir en navegador
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                    startActivity(browserIntent)
                }
            } catch (e: Exception) {
                Log.e("Google Maps", "Error al abrir Google Maps", e)
                // Fallback - abrir en navegador si hay error
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
                startActivity(browserIntent)
            }
        } else {
            Log.e("Google Maps", "Coordenadas no disponibles (lat=$lat, lng=$lng)")
        }
    }

    private fun iniciarLlamada(numero: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$numero")
        }

        // Verificar permiso antes de iniciar la llamada
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            // Solicitar permiso en tiempo de ejecución
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    private fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

    private fun subirFotosFirestore(imageBitmap: Bitmap, tipo: Int) {
        val storageRef = Firebase.storage.reference
        val pedidoId = item?.id ?: return

        val tipoOperacion = if (tipo == 1) "Recojo" else "Entrega"
        val collectionName = "recojos"

        // Comprimir imagen principal
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageData = baos.toByteArray()

        // Crear thumbnail (imagen pequeña)
        val baosThumbnail = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baosThumbnail)
        val thumbnailData = baosThumbnail.toByteArray()

        // Subir imagen principal
        val imageRef = storageRef.child("fotospedidos/$pedidoId/$tipoOperacion.jpg")
        val thumbnailRef = storageRef.child("fotospedidos/$pedidoId/${tipoOperacion}_thumbnail.jpg")

        imageRef.putBytes(imageData).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                Firebase.firestore.collection(collectionName).document(pedidoId)
                    .update("pedidoFoto$tipoOperacion", imageUrl.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error al subir imagen de $tipoOperacion")
            Toast.makeText(this, "Error al subir la imagen de $tipoOperacion", Toast.LENGTH_SHORT).show()
        }

        // Subir thumbnail
        thumbnailRef.putBytes(thumbnailData).addOnSuccessListener {
            thumbnailRef.downloadUrl.addOnSuccessListener { thumbnailUrl ->
                Firebase.firestore.collection(collectionName).document(pedidoId)
                    .update("thumbnailFoto$tipoOperacion", thumbnailUrl.toString())

                // Activar botones después de subir la imagen
                btnCamara.background = null
                val typedValue = TypedValue()
                theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
                btnCheck.setBackgroundColor(typedValue.data)
                btnCheck.isEnabled = true
                if (tipo == 1) seSubioRecogioImagen = true else seSubioEntregaImagen = true
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error al subir thumbnail de $tipoOperacion")
            Toast.makeText(this, "Error al subir el thumbnail de $tipoOperacion", Toast.LENGTH_SHORT).show()
        }
    }


        private fun actualizarEstadoPedido(tipo: Int) {
            val pedidoId = item?.id ?: return
            val fecha = Timestamp.now()

            val tipoOperacion = if (tipo == 1) "Recojo" else "Entrega"
            val campoFecha = if (tipo == 1) "fechaRecojoPedidoMotorizado" else "fechaEntregaPedidoMotorizado"
            val collectionName = "recojos"

            val db = FirebaseFirestore.getInstance()
            db.collection(collectionName).document(pedidoId)
                .update(campoFecha, fecha)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido de $tipoOperacion actualizado con éxito.", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la actividad y vuelve a la anterior
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al actualizar la fecha de $tipoOperacion", e)
                    Toast.makeText(this, "Error al actualizar el pedido.", Toast.LENGTH_SHORT).show()
                }
        }


    }
