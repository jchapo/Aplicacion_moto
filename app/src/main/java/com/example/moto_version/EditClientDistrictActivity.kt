package com.example.moto_version
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import java.net.URLDecoder
import android.util.Log
import android.widget.LinearLayout
import java.net.URL
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


class EditClientDistrictActivity : AppCompatActivity() {

    private lateinit var spinnerDistritos: Spinner
    private lateinit var btnPegarCoordenadas: Button
    private lateinit var tvCoordenadas: TextView
    private lateinit var btnGuardar: Button
    private lateinit var cbSupera30x30: CheckBox
    private lateinit var tvComisionTarifa: TextView
    private lateinit var tvComisionTarifaContainer: LinearLayout

    private var clienteId: String = ""
    private var distritoActual: String = ""
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var comisionTarifa: Int = 10
    private var coordenadas: Pair<Double, Double>? = null

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // Expresiones regulares para encontrar coordenadas (adaptadas del código Python)
    private val latPattern = Pattern.compile("(-1[1-2]\\.\\d+)")
    private val lonPattern = Pattern.compile("(-7[6-7]\\.\\d+)")
    private val atCoordsPattern = Pattern.compile("@(-1[1-2]\\.\\d+),(-7[6-7]\\.\\d+)")
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_client_district)

        // Obtener datos pasados desde la actividad anterior
        clienteId = intent.getStringExtra("clienteId") ?: ""
        distritoActual = intent.getStringExtra("clienteDistrito") ?: ""

        // Inicializar vistas
        spinnerDistritos = findViewById(R.id.spinnerDistritos)
        btnPegarCoordenadas = findViewById(R.id.btnPegarCoordenadas)
        tvCoordenadas = findViewById(R.id.tvCoordenadas)
        btnGuardar = findViewById(R.id.btnGuardar)
        cbSupera30x30 = findViewById(R.id.cbSupera30x30)
        tvComisionTarifa = findViewById(R.id.tvComisionTarifa)
        tvComisionTarifaContainer = findViewById(R.id.tvComisionTarifaContainer)

        tvComisionTarifaContainer.visibility = View.GONE

        btnGuardar.visibility = View.GONE
        btnGuardar.isEnabled = false

        // Configurar spinner de distritos
        setupDistritosSpinner()

        // Calcular comisión inicial
        calcularComision()

        // Configurar listeners
        btnPegarCoordenadas.setOnClickListener {
            btnPegarCoordenadas.isEnabled = false
            obtenerCoordenadasDelPortapapeles()
            btnPegarCoordenadas.isEnabled = true

            if (checkValues()){
                btnGuardar.visibility = View.VISIBLE
                btnGuardar.isEnabled = true
            }
        }

        btnGuardar.setOnClickListener {
            btnGuardar.isEnabled = false
            guardarCambios()
        }

        // Listener para el checkbox
        cbSupera30x30.setOnCheckedChangeListener { _, _ ->
            calcularComision()
            val checkValues = checkValues()
            Log.d("chek", checkValues.toString())
            if (checkValues){
                btnGuardar.visibility = View.VISIBLE
                btnGuardar.isEnabled = true
            }
        }

        // Listener para cambios en el spinner
        spinnerDistritos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calcularComision()
                if (checkValues()){
                    btnGuardar.visibility = View.VISIBLE
                    btnGuardar.isEnabled = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    private fun setupDistritosSpinner() {
        // Lista de distritos de Lima
        val distritos = arrayOf("",
            "Ancón", "Ate", "Barranco", "Breña", "Carabayllo (Lima)", "Chaclacayo", "Chorrillos", "Cieneguilla",
            "Comas (Lima)", "El Agustino", "Independencia", "Jesús María", "La Molina", "La Victoria", "Lima",
            "Lince", "Los Olivos", "Lurigancho", "Lurín", "Magdalena del Mar", "Miraflores", "Pachacámac",
            "Pucusana", "Pueblo Libre", "Puente Piedra (Lima)", "Punta Hermosa", "Punta Negra", "Rímac",
            "San Bartolo", "San Borja", "San Isidro", "San Juan de Lurigancho", "San Juan de Miraflores",
            "San Luis", "San Martín de Porres", "San Miguel", "Santa Anita", "Santa Clara (Ate, Lima)",
            "Santa María del Mar", "Santa Rosa", "Santiago de Surco", "Surquillo",
            "Ventanilla (Callao)", "Oquendo (Callao)",
            "Villa El Salvador (Lima)", "Villa María del Triunfo (Lima)"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distritos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistritos.adapter = adapter

    }

    private fun calcularComision() {
        val distritoSeleccionado = spinnerDistritos.selectedItem.toString()
        val supera30x30 = cbSupera30x30.isChecked

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
        tvComisionTarifa.text = "$comisionTarifa"
    }

    private fun obtenerCoordenadasDelPortapapeles() {
        // Mostrar loader
        tvCoordenadas.text = "Obteniendo coordenadas..."
        tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(R.drawable.loading_animation, 0, 0, 0)

        // Obtener texto del portapapeles
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val texto = clipboard.primaryClip?.getItemAt(0)?.text.toString()

        // Procesar texto en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            coordenadas = obtenerCoordenadas(texto)

            withContext(Dispatchers.Main) {
                // Quitar loader
                tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                if (coordenadas != null) {
                    latitud = coordenadas?.first
                    longitud = coordenadas?.second

                    tvCoordenadas.text = "Lat: $latitud, Lng: $longitud"
                    Toast.makeText(this@EditClientDistrictActivity,
                        "¡Coordenadas obtenidas con éxito!",
                        Toast.LENGTH_SHORT).show()
                    if (checkValues()){
                        btnGuardar.visibility = View.VISIBLE
                        btnGuardar.isEnabled = true
                    }
                } else {
                    tvCoordenadas.text = "No se pudieron obtener coordenadas"
                    Toast.makeText(this@EditClientDistrictActivity,
                        "No se pudieron extraer coordenadas del texto",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    fun obtenerCoordenadas(url: String): Pair<Double, Double>? {
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

    private fun expandirUrl(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            response.request.url.toString()
        } catch (e: Exception) {
            Log.e("CoordenadasExtractor", "Error al expandir URL", e)
            null
        }
    }

    private fun extraerCoordenadasUrl(url: String): Pair<Double, Double>? {
        val decodedUrl = URLDecoder.decode(url, "UTF-8")

        atCoordsPattern.matcher(decodedUrl).takeIf { it.find() }?.let {
            return Pair(it.group(1).toDouble(), it.group(2).toDouble())
        }

        val latMatches = latPattern.matcher(decodedUrl)
        val lonMatches = lonPattern.matcher(decodedUrl)
        if (latMatches.find() && lonMatches.find()) {
            Log.d("CoordenadasExtractor", "Coordenadas encontradas: ${latMatches.group(1)}, ${lonMatches.group(1)}")
            return Pair(latMatches.group(1).toDouble(), lonMatches.group(1).toDouble())
        }
        Log.d("CoordenadasExtractor", "No se encontraron coordenadas en la URL: $url")
        return null
    }

    private fun extraerNombreLugar(url: String): String? {
        val match = Regex("/place/(.*?)(/data=|/@)").find(url)
        return match?.groups?.get(1)?.value?.replace("+", " ")
    }

    private fun extraerNombreLugar2(url: String): String? {
        val match = Regex("/maps\\?q=(.*?)&ftid").find(url)
        return match?.groups?.get(1)?.value?.replace("+", " ")
    }

    private fun obtenerCoordenadasGeocoding(lugar: String): Pair<Double, Double>? {
        val apiKey = BuildConfig.GEO_API_KEY
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${URLEncoder.encode(lugar, "UTF-8")}&key=$apiKey"

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")

            if (json.getString("status") == "OK") {
                val location = json.getJSONArray("results").getJSONObject(0)
                    .getJSONObject("geometry").getJSONObject("location")
                val lat = location.getDouble("lat")
                val lon = location.getDouble("lng")

                if (lat in -12.999999..-11.000000 && lon in -77.999999..-76.000000) {
                    return Pair(lat, lon)
                }
            }
            null // Retorna null explícitamente si la condición no se cumple
        } catch (e: Exception) {
            Log.e("CoordenadasExtractor", "Error en Geocoding API", e)
            null // Retorna null en caso de excepción
        }
    }
    private fun guardarCambios() {
        val clienteDistrito = spinnerDistritos.selectedItem.toString()
        val comisionTarifa = tvComisionTarifa.text.toString().toDoubleOrNull()
        val pedidoCoordenadas = coordenadas?.let { mapOf("lat" to it.first, "lng" to it.second) }

        // Verificar si los valores requeridos existen
        val valoresValidos = checkValues()

        if (valoresValidos) {
            // Guardar en Firebase sin sobrescribir los otros datos
            val recojoData = hashMapOf(
                "clienteDistrito" to clienteDistrito,
                "comisionTarifa" to comisionTarifa,
                "pedidoCoordenadas" to pedidoCoordenadas
            ).filterValues { it != null } // Elimina valores nulos

            db.collection("recojos").document(clienteId)
                .update(recojoData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Datos guardados con éxito", Toast.LENGTH_SHORT).show()
                    finish() // Cierra la actividad actual
                }
                .addOnFailureListener {
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun checkValues(): Boolean {
        val clienteDistrito = spinnerDistritos.selectedItem.toString()
        val textoCoordenadas = tvCoordenadas.text.toString()

        // Verificar si los valores requeridos existen
        val valoresValidos = clienteDistrito.isNotEmpty() && clienteDistrito != "" &&
                textoCoordenadas != "No se pudieron obtener coordenadas" &&
                textoCoordenadas != "No hay coordenadas"
        return valoresValidos
    }

}