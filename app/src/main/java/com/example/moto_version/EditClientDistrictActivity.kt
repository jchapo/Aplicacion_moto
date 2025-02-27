package com.example.moto_version
import android.content.ClipboardManager
import android.content.Context
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


class EditClientDistrictActivity : AppCompatActivity() {

    private lateinit var spinnerDistritos: Spinner
    private lateinit var btnPegarCoordenadas: Button
    private lateinit var tvCoordenadas: TextView
    private lateinit var btnGuardar: Button
    private lateinit var cbSupera30x30: CheckBox
    private lateinit var tvComisionTarifa: TextView

    private var clienteId: String = ""
    private var distritoActual: String = ""
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var comisionTarifa: Int = 10

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // Expresiones regulares para encontrar coordenadas (adaptadas del código Python)
    private val latPattern = Pattern.compile("(-1[1-2]\\.\\d+)")
    private val lonPattern = Pattern.compile("(-7[6-7]\\.\\d+)")
    private val atCoordsPattern = Pattern.compile("@(-1[1-2]\\.\\d+),(-7[6-7]\\.\\d+)")

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

        // Configurar spinner de distritos
        setupDistritosSpinner()

        // Calcular comisión inicial
        calcularComision()

        // Configurar listeners
        btnPegarCoordenadas.setOnClickListener {
            obtenerCoordenadasDelPortapapeles()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        // Listener para el checkbox
        cbSupera30x30.setOnCheckedChangeListener { _, _ ->
            calcularComision()
        }

        // Listener para cambios en el spinner
        spinnerDistritos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calcularComision()
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

        // Seleccionar el distrito actual del cliente
        val position = distritos.indexOf(distritoActual)
        if (position >= 0) {
            spinnerDistritos.setSelection(position)
        }
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
        tvComisionTarifa.text = "S/ $comisionTarifa"
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
            val coordenadas = extraerCoordenadas(texto)

            withContext(Dispatchers.Main) {
                // Quitar loader
                tvCoordenadas.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

                if (coordenadas != null) {
                    latitud = coordenadas.first
                    longitud = coordenadas.second

                    tvCoordenadas.text = "Lat: $latitud, Lng: $longitud"
                    Toast.makeText(this@EditClientDistrictActivity,
                        "¡Coordenadas obtenidas con éxito!",
                        Toast.LENGTH_SHORT).show()
                } else {
                    tvCoordenadas.text = "No se pudieron obtener coordenadas"
                    Toast.makeText(this@EditClientDistrictActivity,
                        "No se pudieron extraer coordenadas del texto",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun extraerCoordenadas(texto: String): Pair<Double, Double>? {
        // 1. Buscar patrón @latitud,longitud
        val atMatch = atCoordsPattern.matcher(texto)
        if (atMatch.find()) {
            return Pair(atMatch.group(1).toDouble(), atMatch.group(2).toDouble())
        }

        // 2. Buscar coordenadas en cualquier parte del texto
        val latMatches = latPattern.matcher(texto)
        val lonMatches = lonPattern.matcher(texto)

        val latitudes = mutableListOf<String>()
        val longitudes = mutableListOf<String>()

        while (latMatches.find()) {
            latitudes.add(latMatches.group(1))
        }

        while (lonMatches.find()) {
            longitudes.add(lonMatches.group(1))
        }

        // Verificar si encontramos pares de coordenadas válidas
        if (latitudes.isNotEmpty() && longitudes.isNotEmpty()) {
            for (lat in latitudes) {
                for (lon in longitudes) {
                    try {
                        val latDouble = lat.toDouble()
                        val lonDouble = lon.toDouble()

                        // Verificar que las coordenadas estén en el rango apropiado para Lima
                        if (latDouble in -13.0..-11.0 && lonDouble in -78.0..-76.0) {
                            return Pair(latDouble, lonDouble)
                        }
                    } catch (e: NumberFormatException) {
                        continue
                    }
                }
            }
        }

        // 3. Buscar en formatos específicos como !3d{lat}!4d{lon}
        val lat3dPattern = Pattern.compile("!3d(-1[1-2]\\.\\d+)")
        val lon4dPattern = Pattern.compile("!4d(-7[6-7]\\.\\d+)")

        val lat3dMatch = lat3dPattern.matcher(texto)
        val lon4dMatch = lon4dPattern.matcher(texto)

        if (lat3dMatch.find() && lon4dMatch.find()) {
            return Pair(lat3dMatch.group(1).toDouble(), lon4dMatch.group(1).toDouble())
        }

        return null
    }

    private fun guardarCambios() {
        val distritoSeleccionado = spinnerDistritos.selectedItem.toString()

        // Verificar si tenemos coordenadas
        if (latitud == null || longitud == null) {
            Toast.makeText(this, "No hay coordenadas para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loader en botón guardar
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        // Crear mapa de coordenadas
        val coordenadas = hashMapOf(
            "lat" to latitud!!,
            "lng" to longitud!!
        )

        // Guardar en Firebase
        db.collection("recojos").document(clienteId)
            .update(
                "distrito", distritoSeleccionado,
                "pedidoCoordenadas", coordenadas,
                "comisionTarifa", comisionTarifa
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                finish() // Volver a la actividad anterior
            }
            .addOnFailureListener { e ->
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar"
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}