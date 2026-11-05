package com.example.comunamadsistemacitas.model

data class CategoriaNegocio(
    val id: Int,
    val nombre: String,
    val slug: String,
    val emoji: String
)

data class NegocioMarketplace(
    val id: Int,
    val propietarioUsuarioId: Int,
    val categoriaId: Int,
    val categoria: String,
    val emoji: String,
    val nombre: String,
    val descripcion: String,
    val telefono: String,
    val departamento: String,
    val provincia: String,
    val distrito: String,
    val direccion: String,
    val modalidades: Set<String>,
    val estadoSuscripcion: String,
    val fechaFinSuscripcion: String?
)

data class PublicacionMarketplace(
    val id: Int,
    val negocioId: Int,
    val titulo: String,
    val descripcion: String,
    val tipo: String,
    val modalidad: String,
    val precio: Double,
    val precioAnterior: Double?,
    val duracionMinutos: Int?,
    val stock: Int?
)

data class ReservaMarketplace(
    val id: Long,
    val codigo: String,
    val estado: String,
    val modalidadPago: String,
    val total: Double
)

data class ReservaNegocio(
    val id: Int,
    val codigo: String,
    val cliente: String,
    val telefono: String,
    val publicacion: String,
    val modalidad: String,
    val fechaHora: String,
    val cantidad: Int,
    val total: Double,
    val estado: String,
    val estadoPago: String,
    val modalidadPago: String
)

data class PersonalNegocio(
    val id: Int,
    val negocioId: Int,
    val nombre: String,
    val especialidad: String,
    val estado: String
)

data class RecursoNegocio(
    val id: Int,
    val negocioId: Int,
    val nombre: String,
    val tipo: String,
    val estado: String
)
