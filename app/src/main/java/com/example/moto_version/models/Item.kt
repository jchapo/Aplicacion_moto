package com.example.moto_version.models

import java.security.Timestamp

data class Item (
    val id: String = "",
    val clienteNombre: String = "",
    val proveedorNombre: String = "",
    val pedidoCantidadCobrar: String = "0.00",
    val clienteDistrito: String = "",
    val clienteTelefono: String = "",
    val comisionTarifa: Double = 0.0,
    val fechaAnulacionPedido: Timestamp? = null,
    val fechaEntregaPedidoMotorizado: Timestamp? = null,
    val fechaRecojoPedidoMotorizado: Timestamp? = null,
    val pedidoCoordenadas: Map<String, Double>? = null,
    val pedidoDetalle: String = "",
    val pedidoDireccionLink: String = "",
    val pedidoFotoEntrega: String = "",
    val pedidoFotoRecojo: String = "",
    val pedidoMetodoPago: String = "",
    val pedidoObservaciones: String = "",
    val pedidoSeCobra: String = "",
    val proveedorDireccionLink: String = "",
    val proveedorDistrito: String = "",
    val proveedorTelefono: String = "",
    val recojoCoordenadas: Map<String, Double>? = null,
    val thumbnailFotoEntrega: String = "",
    val thumbnailFotoRecojo: String = ""
)