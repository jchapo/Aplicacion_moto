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

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MiAdapter
    private var mMap: GoogleMap? = null
    private lateinit var drawerLayout: DrawerLayout
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    private fun obtenerDatosFirestore() {
        db.collection("recojos")
            .get()
            .addOnSuccessListener { result ->
                val listaRecojos = result.documents.mapNotNull { doc ->
                    val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                    val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                    val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "0.00"
                    Recojo(clienteNombre, proveedorNombre, pedidoCantidadCobrar)
                }
                adapter.actualizarLista(listaRecojos)  // Actualizar RecyclerView
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error al obtener documentos", exception)
            }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val ubicacion = LatLng(-12.0464, -77.0428) // Lima, Perú
        mMap?.apply {
            addMarker(MarkerOptions().position(ubicacion).title("Ubicación Inicial"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 12f))
        }
    }

    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}
