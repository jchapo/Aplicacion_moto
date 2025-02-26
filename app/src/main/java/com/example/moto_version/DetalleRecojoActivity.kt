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


class DetalleRecojoActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var item: Item? = null  // Variable global

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_recojo)

        val id = intent.getStringExtra("id")
        val clienteNombre = intent.getStringExtra("clienteNombre")
        val proveedorNombre = intent.getStringExtra("proveedorNombre")
        val pedidoCantidadCobrar = intent.getStringExtra("pedidoCantidadCobrar")

        val tvCliente = findViewById<TextView>(R.id.tvDetalleCliente)
        val tvProveedor = findViewById<TextView>(R.id.tvDetalleProveedor)
        val tvPrecio = findViewById<TextView>(R.id.tvDetallePrecio)

        val btnTelefonoCliente = findViewById<ImageButton>(R.id.btnTelefonoCliente)
        val btnTelefonoProveedor = findViewById<ImageButton>(R.id.btnTelefonoProveedor)
        val btnWhatsappCliente = findViewById<ImageButton>(R.id.btnWhatsappCliente)
        val btnWhatsappProveedor = findViewById<ImageButton>(R.id.btnWhatsappProveedor)

        val btnMapsPedido = findViewById<ImageButton>(R.id.btnMapsCliente)
        val btnMapsRecojo = findViewById<ImageButton>(R.id.btnMapsProveedor)



        // Inicializamos los textos con los valores del intent
        tvCliente.text = clienteNombre
        tvProveedor.text = proveedorNombre
        tvPrecio.text = "Monto a cobrar: S/ $pedidoCantidadCobrar"

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
                    item?.let {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Si el usuario otorgó el permiso, puedes volver a intentar la llamada aquí si lo deseas
        } else {
            Log.e("Permisos", "El usuario denegó el permiso para realizar llamadas.")
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
}
