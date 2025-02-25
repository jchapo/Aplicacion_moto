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
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val REQUEST_LOCATION_SETTINGS = 1002

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MiAdapter
    private var mMap: GoogleMap? = null
    private lateinit var drawerLayout: DrawerLayout
    private val db = FirebaseFirestore.getInstance()
    private var usuarioListener: ListenerRegistration? = null
    private val coordenadasLista = mutableListOf<LatLng>()
    private var datosCargados = false  // Variable para controlar si los datos están cargados
    private var ubicacionDisponible = false  // Variable para controlar si la ubicación está disponible
    private var mapaListo = false // Variable para saber si el mapa ya está inicializado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        fabMenu.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Configurar navegación del menú lateral
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> showToast("Inicio")
                R.id.nav_profile -> showToast("Perfil")
                R.id.nav_settings -> showToast("Configuración")
                R.id.nav_about -> showToast("Acerca de")
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
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
                    centrarMapaSinUbicacion()
                }
            } else {
                // No tenemos permisos de ubicación
                Log.d("MainActivity", "onResume: Sin permisos de ubicación")
                ubicacionDisponible = false
                centrarMapaSinUbicacion()
            }
        }
    }

    private fun obtenerDatosFirestore() {
        db.collection("recojos")
            .get()
            .addOnSuccessListener { result ->
                Log.d("Firestore", "Cantidad de documentos: ${result.size()}")
                coordenadasLista.clear() // Limpiar lista antes de agregar nuevas coordenadas

                val listaRecojos = result.documents.mapNotNull { doc ->
                    val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                    val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                    val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "0.00"

                    // Obtener coordenadas
                    val coordenadas = doc.get("pedidoCoordenadas") as? Map<String, Any>
                    val latitud = coordenadas?.get("lat") as? Double
                    val longitud = coordenadas?.get("lng") as? Double

                    if (latitud != null && longitud != null) {
                        val ubicacion = LatLng(latitud, longitud)
                        coordenadasLista.add(ubicacion)
                        Log.d("Firestore", "Coordenadas obtenidas: $ubicacion")
                    }

                    Recojo(clienteNombre, proveedorNombre, pedidoCantidadCobrar)
                }
                adapter.actualizarLista(listaRecojos)

                // Indicar que los datos están cargados
                datosCargados = true

                // Agregar marcadores y centrar el mapa solo si ya está inicializado
                mMap?.let {
                    actualizarMapa()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al obtener documentos", exception)
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

    // Función para actualizar el mapa con marcadores y centrado
    private fun actualizarMapa() {
        agregarMarcadores()
        centrarMapa()
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
                centrarMapaSinUbicacion()
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
                Log.d("MainActivity", "Ubicación obtenida: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                val ubicacionUsuario = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                centrarMapaConUbicacion(ubicacionUsuario)
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

    private fun agregarMarcadores() {
        mMap?.clear() // Limpiar marcadores existentes
        mMap?.let { map ->
            for (ubicacion in coordenadasLista) {
                map.addMarker(MarkerOptions().position(ubicacion).title("Recojo"))
            }
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

    override fun onDestroy() {
        super.onDestroy()
        usuarioListener?.remove()
    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()  // Cerrar sesión en Firebase
        val intent = Intent(this, LoginActivity::class.java)  // Redirigir a la pantalla de login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Eliminar historial de actividades
        startActivity(intent)
        finish()  // Cerrar la actividad actual
    }

    private fun centrarMapa() {
        if (ubicacionDisponible) {
            obtenerUbicacionActual()
        } else {
            centrarMapaSinUbicacion()
        }
    }

    private fun centrarMapaConUbicacion(ubicacionUsuario: LatLng) {
        if (coordenadasLista.isEmpty() && mMap != null) {
            // Si no hay marcadores, centrar solo en la ubicación del usuario
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 15f))
            return
        }

        try {
            val boundsBuilder = LatLngBounds.Builder()

            // Incluir la ubicación del usuario
            boundsBuilder.include(ubicacionUsuario)

            // Incluir todas las coordenadas de marcadores
            for (ubicacion in coordenadasLista) {
                boundsBuilder.include(ubicacion)
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 200  // Margen en píxeles

            // Usar Handler para asegurar que la operación se realice en el hilo principal
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error en animateCamera con ubicación", e)
                    try {
                        // Si falla, intentar con moveCamera
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        // Si todo falla, centrar en la ubicación del usuario
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al centrar mapa con ubicación", e)
            // Plan B: centrar en la ubicación del usuario
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionUsuario, 12f))
        }
    }

    private fun centrarMapaSinUbicacion() {
        if (coordenadasLista.isEmpty()) {
            Log.d("MainActivity", "No hay coordenadas para centrar el mapa")
            return
        }

        try {
            val boundsBuilder = LatLngBounds.Builder()

            // Incluir todas las coordenadas
            for (ubicacion in coordenadasLista) {
                boundsBuilder.include(ubicacion)
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 200  // Margen en píxeles

            // Usar Handler para asegurar que la operación se realice en el hilo principal
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error en animateCamera", e)
                    try {
                        // Si falla, intentar con moveCamera
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        // Si todo falla, centrar en la primera coordenada
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadasLista[0], 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al centrar mapa", e)
            // Plan B: Si hay un error, centrar en la primera coordenada
            Handler(Looper.getMainLooper()).postDelayed({
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(coordenadasLista[0], 12f))
            }, 300)
        }
    }

    private fun solicitarPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
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
                centrarMapaSinUbicacion()
            }
        }
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
}