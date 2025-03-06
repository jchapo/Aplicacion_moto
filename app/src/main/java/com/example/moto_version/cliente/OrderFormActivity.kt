package com.example.moto_version.cliente

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moto_version.BuildConfig
import com.example.moto_version.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.random.Random

class OrderFormActivity : AppCompatActivity() {

    private var phone: String = ""
    private var nombreEmpresa: String = ""
    // Nuevas variables para el modo de edición
    private var isEditMode = false
    private var orderId = ""

    data class Coordenada(val latitud: Double, val longitud: Double, val direccion1: String, val direccion2: String)

    // Inputs para el proveedor
    private lateinit var etProveedorNombre: TextInputEditText
    private lateinit var etProveedorTelefono: TextInputEditText
    private lateinit var etProveedorDireccion: TextInputEditText
    private lateinit var spinnerProveedorDistrito: Spinner
    private lateinit var btnObtenerCoordenadasProveedor: Button
    private lateinit var tvCoordenadasProveedor: TextView
    private lateinit var tvCardCoordenadasProveedor: androidx.cardview.widget.CardView
    private lateinit var tvTitulo: TextView

    // Inputs para el cliente
    private lateinit var etClienteNombre: TextInputEditText
    private lateinit var etClienteTelefono: TextInputEditText
    private lateinit var spinnerClienteDistrito: Spinner
    private lateinit var etClienteDireccion: TextInputEditText
    private lateinit var btnObtenerCoordenadasCliente: Button
    private lateinit var tvCoordenadasCliente: TextView
    private lateinit var tvCardCoordenadasCliente: androidx.cardview.widget.CardView

    // Inputs para el pedido
    private lateinit var etPedidoDetalle: TextInputEditText
    private lateinit var etFechaEntrega: TextInputEditText
    private lateinit var etPedidoObservaciones: TextInputEditText
    private lateinit var switchPedidoSeCobra: SwitchMaterial
    private lateinit var spinnerMetodoPago: Spinner
    private lateinit var etPedidoCantidadCobrar: TextInputEditText
    private lateinit var switchPaqueteGrande: SwitchMaterial
    private lateinit var tvComisionTarifa: TextView

    // Botón para crear pedido
    private lateinit var btnActionPedido: Button
    private lateinit var progressBar: ProgressBar

    // Variables para almacenar las coordenadas
    private var coordenadasProveedor: Pair<Double, Double>? = null
    private var coordenadasCliente: Pair<Double, Double>? = null

    // Variables para calcular comisión
    private var comisionTarifa: Int = 10

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // OkHttp Client
    private val client = OkHttpClient()

    // Patrones para detección de coordenadas
    private val latPattern = Pattern.compile("(-1[1-2]\\.\\d+)")
    private val lonPattern = Pattern.compile("(-7[6-7]\\.\\d+)")
    private val atCoordsPattern = Pattern.compile("@(-1[1-2]\\.\\d+),(-7[6-7]\\.\\d+)")

    val distritos = arrayOf(
        "Selecciona una opción",
        "Ate (Lima)",
        "Barranco (Lima)",
        "Breña (Lima)",
        "Carabayllo (Lima)",
        "Chaclacayo (Lima)",
        "Chorrillos (Lima)",
        "Comas (Lima)",
        "Cercado de Lima (Lima)",
        "El Agustino (Lima)",
        "Huachipa (Ate, Lima)",
        "Independencia (Lima)",
        "Jesús María (Lima)",
        "La Molina (Lima)",
        "La Victoria (Lima)",
        "Lince (Lima)",
        "Los Olivos (Lima)",
        "Lurín (Lima)",
        "Magdalena del Mar (Lima)",
        "Mi Perú (Callao)",
        "Oquendo (Callao)",
        "Pueblo Libre (Lima)",
        "Puente Piedra (Lima)",
        "Rímac (Lima)",
        "San Borja (Lima)",
        "San Isidro (Lima)",
        "San Juan de Lurigancho (Lima)",
        "San Juan de Miraflores (Lima)",
        "San Luis (Lima)",
        "San Martín de Porres (Lima)",
        "San Miguel (Lima)",
        "Santa Anita (Lima)",
        "Santa Clara (Ate, Lima)",
        "Santa Rosa (Callao)",
        "Surco (Lima)",
        "Surquillo (Lima)",
        "Villa El Salvador (Lima)",
        "Villa María del Triunfo (Lima)",
        "Bellavista (Callao)",
        "Callao (Callao)",
        "Carmen de la Legua (Callao)",
        "La Perla (Callao)",
        "La Punta (Callao)",
        "Ventanilla (Callao)"
    )

