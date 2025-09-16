package com.example.moto_version.gimi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.CustomInfoWindowAdapter
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


class MapaPedidoFragment : Fragment(R.layout.fragment_mapa_pedido), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GimiMiAdapter
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
    // En MapaPedidoFragment
    private val puntosRecojoLista = mutableListOf<PuntoPedidoCliente>()
    private var listaRecojosOriginal = listOf<ClienteRecojo>() // Lista original para mantener todos los items
    private var listaRecojosFiltrada = listOf<ClienteRecojo>() // Lista filtrada para mostrar
    private var kmlLayer: KmlLayer? = null
    private val marcadores = mutableListOf<Marker>() // Lista para guardar referencia a todos los marcadores
    private val alturaMapa = 700
    private lateinit var gimi_flexboxIndicadores: FlexboxLayout
    private lateinit var gimi_flexboxIndicadores2: FlexboxLayout
    private lateinit var uno_gimi_frame: View
    private var listaRecojos: List<ClienteRecojo> = emptyList()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView correctamente
        uno_gimi_frame = view.findViewById(R.id.uno_gimi_frame)
        recyclerView = view.findViewById(R.id.uno_gimi_recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        gimi_flexboxIndicadores = view.findViewById(R.id.uno_gimi_flexboxIndicadores)
        gimi_flexboxIndicadores2 = view.findViewById(R.id.uno_gimi_flexboxIndicadores2)

        // Inicializar el mapa correctamente
        val mapFragment = childFragmentManager.findFragmentById(R.id.uno_gimi_map_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }



    fun filterList(query: String?) {
        if (query.isNullOrEmpty()) {
            // Si la consulta está vacía, restaura las listas originales
            adapter.actualizarLista(listaRecojosOriginal)
            actualizarMarcadoresEnMapa(puntosRecojoLista)
        } else {
            // Filtra lista de ClienteRecojo para el RecyclerView
            val listaRecojosFiltrada = listaRecojosOriginal.filter { recojo ->
                recojo.clienteNombre.contains(query, ignoreCase = true) ||
                        recojo.proveedorNombre.contains(query, ignoreCase = true)
            }

            // Filtra la lista de puntos para los marcadores en el mapa
            val puntosFiltrados = puntosRecojoLista.filter { punto ->
                punto.clienteNombre.contains(query, ignoreCase = true) ||
                        punto.proveedorNombre.contains(query, ignoreCase = true)
            }

            // Actualiza el RecyclerView
            adapter.actualizarLista(listaRecojosFiltrada)

            // Actualiza los marcadores en el mapa
            actualizarMarcadoresEnMapa(puntosFiltrados)
        }
    }

    private fun actualizarMarcadoresEnMapa(puntosFiltrados: List<PuntoPedidoCliente>) {
        mMap?.let { map ->
            limpiarSoloMarcadores() // Limpiar marcadores antes de agregar nuevos

            for (punto in puntosFiltrados) {
                val color: Float
                val posicion: LatLng
                val titulo: String
                val snippet: String

                when {
                    punto.fechaAnulacionPedido != null -> {
                        color = BitmapDescriptorFactory.HUE_ROSE
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre}" // Título
                        snippet = "Recojo: ${punto.proveedorNombre}" // Snippet
                    }
                    punto.fechaEntregaPedidoMotorizado != null -> {
                        color = BitmapDescriptorFactory.HUE_GREEN
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre}" // Título
                        snippet = "Recojo: ${punto.proveedorNombre}" // Snippet
                    }
                    punto.fechaRecojoPedidoMotorizado == null -> {
                        color = BitmapDescriptorFactory.HUE_YELLOW
                        posicion = punto.ubicacionProveedor
                        titulo = "Entrega: ${punto.clienteNombre}" // Título
                        snippet = "Recojo: ${punto.proveedorNombre}" // Snippet
                    }
                    else -> {
                        color = BitmapDescriptorFactory.HUE_BLUE
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.clienteNombre}" // Título
                        snippet = "Recojo: ${punto.proveedorNombre}" // Snippet
                    }
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(titulo)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )

                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
            }

            // Si hay marcadores filtrados, ajusta la cámara al primero o al área que los contenga
        }
    }

    override fun onResume() {
        super.onResume()
        // Verificar si el mapa ya está inicializado
        if (mapaListo) {
            centrarMapaSinUbicacion()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phone = SessionManager.phone ?: ""
        Log.d("GimiMainActivity", "Phone recibido: $phone")
        escucharCambiosEnUsuario()
        adapter = GimiMiAdapter(emptyList())  // Inicialmente vacío

        // Cargar datos desde Firestore
        obtenerDatosFirestore()

        centrarMapaSinUbicacion()
        actualizarIndicadores()

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

    private fun solicitarPermisoUbicacion() {
        val permisosNecesarios = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permisosNecesarios.add(Manifest.permission.CAMERA)
        }

        if (permisosNecesarios.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(), // Cambia requireContext() por requireActivity()
                permisosNecesarios.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
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
        val intent = Intent(requireActivity(), LoginActivity::class.java)  // Redirigir a la pantalla de login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Eliminar historial de actividades
        startActivity(intent)
        requireActivity().finish()  // Cerrar la actividad actual
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
                        puntosRecojoLista.add(
                            PuntoPedidoCliente(
                                id,
                                ubicacionCliente,
                                clienteNombre,
                                proveedorNombre,
                                pedidoCantidadCobrar,
                                pedidoMetodoPago,
                                fechaEntregaPedidoMotorizado,
                                fechaRecojoPedidoMotorizado,
                                thumbnailFotoRecojo,
                                fechaAnulacionPedido,
                                ubicacionProveedor,
                                thumbnailFotoEntrega,
                                motorizadoEntrega,
                                motorizadoRecojo
                            )
                        )
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
        listaRecojosOriginal = listaRecojos // Guarda la lista original

        adapter.actualizarLista(listaRecojos)
        actualizarIndicadores()

        try {
            val boundsBuilder = LatLngBounds.Builder()
            var hayPuntos = false

            for (punto in puntosRecojoLista) {
                val latCliente = punto.ubicacionCliente.latitude
                val lonCliente = punto.ubicacionCliente.longitude
                val cliente = punto.clienteNombre
                val clienteId = punto.id
                val latProveedor = punto.ubicacionProveedor.latitude
                val lonProveedor = punto.ubicacionProveedor.longitude
                val proveedor = punto.proveedorNombre
                val proveedorId = punto.id

                var clienteDentroDeRango = true
                var proveedorDentroDeRango = true

                // Verificar si la latitud y longitud del cliente están fuera del rango permitido
                if (!(latCliente in -12.5..-11.0 && lonCliente in -77.5..-76.0)) {
                    Log.d("Mapa", "Ubicación Cliente fuera de rango: Lat=$latCliente, Lon=$lonCliente para el cliente $cliente con el Id $clienteId")
                    clienteDentroDeRango = false
                }

                // Verificar si la latitud y longitud del proveedor están fuera del rango permitido
                if (!(latProveedor in -12.5..-11.0 && lonProveedor in -77.5..-76.0)) {
                    Log.d("Mapa", "Ubicación Proveedor fuera de rango: Lat=$latProveedor, Lon=$lonProveedor para el proveedor $proveedor con el Id $proveedorId")
                    proveedorDentroDeRango = false
                }

                // Solo incluir en boundsBuilder si están dentro del rango permitido
                if (clienteDentroDeRango) {
                    boundsBuilder.include(punto.ubicacionCliente)
                    hayPuntos = true
                }
                if (proveedorDentroDeRango) {
                    boundsBuilder.include(punto.ubicacionProveedor)
                    hayPuntos = true
                }
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

    private fun actualizarIndicadores() {
        val cantidadPuntosRecojo = puntosRecojoLista.size

        val cantidadAnulados = puntosRecojoLista.count { it.fechaAnulacionPedido != null }
        val cantidadPendientes = puntosRecojoLista.count { it.fechaRecojoPedidoMotorizado == null && it.fechaAnulacionPedido == null }
        val cantidadFinalizados = puntosRecojoLista.count { it.fechaEntregaPedidoMotorizado != null }
        val cantidadFaltantes = cantidadPuntosRecojo - cantidadFinalizados - cantidadAnulados - cantidadPendientes

        // Asegurar que la vista no es nula antes de acceder a ella
        view?.let {
            val indUno: TextView = it.findViewById(R.id.uno_gimi_indUno)
            val cardIndUno: CardView = it.findViewById(R.id.uno_gimi_cardIndUno)
            indUno.text = "$cantidadAnulados"
            val textUno: TextView = it.findViewById(R.id.uno_gimi_textUno)
            textUno.text = if (cantidadAnulados == 1) "Anulado" else "Anulados"

            val indCero: TextView = it.findViewById(R.id.uno_gimi_indCero)
            val cardIndCero: CardView = it.findViewById(R.id.uno_gimi_cardIndCero)
            indCero.text = "$cantidadPendientes"
            val textCero: TextView = it.findViewById(R.id.uno_gimi_textCero)

            val indDos: TextView = it.findViewById(R.id.uno_gimi_indDos)
            val cardIndDos: CardView = it.findViewById(R.id.uno_gimi_cardIndDos)
            indDos.text = "$cantidadFaltantes"
            val textDos: TextView = it.findViewById(R.id.uno_gimi_textDos)

            val indTres: TextView = it.findViewById(R.id.uno_gimi_indTres)
            val cardIndTres: CardView = it.findViewById(R.id.uno_gimi_cardIndTres)
            indTres.text = "$cantidadFinalizados"
            val textTres: TextView = it.findViewById(R.id.uno_gimi_textTres)
        }
    }

    private fun actualizarMapa() {
        limpiarSoloMarcadores()
        cargarMapaKML()
        agregarMarcadores("pedidos")
        centrarMapaSinUbicacion()
    }

    private fun limpiarSoloMarcadores() {
        // Eliminar todos los marcadores guardados en la lista
        for (marker in marcadores) {
            marker.remove()
        }
        marcadores.clear()
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
                            context,
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
                        kmlLayer = KmlLayer(mMap, kmlInputStream, context)
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
                            context,
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
                        context,
                        "Error al cargar el mapa: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun agregarMarcadores(tipo: String) {
        mMap?.let { map ->
            limpiarSoloMarcadores() // Limpiar marcadores antes de agregar nuevos

            for (punto in puntosRecojoLista) {
                val color: Float
                val posicion: LatLng
                val titulo: String
                val snippet: String

                when (tipo) {
                    "recojos" -> {
                        color = BitmapDescriptorFactory.HUE_BLUE
                        posicion = punto.ubicacionProveedor
                        titulo = "Recojo: ${punto.motorizadoRecojo}"
                        snippet = "Proveedor: ${punto.proveedorNombre}"
                    }
                    "entregas" -> {
                        color = BitmapDescriptorFactory.HUE_GREEN
                        posicion = punto.ubicacionCliente
                        titulo = "Entrega: ${punto.motorizadoEntrega}"
                        snippet = "Cliente: ${punto.clienteNombre}"
                    }
                    else -> {
                        // Marcadores para pedidos (comportamiento por defecto)
                        when {
                            punto.fechaAnulacionPedido != null -> {
                                color = BitmapDescriptorFactory.HUE_RED
                                posicion = punto.ubicacionCliente
                                titulo = "Entrega: ${punto.clienteNombre}"
                                snippet = "Recojo: ${punto.proveedorNombre}"
                            }
                            punto.fechaEntregaPedidoMotorizado != null -> {
                                color = BitmapDescriptorFactory.HUE_GREEN
                                posicion = punto.ubicacionCliente
                                titulo = "Entrega: ${punto.clienteNombre}"
                                snippet = "Recojo: ${punto.proveedorNombre}"
                            }
                            punto.fechaRecojoPedidoMotorizado == null -> {
                                color = BitmapDescriptorFactory.HUE_YELLOW
                                posicion = punto.ubicacionProveedor
                                titulo = "Entrega: ${punto.clienteNombre}"
                                snippet = "Recojo: ${punto.proveedorNombre}"
                            }
                            else -> {
                                color = BitmapDescriptorFactory.HUE_BLUE
                                posicion = punto.ubicacionCliente
                                titulo = "Entrega: ${punto.clienteNombre}"
                                snippet = "Recojo: ${punto.proveedorNombre}"
                            }
                        }
                    }
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(posicion)
                        .title(titulo)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )

                marker?.let { marcadores.add(it) } // Guardar referencia al marcador
            }
        }
    }



    fun actualizarMapaSegunSeleccion(tipo: String) {
        recyclerView.visibility = if (tipo == "pedidos") View.VISIBLE else View.GONE

        val params = uno_gimi_frame.layoutParams
        params.height = if (tipo == "pedidos") alturaMapa else ViewGroup.LayoutParams.MATCH_PARENT
        uno_gimi_frame.layoutParams = params

        when (tipo) {
            "pedidos" -> {
                gimi_flexboxIndicadores.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.visibility = View.GONE
                gimi_flexboxIndicadores2.removeAllViews()
                obtenerDatosFirestore()  // Recargar pedidos
            }
            "recojos" -> {
                limpiarSoloMarcadores()
                gimi_flexboxIndicadores.visibility = View.GONE
                gimi_flexboxIndicadores2.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.removeAllViews()
                agregarMarcadores("recojos") // Agregar marcadores de recojos

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
                limpiarSoloMarcadores()
                gimi_flexboxIndicadores.visibility = View.GONE
                gimi_flexboxIndicadores2.visibility = View.VISIBLE
                gimi_flexboxIndicadores2.removeAllViews()
                agregarMarcadores("entregas") // Agregar marcadores de entregas

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
        val cardView = CardView(requireContext()).apply {
            layoutParams = ViewGroup.MarginLayoutParams(WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            }
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer))
            radius = 16f
            cardElevation = 0f
        }

        val linearLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx())
        }

        val textViewCantidad = TextView(context).apply {
            text = cantidad.toString()
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer))
        }

        val textViewMotorizado = TextView(context).apply {
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
}