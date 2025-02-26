package com.example.moto_version

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetalleRecojoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_recojo)

        val id = intent.getStringExtra("id")
        val clienteNombre = intent.getStringExtra("clienteNombre")
        val proveedorNombre = intent.getStringExtra("proveedorNombre")
        val pedidoCantidadCobrar = intent.getStringExtra("pedidoCantidadCobrar")

        val tvId = findViewById<TextView>(R.id.tvDetalleId)
        val tvCliente = findViewById<TextView>(R.id.tvDetalleCliente)
        val tvProveedor = findViewById<TextView>(R.id.tvDetalleProveedor)
        val tvPrecio = findViewById<TextView>(R.id.tvDetallePrecio)

        tvId.text = "ID: $id"
        tvCliente.text = "Cliente: $clienteNombre"
        tvProveedor.text = "Proveedor: $proveedorNombre"
        tvPrecio.text = "Monto a cobrar: S/ $pedidoCantidadCobrar"
    }
}