    val distritos_limpios = arrayOf(
        "Selecciona una opción",
        "Ate",
        "Barranco",
        "Breña",
        "Carabayllo",
        "Chaclacayo",
        "Chorrillos",
        "Comas",
        "Cercado",
        "El Agustino",
        "Huachipa",
        "Independencia",
        "Jesús María",
        "La Molina",
        "La Victoria",
        "Lince",
        "Los Olivos",
        "Lurín",
        "Magdalena del Mar",
        "Mi Perú",
        "Oquendo",
        "Pueblo Libre",
        "Puente Piedra",
        "Rímac",
        "San Borja",
        "San Isidro",
        "San Juan de Lurigancho",
        "San Juan de Miraflores",
        "San Luis",
        "San Martín de Porres",
        "San Miguel",
        "Santa Anita",
        "Santa Clara",
        "Santa Rosa",
        "Surco",
        "Surquillo",
        "Villa El Salvador",
        "Villa María del Triunfo",
        "Bellavista",
        "Callao",
        "Carmen de la Legua",
        "La Perla",
        "La Punta",
        "Ventanilla"
    )


    // Métodos de pago
    val metodosPago = arrayOf(
        "Selecciona un método de pago",
        "Efectivo",
        "Transferencia",
        "Yape",
        "Plin"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_order)

        // Obtener datos de la intent
        nombreEmpresa = intent.getStringExtra("nombreEmpresa") ?: ""
        phone = intent.getStringExtra("phone") ?: ""

        // Obtener el modo de edición y el ID del pedido si es edición
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        orderId = intent.getStringExtra("orderId") ?: ""

        // Inicializar vistas
        initializeViews()

        // Configurar spinners
        setupSpinners()

        // Configurar UI según el modo
        setupUIForMode()

        // Cargar datos si es modo edición
        if (isEditMode && orderId.isNotEmpty()) {
            loadOrderData(orderId)
        }

        // Configurar listeners
        setupListeners()

