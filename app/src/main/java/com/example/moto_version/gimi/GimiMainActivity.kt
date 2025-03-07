package com.example.moto_version.gimi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.cliente.AnuncioDialogFragment
import com.example.moto_version.cliente.OrderFormActivity
import com.example.moto_version.LoginActivity
import com.example.moto_version.R
import com.example.moto_version.SessionManager
import com.example.moto_version.models.ClienteRecojo
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
private const val REQUEST_LOCATION_SETTINGS = 1002

class GimiMainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GimiMiAdapter
    private lateinit var gimi_frame: FrameLayout
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
    data class PuntoPedidoCliente(val id: String, val ubicacionCliente: LatLng, val clienteNombre: String, val proveedorNombre: String, val pedidoCantidadCobrar: String, val pedidoMetodoPago: String, val fechaEntregaPedidoMotorizado: Timestamp?, val fechaRecojoPedidoMotorizado: Timestamp?, val thumbnailFotoRecojo: String, val fechaAnulacionPedido: Timestamp?, val ubicacionProveedor: LatLng, val thumbnailFotoEntrega: String, val motorizadoEntrega: String, val motorizadoRecojo: String)
    private val puntosRecojoLista = mutableListOf<PuntoPedidoCliente>()
    private val puntosRecojoListaEspecial = mutableListOf<PuntoPedidoCliente>()
    private val puntosEntregaLista = mutableListOf<PuntoPedidoCliente>()
    private var kmlLayer: KmlLayer? = null
    private val marcadores = mutableListOf<Marker>() // Lista para guardar referencia a todos los marcadores
    private lateinit var toolbar: MaterialToolbar
    private val alturaMapa = 700
    private lateinit var gimi_flexboxIndicadores: FlexboxLayout
    private lateinit var gimi_flexboxIndicadores2: FlexboxLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gimi_activity_main)
        toolbar = findViewById(R.id.gimi_toolbar)
        drawerLayout = findViewById(R.id.gimi_drawer_layout)
        gimi_frame = findViewById(R.id.gimi_frame)

        // Configurar el clic en el botón de la Toolbar
        toolbar.setNavigationOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            } else {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        gimi_flexboxIndicadores = findViewById(R.id.gimi_flexboxIndicadores)
        gimi_flexboxIndicadores2 = findViewById(R.id.gimi_flexboxIndicadores2)

        val drawerLayout = findViewById<DrawerLayout>(R.id.gimi_drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.gimi_nav_view)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_map_pedidos -> {
                    actualizarMapaSegunSeleccion("pedidos")
                    drawerLayout.closeDrawer(GravityCompat.START)  // Cierra el Drawer
                    true
                }
                R.id.nav_map_recojos -> {
                    actualizarMapaSegunSeleccion("recojos")
                    drawerLayout.closeDrawer(GravityCompat.START)  // Cierra el Drawer
                    true
                }
                R.id.nav_map_entregas -> {
                    actualizarMapaSegunSeleccion("entregas")
                    drawerLayout.closeDrawer(GravityCompat.START)  // Cierra el Drawer
                    true
                }
                else -> false
            }
        }



        // Cambiar el título dinámicamente
        toolbar.title = "Mapa Pedidos"

        val params = gimi_frame.layoutParams
        params.height = alturaMapa
        gimi_frame.layoutParams = params

        phone = SessionManager.phone ?: ""
        Log.d("GimiMainActivity", "Phone recibido: $phone")

        escucharCambiosEnUsuario()

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.gimi_map_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.gimi_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GimiMiAdapter(emptyList())  // Inicialmente vacío
        recyclerView.adapter = adapter

        // Cargar datos desde Firestore
        obtenerDatosFirestore()

        centrarMapaSinUbicacion()
        actualizarIndicadores()

    }

    private fun actualizarMapaSegunSeleccion(tipo: String) {
        mMap?.clear()  // Eliminar marcadores actuales
        recyclerView.visibility = if (tipo == "pedidos") View.VISIBLE else View.GONE  // Ocultar RecyclerView si no es "pedidos"

        val params = gimi_frame.layoutParams
        params.height = if (tipo == "pedidos") alturaMapa else ViewGroup.LayoutParams.MATCH_PARENT
        gimi_frame.layoutParams = params
        when (tipo) {
            "pedidos" -> {
                toolbar.title = "Mapa Pedidos"
                gimi_flexboxIndicadores.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.visibility = View.GONE
                gimi_flexboxIndicadores2.removeAllViews()
                obtenerDatosFirestore()  // Recargar pedidos
            }
            "recojos" -> {
                toolbar.title = "Mapa Recojos"
                gimi_flexboxIndicadores.visibility = View.GONE
                gimi_flexboxIndicadores2.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.removeAllViews()
                agregarMarcadoresRecojos()

                // Contar pedidos por motorizadoRecojo
                val contadorPedidos = mutableMapOf<String, Int>()
                puntosRecojoLista.forEach { pedido ->
                    val motorizado = pedido.motorizadoRecojo ?: "Desconocido"
                    contadorPedidos[motorizado] = contadorPedidos.getOrDefault(motorizado, 0) + 1
                }

                // Crear un CardView por cada motorizadoRecojo
                contadorPedidos.forEach { (motorizado, cantidad) ->
                    val cardView = crearIndicador(motorizado, cantidad)
                    gimi_flexboxIndicadores2.addView(cardView)
                }
            }
            "entregas" -> {
                toolbar.title = "Mapa Entregas"
                gimi_flexboxIndicadores.visibility = View.GONE
                gimi_flexboxIndicadores2.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.removeAllViews()
                agregarMarcadoresEntregas()

                // Contar pedidos por motorizadoEntrega
                val contadorPedidos = mutableMapOf<String, Int>()
                puntosRecojoLista.forEach { pedido ->
                    val motorizado = pedido.motorizadoEntrega ?: "Desconocido"
                    contadorPedidos[motorizado] = contadorPedidos.getOrDefault(motorizado, 0) + 1
                }

                // Crear un CardView por cada motorizadoRecojo
                contadorPedidos.forEach { (motorizado, cantidad) ->
                    val cardView = crearIndicador(motorizado, cantidad)
                    gimi_flexboxIndicadores2.addView(cardView)
                }
            }
        }
    }

    // Función para crear un CardView dinámicamente
    private fun crearIndicador(motorizado: String, cantidad: Int): CardView {
        val cardView = CardView(this).apply {
            layoutParams = ViewGroup.MarginLayoutParams(WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer))
            radius = 16f
            cardElevation = 0f
        }

        val linearLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx())
        }

        val textViewCantidad = TextView(this).apply {
            text = cantidad.toString()
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))
        }

        val textViewMotorizado = TextView(this).apply {
            text = motorizado
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))
        }

        linearLayout.addView(textViewCantidad)
        linearLayout.addView(textViewMotorizado)
        cardView.addView(linearLayout)

        return cardView
    }

    // Función de extensión para convertir dp a px
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun agregarMarcadoresRecojos() {
        puntosRecojoLista.forEach { punto ->
            val marcador = mMap?.addMarker(
                MarkerOptions()
                    .position(punto.ubicacionProveedor)
                    .title("${punto.motorizadoRecojo}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    private fun agregarMarcadoresEntregas() {
        puntosRecojoLista.forEach { punto ->
            val marcador = mMap?.addMarker(
                MarkerOptions()
                    .position(punto.ubicacionCliente)
                    .title("${punto.motorizadoEntrega}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }
    }

    private fun actualizarIndicadores() {
        val cantidadPuntosRecojo = puntosRecojoLista.size

        // Contar puntos anulados (fechaAnulacionPedido != null)
        val cantidadAnulados = puntosRecojoLista.count { it.fechaAnulacionPedido != null }

        // Contar puntos pendientes (fechaRecojoPedidoMotorizado == null)
        val cantidadPendientes = puntosRecojoLista.count { it.fechaRecojoPedidoMotorizado == null && it.fechaAnulacionPedido == null}

        // Contar puntos finalizados (fechaEntregaPedidoMotorizado != null)
        val cantidadFinalizados = puntosRecojoLista.count { it.fechaEntregaPedidoMotorizado != null }

        // Calcular los faltantes (total - finalizados - anulados)
        val cantidadFaltantes = cantidadPuntosRecojo - cantidadFinalizados - cantidadAnulados - cantidadPendientes

        // Actualizar CardUno (Anulados)
        val indUno: TextView = findViewById(R.id.gimi_indUno)
        val cardIndUno: CardView = findViewById(R.id.gimi_cardIndUno)
        indUno.text = "$cantidadAnulados"
        val textUno: TextView = findViewById(R.id.gimi_textUno)
        textUno.text = if (cantidadAnulados == 1) "Anulado" else "Anulados"
        //cardIndUno.visibility = if (cantidadAnulados == 0) View.GONE else View.VISIBLE

        // Actualizar CardCero (Pendientes)
        val indCero: TextView = findViewById(R.id.gimi_indCero)
        val cardIndCero: CardView = findViewById(R.id.gimi_cardIndCero)
        indCero.text = "$cantidadPendientes"
        val textCero: TextView = findViewById(R.id.gimi_textCero)
        //textCero.text = if (cantidadPendientes == 1) "Pendiente" else "Pendientes"
        //cardIndCero.visibility = if (cantidadPendientes == 0) View.GONE else View.VISIBLE


        // Actualizar CardDos (Faltantes)
        val indDos: TextView = findViewById(R.id.gimi_indDos)
        val cardIndDos: CardView = findViewById(R.id.gimi_cardIndDos)
        indDos.text = "$cantidadFaltantes"
        val textDos: TextView = findViewById(R.id.gimi_textDos)
        //textDos.text = if (cantidadFaltantes == 1) "Faltante" else "Faltantes"
        //cardIndDos.visibility = if (cantidadFaltantes == 0) View.GONE else View.VISIBLE

        // Actualizar CardTres (Finalizados)
        val indTres: TextView = findViewById(R.id.gimi_indTres)
        val cardIndTres: CardView = findViewById(R.id.gimi_cardIndTres)
        indTres.text = "$cantidadFinalizados"
        val textTres: TextView = findViewById(R.id.gimi_textTres)
        //textTres.text = if (cantidadFinalizados == 1) "Listo" else "Listos"
        //cardIndTres.visibility = if (cantidadFinalizados == 0) View.GONE else View.VISIBLE
    }


    private fun obtenerDatosFirestore() {
        // Obtener la hora actual del dispositivo
        val calendar = Calendar.getInstance()
        val horaActual = calendar.get(Calendar.HOUR_OF_DAY)
        // Remover listener anterior si existe
        recojosListener?.remove()
        entregasListener?.remove()

        // Configurar un listener en tiempo real
        recojosListener = db.collection("recojos")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error al obtener documentos", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.d("Firestore", "Snapshot nulo")
                    return@addSnapshotListener
                }

                puntosRecojoLista.clear() // Limpiar lista antes de agregar nuevas coordenadas

                snapshots.documents.forEach { doc ->
                    val id = doc.id
                    val clienteNombre = doc.getString("clienteNombre") ?: "Desconocido"
                    val proveedorNombre = doc.getString("proveedorNombre") ?: "Sin empresa"
                    val pedidoCantidadCobrar = doc.getString("pedidoCantidadCobrar") ?: "0.00"
                    val pedidoMetodoPago = doc.getString("pedidoMetodoPago") ?: "Error"
                    val fechaEntregaPedidoMotorizado = doc.getTimestamp("fechaEntregaPedidoMotorizado")
                    val fechaRecojoPedidoMotorizado = doc.getTimestamp("fechaRecojoPedidoMotorizado")
                    val fechaAnulacionPedido = doc.getTimestamp("fechaAnulacionPedido")
                    val thumbnailFotoRecojo = doc.getString("thumbnailFotoRecojo") ?: ""
                    val thumbnailFotoEntrega = doc.getString("thumbnailFotoEntrega") ?: ""
                    val motorizadoEntrega = doc.getString("motorizadoEntrega") ?: ""
                    val motorizadoRecojo = doc.getString("motorizadoRecojo") ?: ""

                    // Obtener coordenadas
                    val coordenadasProveedor = doc.get("recojoCoordenadas") as? Map<String, Any>
                    val latitudProveedor = coordenadasProveedor?.get("lat") as? Double
                    val longitudProveedor = coordenadasProveedor?.get("lng") as? Double

                    val coordenadasCliente = doc.get("pedidoCoordenadas") as? Map<String, Any>
                    val latitudCliente = coordenadasCliente?.get("lat") as? Double
                    val longitudCliente = coordenadasCliente?.get("lng") as? Double

                    if (latitudProveedor != null && longitudProveedor != null && latitudCliente != null && longitudCliente != null) {
                        val ubicacionProveedor = LatLng(latitudProveedor, longitudProveedor)
                        val ubicacionCliente = LatLng(latitudCliente, longitudCliente)
                        puntosRecojoLista.add(PuntoPedidoCliente(id, ubicacionCliente, clienteNombre, proveedorNombre,
                            pedidoCantidadCobrar, pedidoMetodoPago, fechaEntregaPedidoMotorizado, fechaRecojoPedidoMotorizado,
                            thumbnailFotoRecojo, fechaAnulacionPedido, ubicacionProveedor, thumbnailFotoEntrega, motorizadoEntrega, motorizadoRecojo))
                        //Log.d("Firestore", "Punto recojo: $ubicacion - Cliente: $clienteNombre")
                    }
                }

                datosCargados = true

                // Agregar marcadores y centrar el mapa solo si ya está inicializado
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
                val kmlUrl =
                    URL("https://www.google.com/maps/d/kml?mid=13U820BGFZW20wbx4NE7e56AuJGGvzzM&forcekml=1")
                val connection = kmlUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val inputStream = connection.inputStream
                val kmlData = inputStream.bufferedReader().use { it.readText() }

                // Verificar si el KML contiene datos
                if (kmlData.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "El archivo KML está vacío",
                            Toast.LENGTH_LONG
                        ).show()
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
                        Log.d("GimiMainActivity", "KML cargado correctamente")

                        // Verificar si hay contenedores en el KML
                        val containers = kmlLayer?.containers
                        if (containers != null) {
                            for (container in containers) {
                                Log.d(
                                    "GimiMainActivity",
                                    "Container encontrado: ${container.hasProperties()}"
                                )
                            }
                        } else {
                            Log.d("GimiMainActivity", "No se encontraron containers en el KML")
                        }
                    } catch (e: Exception) {
                        Log.e("GimiMainActivity", "Error al procesar KML", e)
                        Toast.makeText(
                            applicationContext,
                            "Error al procesar el mapa KML: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("GimiMainActivity", "Error al cargar KML", e)
                    Toast.makeText(
                        applicationContext,
                        "Error al cargar el mapa: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun actualizarMapa() {
        limpiarSoloMarcadores()
        cargarMapaKML()
        agregarMarcadores()
        centrarMapaSinUbicacion()
    }
    private fun agregarMarcadores() {
        mMap?.let { map ->
            val boundsBuilder = LatLngBounds.Builder()

            for (punto in puntosRecojoLista) {
                val color: Float
                val posicion: LatLng
                val titulo: String

                when {
                    punto.fechaAnulacionPedido != null -> {
                        color = BitmapDescriptorFactory.HUE_ROSE
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre} \n Recojo: ${punto.proveedorNombre}"
                    }
                    punto.fechaEntregaPedidoMotorizado != null -> {
                        color = BitmapDescriptorFactory.HUE_GREEN
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre} \n Recojo: ${punto.proveedorNombre}"
                    }
                    punto.fechaRecojoPedidoMotorizado == null -> {
                        color = BitmapDescriptorFactory.HUE_YELLOW
                        posicion = punto.ubicacionProveedor
                        titulo = "Entrega: ${punto.clienteNombre} \n Recojo: ${punto.proveedorNombre}"
                    }
                    else -> {
                        color = BitmapDescriptorFactory.HUE_BLUE
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre} \n Recojo: ${punto.proveedorNombre}"
                    }
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )

                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
                boundsBuilder.include(posicion)
            }
        }
    }


    private fun centrarMapaSinUbicacion() {
        val listaRecojos = if (puntosRecojoLista.isNotEmpty()) {
            puntosRecojoLista.map { punto ->
                ClienteRecojo(
                    id = punto.id,
                    clienteNombre = punto.clienteNombre,
                    proveedorNombre = punto.proveedorNombre,
                    pedidoCantidadCobrar = punto.pedidoCantidadCobrar,
                    pedidoMetodoPago = punto.pedidoMetodoPago,
                    fechaEntregaPedidoMotorizado = punto.fechaEntregaPedidoMotorizado,
                    fechaRecojoPedidoMotorizado = punto.fechaRecojoPedidoMotorizado,
                    thumbnailFotoRecojo = punto.thumbnailFotoRecojo,
                    fechaAnulacionPedido = punto.fechaAnulacionPedido,
                    motorizadoEntrega = punto.motorizadoEntrega,
                    motorizadoRecojo = punto.motorizadoRecojo
                )
            }
        } else {
            emptyList() // Si está vacío, devuelve una lista vacía
        }

        adapter.actualizarLista(listaRecojos)
        actualizarIndicadores()

        try {
            val boundsBuilder = LatLngBounds.Builder()
            var hayPuntos = false

            for (punto in puntosRecojoLista) {
                boundsBuilder.include(punto.ubicacionCliente)
                boundsBuilder.include(punto.ubicacionProveedor)
                hayPuntos = true
            }


            if (!hayPuntos) {
                Log.d("GimiMainActivity", "No hay coordenadas válidas para centrar el mapa")
                return
            }

            // Obtener los límites y mover la cámara
            val bounds = boundsBuilder.build()
            val padding = 100  // Margen en píxeles

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e("GimiMainActivity", "Error en animateCamera", e)
                    try {
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e2: Exception) {
                        // Si todo falla, centrar en la primera coordenada disponible
                        val primerPunto = puntosRecojoLista[0].ubicacionCliente
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
                    }
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("GimiMainActivity", "Error al centrar mapa", e)
            Handler(Looper.getMainLooper()).postDelayed({
                val primerPunto = puntosRecojoLista[0].ubicacionCliente
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(primerPunto, 12f))
            }, 300)
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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.CAMERA)
        }

        if (permisosNecesarios.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permisosNecesarios.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
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