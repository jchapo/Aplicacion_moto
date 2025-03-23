package com.example.moto_version.models

data class PagoRegistro(
    val metodoPago: String,
    val receptor: String,
    val monto: Double
)