        // Validación inicial
        validateInputs()
    }

    private fun setupUIForMode() {
        // Cambiar el título en el TextInputEditText
        tvTitulo.setText(if (isEditMode) "Editar Pedido" else "Crear Pedido")

        // Configurar botón según el modo
        btnActionPedido.text = if (isEditMode) "Actualizar Pedido" else "Crear Pedido"
    }


    private fun initializeViews() {

        tvTitulo = findViewById(R.id.tvTitulo)
        // Proveedor
        etProveedorNombre = findViewById(R.id.etProveedorNombre)
        etProveedorNombre.setText(nombreEmpresa)
        etProveedorNombre.isEnabled  = if (isEditMode) true else false

        etProveedorTelefono = findViewById(R.id.etProveedorTelefono)
        etProveedorTelefono.setText(phone)
        etProveedorTelefono.isEnabled  = if (isEditMode) true else false

        etProveedorDireccion = findViewById(R.id.etProveedorDireccion)
        spinnerProveedorDistrito = findViewById(R.id.spinnerProveedorDistrito)
        btnObtenerCoordenadasProveedor = findViewById(R.id.btnObtenerCoordenadasProveedor)
        tvCoordenadasProveedor = findViewById(R.id.tvCoordenadasProveedor)
        tvCardCoordenadasProveedor = findViewById(R.id.tvCardCoordenadasProveedor)

        // Cliente
        etClienteNombre = findViewById(R.id.etClienteNombre)
        etClienteTelefono = findViewById(R.id.etClienteTelefono)
        spinnerClienteDistrito = findViewById(R.id.spinnerClienteDistrito)
        etClienteDireccion = findViewById(R.id.etClienteDireccion)
        btnObtenerCoordenadasCliente = findViewById(R.id.btnObtenerCoordenadasCliente)
        tvCoordenadasCliente = findViewById(R.id.tvCoordenadasCliente)
        tvCardCoordenadasCliente = findViewById(R.id.tvCardCoordenadasCliente)

        // Pedido
        etPedidoDetalle = findViewById(R.id.etPedidoDetalle)
        etFechaEntrega = findViewById(R.id.etFechaEntrega)
        etPedidoObservaciones = findViewById(R.id.etPedidoObservaciones)
        switchPedidoSeCobra = findViewById(R.id.switchPedidoSeCobra)
        spinnerMetodoPago = findViewById(R.id.spinnerMetodoPago)
        etPedidoCantidadCobrar = findViewById(R.id.etPedidoCantidadCobrar)
        switchPaqueteGrande = findViewById(R.id.switchPaqueteGrande)
        tvComisionTarifa = findViewById(R.id.tvComisionTarifa)

        // Botón para crear pedido
        btnActionPedido = findViewById(R.id.btnActionPedido) // Mantener el mismo ID en el layout
        progressBar = findViewById(R.id.progressBar)

        // Deshabilitar botón hasta validación
        btnActionPedido.isEnabled = false
    }

    private fun setupSpinners() {
        // Lista de distritos de Lima

        // Configurar adapters para los spinners de distritos
        val adapterProveedorDistrito = ArrayAdapter(this, android.R.layout.simple_spinner_item, distritos)
        adapterProveedorDistrito.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProveedorDistrito.adapter = adapterProveedorDistrito

        val adapterClienteDistrito =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, distritos)
        adapterClienteDistrito.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerClienteDistrito.adapter = adapterClienteDistrito



        val adapterMetodoPago =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, metodosPago)
        adapterMetodoPago.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMetodoPago.adapter = adapterMetodoPago
    }

    private fun setupListeners() {
        // Cambio de textos para validación
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        }

        etProveedorNombre.addTextChangedListener(textWatcher)
        etProveedorTelefono.addTextChangedListener(textWatcher)
        etProveedorDireccion.addTextChangedListener(textWatcher)
        etClienteNombre.addTextChangedListener(textWatcher)
        etClienteTelefono.addTextChangedListener(textWatcher)
        etClienteDireccion.addTextChangedListener(textWatcher)
        etPedidoDetalle.addTextChangedListener(textWatcher)
        etFechaEntrega.addTextChangedListener(textWatcher)
        etPedidoCantidadCobrar.addTextChangedListener(textWatcher)

        // Fecha de entrega
        etFechaEntrega.setOnClickListener {
            showDateTimePicker()
        }

        // Obtener coordenadas
        btnObtenerCoordenadasProveedor.setOnClickListener {
            tvCardCoordenadasProveedor.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity,
                R.color.gris
            ))
            obtenerCoordenadasBolean(true)
        }

        btnObtenerCoordenadasCliente.setOnClickListener {
            tvCardCoordenadasCliente.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity,
                R.color.gris
            ))
            obtenerCoordenadasBolean(false)
        }

        // Switch para manejar el método de pago
        switchPedidoSeCobra.setOnCheckedChangeListener { _, isChecked ->
            spinnerMetodoPago.visibility = if (isChecked) View.VISIBLE else View.GONE
            etPedidoCantidadCobrar.visibility = if (isChecked) View.VISIBLE else View.GONE
            findViewById<TextInputLayout>(R.id.tilPedidoCantidadCobrar).visibility =
                if (isChecked) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.tvMetodoPago).visibility =
                if (isChecked) View.VISIBLE else View.GONE

            validateInputs()
        }

        // Listeners para los spinners
        spinnerProveedorDistrito.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    validateInputs()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spinnerClienteDistrito.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    calcularComision()
                    validateInputs()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        spinnerMetodoPago.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                validateInputs()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Switch para tamaño del paquete
        switchPaqueteGrande.setOnCheckedChangeListener { _, _ ->
            calcularComision()
        }

        // Botón para crear/actualizar pedido
        btnActionPedido.setOnClickListener {
            if (isEditMode) {
                actualizarPedido()
            } else {
                crearPedido()
            }
        }
    }

    private fun actualizarPedido() {
        Log.d("ActualizarPedido", "Iniciando actualización de pedido")

        if (!validateInputs()) {
            Log.e("ActualizarPedido", "Validación de inputs fallida")
            Toast.makeText(
                this,
                "Por favor completa todos los campos requeridos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnActionPedido.isEnabled = false

        val pathId = "recojos/$orderId"
        Log.d("ActualizarPedido", "Actualizando pedido con ID: $orderId")

        val grupoProveedor = determinarGrupo(spinnerProveedorDistrito.selectedItem.toString())
        val grupoCliente = determinarGrupo(spinnerClienteDistrito.selectedItem.toString())

        val motorizadoRecojo = grupoProveedor?.let { "motorizado${it.capitalize()}" } ?: "Asignar Recojo"
        val motorizadoEntrega = grupoCliente?.let { "motorizado${it.capitalize()}" } ?: "Asignar Entrega"

        Log.d("ActualizarPedido", "Motorizado recojo: $motorizadoRecojo, Motorizado entrega: $motorizadoEntrega")

        val proveedorTelefono = cleanAndValidatePhoneNumber(etProveedorTelefono.text.toString())
        val clienteTelefono = cleanAndValidatePhoneNumber(etClienteTelefono.text.toString())

        if (proveedorTelefono == null || clienteTelefono == null) {
            Log.e("ActualizarPedido", "Número de teléfono inválido")
            Toast.makeText(this, "Número de teléfono inválido", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            btnActionPedido.isEnabled = true
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaEntrega: Date
        try {
            fechaEntrega = dateFormat.parse(etFechaEntrega.text.toString()) ?: Date()
        } catch (e: Exception) {
            Log.e("ActualizarPedido", "Formato de fecha inválido", e)
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            btnActionPedido.isEnabled = true
            return
        }

        // Primero obtenemos el documento actual para preservar los campos que no estamos actualizando
        db.document(pathId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Crear un mapa con los campos que queremos actualizar
                    val updateData = hashMapOf<String, Any?>(
                        "clienteDistrito" to spinnerClienteDistrito.selectedItem.toString(),
                        "clienteNombre" to capitalizeName(etClienteNombre.text.toString()),
                        "clienteTelefono" to clienteTelefono,
                        "comisionTarifa" to comisionTarifa,
                        "fechaEntregaPedido" to fechaEntrega,
                        "motorizadoEntrega" to motorizadoEntrega,
                        "motorizadoRecojo" to motorizadoRecojo,
                        "pedidoCantidadCobrar" to (if (switchPedidoSeCobra.isChecked) etPedidoCantidadCobrar.text.toString().toFloatOrNull()?.toString() ?: "0.00" else "0.00"),
                        "pedidoCoordenadas" to coordenadasCliente?.let {mapOf("lat" to it.first, "lng" to it.second)},
                        "pedidoDetalle" to capitalizeName(etPedidoDetalle.text.toString()),
                        "pedidoDireccionLink" to etClienteDireccion.text.toString(),
                        "pedidoMetodoPago" to (if (switchPedidoSeCobra.isChecked) spinnerMetodoPago.selectedItem.toString() else ""),
                        "pedidoObservaciones" to etPedidoObservaciones.text.toString(),
                        "pedidoSeCobra" to (if (switchPedidoSeCobra.isChecked) "Sí" else "No"),
                        "proveedorDireccionLink" to etProveedorDireccion.text.toString(),
                        "proveedorDistrito" to spinnerProveedorDistrito.selectedItem.toString(),
                        "proveedorNombre" to etProveedorNombre.text.toString().uppercase(),
                        "proveedorTelefono" to proveedorTelefono,
                        "recojoCoordenadas" to coordenadasProveedor?.let {mapOf("lat" to it.first, "lng" to it.second)},
                        "supera30x30" to (if (switchPaqueteGrande.isChecked) 1 else 0)
                    )

                    // Actualizar el documento
                    db.document(pathId)
                        .update(updateData)
                        .addOnSuccessListener {
                            Log.d("ActualizarPedido", "Pedido actualizado con éxito en Firestore")
                            Toast.makeText(this, "Pedido actualizado con éxito", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ActualizarPedido", "Error al actualizar pedido", e)
                            Toast.makeText(this, "Error al actualizar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                            btnActionPedido.isEnabled = true
                        }
                } else {
                    Toast.makeText(this, "No se encontró el pedido para actualizar", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    btnActionPedido.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActualizarPedido", "Error al obtener el pedido para actualizar", e)
                Toast.makeText(this, "Error al actualizar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnActionPedido.isEnabled = true
            }
    }

    private fun loadOrderData(orderId: String) {
        progressBar.visibility = View.VISIBLE

        val docRef = db.document("recojos/$orderId")
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Cargar datos en los campos
                    val data = document.data

                    // Proveedor
                    etProveedorNombre.setText(data?.get("proveedorNombre") as? String ?: "")
                    etProveedorTelefono.setText(data?.get("proveedorTelefono") as? String ?: "")
                    etProveedorDireccion.setText(data?.get("proveedorDireccionLink") as? String ?: "")

                    // Seleccionar distrito del proveedor
                    val proveedorDistrito = data?.get("proveedorDistrito") as? String ?: ""
                    selectSpinnerItemByValue(spinnerProveedorDistrito, proveedorDistrito)

                    // Cliente
                    etClienteNombre.setText(data?.get("clienteNombre") as? String ?: "")
                    etClienteTelefono.setText(data?.get("clienteTelefono") as? String ?: "")
                    etClienteDireccion.setText(data?.get("pedidoDireccionLink") as? String ?: "")

                    // Seleccionar distrito del cliente
                    val clienteDistrito = data?.get("clienteDistrito") as? String ?: ""
                    selectSpinnerItemByValue(spinnerClienteDistrito, clienteDistrito)

                    // Detalles del pedido
                    etPedidoDetalle.setText(data?.get("pedidoDetalle") as? String ?: "")
                    etPedidoObservaciones.setText(data?.get("pedidoObservaciones") as? String ?: "")

                    // Fecha de entrega
                    val fechaEntregaTimestamp = data?.get("fechaEntregaPedido") as? Timestamp
                    val fechaEntrega = fechaEntregaTimestamp?.toDate() // Convierte Timestamp a Date
                    if (fechaEntrega != null) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        etFechaEntrega.setText(dateFormat.format(fechaEntrega))
                    }


                    // Cobro y método de pago
                    val seCobra = (data?.get("pedidoSeCobra") as? String ?: "No") == "Sí"
                    switchPedidoSeCobra.isChecked = seCobra

                    if (seCobra) {
                        val metodoPago = data?.get("pedidoMetodoPago") as? String ?: ""
                        selectSpinnerItemByValue(spinnerMetodoPago, metodoPago)

                        val cantidadCobrar = data?.get("pedidoCantidadCobrar") as? String ?: "0.00"
                        etPedidoCantidadCobrar.setText(cantidadCobrar)
                    }

                    // Tamaño del paquete
                    val supera30x30 = (data?.get("supera30x30") as? Long ?: 0) == 1L
                    switchPaqueteGrande.isChecked = supera30x30

                    // Coordenadas
                    val recojoCoordenadas = data?.get("recojoCoordenadas") as? Map<String, Double>
                    if (recojoCoordenadas != null) {
                        val lat = recojoCoordenadas["lat"] ?: 0.0
                        val lng = recojoCoordenadas["lng"] ?: 0.0
                        coordenadasProveedor = Pair(lat, lng)
                        tvCoordenadasProveedor.text = "¡Coordenadas cargadas!"
                        tvCardCoordenadasProveedor.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity, R.color.verde_claro))
                    }

                    val pedidoCoordenadas = data?.get("pedidoCoordenadas") as? Map<String, Double>
                    if (pedidoCoordenadas != null) {
                        val lat = pedidoCoordenadas["lat"] ?: 0.0
                        val lng = pedidoCoordenadas["lng"] ?: 0.0
                        coordenadasCliente = Pair(lat, lng)
                        tvCoordenadasCliente.text = "¡Coordenadas cargadas!"
                        tvCardCoordenadasCliente.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity, R.color.verde_claro))
                    }

                    // Calcular comisión después de cargar todos los datos
                    calcularComision()

                } else {
                    Toast.makeText(this, "No se encontró el pedido", Toast.LENGTH_SHORT).show()
                    finish()
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("OrderForm", "Error al cargar el pedido", e)
                Toast.makeText(this, "Error al cargar el pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                finish()
            }
    }

    // Función auxiliar para seleccionar items en spinners
    private fun selectSpinnerItemByValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }

    private fun showDateTimePicker() {
        val currentDateTime = Calendar.getInstance()

        // Date picker
        DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                currentDateTime.set(Calendar.YEAR, year)
                currentDateTime.set(Calendar.MONTH, monthOfYear)
                currentDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Time picker
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        currentDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        currentDateTime.set(Calendar.MINUTE, minute)
                        currentDateTime.set(Calendar.SECOND, 0)

                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        etFechaEntrega.setText(dateFormat.format(currentDateTime.time))
                        validateInputs()
                    },
                    currentDateTime.get(Calendar.HOUR_OF_DAY),
                    currentDateTime.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentDateTime.get(Calendar.YEAR),
            currentDateTime.get(Calendar.MONTH),
            currentDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun obtenerCoordenadasBolean(isProveedor: Boolean) {
        val btnCoordenadas = if (isProveedor) btnObtenerCoordenadasProveedor else btnObtenerCoordenadasCliente
        val tvCoordenadas = if (isProveedor) tvCoordenadasProveedor else tvCoordenadasCliente
        val etDireccion = if (isProveedor) etProveedorDireccion else etClienteDireccion

        btnCoordenadas.isEnabled = false
        tvCoordenadas.text = "Obteniendo coordenadas..."
        tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(R.drawable.loading_animation, 0, 0, 0)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val textoPortapapeles = if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount ?: 0 > 0) {
            clipboard.primaryClip?.getItemAt(0)?.text.toString()
        } else {
            ""
        }

        val texto = if (etDireccion.text.isNullOrBlank()) {
            etDireccion.setText(textoPortapapeles)
            textoPortapapeles
        } else {
            etDireccion.text.toString()
        }


        if (texto.isBlank()) {
            Toast.makeText(this@OrderFormActivity,"Ingresa una dirección o copia una URL",Toast.LENGTH_SHORT).show()
            tvCoordenadas.text = "Ingresa una dirección o copia una URL"
            tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            btnCoordenadas.isEnabled = true
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val coordenadas = procesarEntrada(texto)

            withContext(Dispatchers.Main) {
                tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                if (coordenadas != null) {
                    if (isProveedor) {
                        coordenadasProveedor = Pair(coordenadas.latitud, coordenadas.longitud)
                        tvCardCoordenadasProveedor.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity,
                            R.color.verde_claro
                        ))
                    } else {
                        coordenadasCliente = Pair(coordenadas.latitud, coordenadas.longitud)
                        tvCardCoordenadasCliente.setCardBackgroundColor(ContextCompat.getColor(this@OrderFormActivity,
                            R.color.verde_claro
                        ))
                    }
                    //tvCoordenadas.text = "Lat: ${coordenadas.first}, Lng: ${coordenadas.second}"
                    tvCoordenadas.text = "¡Coordenadas obtenidas con éxito!"
                    val distrito1 = coordenadas.direccion1

                    val distrito2 = coordenadas.direccion2

                    // Buscar la mejor coincidencia en la lista de distritos
                    val posicion1 = distritos_limpios.indexOfFirst { it.contains(distrito1, ignoreCase = true) }
                    val posicion2 = distritos_limpios.indexOfFirst { it.contains(distrito2, ignoreCase = true) }

                    // Elegir el distrito con coincidencia válida
                    val mejorPosicion = when {
                        posicion1 >= 0 -> posicion1
                        posicion2 >= 0 -> posicion2
                        else -> -1
                    }

                    if (mejorPosicion >= 0) {
                        if (isProveedor) {
                            spinnerProveedorDistrito.setSelection(mejorPosicion)
                        } else {
                            spinnerClienteDistrito.setSelection(mejorPosicion)
                        }
                    } else {
                        val mensaje = if (isProveedor) {
                            "Seleccione distrito de proveedor"
                        } else {
                            "Seleccione distrito de cliente"
                        }
                        Toast.makeText(this@OrderFormActivity, mensaje, Toast.LENGTH_LONG).show()
                    }

                } else {
                    tvCoordenadas.text = "No se pudieron obtener coordenadas"
                    Toast.makeText(
                        this@OrderFormActivity,
                        "No se pudieron obtener coordenadas",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                btnCoordenadas.isEnabled = true
                validateInputs()
            }
        }
    }

    private fun calcularComision() {
        val distritoSeleccionado = spinnerClienteDistrito.selectedItem.toString()
        val supera30x30 = switchPaqueteGrande.isChecked

        // Calcular comisión base según el distrito
        var comisionBase = 10 // Valor base para distritos distintos de los especificados

        when (distritoSeleccionado) {
            "Carabayllo (Lima)", "Ventanilla (Callao)", "Puente Piedra (Lima)" -> comisionBase = 15
            "Comas (Lima)", "Villa El Salvador (Lima)", "Villa María del Triunfo (Lima)",
            "Oquendo (Callao)", "Santa Clara (Ate, Lima)" -> comisionBase = 13
        }

        // Sumar 5 si el paquete supera 30x30
        comisionTarifa = if (supera30x30) comisionBase + 5 else comisionBase

        // Actualizar el texto de la comisión
        tvComisionTarifa.text = "S/ $comisionTarifa"
    }

    private fun validateInputs(): Boolean {
        // Validar campos requeridos
        val proveedorNombre = etProveedorNombre.text.toString().trim()
        val proveedorTelefono = etProveedorTelefono.text.toString().trim()
        val proveedorDistrito = spinnerProveedorDistrito.selectedItemPosition != 0

        val clienteNombre = etClienteNombre.text.toString().trim()
        val clienteTelefono = etClienteTelefono.text.toString().trim()
        val clienteDistrito = spinnerClienteDistrito.selectedItemPosition != 0
        val coordenadasProveedor = this.coordenadasProveedor
        val coordenadasCliente = this.coordenadasCliente

        val pedidoDetalle = etPedidoDetalle.text.toString().trim()
        val fechaEntrega = etFechaEntrega.text.toString().trim()

        val seCobra = switchPedidoSeCobra.isChecked
        val metodoPago = if (seCobra) spinnerMetodoPago.selectedItemPosition != 0 else true
        val cantidadCobrar =
            if (seCobra) etPedidoCantidadCobrar.text.toString().trim().isNotEmpty() else true

        // En modo de edición, verificar que el ID no esté vacío
        val idValido = if (isEditMode) orderId.isNotEmpty() else true

        val todosLosRequeridos = proveedorNombre.isNotEmpty() &&
                proveedorTelefono.isNotEmpty() &&
                proveedorDistrito &&
                clienteNombre.isNotEmpty() &&
                clienteTelefono.isNotEmpty() &&
                clienteDistrito &&
                pedidoDetalle.isNotEmpty() &&
                fechaEntrega.isNotEmpty() &&
                metodoPago &&
                cantidadCobrar &&
                coordenadasProveedor != null &&
                coordenadasCliente != null &&
                idValido

        btnActionPedido.isEnabled = todosLosRequeridos
        return todosLosRequeridos
    }

    private fun crearPedido() {
        Log.d("CrearPedido", "Iniciando creación de pedido")

        if (!validateInputs()) {
            Log.e("CrearPedido", "Validación de inputs fallida")
            Toast.makeText(
                this,
                "Por favor completa todos los campos requeridos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnActionPedido.isEnabled = false

        val formattedDocId = generarIdPedido()
        val pathId = "recojos/$formattedDocId"
        Log.d("CrearPedido", "ID de pedido generado: $formattedDocId")

        val grupoProveedor = determinarGrupo(spinnerProveedorDistrito.selectedItem.toString())
        val grupoCliente = determinarGrupo(spinnerClienteDistrito.selectedItem.toString())

        val motorizadoRecojo = grupoProveedor?.let { "motorizado${it.capitalize()}" } ?: "Asignar Recojo"
        val motorizadoEntrega = grupoCliente?.let { "motorizado${it.capitalize()}" } ?: "Asignar Entrega"

        Log.d("CrearPedido", "Motorizado recojo: $motorizadoRecojo, Motorizado entrega: $motorizadoEntrega")

        val proveedorTelefono = cleanAndValidatePhoneNumber(etProveedorTelefono.text.toString())
        val clienteTelefono = cleanAndValidatePhoneNumber(etClienteTelefono.text.toString())

        if (proveedorTelefono == null || clienteTelefono == null) {
            Log.e("CrearPedido", "Número de teléfono inválido")
            Toast.makeText(this, "Número de teléfono inválido", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            btnActionPedido.isEnabled = true
            return
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaEntrega: Date
        try {
            fechaEntrega = dateFormat.parse(etFechaEntrega.text.toString()) ?: Date()
        } catch (e: Exception) {
            Log.e("CrearPedido", "Formato de fecha inválido", e)
            Toast.makeText(this, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            btnActionPedido.isEnabled = true
            return
        }

        Log.d("CrearPedido", "Fecha de entrega parseada: $fechaEntrega")

        val pedidoData = hashMapOf(
            "clienteDistrito" to spinnerClienteDistrito.selectedItem.toString(),
            "clienteNombre" to capitalizeName(etClienteNombre.text.toString()),
            "clienteTelefono" to clienteTelefono,
            "comisionTarifa" to comisionTarifa,
            "fechaAnulacionPedido" to null,
            "fechaCreacionPedido" to Date(),
            "fechaEntregaPedido" to fechaEntrega,
            "fechaEntregaPedidoMotorizado" to null,
            "fechaRecojoPedidoMotorizado" to null,
            "id" to formattedDocId,
            "motorizadoEntrega" to motorizadoEntrega,
            "motorizadoRecojo" to motorizadoRecojo,
            "pedidoCantidadCobrar" to (if (switchPedidoSeCobra.isChecked) etPedidoCantidadCobrar.text.toString().toFloatOrNull()?.toString() ?: "0.00" else "0.00"),
            "pedidoCoordenadas" to coordenadasCliente?.let {mapOf("lat" to it.first, "lng" to it.second)},
            "pedidoDetalle" to capitalizeName(etPedidoDetalle.text.toString()),
            "pedidoDireccionLink" to etClienteDireccion.text.toString(),
            "pedidoFotoEntrega" to null,
            "pedidoFotoRecojo" to null,
            "pedidoMetodoPago" to (if (switchPedidoSeCobra.isChecked) spinnerMetodoPago.selectedItem.toString() else ""),
            "pedidoObservaciones" to etPedidoObservaciones.text.toString(),
            "pedidoSeCobra" to (if (switchPedidoSeCobra.isChecked) "Sí" else "No"),
            "proveedorDireccionLink" to etProveedorDireccion.text.toString(),
            "proveedorDistrito" to spinnerProveedorDistrito.selectedItem.toString(),
            "proveedorNombre" to etProveedorNombre.text.toString().uppercase(),
            "proveedorTelefono" to proveedorTelefono,
            "recojoCoordenadas" to coordenadasProveedor?.let {mapOf("lat" to it.first, "lng" to it.second)},
            "supera30x30" to (if (switchPaqueteGrande.isChecked) 1 else 0),
            "thumbnailFotoEntrega" to null,
            "thumbnailFotoRecojo" to null
        )

        Log.d("CrearPedido", "Datos del pedido listos para enviar a Firestore")
        Log.d("CrearPedido", "Datos del pedido: $pedidoData")

        db.document(pathId)
            .set(pedidoData)
            .addOnSuccessListener {
                Log.d("CrearPedido", "Pedido creado con éxito en Firestore")
                Toast.makeText(this, "Pedido creado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("CrearPedido", "Error al crear pedido", e)
                Toast.makeText(this, "Error al crear pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                btnActionPedido.isEnabled = true
            }
    }


    // Funciones auxiliares
    private fun generarIdPedido(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy-HHmmss", Locale.getDefault())
        val randomId = generateRandomDigits()
        return dateFormat.format(Date()) + "-" + randomId
    }

    private fun generateRandomDigits(): String {
        return (Random.nextInt(900000) + 100000).toString()
    }

    private fun cleanAndValidatePhoneNumber(phoneNumber: String): String? {
        val cleanedNumber = phoneNumber.replace(Regex("[\\s\\-+]"), "").replace(Regex("^51"), "")

        return if (Regex("^\\d{9}$").matches(cleanedNumber)) {
            cleanedNumber
        } else {
            null
        }
    }

    private fun capitalizeName(name: String): String {
        val cleanName = name.replace(Regex("[.,]"), "")
        return cleanName.split(" ")
            .map { word ->
                if (word.isNotEmpty()) word[0].uppercase() + word.substring(1).lowercase() else ""
            }
            .joinToString(" ")
    }

    private fun determinarGrupo(distrito: String): String? {
        val grupos = mapOf(
            "norte" to listOf(
                "Carabayllo (Lima)", "Comas (Lima)", "Independencia (Lima)", "Los Olivos (Lima)",
                "Puente Piedra (Lima)", "San Martín de Porres (Lima)", "Santa Rosa (Callao)",
                "Ventanilla (Callao)", "Mi Perú (Callao)", "Oquendo (Callao)"
            ),
            "sur" to listOf(
                "Chorrillos (Lima)", "Lurín (Lima)", "San Juan de Miraflores (Lima)",
                "Surco (Lima)", "Surquillo (Lima)",
                "Villa El Salvador (Lima)", "Villa María del Triunfo (Lima)"
            ),
            "este" to listOf(
                "Ate (Lima)", "Chaclacayo (Lima)", "El Agustino (Lima)", "Huachipa (Ate, Lima)",
                "San Juan de Lurigancho (Lima)", "Santa Anita (Lima)", "Santa Clara (Ate, Lima)"
            ),
            "oeste" to listOf(
                "Bellavista (Callao)", "Callao (Callao)", "Carmen de la Legua (Callao)",
                "La Perla (Callao)", "La Punta (Callao)"
            ),
            "centro" to listOf(
                "Barranco (Lima)",
                "Breña (Lima)",
                "Cercado de Lima (Lima)",
                "Jesús María (Lima)",
                "La Molina (Lima)",
                "La Victoria (Lima)",
                "Lince (Lima)",
                "Magdalena del Mar (Lima)",
                "Pueblo Libre (Lima)",
                "Rímac (Lima)",
                "San Borja (Lima)",
                "San Isidro (Lima)",
                "San Luis (Lima)",
                "San Miguel (Lima)"
            )
        )

        for ((grupo, distritos) in grupos) {
            if (distritos.contains(distrito)) {
                return grupo
            }
        }
        return null
    }

    // Funciones para procesar coordenadas
    suspend fun procesarEntrada(texto: String): Coordenada? {
        return if (esURL(texto)) {
            val resultado: Coordenada? = obtenerCoordenadas(texto)

            if (resultado != null) {
                val (lat, lon, distrito) = resultado
                if (distrito.isEmpty()) {
                    val nuevoTexto = "$lat,$lon"
                    return obtenerCoordenadasGeocoding(nuevoTexto)
                } else {
                    println("Distrito obtenido: $distrito")
                    return resultado
                }
            } else {
                println("No se pudo obtener la ubicación.")
                return null
            }
        } else {
            obtenerCoordenadasGeocoding(texto)
        }
    }

    fun esURL(texto: String): Boolean {
        if (texto.startsWith("http://") || texto.startsWith("https://") || texto.startsWith("www.")) {
            return true
        }

        val dominiosComunes = listOf(".com", ".org", ".net", ".gob", ".edu", ".pe", ".maps")
        return dominiosComunes.any { texto.contains(it) }
    }

    suspend fun obtenerCoordenadas(url: String): Coordenada? {
        extraerCoordenadasUrl(url)?.let { return it }

        return try {
            val expandedUrl = expandirUrl(url) ?: return null
            Log.d("CoordenadasExtractor", "URL expandida: $expandedUrl")
            extraerCoordenadasUrl(expandedUrl)?.let { return it }

            if (expandedUrl.contains("/place/")) {
                Log.d("CoordenadasExtractor", "URL contiene '/place/': $expandedUrl")
                extraerNombreLugar(expandedUrl)?.let { return obtenerCoordenadasGeocoding(it) }
            }

            if (expandedUrl.contains("maps?q=")) {
                Log.d("CoordenadasExtractor", "URL contiene 'maps?q=': $expandedUrl")
                extraerNombreLugar2(expandedUrl)?.let { return obtenerCoordenadasGeocoding(it) }
            }

            if (expandedUrl.contains("%3Fq%3D")) {
                Log.d("CoordenadasExtractor", "URL contiene '%3Fq%3D': $expandedUrl")
                val match = Regex("%3Fq%3D(.*?)(%26|$)").find(expandedUrl)
                match?.groups?.get(1)?.value?.let {
                    return obtenerCoordenadasGeocoding(URLDecoder.decode(it, "UTF-8"))
                }
            }
            null
        } catch (e: Exception) {
            Log.e("CoordenadasExtractor", "Error al obtener coordenadas", e)
            null
        }
    }

    private fun extraerCoordenadasUrl(url: String): Coordenada? {
        val decodedUrl = URLDecoder.decode(url, "UTF-8")

        atCoordsPattern.matcher(decodedUrl).takeIf { it.find() }?.let {
            return Coordenada(it.group(1).toDouble(), it.group(2).toDouble(),"","")
        }

        val latMatches = latPattern.matcher(decodedUrl)
        val lonMatches = lonPattern.matcher(decodedUrl)
        if (latMatches.find() && lonMatches.find()) {
            Log.d(
                "CoordenadasExtractor",
                "Coordenadas encontradas: ${latMatches.group(1)}, ${lonMatches.group(1)}"
            )
            return Coordenada(latMatches.group(1).toDouble(), lonMatches.group(1).toDouble(),"","")
        }
        Log.d("CoordenadasExtractor", "No se encontraron coordenadas en la URL: $url")
        return null
    }

    suspend fun obtenerCoordenadasGeocoding(lugar: String): Coordenada? {
        return withContext(Dispatchers.IO) { // Ejecuta en un hilo de trabajo
            val apiKey = BuildConfig.GEO_API_KEY
            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${
                URLEncoder.encode(lugar, "UTF-8")
            }&key=$apiKey"

            try {
                val request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "")

                if (json.getString("status") == "OK") {
                    val location = json.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                    val lat = location.getDouble("lat")
                    val lon = location.getDouble("lng")

                    val distrito1 = json.getJSONArray("results").getJSONObject(0)
                        .getJSONArray("address_components").getJSONObject(2)
                        .getString("long_name")

                    val distrito2 = json.getJSONArray("results").getJSONObject(0)
                        .getJSONArray("address_components").getJSONObject(3)
                        .getString("long_name")

                    if (lat in -12.999999..-11.000000 && lon in -77.999999..-76.000000) {
                        return@withContext Coordenada(lat, lon, distrito1, distrito2)
                    }
                }
                null // Retorna null si la condición no se cumple
            } catch (e: Exception) {
                Log.e("Geocoding", "Error en la API de Geocoding", e)
                null
            }
        }
    }

    suspend fun expandirUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                response.request.url.toString()
            } catch (e: Exception) {
                Log.e("CoordenadasExtractor", "Error al expandir URL", e)
                null
            }
        }
    }

    private fun extraerNombreLugar(url: String): String? {
        val match = Regex("/place/(.*?)(/data=|/@)").find(url)
        return match?.groups?.get(1)?.value?.replace("+", " ")
    }

    private fun extraerNombreLugar2(url: String): String? {
        val match = Regex("/maps\\?q=(.*?)&ftid").find(url)
        return match?.groups?.get(1)?.value?.replace("+", " ")
    }
}