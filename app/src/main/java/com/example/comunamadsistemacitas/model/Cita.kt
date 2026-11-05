package com.example.comunamadsistemacitas.model

/**
 * Cita: Entidad relacional principal vinculada a Clientes y Servicios mediante Claves Foráneas (FK).
 */
data class Cita(
    val id: Int = 0,
    val clienteId: Int = 0,
    val servicioId: Int = 0,
    val cliente: String = "",
    val telefono: String = "",
    val servicio: String = "",
    val precio: Double = 0.0,
    val montoSena: Double = 0.0,
    val fechaHora: String = "",
    val fechaFin: String = "",
    val estado: EstadoCita = EstadoCita.PENDIENTE,
    val metodoPago: String = "No especificado",
    val duracionMinutos: Int = 30,
    val nota: String = ""
) {
    val saldoPendiente: Double
        get() = (precio - montoSena).coerceAtLeast(0.0)
}
