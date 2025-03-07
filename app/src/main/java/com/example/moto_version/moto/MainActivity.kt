package com.example.moto_version.moto

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
import com.example.moto_version.LoginActivity
import com.example.moto_version.R
import com.example.moto_version.SessionManager
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.Timestamp
import java.util.Calendar
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.CoroutineScope
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val REQUEST_LOCATION_SETTINGS = 1002

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MiAdapter
    private var mMap: GoogleMap? = null
    private lateinit var drawerLayout: DrawerLayout
    private val db = FirebaseFirestore.getInstance()
    private var usuarioListener: ListenerRegistration? = null
    private var datosCargados = false  // Variable para controlar si los datos están cargados
    private var ubicacionDisponible = false  // Variable para controlar si la ubicación está disponible
    private var mapaListo = false // Variable para saber si el mapa ya está inicializado
    private var rutaMotorizado: String = ""
    private var recojosListener: ListenerRegistration? = null
    private var entregasListener: ListenerRegistration? = null
    data class PuntoPedido(val id: String, val ubicacion: LatLng, val clienteNombre: String, val proveedorNombre: String, val pedidoCantidadCobrar: String, val pedidoMetodoPago: String, val fechaEntregaPedidoMotorizado: Timestamp?, val fechaRecojoPedidoMotorizado: Timestamp?, val thumbnailFotoRecojo: String)
    private val puntosRecojoLista = mutableListOf<PuntoPedido>()
    private val puntosRecojoListaEspecial = mutableListOf<PuntoPedido>()
    private val puntosEntregaLista = mutableListOf<PuntoPedido>()
    private var kmlLayer: KmlLayer? = null
    private val marcadores = mutableListOf<Marker>() // Lista para guardar referencia a todos los marcadores




    private fun actualizarIndicadores() {
        val cantidadPuntosRecojo = puntosRecojoLista.size
        val cantidadPuntosRecojoEspecial = puntosRecojoListaEspecial.size
        val cantidadPuntosEntrega = puntosEntregaLista.size

        val cantidadRecojosTotal = cantidadPuntosRecojo
        val cantidadEntregasTotal = cantidadPuntosEntrega + cantidadPuntosRecojoEspecial

        // Obtener referencias a los Cards
        val cardIndUno: CardView = findViewById(R.id.cardIndUno)
        val cardIndDos: CardView = findViewById(R.id.cardIndDos)

        // Obtener referencias a los TextView
        val indUno: TextView = findViewById(R.id.indUno)
        val textUno: TextView = findViewById(R.id.textUno)
        val indDos: TextView = findViewById(R.id.indDos)
        val textDos: TextView = findViewById(R.id.textDos)

        val tarjetaVacia: TextView = findViewById(R.id.tarjetaVacia)

        // Asignar los textos
        indUno.text = "$cantidadRecojosTotal"
        indDos.text = "$cantidadEntregasTotal"

        Log.e("MainActivity", "Cantidad de recojos: $cantidadRecojosTotal")
        Log.e("MainActivity", "Cantidad de entregas: $cantidadEntregasTotal")

        textUno.text = if (indUno.text.equals("1")) "Recojo" else "Recojos"
        textDos.text = if (indDos.text.equals("1")) "Entrega" else "Entregas"

        cardIndUno.visibility = if (cantidadRecojosTotal == 0) View.GONE else View.VISIBLE
        cardIndDos.visibility = if (cantidadEntregasTotal == 0) View.GONE else View.VISIBLE

        /*if (cantidadRecojosTotal == 0 && cantidadEntregasTotal == 0) {
            tarjetaVacia.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tarjetaVacia.text = "No hay pendientes"
        } else {
            tarjetaVacia.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }*/

        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val hayRecojosOPendientes = cantidadRecojosTotal != 0 || cantidadEntregasTotal != 0

        if (hayRecojosOPendientes) {
            tarjetaVacia.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            tarjetaVacia.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tarjetaVacia.text = "No hay pendientes"
        }

    }

    private fun obtenerDatosFirestore() {
        if (rutaMotorizado.isEmpty()) {
            Log.e("Firestore", "Error: rutaMotorizado está vacío")
            return
        }
        // Obtener la hora actual del dispositivo
        val calendar = Calendar.getInstance()
        val horaActual = calendar.get(Calendar.HOUR_OF_DAY)
        // Remover listener anterior si existe
        recojosListener?.remove()
        entregasListener?.remove()

        // Configurar un listener en tiempo real
        recojosListener = db.collection("recojos")
            .whereEqualTo("motorizadoRecojo", rutaMotorizado)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error al obtener documentos", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.d("Firestore", "Snapshot nulo")
                    return@addSnapshotListener
                }

                val documentosFiltrados = snapshots.documents.filter {
                    it.get("fechaRecojoPedidoMotorizado") == null && it.get("fechaAnulacionPedido") == null
                }

                puntosRecojoLista.clear() // Limpiar lista antes de agregar nuevas coordenadas

                documentosFiltrados.forEach { doc ->
                    val id = doc.id
                    val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                    val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                    val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "0.00"
                    val pedidoMetodoPago = doc.getString("pedidoMetodoPago") ?: "Error"
                    val fechaEntregaPedidoMotorizado = doc.getTimestamp("fechaEntregaPedidoMotorizado")
                    val fechaRecojoPedidoMotorizado = doc.getTimestamp("fechaRecojoPedidoMotorizado")

                    // Obtener coordenadas
                    val coordenadas = doc.get("recojoCoordenadas") as? Map<String, Any>
                    val latitud = coordenadas?.get("lat") as? Double
                    val longitud = coordenadas?.get("lng") as? Double

                    if (latitud != null && longitud != null) {
                        val ubicacion = LatLng(latitud, longitud)
                        puntosRecojoLista.add(PuntoPedido(id, ubicacion, clienteNombre, proveedorNombre, pedidoCantidadCobrar, pedidoMetodoPago, fechaEntregaPedidoMotorizado, fechaRecojoPedidoMotorizado, ""))
                        Log.d("Firestore", "Punto recojo: $ubicacion - Cliente: $clienteNombre")
                    }
                }

                datosCargados = true
                mMap?.let {
                    actualizarMapa()
                }
            }

        /*if (horaActual >= 13) {*/

            entregasListener = db.collection("recojos")
                .whereEqualTo("motorizadoEntrega", rutaMotorizado) // Solo un filtro en Firestore
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("Firestore", "Error al obtener documentos", error)
                        return@addSnapshotListener
                    }

                    if (snapshots == null) {
                        Log.d("Firestore", "Snapshot nulo")
                        return@addSnapshotListener
                    }

                    val documentosFiltrados = snapshots.documents.filter {
                        it.get("fechaRecojoPedidoMotorizado") != null && it.get("fechaEntregaPedidoMotorizado") == null && it.get("fechaAnulacionPedido") == null
                    }

                    puntosEntregaLista.clear() // Limpiar lista antes de agregar nuevas coordenadas
                    puntosRecojoListaEspecial.clear() // Limpiar lista antes de agregar nuevas coordenadas

                    documentosFiltrados.forEach { doc ->
                        val id = doc.id
                        val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                        val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                        val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "Error"
                        val pedidoMetodoPago = doc.getString("pedidoMetodoPago") ?: "Error"
                        val fechaEntregaPedidoMotorizado =
                            doc.getTimestamp("fechaEntregaPedidoMotorizado")
                        val fechaRecojoPedidoMotorizado =
                            doc.getTimestamp("fechaRecojoPedidoMotorizado")
                        val thumbnailFotoRecojo = doc.getString("thumbnailFotoRecojo") ?: "Error"

                        // Obtener coordenadas
                        val coordenadas = doc.get("pedidoCoordenadas") as? Map<String, Any>
                        val latitud = coordenadas?.get("lat") as? Double
                        val longitud = coordenadas?.get("lng") as? Double

                        if (latitud != null && longitud != null) {
                            val ubicacion = LatLng(latitud, longitud)
                            puntosEntregaLista.add(
                                PuntoPedido(
                                    id,
                                    ubicacion,
                                    clienteNombre,
                                    proveedorNombre,
                                    pedidoCantidadCobrar,
                                    pedidoMetodoPago,
                                    fechaEntregaPedidoMotorizado,
                                    fechaRecojoPedidoMotorizado,
                                    thumbnailFotoRecojo
                                )

                            )
                            Log.d("Firestore", "Punto entrega: $ubicacion - Cliente: $clienteNombre")

                            /*if (motorizadoRecojo == rutaMotorizado) {
                            Log.d("Firestore", "Pedido recogido y listo para entrega: $id")
                            puntosRecojoListaEspecial.add(PuntoPedido(id, ubicacion, clienteNombre, proveedorNombre, pedidoCantidadCobrar, pedidoMetodoPago, null, fechaRecojoPedidoMotorizado))
                        } else {
                            puntosEntregaLista.add(PuntoPedido(id, ubicacion, clienteNombre, proveedorNombre, pedidoCantidadCobrar, pedidoMetodoPago, fechaEntregaPedidoMotorizado, fechaRecojoPedidoMotorizado))
                            Log.d("Firestore", "Punto entrega: $ubicacion - Cliente: $clienteNombre")
                        }*/
                        }
                    }

                    // Indicar que los datos están cargados
                    datosCargados = true

                    // Agregar marcadores y centrar el mapa solo si ya está inicializado
                    mMap?.let {
                        actualizarMapa()
                    }
                }
        /*}*/
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
                        Log.d("MainActivity", "KML cargado correctamente")

                        // Verificar si hay contenedores en el KML
                        val containers = kmlLayer?.containers
                        if (containers != null) {
                            for (container in containers) {
                                Log.d("MainActivity", "Container encontrado: ${container.hasProperties()}")
                            }
                        } else {
                            Log.d("MainActivity", "No se encontraron containers en el KML")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error al procesar KML", e)
                        Toast.makeText(applicationContext, "Error al procesar el mapa KML: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "Error al cargar KML", e)
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
        if (ubicacionDisponible) {
            obtenerUbicacionActual()
        } else {
            centrarMapaSinUbicacion()
        }
    }
    private fun agregarMarcadores() {
        mMap?.let { map ->
            // NO usar map.clear() aquí porque eliminaría el KML

            val boundsBuilder = LatLngBounds.Builder()

            // Agregar marcadores de recojo con color AZUL
            for (punto in puntosRecojoLista) {
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(punto.ubicacion)
                        .title("Recojo: ${punto.proveedorNombre}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
                boundsBuilder.include(punto.ubicacion)
            }

            // Agregar marcadores de entrega con color ROJO
            for (punto in puntosEntregaLista) {
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(punto.ubicacion)
                        .title("Entrega: ${punto.clienteNombre}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
                boundsBuilder.include(punto.ubicacion)
            }

            for (punto in puntosRecojoListaEspecial) {
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(punto.ubicacion)
                        .title("Entrega: ${punto.clienteNombre}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
                boundsBuilder.include(punto.ubicacion)
            }
        }
    }
    private fun centrarMapaConUbicacion(ubicacionUsuario: LatLng) {
        if (puntosRecojoLista.isEmpty() && puntosEntregaLista.isEmpty() && puntosRecojoListaEspecial.isEmpty() && mMap != null) {
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
            if (!puntosRecojoListaEspecial.isNullOrEmpty()) {
                for (punto in puntosRecojoListaEspecial) {
                    boundsBuilder.include(punto.ubicacion)
                }
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 80  // Margen en píxeles

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error en animateCamera con ubicación", e)
                    try {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al centrar mapa con ubicación", e)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
        }
    }
    private fun centrarMapaSinUbicacion() {
        if (puntosRecojoLista.isEmpty() && puntosEntregaLista.isEmpty()) {
            Log.d("MainActivity", "No hay coordenadas para centrar el mapa")
            return
        }

        try {
            val boundsBuilder = LatLngBounds.Builder()
            var hayPuntos = false

            // Incluir todas las coordenadas de los puntos de recojo
            for (punto in puntosRecojoLista) {
                boundsBuilder.include(punto.ubicacion)
                hayPuntos = true
            }

            // Incluir todas las coordenadas de los puntos de entrega
            for (punto in puntosEntregaLista) {
                boundsBuilder.include(punto.ubicacion)
                hayPuntos = true
            }

            if (!hayPuntos) {
                Log.d("MainActivity", "No hay coordenadas válidas para centrar el mapa")
                return
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 200  // Margen en píxeles

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error en animateCamera", e)
                    try {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        // Si todo falla, centrar en la primera coordenada disponible
                        val primerPunto = if (puntosRecojoLista.isNotEmpty()) {
                            puntosRecojoLista[0].ubicacion
                        } else {
                            puntosEntregaLista[0].ubicacion
                        }
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al centrar mapa", e)
            Handler(Looper.getMainLooper()).postDelayed({
                val primerPunto = if (puntosRecojoLista.isNotEmpty()) {
                    puntosRecojoLista[0].ubicacion
                } else {
                    puntosEntregaLista[0].ubicacion
                }
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
            }, 300)
        }
    }
    private fun limpiarSoloMarcadores() {
        // Eliminar todos los marcadores guardados en la lista
        for (marker in marcadores) {
            marker.remove()
        }
        marcadores.clear()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rutaMotorizado = SessionManager.ruta ?: ""
        Log.d("MainActivity", "Ruta recibida: $rutaMotorizado")

        escucharCambiosEnUsuario()

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MiAdapter(emptyList())  // Inicialmente vacío
        recyclerView.adapter = adapter

        // Cargar datos desde Firestore
        obtenerDatosFirestore()

        // Inicializar DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // Configurar listener para el botón flotante
        val fabMenu = findViewById<FloatingActionButton>(R.id.fab_menu)
        fabMenu.visibility = GONE
        /*fabMenu.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }*/

        // Configurar navegación del menú lateral
        /*navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> showToast("Inicio")
                R.id.nav_profile -> showToast("Perfil")
                R.id.nav_settings -> showToast("Configuración")
                R.id.nav_about -> showToast("Acerca de")
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }*/

        actualizarIndicadores()

    }

    override fun onResume() {
        super.onResume()

        // Verificar si el mapa ya está inicializado
        if (mapaListo) {
            // Verificar permisos de ubicación
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                // Activar la capa de "Mi ubicación" en el mapa
                mMap?.isMyLocationEnabled = true

                // Verificar si el servicio de ubicación está activado
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

                    // Actualizar el estado de la ubicación
                    ubicacionDisponible = true

                    // Intentar obtener la ubicación y centrar el mapa
                    Log.d("MainActivity", "onResume: Intentando actualizar ubicación")
                    obtenerUbicacionActual()
                } else {
                    // Los servicios de ubicación están desactivados
                    ubicacionDisponible = false
                    // No mostramos el diálogo aquí para evitar mostrarlo cada vez que se reanude la actividad
                    // Solo centramos el mapa con los marcadores
                    //centrarMapaSinUbicacion()
                    mostrarDialogoActivarUbicacion()
                }
            } else {
                // No tenemos permisos de ubicación
                Log.d("MainActivity", "onResume: Sin permisos de ubicación")
                ubicacionDisponible = false
                activarUbicacionUsuario()
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
                //Log.d("MainActivity", "Ubicación obtenida: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                val ubicacionUsuario = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                centrarMapaConUbicacion(ubicacionUsuario)
                actualizarListaOrdenada(ubicacionUsuario)
            } else {
                Log.d("MainActivity", "No se pudo obtener la ubicación actual")
                // Si no hay ubicación disponible, centrar solo con marcadores
                ubicacionDisponible = false
                centrarMapaSinUbicacion()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al obtener ubicación", e)
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
        } else {
            // Todos los permisos ya fueron concedidos
            activarUbicacionUsuario()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                activarUbicacionUsuario()
            } else {
                ubicacionDisponible = false
                showToast("Permiso de ubicación denegado")

                // Verificar si el usuario seleccionó "No volver a preguntar"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    mostrarDialogoPermisoDenegado()
                } else {
                    solicitarPermisoUbicacion()
                }
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
        usuarioListener?.remove()
        recojosListener?.remove() // Añadir esta línea
        entregasListener?.remove()
    }


}