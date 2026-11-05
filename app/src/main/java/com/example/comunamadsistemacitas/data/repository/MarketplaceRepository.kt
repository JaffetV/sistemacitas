package com.example.comunamadsistemacitas.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.comunamadsistemacitas.data.local.CitasDbHelper
import com.example.comunamadsistemacitas.model.CategoriaNegocio
import com.example.comunamadsistemacitas.model.NegocioMarketplace
import com.example.comunamadsistemacitas.model.PublicacionMarketplace
import com.example.comunamadsistemacitas.model.ReservaMarketplace
import com.example.comunamadsistemacitas.model.ReservaNegocio
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Operaciones locales del marketplace. Preparado para sustituir la persistencia por API en la fase cloud. */
class MarketplaceRepository(context: Context) {
    private val helper = CitasDbHelper(context.applicationContext)

    private fun ahora(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

    fun obtenerCategorias(): List<CategoriaNegocio> {
        val result = mutableListOf<CategoriaNegocio>()
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT id, nombre, slug, emoji FROM categorias_negocio WHERE activa = 1 ORDER BY nombre", null
        )
        cursor.use {
            while (it.moveToNext()) result.add(
                CategoriaNegocio(it.getInt(0), it.getString(1), it.getString(2), it.getString(3))
            )
        }
        return result
    }

    /** Registro gratis. Se reutiliza la cuenta por email o teléfono para evitar duplicados simples. */
    fun registrarCliente(nombre: String, telefono: String, email: String = ""): Int {
        require(nombre.isNotBlank()) { "Escribe tu nombre." }
        val telefonoLimpio = telefono.trim()
        require(telefonoLimpio.length >= 7) { "Ingresa un teléfono válido." }
        val db = helper.writableDatabase
        val correo = email.trim().ifBlank { null }
        if (correo != null) {
            db.rawQuery("SELECT id FROM usuarios_marketplace WHERE email = ? LIMIT 1", arrayOf(correo)).use {
                if (it.moveToFirst()) return it.getInt(0)
            }
        }
        db.rawQuery("SELECT id FROM usuarios_marketplace WHERE telefono = ? LIMIT 1", arrayOf(telefonoLimpio)).use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        val values = ContentValues().apply {
            put("nombre", nombre.trim())
            if (correo == null) putNull("email") else put("email", correo)
            put("telefono", telefonoLimpio)
            put("tipo_cuenta", "GRATUITA")
            put("estado", "ACTIVO")
            put("fecha_registro", ahora())
        }
        return db.insertOrThrow("usuarios_marketplace", null, values).toInt()
    }

