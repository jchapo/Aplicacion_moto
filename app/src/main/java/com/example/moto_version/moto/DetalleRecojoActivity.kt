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
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.example.moto_version.gimi.PagosAdapter
import com.example.moto_version.models.PagoRegistro
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class DetalleRecojoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var item: Item? = null  // Variable global
    private lateinit var btnCamara: ImageButton
    private lateinit var btnDinero: ImageButton
    private lateinit var btnCheck: ImageButton
    private lateinit var btnEditarCliente: ImageButton
    private lateinit var btnEditarPedido: ImageButton
    private lateinit var imagenRecojo: ImageView
    private lateinit var imagenEntrega: ImageView
    private lateinit var imagenDinero: ImageView
    private lateinit var linearLayoutContacto: LinearLayout
    private lateinit var layout_botones: LinearLayout
    private lateinit var cardDinero: CardView
    private lateinit var tvCardProveedor: LinearLayout
    private var seRecogioImagen: Boolean = false
    private var seEntregoImagen: Boolean = false
    private var seDineroImagen: Boolean = false
    private var seSubioRecogioImagen: Boolean = false
    private var seSubioEntregaImagen: Boolean = false
    private var seSubioDineroImagen: Boolean = false

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
        val tvDetalleScroll: ScrollView = findViewById(R.id.tvDetalleScroll)

        val btnTelefonoCliente = findViewById<ImageButton>(R.id.btnTelefonoCliente)
        val btnTelefonoProveedor = findViewById<ImageButton>(R.id.btnTelefonoProveedor)
        val btnWhatsappCliente = findViewById<ImageButton>(R.id.btnWhatsappCliente)
        val btnWhatsappProveedor = findViewById<ImageButton>(R.id.btnWhatsappProveedor)
        val frameEditarPedido = findViewById<FrameLayout>(R.id.frameEditarPedido)
        btnCamara = findViewById(R.id.btnCamara)
        btnDinero = findViewById(R.id.btnDinero)

        val btnMapsPedido = findViewById<ImageButton>(R.id.btnMapsCliente)
        val btnMapsRecojo = findViewById<ImageButton>(R.id.btnMapsProveedor)

        imagenRecojo = findViewById(R.id.imagenRecojo)
        imagenEntrega = findViewById(R.id.imagenEntrega)
        imagenDinero = findViewById(R.id.imagenDinero)
        btnCheck = findViewById(R.id.btnCheck)
        btnEditarCliente = findViewById(R.id.btnEditarCliente)
        btnEditarPedido = findViewById(R.id.btnEditarPedido)
        linearLayoutContacto = findViewById(R.id.linearLayoutContacto)
        cardDinero = findViewById(R.id.cardDinero)
        tvCardProveedor = findViewById(R.id.tvCardProveedor)
        layout_botones = findViewById(R.id.layout_botones)

        if (SessionManager.rol == "Admin") {
            if(fechaEntregaPedidoMotorizado != null){
                layout_botones.visibility = GONE
                frameEditarPedido.visibility = VISIBLE
            } else {
                frameEditarPedido.visibility = VISIBLE
            }
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
            seSubioEntregaImagen = false
            btnCamara.isEnabled = false
            val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
            btnCamara.backgroundTintList = color
            btnCheck.isEnabled = false  // Deshabilita el botón
            btnCheck.alpha = 0.5f
            btnCheck.backgroundTintList = null
            btnDinero.isEnabled = false  // Deshabilita el botón
            btnDinero.alpha = 0.5f
            btnDinero.backgroundTintList = null
            abrirCamara()
        }

        btnCheck.setOnClickListener {
            if (item?.fechaRecojoPedidoMotorizado == null && seSubioRecogioImagen) {
                actualizarEstadoPedido(1)
            } else if (item?.fechaRecojoPedidoMotorizado != null && seSubioEntregaImagen && seSubioDineroImagen) {
                //actualizarEstadoPedido(2)
                mostrarDialogoConfirmacionPago()
            }
        }

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
            btnDinero.visibility = VISIBLE
            btnDinero.alpha = 0.5f       // Reduce la opacidad al 50%
            btnDinero.setOnClickListener {
                val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
                btnDinero.backgroundTintList = color
                btnCheck.isEnabled = false  // Deshabilita el botón
                btnCheck.alpha = 0.5f
                btnCheck.backgroundTintList = null
                //abrirGaleria() // Ahora abre la galería en lugar de la cámara
                abrirCamara()
            }
            btnDinero.isEnabled = false  // Deshabilita el botón
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

    private fun saveExtractedTextToFirestore(extractedText: String) {
        val pedidoId = item?.id ?: return
        val collectionName = "recojos"

        val data = mapOf(
            "textoExtraido" to extractedText
        )

        Firebase.firestore.collection(collectionName).document(pedidoId)
            .update(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Texto extraído guardado con éxito")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al guardar texto en Firestore: ${e.message}")
            }
    }

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

    private fun analyzeImageWithMLKit(imageUri: Uri) {
        try {
            val image: InputImage = InputImage.fromFilePath(this, imageUri)

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text.trim()

                    if (extractedText.isNotEmpty()) {
                        Log.d("MLKit", "Texto extraído: $extractedText")

                        // Guardar el texto en Firestore
                        saveExtractedTextToFirestore(extractedText)
                    } else {
                        Log.w("MLKit", "No se detectó texto en la imagen")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Error al analizar la imagen: ${e.message}")
                }

        } catch (e: Exception) {
            Log.e("MLKit", "Error al procesar imagen para OCR: ${e.message}")
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
                } else if (item?.fechaEntregaPedidoMotorizado == null && seSubioEntregaImagen.equals(false)){
                    // Caso de entrega
                    seSubioEntregaImagen = false
                    seEntregoImagen = true
                    imagenEntrega.setImageURI(photoUri) // Mostrar la imagen capturada
                    subirFotosFirestore(photoUri!!, 2) // Subir imagen a Firestore
                } else {
                    // Caso de dinero
                    seSubioDineroImagen = false
                    seDineroImagen = true
                    imagenDinero.setImageURI(photoUri) // Mostrar la imagen capturada

                    // 1. Subir la imagen a Firestore
                    subirFotosFirestore(photoUri!!, 3)

                    // 2. Extraer texto con ML Kit
                    // analyzeImageWithMLKit(photoUri!!)
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
        compressionQuality: Int = 60, // Calidad de compresión
        maxSize: Int = 550, // Tamaño máximo de la imagen original
        thumbnailSize: Int = 250 // Tamaño máximo del thumbnail
    ): Pair<ByteArray, ByteArray> {
        // Obtener dimensiones originales
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // **1. Calcular inSampleSize para una carga eficiente**
        val inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxSize, maxSize)

        // **2. Decodificar la imagen con inSampleSize**
        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return Pair(ByteArray(0), ByteArray(0))

        // **3. Corregir la orientación de la imagen**
        val correctedBitmap = correctOrientation(context, uri, originalBitmap)

        // **4. Redimensionar la imagen manteniendo proporciones**
        val (newWidth, newHeight) = calculateNewDimensions(correctedBitmap.width, correctedBitmap.height, maxSize)
        val resizedBitmap = Bitmap.createScaledBitmap(correctedBitmap, newWidth, newHeight, true)

        // **5. Comprimir la imagen redimensionada**
        val compressedBaos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, compressedBaos)
        val compressedBytes = compressedBaos.toByteArray()

        // **6. Generar el thumbnail con el mismo principio**
        val (thumbnailWidth, thumbnailHeight) = calculateNewDimensions(newWidth, newHeight, thumbnailSize)
        val thumbnailBitmap = Bitmap.createScaledBitmap(resizedBitmap, thumbnailWidth, thumbnailHeight, true)

        val thumbnailBaos = ByteArrayOutputStream()
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 60, thumbnailBaos)
        val thumbnailBytes = thumbnailBaos.toByteArray()

        // **Liberar memoria**
        resizedBitmap.recycle()
        if (thumbnailBitmap != resizedBitmap) thumbnailBitmap.recycle()
        if (correctedBitmap != originalBitmap) correctedBitmap.recycle()
        originalBitmap.recycle()

        return Pair(compressedBytes, thumbnailBytes)
    }


    private fun calculateInSampleSize(origWidth: Int, origHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (origHeight > reqHeight || origWidth > reqWidth) {
            val halfHeight = origHeight / 2
            val halfWidth = origWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
    private fun calculateNewDimensions(width: Int, height: Int, maxSize: Int): Pair<Int, Int> {
        return if (width > height) {
            if (width > maxSize) maxSize to (height * maxSize / width) else width to height
        } else {
            if (height > maxSize) (width * maxSize / height) to maxSize else width to height
        }
    }
    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(inputStream)
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

                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                if (rotatedBitmap != bitmap) bitmap.recycle()

                rotatedBitmap
            } ?: bitmap
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error al corregir orientación: ${e.message}")
            bitmap
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
        var tipoOperacion = ""
        if (tipo == 1){
            btnCamara.setImageResource(R.drawable.loading_animation)
            tipoOperacion = "Recojo"
        } else if (tipo == 2){
            btnCamara.setImageResource(R.drawable.loading_animation)
            tipoOperacion = "Entrega"
        } else {
            btnDinero.setImageResource(R.drawable.loading_animation)
            tipoOperacion = "Dinero"
        }

        val pedidoId = item?.id ?: return
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
        Log.d("checkAllUploadsCompleted", "Tipo recibido: $tipo")
        if (mainImageUploaded && thumbnailUploaded) {
            Log.d("checkAllUploadsCompleted", "mainImageUploaded y thumbnailUploaded son true")
            when (tipo) {
                1 -> {
                    Log.d("checkAllUploadsCompleted", "Caso 1: Recojo")
                    seSubioRecogioImagen = true
                    btnCamara.setImageResource(R.drawable.camera_solid)
                    btnCamara.backgroundTintList = null
                    btnCamara.isEnabled = true

                    val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
                    btnCheck.backgroundTintList = color
                    btnCheck.isEnabled = true
                    btnCheck.alpha = 1f
                }
                2 -> {
                    Log.d("checkAllUploadsCompleted", "Caso 2: Entrega")
                    seSubioEntregaImagen = true
                    btnCamara.setImageResource(R.drawable.camera_solid)
                    btnCamara.backgroundTintList = null
                    btnCamara.isEnabled = true

                    cardDinero.visibility = VISIBLE
                    linearLayoutContacto.visibility = GONE
                    tvCardProveedor.visibility = GONE

                    val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
                    btnDinero.backgroundTintList = color
                    btnDinero.isEnabled = true
                    btnDinero.alpha = 1f
                }
                else -> {
                    Log.d("checkAllUploadsCompleted", "Caso 3: Dinero")
                    seSubioDineroImagen = true
                    btnDinero.setImageResource(R.drawable.money_bill_wave_solid)
                    btnDinero.backgroundTintList = null
                    btnDinero.isEnabled = true

                    val color = ContextCompat.getColorStateList(this, android.R.color.holo_orange_light)
                    btnCheck.backgroundTintList = color
                    btnCheck.isEnabled = true
                    btnCheck.alpha = 1f
                }
            }

            // Eliminar la imagen después de subida completada
            photoUri?.let { eliminarArchivo(it) }
        } else {
            Log.d("checkAllUploadsCompleted", "mainImageUploaded o thumbnailUploaded son false")
        }
    }

    private fun mostrarErrorYReiniciar() {
        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
        btnCamara.setImageResource(R.drawable.camera_solid)
        btnCamara.isEnabled = true
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
                //Toast.makeText(this, "$tipoOperacion finalizado con éxito.", Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad y vuelve a la anterior
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al actualizar la fecha de $tipoOperacion", e)
                //Toast.makeText(this, "Error al actualizar el pedido.", Toast.LENGTH_SHORT).show()
            }
    }

    // Lanzador para seleccionar una imagen de la galería
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                photoUri = uri

                // Caso de dinero
                seSubioDineroImagen = false
                seDineroImagen = true
                imagenDinero.setImageURI(photoUri) // Mostrar la imagen seleccionada

                // 1. Subir la imagen a Firestore
                subirFotosFirestore(photoUri!!, 3)

                // 2. Extraer texto con ML Kit
                analyzeImageWithMLKit(photoUri!!)

            } catch (e: Exception) {
                Log.e("Galería", "Error al procesar la imagen: ${e.message}")
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("Galería", "Selección de imagen cancelada")
        }
    }

    // Función para abrir la galería
    private fun abrirGaleria() {
        galleryLauncher.launch("image/*")
    }

    private fun mostrarDialogoConfirmacion() {
        val opciones = arrayOf("Ñanpi", "Proveedor", "Motorizado")
        var seleccionIndex = -1 // Ninguna opción seleccionada por defecto

        AlertDialog.Builder(this)
            .setTitle("¿Quién recibió el pago?")
            .setSingleChoiceItems(opciones, seleccionIndex) { _, which ->
                seleccionIndex = which // Guardar índice seleccionado
            }
            .setPositiveButton("Aceptar") { _, _ ->
                if (seleccionIndex != -1) {
                    val seleccion = opciones[seleccionIndex]
                    actualizarEstadoPedido(2) // Ejecutar después de la selección
                } else {
                    Toast.makeText(this, "Debes seleccionar una opción", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss() // Cierra el diálogo sin hacer nada
            }
            .setCancelable(true) // Permite cerrar tocando fuera del diálogo
            .show()
    }

    private fun mostrarDialogoConfirmacionPago() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialogo_confirmacion_pago, null)
        val recyclerPagos = dialogView.findViewById<RecyclerView>(R.id.recyclerPagos)
        val btnAgregarPago = dialogView.findViewById<Button>(R.id.btnAgregarPago)
        val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmar)

        // Lista para almacenar los pagos registrados
        val pagosRegistrados = ArrayList<PagoRegistro>()
        val adapter = PagosAdapter(pagosRegistrados)

        recyclerPagos.layoutManager = LinearLayoutManager(this)
        recyclerPagos.adapter = adapter

        // Diálogo principal
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Confirmación de Pago")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Botón para agregar nuevo pago
        btnAgregarPago.setOnClickListener {
            mostrarDialogoAgregarPago { pago ->
                pagosRegistrados.add(pago)
                adapter.notifyItemInserted(pagosRegistrados.size - 1)
            }
        }

        // Botón para confirmar todos los pagos
        btnConfirmar.setOnClickListener {
            if (pagosRegistrados.isEmpty()) {
                Toast.makeText(this, "Debes agregar al menos un pago", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar si el total de los pagos coincide con el monto del pedido
            val totalPagado = pagosRegistrados.sumOf { it.monto }
            val montoPedido = item?.pedidoCantidadCobrar?.toDoubleOrNull() ?: 0.0

            if (totalPagado == montoPedido) {
                guardarPagosYActualizar(pagosRegistrados)
                alertDialog.dismiss()
            } else {
                // Mostrar advertencia y evitar continuar
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("El monto total pagado (S/ $totalPagado) no coincide con el valor del pedido (S/ $montoPedido).")
                    .setPositiveButton("Aceptar", null)
                    .show()
            }

        }

        // Botón para cancelar
        dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun mostrarDialogoAgregarPago(callback: (PagoRegistro) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialogo_agregar_pago, null)

        val spinnerMetodo = dialogView.findViewById<Spinner>(R.id.spinnerMetodoPago)
        val spinnerReceptor = dialogView.findViewById<Spinner>(R.id.spinnerReceptor)
        val editMonto = dialogView.findViewById<EditText>(R.id.editMonto)

        // Configurar spinner de métodos de pago
        val metodosPago = arrayOf("Efectivo", "Yape", "Plin", "Transferencia")
        val adapterMetodos = ArrayAdapter(this, android.R.layout.simple_spinner_item, metodosPago)
        adapterMetodos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMetodo.adapter = adapterMetodos

        // Configurar spinner de receptores
        val receptores = arrayOf("Ñanpi", "Proveedor", "Motorizado")
        val adapterReceptores = ArrayAdapter(this, android.R.layout.simple_spinner_item, receptores)
        adapterReceptores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReceptor.adapter = adapterReceptores

        // Configurar listeners para mostrar/ocultar campos según el método de pago
        spinnerMetodo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val metodo = metodosPago[position]
                // Si es efectivo, el receptor debería ser solo el motorizado
                if (metodo == "Efectivo") {
                    spinnerReceptor.setSelection(receptores.indexOf("Motorizado"))
                    spinnerReceptor.isEnabled = false
                } else {
                    // Para métodos digitales, excluir motorizado como opción predeterminada
                    if (spinnerReceptor.selectedItemPosition == receptores.indexOf("Motorizado")) {
                        spinnerReceptor.setSelection(0) // Seleccionar Ñanpi por defecto
                    }
                    spinnerReceptor.isEnabled = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        AlertDialog.Builder(this)
            .setTitle("Agregar Pago")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val monto = editMonto.text.toString().toDoubleOrNull() ?: 0.0
                if (monto <= 0) {
                    Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val metodoPago = spinnerMetodo.selectedItem.toString()
                val receptor = spinnerReceptor.selectedItem.toString()
                val ruta = SessionManager.ruta ?: ""
                val resultado = receptor + "," + ruta

                callback(PagoRegistro(metodoPago, resultado, monto))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarPagosYActualizar(pagos: List<PagoRegistro>) {
        val pedidoId = item?.id ?: return
        val fecha = Timestamp.now()

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("recojos").document(pedidoId)

        // Preparar los datos para Firestore
        val pagosMap = pagos.mapIndexed { index, pago ->
            mapOf(
                "metodoPago" to pago.metodoPago,
                "receptor" to pago.receptor,
                "monto" to pago.monto
            )
        }

        val updates = hashMapOf<String, Any>(
            "fechaEntregaPedidoMotorizado" to fecha,
            "pagosRegistrados" to pagosMap
        )

        docRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Pagos registrados correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al actualizar los pagos", e)
                Toast.makeText(this, "Error al registrar los pagos", Toast.LENGTH_SHORT).show()
            }
    }

}
