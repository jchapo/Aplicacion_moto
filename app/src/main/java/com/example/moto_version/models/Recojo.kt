package com.example.moto_version.models;

import com.google.firebase.Timestamp

data class Recojo(
    val id: String ="",
    val clienteNombre: String = "",
    val proveedorNombre: String = "",
    val pedidoCantidadCobrar: String = "0.00",
    val pedidoMetodoPago: String = "",
    val fechaEntregaPedidoMotorizado: Timestamp? = null,
    val fechaRecojoPedidoMotorizado: Timestamp? = null,
    val thumbnailFotoRecojo: String = ""
    )