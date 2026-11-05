package com.example.comunamadsistemacitas.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.comunamadsistemacitas.data.local.CitasDbHelper
import com.example.comunamadsistemacitas.model.Cita
import com.example.comunamadsistemacitas.model.Cliente
import com.example.comunamadsistemacitas.model.EstadoCita
import com.example.comunamadsistemacitas.model.Servicio
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * SemafroCliente: Estructura para el semáforo y score de confiabilidad de un cliente.
 */
data class SemafroCliente(
    val nivel: String, // VERDE, AMARILLO, ROJO, NUEVO
    val etiqueta: String,
    val recomendacion: String,
    val atendidos: Int,
    val cancelados: Int
)

/**
 * CitaRepository: Repositorio relacional seguro conectado con las tablas Clientes, Servicios y Citas.
 */
class CitaRepository(context: Context) {

    private val dbHelper = CitasDbHelper(context)

    companion object {
        fun sanitizarTelefonoWhatsapp(telefonoRaw: String): String {
            val digitos = telefonoRaw.replace("[^0-9]".toRegex(), "")
            return when {
                digitos.startsWith("51") && digitos.length >= 11 -> digitos
                digitos.length == 9 -> "51$digitos"
                else -> digitos
            }
        }
    }

    // Registrar o recuperar ID de cliente existente en tabla Clientes
    fun registrarOConsignarCliente(nombre: String, telefono: String): Int {
        if (nombre.isBlank()) return -1
        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery(
            "SELECT ${CitasDbHelper.COLUMN_CLIENTE_ID} FROM ${CitasDbHelper.TABLE_CLIENTES} WHERE LOWER(${CitasDbHelper.COLUMN_CLIENTE_NOMBRE}) = LOWER(?)",
            arrayOf(nombre.trim())
        )
        if (cursor.moveToFirst()) {
            val existingId = cursor.getInt(0)
            cursor.close()
            return existingId
        }
        cursor.close()

        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(CitasDbHelper.COLUMN_CLIENTE_NOMBRE, nombre.trim())
            put(CitasDbHelper.COLUMN_CLIENTE_TELEFONO, sanitizarTelefonoWhatsapp(telefono))
            put(CitasDbHelper.COLUMN_CLIENTE_FECHA_REGISTRO, fechaHoy)
        }
        return db.insert(CitasDbHelper.TABLE_CLIENTES, null, values).toInt()
    }

    // Obtener catálogo de servicios activos
    fun obtenerServiciosActivos(): List<Servicio> {
        val lista = mutableListOf<Servicio>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${CitasDbHelper.TABLE_SERVICIOS} WHERE ${CitasDbHelper.COLUMN_SERVICIO_ACTIVO} = 1",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO_NOMBRE))
                val emoji = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO_EMOJI))
                val precioBase = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO_PRECIO_BASE))
                val duracion = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO_DURACION))

                lista.add(Servicio(id, nombre, emoji, precioBase, duracion, true))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // Crear cita (CREATE)
    fun insertarCita(cita: Cita): Long {
        val clienteId = registrarOConsignarCliente(cita.cliente, cita.telefono)
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            if (clienteId != -1) put(CitasDbHelper.COLUMN_FK_CLIENTE_ID, clienteId)
            if (cita.servicioId > 0) put(CitasDbHelper.COLUMN_FK_SERVICIO_ID, cita.servicioId)
            put(CitasDbHelper.COLUMN_CLIENTE, cita.cliente.ifBlank { "Cliente General" })
            put(CitasDbHelper.COLUMN_TELEFONO, sanitizarTelefonoWhatsapp(cita.telefono))
            put(CitasDbHelper.COLUMN_SERVICIO, cita.servicio.ifBlank { "Servicio General" })
            put(CitasDbHelper.COLUMN_PRECIO, cita.precio.coerceAtLeast(0.0))
            put(CitasDbHelper.COLUMN_MONTO_SENA, cita.montoSena.coerceAtLeast(0.0))
            put(CitasDbHelper.COLUMN_FECHA_HORA, cita.fechaHora)
            put(CitasDbHelper.COLUMN_FECHA_FIN, cita.fechaFin)
            put(CitasDbHelper.COLUMN_ESTADO, cita.estado.name)
            put(CitasDbHelper.COLUMN_METODO_PAGO, cita.metodoPago.ifBlank { "Efectivo" })
            put(CitasDbHelper.COLUMN_DURACION_MINUTOS, cita.duracionMinutos.coerceAtLeast(15))
            put(CitasDbHelper.COLUMN_NOTA, cita.nota)
        }
        return db.insert(CitasDbHelper.TABLE_CITAS, null, values)
    }

    // Validación avanzada de traslape por rango de tiempo completo (Inicio -> Fin)
    fun existeTraslapeHorario(fechaHoraInicioStr: String, duracionMinutos: Int, idCitaExcluir: Int = -1): Boolean {
        if (fechaHoraInicioStr.isBlank()) return false
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val inicioNueva: Long
        val finNueva: Long
        try {
            val dateInicio = sdf.parse(fechaHoraInicioStr) ?: return false
            inicioNueva = dateInicio.time
            finNueva = inicioNueva + (duracionMinutos.coerceAtLeast(15) * 60 * 1000L)
        } catch (e: Exception) {
            return false
        }

        val todasActivas = obtenerTodasLasCitas(limite = 200).filter {
            it.estado != EstadoCita.CANCELADO && it.id != idCitaExcluir
        }

        for (citaExistente in todasActivas) {
            try {
                val dateExistente = sdf.parse(citaExistente.fechaHora) ?: continue
                val inicioExistente = dateExistente.time
                val finExistente = inicioExistente + (citaExistente.duracionMinutos.coerceAtLeast(15) * 60 * 1000L)

                if (inicioNueva < finExistente && finNueva > inicioExistente) {
                    return true
                }
            } catch (e: Exception) {
                continue
            }
        }
        return false
    }

    fun existeCitaEnHorario(fechaHora: String, idCitaExcluir: Int = -1): Boolean {
        return existeTraslapeHorario(fechaHora, 30, idCitaExcluir)
    }

    // Semáforo de Confiabilidad y Score de Asistencia del Cliente
    fun obtenerSemaforoCliente(nombreCliente: String): SemafroCliente {
        if (nombreCliente.isBlank()) {
            return SemafroCliente("NUEVO", "Nuevo Cliente", "Sin historial registrado.", 0, 0)
        }

        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${CitasDbHelper.COLUMN_ESTADO}, COUNT(*) FROM ${CitasDbHelper.TABLE_CITAS} WHERE LOWER(${CitasDbHelper.COLUMN_CLIENTE}) = LOWER(?) GROUP BY ${CitasDbHelper.COLUMN_ESTADO}",
            arrayOf(nombreCliente.trim())
        )

        var atendidos = 0
        var cancelados = 0

        if (cursor.moveToFirst()) {
            do {
                val estadoStr = cursor.getString(0)
                val cnt = cursor.getInt(1)
                if (estadoStr == EstadoCita.ATENDIDO.name) {
                    atendidos = cnt
                } else if (estadoStr == EstadoCita.CANCELADO.name || estadoStr == EstadoCita.NO_SHOW.name) {
                    cancelados += cnt
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        val totalPrevios = atendidos + cancelados
        if (totalPrevios == 0) {
            return SemafroCliente("NUEVO", "Nuevo Cliente", "Primer registro en el sistema.", 0, 0)
        }

        return when {
            cancelados >= 2 -> SemafroCliente(
                "ROJO",
                "🔴 Alto Riesgo de Ausentismo ($cancelados cancelaciones/no-shows)",
                "⚠️ EXIGIR SEÑA PREVIA (50%) antes de confirmar el turno.",
                atendidos,
                cancelados
            )
            cancelados == 1 -> SemafroCliente(
                "AMARILLO",
                "🟡 Riesgo Moderado (1 cancelación previa)",
                "Recomendar abono de seña previa para asegurar reserva.",
                atendidos,
                cancelados
            )
            else -> SemafroCliente(
                "VERDE",
                "🟢 Cliente Confiable ($atendidos citas atendidas)",
                "Historial de asistencia 100% positivo.",
                atendidos,
                cancelados
            )
        }
    }

    // Obtener cita por ID (READ SINGLE)
    fun obtenerCitaPorId(idCita: Int): Cita? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${CitasDbHelper.TABLE_CITAS} WHERE ${CitasDbHelper.COLUMN_ID} = ?",
            arrayOf(idCita.toString())
        )
        var cita: Cita? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ID))
            val clienteId = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FK_CLIENTE_ID))
            val servicioId = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FK_SERVICIO_ID))
            val cliente = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE)) ?: "Cliente General"
            val telefono = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_TELEFONO)) ?: ""
            val servicio = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO)) ?: "Servicio General"
            val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_PRECIO))
            val sena = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_MONTO_SENA))
            val fechaHora = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FECHA_HORA)) ?: ""
            val fechaFin = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FECHA_FIN)) ?: ""
            val estadoString = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ESTADO)) ?: EstadoCita.PENDIENTE.name
            val metodoPago = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_METODO_PAGO)) ?: "Efectivo"
            val duracion = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_DURACION_MINUTOS))
            val nota = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_NOTA)) ?: ""

            cita = Cita(
                id = id,
                clienteId = clienteId,
                servicioId = servicioId,
                cliente = cliente,
                telefono = telefono,
                servicio = servicio,
                precio = precio,
                montoSena = sena,
                fechaHora = fechaHora,
                fechaFin = fechaFin,
                estado = runCatching { EstadoCita.valueOf(estadoString) }.getOrDefault(EstadoCita.PENDIENTE),
                metodoPago = metodoPago,
                duracionMinutos = if (duracion > 0) duracion else 30,
                nota = nota
            )
        }
        cursor.close()
        return cita
    }

    // Obtener todas las citas con paginación
    fun obtenerTodasLasCitas(limite: Int = 100): List<Cita> {
        val listaCitas = mutableListOf<Cita>()
        val db = dbHelper.readableDatabase
        val query = "SELECT * FROM ${CitasDbHelper.TABLE_CITAS} ORDER BY ${CitasDbHelper.COLUMN_ID} DESC LIMIT $limite"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ID))
                val clienteId = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FK_CLIENTE_ID))
                val servicioId = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FK_SERVICIO_ID))
                val cliente = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE)) ?: "Cliente General"
                val telefono = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_TELEFONO)) ?: ""
                val servicio = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO)) ?: "Servicio General"
                val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_PRECIO))
                val sena = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_MONTO_SENA))
                val fechaHora = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FECHA_HORA)) ?: ""
                val fechaFin = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FECHA_FIN)) ?: ""
                val estadoString = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ESTADO)) ?: EstadoCita.PENDIENTE.name
                val metodoPago = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_METODO_PAGO)) ?: "Efectivo"
                val duracion = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_DURACION_MINUTOS))
                val nota = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_NOTA)) ?: ""

                val cita = Cita(
                    id = id,
                    clienteId = clienteId,
                    servicioId = servicioId,
                    cliente = cliente,
                    telefono = telefono,
                    servicio = servicio,
                    precio = precio,
                    montoSena = sena,
                    fechaHora = fechaHora,
                    fechaFin = fechaFin,
                    estado = runCatching { EstadoCita.valueOf(estadoString) }.getOrDefault(EstadoCita.PENDIENTE),
                    metodoPago = metodoPago,
                    duracionMinutos = if (duracion > 0) duracion else 30,
                    nota = nota
                )
                listaCitas.add(cita)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return listaCitas
    }

    // Obtener directorio de clientes desde la tabla relacional Clientes
    fun obtenerDirectorioClientes(): List<Cliente> {
        val lista = mutableListOf<Cliente>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${CitasDbHelper.TABLE_CLIENTES} ORDER BY ${CitasDbHelper.COLUMN_CLIENTE_NOMBRE} ASC",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE_NOMBRE))
                val telefono = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE_TELEFONO))
                val fechaReg = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE_FECHA_REGISTRO))

                lista.add(Cliente(id, nombre, telefono, fechaReg))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // Conteo de citas por cliente
    fun obtenerConteoCitasCliente(nombreCliente: String): Int {
        if (nombreCliente.isBlank()) return 0
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${CitasDbHelper.TABLE_CITAS} WHERE LOWER(${CitasDbHelper.COLUMN_CLIENTE}) = LOWER(?)",
            arrayOf(nombreCliente.trim())
        )
        var conteo = 0
        if (cursor.moveToFirst()) {
            conteo = cursor.getInt(0)
        }
        cursor.close()
        return conteo
    }

    // Total pendientes
    fun obtenerTotalPendientes(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${CitasDbHelper.TABLE_CITAS} WHERE ${CitasDbHelper.COLUMN_ESTADO} = ?",
            arrayOf(EstadoCita.PENDIENTE.name)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    // Ingreso Real
    fun obtenerTotalIngresosAtendidos(): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(${CitasDbHelper.COLUMN_PRECIO}) FROM ${CitasDbHelper.TABLE_CITAS} WHERE ${CitasDbHelper.COLUMN_ESTADO} = ?",
            arrayOf(EstadoCita.ATENDIDO.name)
        )
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    // Ingreso Proyectado
    fun obtenerIngresoProyectado(): Double {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(${CitasDbHelper.COLUMN_PRECIO}) FROM ${CitasDbHelper.TABLE_CITAS} WHERE ${CitasDbHelper.COLUMN_ESTADO} != ? AND ${CitasDbHelper.COLUMN_ESTADO} != ?",
            arrayOf(EstadoCita.CANCELADO.name, EstadoCita.NO_SHOW.name)
        )
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        return total
    }

    // Próxima Cita
    fun obtenerProximaCita(): Cita? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM ${CitasDbHelper.TABLE_CITAS} WHERE ${CitasDbHelper.COLUMN_ESTADO} = ? ORDER BY ${CitasDbHelper.COLUMN_ID} DESC LIMIT 1",
            arrayOf(EstadoCita.PENDIENTE.name)
        )
        var cita: Cita? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ID))
            val cliente = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_CLIENTE)) ?: "Cliente General"
            val telefono = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_TELEFONO)) ?: ""
            val servicio = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_SERVICIO)) ?: "Servicio General"
            val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_PRECIO))
            val sena = cursor.getDouble(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_MONTO_SENA))
            val fechaHora = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_FECHA_HORA)) ?: ""
            val estadoString = cursor.getString(cursor.getColumnIndexOrThrow(CitasDbHelper.COLUMN_ESTADO)) ?: EstadoCita.PENDIENTE.name

            cita = Cita(
                id = id,
                cliente = cliente,
                telefono = telefono,
                servicio = servicio,
                precio = precio,
                montoSena = sena,
                fechaHora = fechaHora,
                estado = runCatching { EstadoCita.valueOf(estadoString) }.getOrDefault(EstadoCita.PENDIENTE)
            )
        }
        cursor.close()
        return cita
    }

    // Actualizar estado y método de pago
    fun actualizarEstadoYMetodoPago(idCita: Int, nuevoEstado: EstadoCita, metodoPago: String = "Efectivo"): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CitasDbHelper.COLUMN_ESTADO, nuevoEstado.name)
            put(CitasDbHelper.COLUMN_METODO_PAGO, metodoPago)
        }
        return db.update(
            CitasDbHelper.TABLE_CITAS,
            values,
            "${CitasDbHelper.COLUMN_ID} = ?",
            arrayOf(idCita.toString())
        )
    }

    // Actualizar solo el estado
    fun actualizarEstadoCita(idCita: Int, nuevoEstado: EstadoCita): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(CitasDbHelper.COLUMN_ESTADO, nuevoEstado.name)
        }
        return db.update(
            CitasDbHelper.TABLE_CITAS,
            values,
            "${CitasDbHelper.COLUMN_ID} = ?",
            arrayOf(idCita.toString())
        )
    }

    // Eliminar cita (DELETE)
    fun eliminarCita(idCita: Int): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            CitasDbHelper.TABLE_CITAS,
            "${CitasDbHelper.COLUMN_ID} = ?",
            arrayOf(idCita.toString())
        )
    }
}
