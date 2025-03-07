package com.example.moto_version.moto

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
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.example.moto_version.R
import com.example.moto_version.cliente.EditClientDistrictActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import com.google.firebase.Timestamp
import java.io.File
import java.io.InputStream
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.moto_version.SessionManager
import com.example.moto_version.cliente.OrderFormActivity


class DetalleRecojoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var item: Item? = null  // Variable global
    private lateinit var btnCamara: ImageButton
    private lateinit var btnCheck: ImageButton
    private lateinit var btnEditarCliente: ImageButton
    private lateinit var btnEditarPedido: ImageButton
    private lateinit var imagenRecojo: ImageView
    private lateinit var imagenEntrega: ImageView
    private lateinit var tvDetalleScroll: ScrollView
    private var seRecogioImagen: Boolean = false
    private var seEntregoImagen: Boolean = false
    private var seSubioRecogioImagen: Boolean = false
    private var seSubioEntregaImagen: Boolean = false
    private var nombreEmpresa = SessionManager.nombreEmpresa ?: ""
    private var orderId: String = ""

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_recojo)

        val id = intent.getStringExtra("id") ?: ""
        orderId = id
        val clienteNombre = intent.getStringExtra("clienteNombre")
        val proveedorNombre = intent.getStringExtra("proveedorNombre")
        val pedidoMetodoPago = intent.getStringExtra("pedidoMetodoPago")
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
        val tvDetalleScroll: ScrollView = findViewById(R.id.tvDetalleScroll)


        val btnTelefonoCliente = findViewById<ImageButton>(R.id.btnTelefonoCliente)
        val btnTelefonoProveedor = findViewById<ImageButton>(R.id.btnTelefonoProveedor)
        val btnWhatsappCliente = findViewById<ImageButton>(R.id.btnWhatsappCliente)
        val btnWhatsappProveedor = findViewById<ImageButton>(R.id.btnWhatsappProveedor)
        val frameEditarPedido = findViewById<FrameLayout>(R.id.frameEditarPedido)
        btnCamara = findViewById<ImageButton>(R.id.btnCamara)

        val btnMapsPedido = findViewById<ImageButton>(R.id.btnMapsCliente)
        val btnMapsRecojo = findViewById<ImageButton>(R.id.btnMapsProveedor)

        imagenRecojo = findViewById(R.id.imagenRecojo)
        imagenEntrega = findViewById(R.id.imagenEntrega)
        btnCheck = findViewById(R.id.btnCheck)
        btnEditarCliente = findViewById(R.id.btnEditarCliente)
        btnEditarPedido = findViewById(R.id.btnEditarPedido)

        if (SessionManager.nombreEmpresa == "ADMIN_NANPI_COURIER") {
            frameEditarPedido.visibility = View.VISIBLE  // Mostrar botón
        }

        btnEditarPedido.setOnClickListener {
            val intent = Intent(this, OrderFormActivity::class.java)
            intent.putExtra("orderId", orderId) // orderId es el ID del pedido que se desea editar
            intent.putExtra("isEditMode", true) // Pasar el extra como booleano
            this.startActivity(intent)
        }

        val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
        btnCamara.backgroundTintList = color
        btnCheck.isEnabled = false  // Deshabilita el botón
        btnCheck.alpha = 0.5f       // Reduce la opacidad al 50%


        btnCamara.setOnClickListener {
            btnCamara.isEnabled = false
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
        tvPrecio.text = "S/ $pedidoCantidadCobrar - $pedidoMetodoPago"

        // Ocultar el LinearLayout si fechaRecojoPedidoMotorizado es null o está vacío
        if (fechaRecojoPedidoMotorizado == null) {
            tvPrecio.text = "RECOJO PENDIENTE"
            tvCardCliente.visibility = View.GONE
        } else {
            tvPrecio.isClickable = true  // Habilita clics
            tvPrecio.setOnClickListener {
                mostrarDialogoConImagen()
            }
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

                        it.thumbnailFotoEntrega?.let { imageUrl ->
                            // Si no es null, se carga la imagen en imagenRecojo
                            Glide.with(this)
                                .load(imageUrl)
                                .into(imagenEntrega)
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

        btnEditarCliente.setOnClickListener {
            val clienteId = item?.id // Asumiendo que item tiene una propiedad id
            val clienteDistrito = item?.clienteDistrito // Asumiendo que la variable contiene el distrito actual

            // Crear intent para iniciar la nueva actividad
            val intent = Intent(this, EditClientDistrictActivity::class.java).apply {
                putExtra("clienteId", clienteId)
                putExtra("clienteDistrito", clienteDistrito)
            }
            this.startActivity(intent)
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

    private fun mostrarDialogoConImagen() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_imagen, null)

        // Obtener el ImageView y asignarle la imagen
        val imgDialog = dialogView.findViewById<ImageView>(R.id.imgDialog)
        imgDialog.setImageResource(R.drawable.logo_nanpi)

        // Usar MaterialAlertDialogBuilder para un diseño más moderno
        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .show()
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
                // URI para abrir el punto en el mapa
                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Punto Seleccionado)")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")

                // Verificar si Google Maps está instalado
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Si Google Maps no está instalado, abrir en navegador
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                    )
                    startActivity(browserIntent)
                }
            } catch (e: Exception) {
                Log.e("Google Maps", "Error al abrir Google Maps", e)
                // Fallback - abrir en navegador si hay error
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                )
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


    private var photoUri: Uri? = null
    private var mainImageUploaded = false
    private var thumbnailUploaded = false

    private fun abrirCamara() {
        try {
            val photoFile = File.createTempFile("IMG_", ".jpg", cacheDir).apply {
                deleteOnExit()  // Se eliminará cuando la app cierre
            }
            photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }

            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            btnCamara.isEnabled = true
            btnCheck.alpha = 1f       // Reduce la opacidad al 50%
            Log.e("Cámara", "Error al abrir la cámara: ${e.message}")
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && photoUri != null) {
            try {
                // Aquí ya no buscamos el bitmap en data?.extras
                // La imagen está guardada en photoUri
                if (item?.fechaRecojoPedidoMotorizado == null) {
                    // Caso de recojo
                    seSubioRecogioImagen = false
                    seRecogioImagen = true
                    imagenRecojo.setImageURI(photoUri) // Mostrar la imagen capturada
                    subirFotosFirestore(photoUri!!, 1) // Subir imagen a Firestore
                } else {
                    // Caso de entrega
                    seSubioEntregaImagen = false
                    seEntregoImagen = true
                    imagenEntrega.setImageURI(photoUri) // Mostrar la imagen capturada
                    subirFotosFirestore(photoUri!!, 21) // Subir imagen a Firestore
                }
            } catch (e: Exception) {
                Log.e("Cámara", "Error al procesar la imagen: ${e.message}")
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                btnCamara.setImageResource(R.drawable.camera_solid)
                btnCamara.isEnabled = true
            }
        } else {
            Log.d("Cámara", "Operación cancelada o fallida: ${result.resultCode}")
            btnCamara.setImageResource(R.drawable.camera_solid)
            btnCamara.isEnabled = true
        }
    }

    private fun getCompressedImageAndThumbnail(
        context: Context,
        uri: Uri,
        compressionQuality: Int = 60, // Similar a WhatsApp (~70%)
        thumbnailSize: Int = 250 // Tamaño máximo del thumbnail
    ): Pair<ByteArray, ByteArray> {
        // Abrir el stream de la imagen original
        val inputStream = context.contentResolver.openInputStream(uri)

        // Obtener las dimensiones originales sin cargar toda la imagen
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // Reabrir el stream para decodificar la imagen real
        val inputStream2 = context.contentResolver.openInputStream(uri)

        // Decodificar la imagen completa
        val originalBitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2?.close()

        // Corregir la orientación de la imagen
        val correctedBitmap = correctOrientation(context, uri, originalBitmap)

        // 1. Generar la imagen comprimida (estilo WhatsApp)
        val compressedBaos = ByteArrayOutputStream()
        correctedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, compressedBaos)
        val compressedBytes = compressedBaos.toByteArray()

        // 2. Calcular las dimensiones del thumbnail manteniendo la relación de aspecto
        val width = correctedBitmap.width
        val height = correctedBitmap.height
        val ratio = width.toFloat() / height.toFloat()

        val thumbnailWidth: Int
        val thumbnailHeight: Int

        if (width > height) {
            thumbnailWidth = thumbnailSize
            thumbnailHeight = (thumbnailSize / ratio).toInt()
        } else {
            thumbnailHeight = thumbnailSize
            thumbnailWidth = (thumbnailSize * ratio).toInt()
        }

        // Crear el thumbnail redimensionando el bitmap
        val thumbnailBitmap = Bitmap.createScaledBitmap(
            correctedBitmap,
            thumbnailWidth,
            thumbnailHeight,
            true
        )

        // Comprimir el thumbnail
        val thumbnailBaos = ByteArrayOutputStream()
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 60, thumbnailBaos)
        val thumbnailBytes = thumbnailBaos.toByteArray()

        // Liberar los recursos
        if (thumbnailBitmap != correctedBitmap) {
            thumbnailBitmap.recycle()
        }
        if (correctedBitmap != originalBitmap) {
            correctedBitmap.recycle()
        }
        originalBitmap.recycle()

        return Pair(compressedBytes, thumbnailBytes)
    }

    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface(inputStream!!)
            } else {
                uri.path?.let { ExifInterface(it) } ?: return bitmap
            }

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.preRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.preRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return bitmap
            }

            return try {
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )

                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }

                rotatedBitmap
            } catch (e: OutOfMemoryError) {
                Log.e("ImageProcessing", "OutOfMemoryError al rotar la imagen: ${e.message}")
                bitmap
            }
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error al corregir orientación: ${e.message}")
            return bitmap
        } finally {
            inputStream?.close()
        }
    }

    private fun eliminarArchivo(uri: Uri) {
        try {
            val file = File(uri.path!!)
            if (file.exists()) {
                file.delete()
                Log.d("Archivo", "Imagen eliminada: ${uri.path}")
            }
        } catch (e: Exception) {
            Log.e("Archivo", "Error al eliminar archivo: ${e.message}")
        }
    }

    private fun subirFotosFirestore(imageUri: Uri, tipo: Int) {
        btnCamara.setImageResource(R.drawable.loading_animation)

        val pedidoId = item?.id ?: return
        val tipoOperacion = if (tipo == 1) "Recojo" else "Entrega"
        val collectionName = "recojos"

        mainImageUploaded = false
        thumbnailUploaded = false

        val storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("fotospedidos/$pedidoId/$tipoOperacion.jpg")
        val thumbnailRef = storageRef.child("fotospedidos/$pedidoId/${tipoOperacion}_thumbnail.jpg")

        try {
            // Convertir URI a bytes comprimidos antes de subir
            val (imageData, thumbnailData) = getCompressedImageAndThumbnail(this, imageUri)

            // Subir imagen principal
            imageRef.putBytes(imageData).addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                    Firebase.firestore.collection(collectionName).document(pedidoId)
                        .update("pedidoFoto$tipoOperacion", imageUrl.toString())
                        .addOnSuccessListener {
                            mainImageUploaded = true
                            checkAllUploadsCompleted(tipo)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error al actualizar URL en Firestore: ${e.message}")
                            mostrarErrorYReiniciar()
                        }
                }.addOnFailureListener { e ->
                    Log.e("Firebase", "Error al obtener URL de descarga: ${e.message}")
                    mostrarErrorYReiniciar()
                }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error al subir imagen de $tipoOperacion: ${e.message}")
                mostrarErrorYReiniciar()
            }

            // Subir el thumbnail
            thumbnailRef.putBytes(thumbnailData).addOnSuccessListener {
                thumbnailRef.downloadUrl.addOnSuccessListener { thumbnailUrl ->
                    Firebase.firestore.collection(collectionName).document(pedidoId)
                        .update("thumbnailFoto$tipoOperacion", thumbnailUrl.toString())
                        .addOnSuccessListener {
                            thumbnailUploaded = true
                            checkAllUploadsCompleted(tipo)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error al actualizar URL del thumbnail: ${e.message}")
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("Firebase", "Error al subir thumbnail: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error general al procesar imagen: ${e.message}")
            mostrarErrorYReiniciar()
        }
    }

    private fun checkAllUploadsCompleted(tipo: Int) {
        if (mainImageUploaded && thumbnailUploaded) {
            if (tipo == 1) {
                seSubioRecogioImagen = true
                //Toast.makeText(this, "Imagen de recojo subida correctamente", Toast.LENGTH_SHORT).show()
            } else {
                seSubioEntregaImagen = true
                //Toast.makeText(this, "Imagen de entrega subida correctamente", Toast.LENGTH_SHORT).show()
            }

            btnCamara.setImageResource(R.drawable.camera_solid)
            btnCamara.backgroundTintList = null
            btnCamara.isEnabled = true

            val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
            btnCheck.backgroundTintList = color
            btnCheck.isEnabled = true
            btnCheck.alpha = 1f

            // Eliminar la imagen después de subida completada
            photoUri?.let { eliminarArchivo(it) }
        }
    }

    private fun mostrarErrorYReiniciar() {
        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        btnCamara.setImageResource(R.drawable.camera_solid)
        btnCamara.isEnabled = true
    }


    // Método para verificar si todas las subidas han terminado
    /*private fun checkAllUploadsCompleted(tipo: Int, mainImageUploaded: Boolean, thumbnailUploaded: Boolean) {
        if (mainImageUploaded && thumbnailUploaded) {
            // Restaurar el ícono de la cámara
            btnCamara.setImageResource(R.drawable.camera_solid)
            btnCamara.isEnabled = true
            btnCamara.background = null

            // Activar botón de verificación
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            btnCheck.setBackgroundColor(typedValue.data)
            btnCheck.isEnabled = true

            if (tipo == 1) seSubioRecogioImagen = true else seSubioEntregaImagen = true
        }
    }*/


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
                    Toast.makeText(this, "$tipoOperacion finalizado con éxito.", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la actividad y vuelve a la anterior
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al actualizar la fecha de $tipoOperacion", e)
                    Toast.makeText(this, "Error al actualizar el pedido.", Toast.LENGTH_SHORT).show()
                }
        }


    }
