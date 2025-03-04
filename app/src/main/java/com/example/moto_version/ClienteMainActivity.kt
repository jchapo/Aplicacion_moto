package com.example.moto_version

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.models.Recojo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.location.Location
import android.location.LocationManager
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.View.GONE
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.Timestamp
import java.util.Calendar
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.CoroutineScope
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val REQUEST_LOCATION_SETTINGS = 1002

class ClienteMainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClienteMiAdapter
    private var mMap: GoogleMap? = null
    private lateinit var drawerLayout: DrawerLayout
    private val db = FirebaseFirestore.getInstance()
    private var usuarioListener: ListenerRegistration? = null
    private var datosCargados = false  // Variable para controlar si los datos están cargados
    private var ubicacionDisponible = false  // Variable para controlar si la ubicación está disponible
    private var mapaListo = false // Variable para saber si el mapa ya está inicializado
    private var phone: String = ""
    private var nombreEmpresa: String = ""
    private var recojosListener: ListenerRegistration? = null
    private var entregasListener: ListenerRegistration? = null
    data class PuntoPedidoCliente(val id: String, val ubicacion: LatLng, val clienteNombre: String, val proveedorNombre: String, val pedidoCantidadCobrar: String, val pedidoMetodoPago: String, val fechaEntregaPedidoMotorizado: Timestamp?, val fechaRecojoPedidoMotorizado: Timestamp?, val thumbnailFotoRecojo: String, val fechaAnulaciónPedido: Timestamp?)
    private val puntosRecojoLista = mutableListOf<PuntoPedidoCliente>()
    private val puntosRecojoListaEspecial = mutableListOf<PuntoPedidoCliente>()
    private val puntosEntregaLista = mutableListOf<PuntoPedidoCliente>()
    private var kmlLayer: KmlLayer? = null
    private val marcadores = mutableListOf<Marker>() // Lista para guardar referencia a todos los marcadores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cliente_activity_main)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val modalMostrado = sharedPreferences.getBoolean("modal_mostrado", false)

        if (!modalMostrado) {
            val anuncioDialog = AnuncioDialogFragment()
            anuncioDialog.show(supportFragmentManager, "AnuncioDialog")

            // Guardar que el modal ya se mostró
            sharedPreferences.edit().putBoolean("modal_mostrado", true).apply()
        }

        phone = intent.getStringExtra("phone") ?: ""
        nombreEmpresa = intent.getStringExtra("nombreEmpresa") ?: ""
        Log.d("ClienteMainActivity", "Phone recibido: $phone")

        escucharCambiosEnUsuario()

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.cliente_map_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.cliente_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ClienteMiAdapter(emptyList())  // Inicialmente vacío
        recyclerView.adapter = adapter

        // Cargar datos desde Firestore
        obtenerDatosFirestore()

        // Inicializar DrawerLayout
        drawerLayout = findViewById(R.id.cliente_drawer_layout)
        val navView = findViewById<NavigationView>(R.id.cliente_nav_view)

        // Configurar listener para el botón flotante
        val fabMenu = findViewById<FloatingActionButton>(R.id.cliente_fab_menu)
        //fabMenu.visibility = GONE
        fabMenu.setOnClickListener {
            // Crear intent para iniciar la nueva actividad
            val intent = Intent(this, CreateOrderActivity::class.java).apply {
                putExtra("phone", phone)  // Pasa la variable "proveedorTelefono"
                putExtra("nombreEmpresa", nombreEmpresa)  // Pasa la variable "ruta"
            }
            this.startActivity(intent)
        }

        actualizarIndicadores()

    }



    private fun actualizarIndicadores() {
        val cantidadPuntosRecojo = puntosRecojoLista.size

        // Contar puntos anulados (fechaAnulaciónPedido != null)
        val cantidadAnulados = puntosRecojoLista.count { it.fechaAnulaciónPedido != null }

        // Contar puntos finalizados (fechaEntregaPedidoMotorizado != null)
        val cantidadFinalizados = puntosRecojoLista.count { it.fechaEntregaPedidoMotorizado != null }

        // Calcular los faltantes (total - finalizados - anulados)
        val cantidadFaltantes = cantidadPuntosRecojo - cantidadFinalizados - cantidadAnulados

        // Actualizar CardUno (Anulados)
        val indUno: TextView = findViewById(R.id.cliente_indUno)
        val cardIndUno: CardView = findViewById(R.id.cliente_cardIndUno)
        indUno.text = "$cantidadAnulados"
        val textUno: TextView = findViewById(R.id.cliente_textUno)
        textUno.text = if (cantidadAnulados == 1) "Anulado" else "Anulados"
        cardIndUno.visibility = if (cantidadAnulados == 0) View.GONE else View.VISIBLE

        // Actualizar CardDos (Faltantes)
        val indDos: TextView = findViewById(R.id.cliente_indDos)
        val cardIndDos: CardView = findViewById(R.id.cliente_cardIndDos)
        indDos.text = "$cantidadFaltantes"
        val textDos: TextView = findViewById(R.id.cliente_textDos)
        textDos.text = if (cantidadFaltantes == 1) "Faltante" else "Faltantes"
        cardIndDos.visibility = if (cantidadFaltantes == 0) View.GONE else View.VISIBLE

        // Actualizar CardTres (Finalizados)
        val indTres: TextView = findViewById(R.id.cliente_indTres)
        val cardIndTres: CardView = findViewById(R.id.cliente_cardIndTres)
        indTres.text = "$cantidadFinalizados"
        val textTres: TextView = findViewById(R.id.cliente_textTres)
        textTres.text = if (cantidadFinalizados == 1) "Listo" else "Listos"
        cardIndTres.visibility = if (cantidadFinalizados == 0) View.GONE else View.VISIBLE
    }


    private fun obtenerDatosFirestore() {
        if (phone.isEmpty()) {
            Log.e("Firestore", "Error: phone está vacío")
            return
        }
        // Remover listener anterior si existe
        recojosListener?.remove()
        entregasListener?.remove()

        // Configurar un listener en tiempo real
        recojosListener = db.collection("recojos")
            .whereEqualTo("proveedorTelefono", phone)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error al obtener documentos", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.d("Firestore", "Snapshot nulo")
                    return@addSnapshotListener
                }

                /*val documentosFiltrados = snapshots.documents.filter {
                    it.get("fechaRecojoPedidoMotorizado") == null && it.get("fechaAnulacionPedido") == null
                }*/

                puntosRecojoLista.clear() // Limpiar lista antes de agregar nuevas coordenadas

                snapshots.documents.forEach { doc ->
                    val id = doc.id
                    val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                    val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                    val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "0.00"
                    val pedidoMetodoPago = doc.getString("pedidoMetodoPago") ?: "Error"
                    val fechaEntregaPedidoMotorizado = doc.getTimestamp("fechaEntregaPedidoMotorizado")
                    val fechaRecojoPedidoMotorizado = doc.getTimestamp("fechaRecojoPedidoMotorizado")
                    val fechaAnulaciónPedido = doc.getTimestamp("fechaAnulaciónPedido")

                    // Obtener coordenadas
                    val coordenadas = doc.get("pedidoCoordenadas") as? Map<String, Any>
                    val latitud = coordenadas?.get("lat") as? Double
                    val longitud = coordenadas?.get("lng") as? Double

                    if (latitud != null && longitud != null) {
                        val ubicacion = LatLng(latitud, longitud)
                        puntosRecojoLista.add(PuntoPedidoCliente(id, ubicacion, clienteNombre, proveedorNombre, pedidoCantidadCobrar, pedidoMetodoPago, fechaEntregaPedidoMotorizado, fechaRecojoPedidoMotorizado, "",fechaAnulaciónPedido))
                        Log.d("Firestore", "Punto recojo: $ubicacion - Cliente: $clienteNombre")
                    }
                }

                datosCargados = true
                mMap?.let {
                    actualizarMapa()
                }
            }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapaListo = true

        // Verificar si los datos ya están cargados y actualizar el mapa
        if (datosCargados) {
            actualizarMapa()
        }
        // Solicitar permiso de ubicación
        solicitarPermisoUbicacion()
    }


    private fun cargarMapaKML() {
        if (!mapaListo) return

        // Usar coroutine para la operación de red
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // URL de KML - asegúrate de que sea accesible públicamente
                val kmlUrl = URL("https://www.google.com/maps/d/kml?mid=13U820BGFZW20wbx4NE7e56AuJGGvzzM&forcekml=1")
                val connection = kmlUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val inputStream = connection.inputStream
                val kmlData = inputStream.bufferedReader().use { it.readText() }

                // Verificar si el KML contiene datos
                if (kmlData.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "El archivo KML está vacío", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Convertir el texto de vuelta a InputStream para KmlLayer
                val kmlInputStream = kmlData.byteInputStream()

                withContext(Dispatchers.Main) {
                    try {
                        // Eliminar la capa KML anterior si existe
                        kmlLayer?.removeLayerFromMap()

                        // Crear nueva capa KML
                        kmlLayer = KmlLayer(mMap, kmlInputStream, applicationContext)
                        kmlLayer?.addLayerToMap()

                        // Log para confirmar que la capa se añadió correctamente
                        Log.d("ClienteMainActivity", "KML cargado correctamente")

                        // Verificar si hay contenedores en el KML
                        val containers = kmlLayer?.containers
                        if (containers != null) {
                            for (container in containers) {
                                Log.d("ClienteMainActivity", "Container encontrado: ${container.hasProperties()}")
                            }
                        } else {
                            Log.d("ClienteMainActivity", "No se encontraron containers en el KML")
                        }
                    } catch (e: Exception) {
                        Log.e("ClienteMainActivity", "Error al procesar KML", e)
                        Toast.makeText(applicationContext, "Error al procesar el mapa KML: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("ClienteMainActivity", "Error al cargar KML", e)
                    Toast.makeText(applicationContext, "Error al cargar el mapa: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun actualizarMapa() {
        limpiarSoloMarcadores()
        cargarMapaKML()
        agregarMarcadores()
        centrarMapa()
    }
    private fun centrarMapa() {
        centrarMapaSinUbicacion()
    }
    private fun agregarMarcadores() {
        mMap?.let { map ->
            // NO usar map.clear() aquí porque eliminaría el KML

            val boundsBuilder = LatLngBounds.Builder()

            // Agregar marcadores de recojo con color según la fecha de entrega
            for (punto in puntosRecojoLista) {
                val color = if (punto.fechaEntregaPedidoMotorizado != null) {
                    BitmapDescriptorFactory.HUE_GREEN
                } else {
                    BitmapDescriptorFactory.HUE_BLUE
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(punto.ubicacion)
                        .title("Entrega: ${punto.clienteNombre}")
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )

                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
                boundsBuilder.include(punto.ubicacion)
            }

            if (puntosRecojoLista.isNotEmpty()) {
                val bounds = boundsBuilder.build()
                val padding = 100 // Espaciado en píxeles alrededor de los puntos
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
        }
    }
    private fun centrarMapaConUbicacion(ubicacionUsuario: LatLng) {
        //if (puntosRecojoLista.isEmpty() && puntosEntregaLista.isEmpty() && puntosRecojoListaEspecial.isEmpty() && mMap != null) {
        if (puntosRecojoLista.isEmpty() && mMap != null) {
            // Si no hay marcadores, centrar solo en la ubicación del usuario
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 15f))
            return
        }

        try {
            val boundsBuilder = LatLngBounds.Builder()

            // Incluir la ubicación del usuario
            boundsBuilder.include(ubicacionUsuario)

            // Incluir todas las coordenadas de los puntos de recojo
            for (punto in puntosRecojoLista) {
                boundsBuilder.include(punto.ubicacion)
            }

            // Incluir todas las coordenadas de los puntos de entrega
            for (punto in puntosEntregaLista) {
                boundsBuilder.include(punto.ubicacion)
            }

            // Incluir todas las coordenadas de los puntos de entrega solo si la lista no es nula ni está vacía
            /*if (!puntosRecojoListaEspecial.isNullOrEmpty()) {
                for (punto in puntosRecojoListaEspecial) {
                    boundsBuilder.include(punto.ubicacion)
                }
            }*/

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 80  // Margen en píxeles

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("ClienteMainActivity", "Error en animateCamera con ubicación", e)
                    try {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("ClienteMainActivity", "Error al centrar mapa con ubicación", e)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar si el mapa ya está inicializado
        if (mapaListo) {
            centrarMapaSinUbicacion()
        }
    }
    private fun limpiarSoloMarcadores() {
        // Eliminar todos los marcadores guardados en la lista
        for (marker in marcadores) {
            marker.remove()
        }
        marcadores.clear()
    }
    private fun centrarMapaSinUbicacion() {
        if (puntosRecojoLista.isEmpty()) {
            Log.d("ClienteMainActivity", "No hay coordenadas para centrar el mapa")
            return
        }
        val listaRecojos = puntosRecojoLista.map { punto ->
            Recojo(
                id = punto.id,
                clienteNombre = punto.clienteNombre,
                proveedorNombre = punto.proveedorNombre,
                pedidoCantidadCobrar = punto.pedidoCantidadCobrar,
                pedidoMetodoPago = punto.pedidoMetodoPago,
                fechaEntregaPedidoMotorizado = punto.fechaEntregaPedidoMotorizado,
                fechaRecojoPedidoMotorizado = punto.fechaRecojoPedidoMotorizado,
                thumbnailFotoRecojo = punto.thumbnailFotoRecojo
            )
        }

        // Actualizar la lista en el adaptador
        adapter.actualizarLista(listaRecojos)
        actualizarIndicadores()

        try {
            val boundsBuilder = LatLngBounds.Builder()
            var hayPuntos = false

            // Incluir todas las coordenadas de los puntos de recojo
            for (punto in puntosRecojoLista) {
                boundsBuilder.include(punto.ubicacion)
                hayPuntos = true
            }

            if (!hayPuntos) {
                Log.d("ClienteMainActivity", "No hay coordenadas válidas para centrar el mapa")
                return
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 200  // Margen en píxeles

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("ClienteMainActivity", "Error en animateCamera", e)
                    try {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        // Si todo falla, centrar en la primera coordenada disponible
                        val primerPunto = puntosRecojoLista[0].ubicacion
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("ClienteMainActivity", "Error al centrar mapa", e)
            Handler(Looper.getMainLooper()).postDelayed({
                val primerPunto = puntosRecojoLista[0].ubicacion
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
            }, 300)
        }
    }
    private fun activarUbicacionUsuario() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true

            // Verificar si el servicio de ubicación está activado
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Si la ubicación está desactivada, mostrar diálogo para activarla
                mostrarDialogoActivarUbicacion()
            } else {
                // La ubicación está activada, intentar obtener la posición actual
                ubicacionDisponible = true
                obtenerUbicacionActual()
            }
        }
    }

    private fun mostrarDialogoActivarUbicacion() {
        AlertDialog.Builder(this)
            .setTitle("Ubicación desactivada")
            .setMessage("Para mostrar tu ubicación en el mapa, necesitas activar el servicio de ubicación")
            .setPositiveButton("Configuración") { _, _ ->
                // Abrir configuración de ubicación
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_LOCATION_SETTINGS)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                // Continuar sin ubicación
                ubicacionDisponible = false
                //centrarMapaSinUbicacion()
            }
            .setCancelable(false)
            .show()
    }

    private fun obtenerUbicacionActual() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            // Intentar obtener la última ubicación conocida
            val lastKnownLocation = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Intentar primero con GPS por mayor precisión
                var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                // Si no hay ubicación de GPS, intentar con NETWORK
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                // Si aún no hay ubicación, intentar con PASSIVE (cualquier proveedor)
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                }

                location
            } else {
                null
            }

            if (lastKnownLocation != null) {
                //Log.d("ClienteMainActivity", "Ubicación obtenida: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                val ubicacionUsuario = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                centrarMapaConUbicacion(ubicacionUsuario)
                actualizarListaOrdenada(ubicacionUsuario)
            } else {
                Log.d("ClienteMainActivity", "No se pudo obtener la ubicación actual")
                // Si no hay ubicación disponible, centrar solo con marcadores
                ubicacionDisponible = false
                centrarMapaSinUbicacion()
            }
        } catch (e: Exception) {
            Log.e("ClienteMainActivity", "Error al obtener ubicación", e)
            ubicacionDisponible = false
            centrarMapaSinUbicacion()
        }
    }


    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun escucharCambiosEnUsuario() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            usuarioListener = db.collection("usuarios")
                .whereEqualTo("email", user.email)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("Firestore", "Error al escuchar cambios", error)
                        return@addSnapshotListener
                    }

                    // Si el documento del usuario no existe, cerrar sesión
                    if (snapshots == null || snapshots.isEmpty) {
                        cerrarSesion()
                    }
                }
        }
    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()  // Cerrar sesión en Firebase
        val intent = Intent(this, LoginActivity::class.java)  // Redirigir a la pantalla de login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Eliminar historial de actividades
        startActivity(intent)
        finish()  // Cerrar la actividad actual
    }




    private fun solicitarPermisoUbicacion() {
        val permisosNecesarios = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.CAMERA)
        }

        if (permisosNecesarios.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permisosNecesarios.toTypedArray(), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // Manejar la respuesta del usuario
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Todos los permisos concedidos
                activarUbicacionUsuario()
            } else {
                // Algún permiso fue denegado
                Toast.makeText(this, "Debe conceder todos los permisos para continuar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDialogoPermisoDenegado() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Para usar el mapa, habilita el permiso de ubicación en la configuración.")
            .setPositiveButton("Ir a configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:com.example.moto_version")
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOCATION_SETTINGS) {
            // Verificar nuevamente si la ubicación está activada después de regresar de configuración
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                ubicacionDisponible = true
                obtenerUbicacionActual()
            } else {
                ubicacionDisponible = false
                centrarMapaSinUbicacion()
            }
        }
    }

    private fun actualizarListaOrdenada(ubicacionUsuario: LatLng) {
        // Obtenemos la hora actual del dispositivo
        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Verificamos si puntosRecojoListaEspecial no está vacío antes de calcular distancias
        val puntosRecojoConDistanciaEspecial = if (puntosRecojoListaEspecial.isNotEmpty()) {
            puntosRecojoListaEspecial.map { punto ->
                val resultado = FloatArray(1)
                Location.distanceBetween(
                    ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                    punto.ubicacion.latitude, punto.ubicacion.longitude,
                    resultado
                )
                punto to resultado[0]
            }
        } else {
            emptyList() // Si está vacío, devuelve una lista vacía
        }

        Log.e("listaCombinada", "puntosRecojoConDistanciaEspecial: $puntosRecojoConDistanciaEspecial")

        // Calculamos las distancias para puntosRecojoLista
        val puntosRecojoConDistancia = puntosRecojoLista.map { punto ->
            val resultado = FloatArray(1)
            Location.distanceBetween(
                ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                punto.ubicacion.latitude, punto.ubicacion.longitude,
                resultado
            )
            punto to resultado[0]
        }

        // Lista combinada solo si hay elementos en puntosRecojoListaEspecial
        val listaCombinada = puntosRecojoConDistancia.toMutableList()
        if (puntosRecojoConDistanciaEspecial.isNotEmpty()) {
            listaCombinada.addAll(puntosRecojoConDistanciaEspecial)
        }
        Log.e("listaCombinada", "listaCombinada1: $listaCombinada")

        /*if (horaActual >= 13) {*/
        val puntosEntregaConDistancia = puntosEntregaLista.map { punto ->
            val resultado = FloatArray(1)
            Location.distanceBetween(
                ubicacionUsuario.latitude, ubicacionUsuario.longitude,
                punto.ubicacion.latitude, punto.ubicacion.longitude,
                resultado
            )
            punto to resultado[0]
        }

        // Combinamos ambas listas
        listaCombinada.addAll(puntosEntregaConDistancia)
        Log.e("listaCombinada", "listaCombinada2: $listaCombinada")
        /*}*/

        // Ordenamos la lista combinada por distancia
        val listaOrdenada = listaCombinada.sortedBy { it.second }
        Log.e("listaCombinada", "listaCombinada3: $listaCombinada")


        // Convertimos a la lista final para el adaptador
        val listaFinal = listaOrdenada.map { (punto, _) ->
            Recojo(punto.id, punto.clienteNombre, punto.proveedorNombre, punto.pedidoCantidadCobrar, punto.pedidoMetodoPago, punto.fechaEntregaPedidoMotorizado, punto.fechaRecojoPedidoMotorizado, punto.thumbnailFotoRecojo)
        }

        // Actualizamos el adaptador con la lista final
        adapter.actualizarLista(listaFinal)
        actualizarIndicadores()
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("modal_mostrado", false).apply()
        usuarioListener?.remove()
        recojosListener?.remove() // Añadir esta línea
        entregasListener?.remove()

    }


}