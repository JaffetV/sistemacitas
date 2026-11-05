package com.example.comunamadsistemacitas.model

/**
 * Servicio: Entidad relacional para el catálogo de servicios con soporte para Soft Delete (activo).
 */
data class Servicio(
    val id: Int = 0,
    val nombre: String,
    val emoji: String = "🛠️",
    val precioBase: Double,
    val duracionMinutos: Int = 30,
    val activo: Boolean = true
)
