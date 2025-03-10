package com.example.moto_version.gimi

class Usuario {
    // Getters y Setters
    var nombre: String? = null
    var apellido: String? = null
    var email: String? = null
    var nombreEmpresa: String? = null
    var phone: String? = null
    var rol: String? = null
    var ruta: String? = null

    // Constructor vacío para Firebase
    constructor()

    // Constructor completo
    constructor(
        nombre: String?,
        apellido: String?,
        email: String?,
        nombreEmpresa: String?,
        phone: String?,
        rol: String?,
        ruta: String?
    ) {
        this.nombre = nombre
        this.apellido = apellido
        this.email = email
        this.nombreEmpresa = nombreEmpresa
        this.phone = phone
        this.rol = rol
        this.ruta = ruta
    }

    val nombreCompleto: String
        // Método para obtener el nombre completo
        get() = "$nombre $apellido"
}