    /** Alta automática: crea propietario, negocio, modalidades y período de prueba comercial. */
    fun registrarNegocio(
        propietarioNombre: String,
        propietarioEmail: String,
        propietarioTelefono: String,
        nombre: String,
        categoriaId: Int,
        descripcion: String,
        departamento: String,
        provincia: String,
        distrito: String,
        direccion: String,
        modalidades: Set<String>
    ): Int {
        require(propietarioNombre.isNotBlank()) { "Ingresa el nombre del responsable." }
        require(propietarioTelefono.trim().length >= 7) { "Ingresa un teléfono válido." }
        require(nombre.isNotBlank()) { "Ingresa el nombre del negocio." }
        require(categoriaId > 0) { "Selecciona una categoría." }
        require(distrito.isNotBlank() && direccion.isNotBlank()) { "Completa la zona y dirección del negocio." }
        require(modalidades.isNotEmpty() && modalidades.all { it == "CITA" || it == "RECOJO" }) {
            "Selecciona al menos una modalidad válida."
        }

        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val usuarioId = buscarOCrearUsuario(db, propietarioNombre, propietarioEmail, propietarioTelefono)
            val timestamp = ahora()
            val negocioValues = ContentValues().apply {
                put("propietario_usuario_id", usuarioId)
                put("categoria_id", categoriaId)
                put("nombre", nombre.trim())
                put("descripcion", descripcion.trim())
                put("telefono", propietarioTelefono.trim())
                if (propietarioEmail.isBlank()) putNull("email_contacto") else put("email_contacto", propietarioEmail.trim())
                put("pais_codigo", "PE")
                put("departamento", departamento.trim())
                put("provincia", provincia.trim())
                put("distrito", distrito.trim())
                put("direccion", direccion.trim())
                put("cobertura", "LOCAL")
                put("estado", "ACTIVO")
                put("visible_marketplace", 1)
                put("fecha_creacion", timestamp)
                put("fecha_actualizacion", timestamp)
            }
            val negocioId = db.insertOrThrow("negocios", null, negocioValues)
            modalidades.forEach { modalidad ->
                val modeValues = ContentValues().apply {
                    put("negocio_id", negocioId)
                    put("modalidad", modalidad)
                }
                db.insertOrThrow("negocio_modalidades", null, modeValues)
            }
            val planId = db.rawQuery("SELECT id FROM planes_suscripcion WHERE codigo = 'PRUEBA'", null).use {
                check(it.moveToFirst()) { "No existe el plan de prueba." }
                it.getInt(0)
            }
            val finPrueba = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }
            val subscriptionValues = ContentValues().apply {
                put("negocio_id", negocioId)
                put("plan_id", planId)
                put("estado", "PRUEBA")
                put("fecha_inicio", timestamp)
                put("fecha_fin", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(finPrueba.time))
                put("renovacion_automatica", 0)
                put("fecha_creacion", timestamp)
            }
            db.insertOrThrow("suscripciones_negocio", null, subscriptionValues)
            db.setTransactionSuccessful()
            return negocioId.toInt()
        } finally {
            db.endTransaction()
        }
    }

    private fun buscarOCrearUsuario(db: SQLiteDatabase, nombre: String, email: String, telefono: String): Long {
        if (email.isNotBlank()) {
            db.rawQuery("SELECT id FROM usuarios_marketplace WHERE email = ? LIMIT 1", arrayOf(email.trim())).use {
                if (it.moveToFirst()) return it.getLong(0)
            }
        }
        db.rawQuery("SELECT id FROM usuarios_marketplace WHERE telefono = ? LIMIT 1", arrayOf(telefono.trim())).use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        val values = ContentValues().apply {
            put("nombre", nombre.trim())
            if (email.isBlank()) putNull("email") else put("email", email.trim())
            put("telefono", telefono.trim())
            put("tipo_cuenta", "GRATUITA")
            put("estado", "ACTIVO")
            put("fecha_registro", ahora())
        }
        return db.insertOrThrow("usuarios_marketplace", null, values)
    }

    fun obtenerNegociosActivos(texto: String = "", categoriaId: Int? = null): List<NegocioMarketplace> {
        val result = mutableListOf<NegocioMarketplace>()
        val db = helper.writableDatabase
        db.execSQL("UPDATE suscripciones_negocio SET estado = 'VENCIDA' WHERE estado IN ('PRUEBA','ACTIVA') AND fecha_fin IS NOT NULL AND fecha_fin < ?", arrayOf(ahora()))
        val args = mutableListOf<String>()
        val filters = StringBuilder("n.estado = 'ACTIVO' AND n.visible_marketplace = 1 AND EXISTS (SELECT 1 FROM suscripciones_negocio sx WHERE sx.negocio_id = n.id AND sx.estado IN ('PRUEBA','ACTIVA') AND (sx.fecha_fin IS NULL OR sx.fecha_fin >= ?))")
        args.add(ahora())
        if (texto.isNotBlank()) {
            filters.append(" AND (n.nombre LIKE ? OR n.descripcion LIKE ? OR n.distrito LIKE ? OR c.nombre LIKE ?)")
            val term = "%${texto.trim()}%"
            repeat(4) { args.add(term) }
        }
        if (categoriaId != null && categoriaId > 0) {
            filters.append(" AND n.categoria_id = ?")
            args.add(categoriaId.toString())
        }
        val sql = """
            SELECT n.id, n.propietario_usuario_id, n.categoria_id, c.nombre, c.emoji,
                   n.nombre, n.descripcion, n.telefono, n.departamento, n.provincia,
                   n.distrito, n.direccion,
                   COALESCE((SELECT group_concat(modalidad, ',') FROM negocio_modalidades nm WHERE nm.negocio_id = n.id), ''),
                   COALESCE((SELECT estado FROM suscripciones_negocio s WHERE s.negocio_id = n.id ORDER BY s.id DESC LIMIT 1), 'SIN_PLAN'),
                   (SELECT fecha_fin FROM suscripciones_negocio s WHERE s.negocio_id = n.id ORDER BY s.id DESC LIMIT 1)
            FROM negocios n JOIN categorias_negocio c ON c.id = n.categoria_id
            WHERE $filters ORDER BY n.fecha_creacion DESC
        """.trimIndent()
        helper.readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    NegocioMarketplace(
                        id = cursor.getInt(0), propietarioUsuarioId = cursor.getInt(1), categoriaId = cursor.getInt(2),
                        categoria = cursor.getString(3), emoji = cursor.getString(4), nombre = cursor.getString(5),
                        descripcion = cursor.getString(6), telefono = cursor.getString(7), departamento = cursor.getString(8),
                        provincia = cursor.getString(9), distrito = cursor.getString(10), direccion = cursor.getString(11),
                        modalidades = cursor.getString(12).split(",").filter { it.isNotBlank() }.toSet(),
                        estadoSuscripcion = cursor.getString(13), fechaFinSuscripcion = cursor.getString(14)
                    )
                )
            }
        }
        return result
    }

    fun obtenerNegocio(id: Int): NegocioMarketplace? = obtenerNegociosActivos().firstOrNull { it.id == id }

    /** Acceso administrativo: permite abrir el panel incluso cuando una prueba ya venció. */
    fun obtenerNegocioParaGestion(id: Int): NegocioMarketplace? {
        val db = helper.readableDatabase
        val cursor = db.rawQuery("""
            SELECT n.id, n.propietario_usuario_id, n.categoria_id, c.nombre, c.emoji,
                   n.nombre, n.descripcion, n.telefono, n.departamento, n.provincia,
                   n.distrito, n.direccion,
                   COALESCE((SELECT group_concat(modalidad, ',') FROM negocio_modalidades nm WHERE nm.negocio_id = n.id), ''),
                   COALESCE((SELECT estado FROM suscripciones_negocio s WHERE s.negocio_id = n.id ORDER BY s.id DESC LIMIT 1), 'SIN_PLAN'),
                   (SELECT fecha_fin FROM suscripciones_negocio s WHERE s.negocio_id = n.id ORDER BY s.id DESC LIMIT 1)
            FROM negocios n JOIN categorias_negocio c ON c.id = n.categoria_id WHERE n.id = ? LIMIT 1
        """.trimIndent(), arrayOf(id.toString()))
        cursor.use {
            if (!it.moveToFirst()) return null
            return NegocioMarketplace(
                id = it.getInt(0), propietarioUsuarioId = it.getInt(1), categoriaId = it.getInt(2),
                categoria = it.getString(3), emoji = it.getString(4), nombre = it.getString(5),
                descripcion = it.getString(6), telefono = it.getString(7), departamento = it.getString(8),
                provincia = it.getString(9), distrito = it.getString(10), direccion = it.getString(11),
                modalidades = it.getString(12).split(",").filter { value -> value.isNotBlank() }.toSet(),
                estadoSuscripcion = it.getString(13), fechaFinSuscripcion = it.getString(14)
            )
        }
    }

    fun crearPublicacion(
        negocioId: Int,
        titulo: String,
        descripcion: String,
        tipo: String,
        modalidad: String,
        precio: Double,
        precioAnterior: Double?,
        duracionMinutos: Int?,
        stock: Int? = null
    ): Long {
        require(titulo.isNotBlank()) { "Escribe el nombre del servicio o producto." }
        require(tipo in setOf("SERVICIO", "PRODUCTO")) { "Tipo de publicación inválido." }
        if (tipo == "PRODUCTO") require(modalidad == "RECOJO") { "Los productos se publican para pedidos de recogida." }
        require(modalidad in setOf("CITA", "RECOJO", "AMBOS")) { "Modalidad inválida." }
        require(precio >= 0.0) { "El precio no puede ser negativo." }
        require(precioAnterior == null || precioAnterior > precio) { "El precio anterior debe ser mayor al precio de oferta." }
        require(stock == null || stock >= 0) { "El stock no puede ser negativo." }
        require(modalidadDisponible(negocioId, modalidad)) { "El negocio no tiene habilitada esa modalidad." }
        val timestamp = ahora()
        val values = ContentValues().apply {
            put("negocio_id", negocioId); put("titulo", titulo.trim()); put("descripcion", descripcion.trim())
            put("tipo", tipo); put("modalidad", modalidad); put("precio", precio)
            if (precioAnterior == null) putNull("precio_anterior") else put("precio_anterior", precioAnterior)
            if (duracionMinutos == null) putNull("duracion_minutos") else put("duracion_minutos", duracionMinutos)
            if (stock == null) putNull("stock") else put("stock", stock)
            put("activa", 1); put("fecha_creacion", timestamp); put("fecha_actualizacion", timestamp)
        }
        return helper.writableDatabase.insertOrThrow("publicaciones", null, values)
    }

    fun obtenerPublicaciones(negocioId: Int): List<PublicacionMarketplace> {
        val result = mutableListOf<PublicacionMarketplace>()
        helper.readableDatabase.rawQuery(
            "SELECT id, negocio_id, titulo, descripcion, tipo, modalidad, precio, precio_anterior, duracion_minutos, stock FROM publicaciones WHERE negocio_id = ? AND activa = 1 ORDER BY fecha_creacion DESC",
            arrayOf(negocioId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(
                PublicacionMarketplace(
                    cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5), cursor.getDouble(6),
                    if (cursor.isNull(7)) null else cursor.getDouble(7),
                    if (cursor.isNull(8)) null else cursor.getInt(8),
                    if (cursor.isNull(9)) null else cursor.getInt(9)
                )
            )
        }
        return result
    }

    fun crearReserva(
        negocioId: Int,
        publicacion: PublicacionMarketplace,
        nombreCliente: String,
        telefonoCliente: String,
        emailCliente: String,
        modalidad: String,
        fechaHora: String,
        cantidad: Int,
        modalidadPago: String,
        nota: String
    ): ReservaMarketplace {
        require(modalidad in setOf("CITA", "RECOJO")) { "Selecciona una modalidad válida." }
        require(publicacion.tipo != "PRODUCTO" || modalidad == "RECOJO") { "Los productos se reservan para recoger." }
        require(modalidadPago in setOf("EN_LINEA", "EN_LOCAL")) { "Selecciona un medio de pago válido." }
        require(nombreCliente.isNotBlank()) { "Escribe tu nombre." }
        require(telefonoCliente.trim().length >= 7) { "Ingresa un teléfono válido." }
        require(cantidad > 0) { "La cantidad debe ser mayor que cero." }
        require(fechaHora.isNotBlank()) { "Indica fecha y hora para la reserva." }
        require(publicacion.negocioId == negocioId) { "La publicación no pertenece a este negocio." }
        require(publicacion.modalidad == "AMBOS" || publicacion.modalidad == modalidad) { "La publicación no admite esa modalidad." }
        require(modalidadDisponible(negocioId, modalidad)) { "El negocio no ofrece esa modalidad." }
        if (modalidad == "CITA") require(publicacion.tipo == "SERVICIO") { "Para una cita debes seleccionar un servicio." }
        if (modalidad == "RECOJO" && publicacion.stock != null) require(publicacion.stock >= cantidad) { "No hay stock suficiente." }

        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val userId = registrarCliente(nombreCliente, telefonoCliente, emailCliente)
            val duracion = (publicacion.duracionMinutos ?: 30).coerceAtLeast(15)
            val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }
            val storageFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val start = inputFormat.parse(fechaHora)
                ?: throw IllegalArgumentException("La fecha debe tener formato dd/MM/aaaa HH:mm.")
            require(start.after(Date())) { "La fecha y hora deben ser futuras." }
            val fechaInicioDb = storageFormat.format(start)
            var fechaFin: String? = null
            var fechaRecogidaDb: String? = null
            if (modalidad == "CITA") {
                val end = Date(start.time + duracion * 60_000L)
                fechaFin = storageFormat.format(end)
                val overlap = db.rawQuery(
                    "SELECT COUNT(*) FROM reservas_marketplace WHERE negocio_id = ? AND modalidad = 'CITA' AND estado NOT IN ('CANCELADA','NO_SHOW') AND fecha_inicio < ? AND fecha_fin > ?",
                    arrayOf(negocioId.toString(), fechaFin, fechaInicioDb)
                ).use { it.moveToFirst(); it.getInt(0) > 0 }
                require(!overlap) { "Ese horario ya tiene una reserva. Elige otra hora." }
            } else {
                fechaRecogidaDb = fechaInicioDb
            }
            val codigo = "RSV-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
            val total = publicacion.precio * cantidad
            val values = ContentValues().apply {
                put("codigo", codigo); put("negocio_id", negocioId); put("publicacion_id", publicacion.id)
                put("usuario_id", userId); put("modalidad", modalidad)
                if (modalidad == "CITA") {
                    put("fecha_inicio", fechaInicioDb); put("fecha_fin", fechaFin)
                } else {
                    put("fecha_recogida", fechaRecogidaDb)
                }
                put("cantidad", cantidad); put("precio_unitario", publicacion.precio); put("total", total)
                put("estado", "SOLICITADA"); put("modalidad_pago", modalidadPago); put("estado_pago", "PENDIENTE")
                put("nota", nota.trim()); put("fecha_creacion", ahora())
            }
            val reservaId = db.insertOrThrow("reservas_marketplace", null, values)
            if (modalidad == "RECOJO" && publicacion.stock != null) {
                db.execSQL(
                    "UPDATE publicaciones SET stock = stock - ? WHERE id = ? AND stock >= ? AND activa = 1",
                    arrayOf(cantidad, publicacion.id, cantidad)
                )
                val updated = db.rawQuery("SELECT changes()", null).use { it.moveToFirst(); it.getInt(0) }
                require(updated == 1) { "El stock acaba de cambiar. Actualiza el catálogo e inténtalo de nuevo." }
            }
            val paymentValues = ContentValues().apply {
                put("usuario_id", userId); put("reserva_id", reservaId); putNull("suscripcion_id")
                put("concepto", "RESERVA"); put("monto", total); put("moneda", "PEN")
                put("canal", modalidadPago); put("estado", "PENDIENTE"); put("fecha_creacion", ahora())
            }
            db.insertOrThrow("pagos_marketplace", null, paymentValues)
            db.setTransactionSuccessful()
            return ReservaMarketplace(reservaId, codigo, "SOLICITADA", modalidadPago, total)
        } finally {
            db.endTransaction()
        }
    }

    private fun modalidadDisponible(negocioId: Int, modalidad: String): Boolean {
        val count = if (modalidad == "AMBOS") {
            helper.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM negocio_modalidades WHERE negocio_id = ? AND modalidad IN ('CITA','RECOJO')",
                arrayOf(negocioId.toString())
            ).use { it.moveToFirst(); it.getInt(0) == 2 }
        } else {
            helper.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM negocio_modalidades WHERE negocio_id = ? AND modalidad = ?",
                arrayOf(negocioId.toString(), modalidad)
            ).use { it.moveToFirst(); it.getInt(0) == 1 }
        }
        return count
    }

    fun obtenerPublicacionesNegocioDePropietario(negocioId: Int): List<PublicacionMarketplace> = obtenerPublicaciones(negocioId)

    fun obtenerReservasNegocio(negocioId: Int): List<ReservaNegocio> {
        val result = mutableListOf<ReservaNegocio>()
        helper.readableDatabase.rawQuery("""
            SELECT r.id, r.codigo, u.nombre, u.telefono, p.titulo, r.modalidad,
                   COALESCE(r.fecha_inicio, r.fecha_recogida, ''), r.cantidad, r.total,
                   r.estado, r.estado_pago, r.modalidad_pago
            FROM reservas_marketplace r
            JOIN usuarios_marketplace u ON u.id = r.usuario_id
            JOIN publicaciones p ON p.id = r.publicacion_id
            WHERE r.negocio_id = ? ORDER BY r.fecha_creacion DESC
        """.trimIndent(), arrayOf(negocioId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val storedDate = cursor.getString(6)
                val displayDate = runCatching {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(storedDate)!!
                    )
                }.getOrDefault(storedDate)
                result.add(ReservaNegocio(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                    cursor.getString(4), cursor.getString(5), displayDate, cursor.getInt(7),
                    cursor.getDouble(8), cursor.getString(9), cursor.getString(10), cursor.getString(11)))
            }
        }
        return result
    }

    fun actualizarEstadoReserva(reservaId: Int, nuevoEstado: String): Boolean {
        require(nuevoEstado in setOf("CONFIRMADA", "LISTA", "COMPLETADA", "CANCELADA", "NO_SHOW")) {
            "Estado de reserva no permitido."
        }
        val db = helper.writableDatabase
        val values = ContentValues().apply { put("estado", nuevoEstado) }
        val updated = db.update(
            "reservas_marketplace", values,
            "id = ? AND ((estado = 'SOLICITADA' AND ? IN ('CONFIRMADA','CANCELADA')) OR " +
                "(estado = 'CONFIRMADA' AND ? IN ('LISTA','COMPLETADA','CANCELADA','NO_SHOW')) OR " +
                "(estado = 'LISTA' AND ? IN ('COMPLETADA','CANCELADA'))) ",
            arrayOf(reservaId.toString(), nuevoEstado, nuevoEstado, nuevoEstado)
        )
        return updated == 1
    }

    fun obtenerEstadoSuscripcion(negocioId: Int): String {
        helper.writableDatabase.execSQL(
            "UPDATE suscripciones_negocio SET estado = 'VENCIDA' WHERE negocio_id = ? AND estado IN ('PRUEBA','ACTIVA') AND fecha_fin IS NOT NULL AND fecha_fin < ?",
            arrayOf(negocioId.toString(), ahora())
        )
        return helper.readableDatabase.rawQuery(
            "SELECT p.nombre || ' · ' || s.estado || COALESCE(' hasta ' || s.fecha_fin, '') FROM suscripciones_negocio s JOIN planes_suscripcion p ON p.id = s.plan_id WHERE s.negocio_id = ? ORDER BY s.id DESC LIMIT 1",
            arrayOf(negocioId.toString())
        ).use { if (it.moveToFirst()) it.getString(0) else "Sin suscripción" }
    }

    fun obtenerPersonalNegocio(negocioId: Int): List<com.example.comunamadsistemacitas.model.PersonalNegocio> {
        val list = mutableListOf<com.example.comunamadsistemacitas.model.PersonalNegocio>()
        helper.readableDatabase.rawQuery(
            "SELECT id, negocio_id, nombre, COALESCE(especialidad, ''), estado FROM personal_negocio WHERE negocio_id = ? ORDER BY id",
            arrayOf(negocioId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(com.example.comunamadsistemacitas.model.PersonalNegocio(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
            }
        }
        return list
    }

    fun agregarPersonalNegocio(negocioId: Int, nombre: String, especialidad: String): Long {
        require(nombre.isNotBlank()) { "Ingresa el nombre del trabajador." }
        val values = ContentValues().apply {
            put("negocio_id", negocioId)
            put("nombre", nombre.trim())
            put("especialidad", especialidad.trim())
            put("estado", "ACTIVO")
        }
        return helper.writableDatabase.insert("personal_negocio", null, values)
    }

    fun cambiarEstadoPersonal(id: Int, nuevoEstado: String): Int {
        val values = ContentValues().apply { put("estado", nuevoEstado) }
        return helper.writableDatabase.update("personal_negocio", values, "id = ?", arrayOf(id.toString()))
    }

    fun obtenerRecursosNegocio(negocioId: Int): List<com.example.comunamadsistemacitas.model.RecursoNegocio> {
        val list = mutableListOf<com.example.comunamadsistemacitas.model.RecursoNegocio>()
        helper.readableDatabase.rawQuery(
            "SELECT id, negocio_id, nombre, tipo, estado FROM recursos_negocio WHERE negocio_id = ? ORDER BY id",
            arrayOf(negocioId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(com.example.comunamadsistemacitas.model.RecursoNegocio(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
            }
        }
        return list
    }

    fun agregarRecursoNegocio(negocioId: Int, nombre: String, tipo: String): Long {
        require(nombre.isNotBlank()) { "Ingresa el nombre del recurso." }
        val values = ContentValues().apply {
            put("negocio_id", negocioId)
            put("nombre", nombre.trim())
            put("tipo", tipo.trim().ifBlank { "SILLA" })
            put("estado", "ACTIVO")
        }
        return helper.writableDatabase.insert("recursos_negocio", null, values)
    }

    fun cambiarEstadoRecurso(id: Int, nuevoEstado: String): Int {
        val values = ContentValues().apply { put("estado", nuevoEstado) }
        return helper.writableDatabase.update("recursos_negocio", values, "id = ?", arrayOf(id.toString()))
    }

    fun actualizarNegocio(
        negocioId: Int,
        nombre: String,
        descripcion: String,
        telefono: String,
        departamento: String,
        provincia: String,
        distrito: String,
        direccion: String
    ): Int {
        require(nombre.isNotBlank()) { "El nombre del negocio no puede estar vacío." }
        val values = ContentValues().apply {
            put("nombre", nombre.trim())
            put("descripcion", descripcion.trim())
            put("telefono", telefono.trim())
            put("departamento", departamento.trim())
            put("provincia", provincia.trim())
            put("distrito", distrito.trim())
            put("direccion", direccion.trim())
            put("fecha_actualizacion", ahora())
        }
        return helper.writableDatabase.update("negocios", values, "id = ?", arrayOf(negocioId.toString()))
    }

    fun actualizarPublicacion(
        id: Int,
        titulo: String,
        descripcion: String,
        precio: Double,
        precioAnterior: Double?,
        duracionMinutos: Int?,
        stock: Int?
    ): Int {
        require(titulo.isNotBlank()) { "El título no puede estar vacío." }
        require(precio >= 0.0) { "El precio no puede ser negativo." }
        val values = ContentValues().apply {
            put("titulo", titulo.trim())
            put("descripcion", descripcion.trim())
            put("precio", precio)
            if (precioAnterior == null) putNull("precio_anterior") else put("precio_anterior", precioAnterior)
            if (duracionMinutos == null) putNull("duracion_minutos") else put("duracion_minutos", duracionMinutos)
            if (stock == null) putNull("stock") else put("stock", stock)
            put("fecha_actualizacion", ahora())
        }
        return helper.writableDatabase.update("publicaciones", values, "id = ?", arrayOf(id.toString()))
    }

    /** Soft-delete / desactivación para preservar reservas históricas existentes. */
    fun desactivarPublicacion(id: Int): Int {
        val values = ContentValues().apply {
            put("activa", 0)
            put("fecha_actualizacion", ahora())
        }
        return helper.writableDatabase.update("publicaciones", values, "id = ?", arrayOf(id.toString()))
    }

    fun actualizarPersonal(id: Int, nombre: String, especialidad: String): Int {
        require(nombre.isNotBlank()) { "El nombre del trabajador no puede estar vacío." }
        val values = ContentValues().apply {
            put("nombre", nombre.trim())
            put("especialidad", especialidad.trim())
        }
        return helper.writableDatabase.update("personal_negocio", values, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarPersonal(id: Int): Int {
        return helper.writableDatabase.delete("personal_negocio", "id = ?", arrayOf(id.toString()))
    }

    fun actualizarRecurso(id: Int, nombre: String, tipo: String): Int {
        require(nombre.isNotBlank()) { "El nombre del recurso no puede estar vacío." }
        val values = ContentValues().apply {
            put("nombre", nombre.trim())
            put("tipo", tipo.trim())
        }
        return helper.writableDatabase.update("recursos_negocio", values, "id = ?", arrayOf(id.toString()))
    }

    fun eliminarRecurso(id: Int): Int {
        return helper.writableDatabase.delete("recursos_negocio", "id = ?", arrayOf(id.toString()))
    }

    fun cerrar() = helper.close()
}
