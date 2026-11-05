package com.example.comunamadsistemacitas.model

/**
 * Cliente: Entidad relacional para la gestión de clientes y formato E.164.
 */
data class Cliente(
    val id: Int = 0,
    val nombre: String,
    val telefono: String,
    val fechaRegistro: String = ""
)
