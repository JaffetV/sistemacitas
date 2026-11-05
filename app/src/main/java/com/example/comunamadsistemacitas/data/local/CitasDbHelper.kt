package com.example.comunamadsistemacitas.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base local de la app.
 * Conserva intactas las tablas operativas anteriores (clientes, servicios y citas)
 * y agrega el esquema marketplace en la migración 7 -> 8.
 */
class CitasDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "citas_unamad.db"
        private const val DATABASE_VERSION = 8

        const val TABLE_CLIENTES = "clientes"
        const val COLUMN_CLIENTE_ID = "id"
        const val COLUMN_CLIENTE_NOMBRE = "nombre"
        const val COLUMN_CLIENTE_TELEFONO = "telefono"
        const val COLUMN_CLIENTE_FECHA_REGISTRO = "fecha_registro"

        const val TABLE_SERVICIOS = "servicios"
        const val COLUMN_SERVICIO_ID = "id"
        const val COLUMN_SERVICIO_NOMBRE = "nombre"
        const val COLUMN_SERVICIO_EMOJI = "emoji"
        const val COLUMN_SERVICIO_PRECIO_BASE = "precio_base"
        const val COLUMN_SERVICIO_DURACION = "duracion_minutos"
        const val COLUMN_SERVICIO_ACTIVO = "activo"

        const val TABLE_CITAS = "citas"
        const val COLUMN_ID = "id"
        const val COLUMN_FK_CLIENTE_ID = "cliente_id"
        const val COLUMN_FK_SERVICIO_ID = "servicio_id"
        const val COLUMN_CLIENTE = "cliente"
        const val COLUMN_TELEFONO = "telefono"
        const val COLUMN_SERVICIO = "servicio"
        const val COLUMN_PRECIO = "precio_cobrado"
        const val COLUMN_MONTO_SENA = "monto_sena"
        const val COLUMN_FECHA_HORA = "fecha_inicio"
        const val COLUMN_FECHA_FIN = "fecha_fin"
        const val COLUMN_ESTADO = "estado"
        const val COLUMN_METODO_PAGO = "metodo_pago"
        const val COLUMN_DURACION_MINUTOS = "duracion_minutos"
        const val COLUMN_NOTA = "nota"

        const val TABLE_USUARIOS = "usuarios_marketplace"
        const val TABLE_CATEGORIAS_NEGOCIO = "categorias_negocio"
        const val TABLE_NEGOCIOS = "negocios"
        const val TABLE_NEGOCIO_MODALIDADES = "negocio_modalidades"
        const val TABLE_PLANES = "planes_suscripcion"
        const val TABLE_SUSCRIPCIONES = "suscripciones_negocio"
        const val TABLE_PUBLICACIONES = "publicaciones"
        const val TABLE_RESERVAS = "reservas_marketplace"
        const val TABLE_PAGOS = "pagos_marketplace"
        const val TABLE_FAVORITOS = "favoritos_negocio"
        const val TABLE_HORARIOS = "horarios_negocio"
    }

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        requireNotNull(db) { "No se pudo crear la base de datos." }
        crearEsquemaOperativo(db)
        crearIndicesOperativos(db)
        crearEsquemaMarketplace(db)
        poblarServiciosIniciales(db)
        poblarCatalogosMarketplace(db)
    }

    private fun crearEsquemaOperativo(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CLIENTES (
                $COLUMN_CLIENTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CLIENTE_NOMBRE TEXT NOT NULL,
                $COLUMN_CLIENTE_TELEFONO TEXT NOT NULL,
                $COLUMN_CLIENTE_FECHA_REGISTRO TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE $TABLE_SERVICIOS (
                $COLUMN_SERVICIO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SERVICIO_NOMBRE TEXT NOT NULL,
                $COLUMN_SERVICIO_EMOJI TEXT NOT NULL,
                $COLUMN_SERVICIO_PRECIO_BASE REAL NOT NULL,
                $COLUMN_SERVICIO_DURACION INTEGER NOT NULL,
                $COLUMN_SERVICIO_ACTIVO INTEGER DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE $TABLE_CITAS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FK_CLIENTE_ID INTEGER,
                $COLUMN_FK_SERVICIO_ID INTEGER,
                $COLUMN_CLIENTE TEXT NOT NULL,
                $COLUMN_TELEFONO TEXT,
                $COLUMN_SERVICIO TEXT NOT NULL,
                $COLUMN_PRECIO REAL NOT NULL,
                $COLUMN_MONTO_SENA REAL DEFAULT 0.0,
                $COLUMN_FECHA_HORA TEXT NOT NULL,
                $COLUMN_FECHA_FIN TEXT,
                $COLUMN_ESTADO TEXT NOT NULL,
                $COLUMN_METODO_PAGO TEXT DEFAULT 'Efectivo',
                $COLUMN_DURACION_MINUTOS INTEGER DEFAULT 30,
                $COLUMN_NOTA TEXT,
                FOREIGN KEY ($COLUMN_FK_CLIENTE_ID) REFERENCES $TABLE_CLIENTES($COLUMN_CLIENTE_ID) ON DELETE SET NULL,
                FOREIGN KEY ($COLUMN_FK_SERVICIO_ID) REFERENCES $TABLE_SERVICIOS($COLUMN_SERVICIO_ID) ON DELETE SET NULL
            )
        """.trimIndent())
    }

    private fun crearIndicesOperativos(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_citas_fecha_inicio ON $TABLE_CITAS($COLUMN_FECHA_HORA)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_citas_estado_fecha ON $TABLE_CITAS($COLUMN_ESTADO, $COLUMN_FECHA_HORA)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_citas_cliente_id ON $TABLE_CITAS($COLUMN_FK_CLIENTE_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_citas_servicio_id ON $TABLE_CITAS($COLUMN_FK_SERVICIO_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_clientes_telefono ON $TABLE_CLIENTES($COLUMN_CLIENTE_TELEFONO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_servicios_activo ON $TABLE_SERVICIOS($COLUMN_SERVICIO_ACTIVO)")
    }

    /** Modelo marketplace: cuentas gratuitas, negocios, suscripciones, publicaciones y reservas. */
    private fun crearEsquemaMarketplace(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_USUARIOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                email TEXT COLLATE NOCASE UNIQUE,
                telefono TEXT NOT NULL,
                tipo_cuenta TEXT NOT NULL DEFAULT 'GRATUITA' CHECK(tipo_cuenta IN ('GRATUITA','ADMIN')),
                estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO','BLOQUEADO')),
                fecha_registro TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS_NEGOCIO (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE,
                slug TEXT NOT NULL UNIQUE,
                emoji TEXT NOT NULL DEFAULT '🏪',
                activa INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_NEGOCIOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                propietario_usuario_id INTEGER NOT NULL,
                categoria_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                descripcion TEXT DEFAULT '',
                telefono TEXT NOT NULL,
                email_contacto TEXT,
                pais_codigo TEXT NOT NULL DEFAULT 'PE',
                departamento TEXT NOT NULL DEFAULT '',
                provincia TEXT NOT NULL DEFAULT '',
                distrito TEXT NOT NULL DEFAULT '',
                direccion TEXT NOT NULL DEFAULT '',
                referencia TEXT DEFAULT '',
                latitud REAL,
                longitud REAL,
                cobertura TEXT NOT NULL DEFAULT 'LOCAL' CHECK(cobertura IN ('LOCAL')),
                estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO','PAUSADO','SUSPENDIDO')),
                visible_marketplace INTEGER NOT NULL DEFAULT 1,
                fecha_creacion TEXT NOT NULL,
                fecha_actualizacion TEXT NOT NULL,
                FOREIGN KEY(propietario_usuario_id) REFERENCES $TABLE_USUARIOS(id) ON DELETE RESTRICT,
                FOREIGN KEY(categoria_id) REFERENCES $TABLE_CATEGORIAS_NEGOCIO(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_NEGOCIO_MODALIDADES (
                negocio_id INTEGER NOT NULL,
                modalidad TEXT NOT NULL CHECK(modalidad IN ('CITA','RECOJO')),
                PRIMARY KEY(negocio_id, modalidad),
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS personal_negocio (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                negocio_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                especialidad TEXT,
                estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO','INACTIVO')),
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS recursos_negocio (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                negocio_id INTEGER NOT NULL,
                nombre TEXT NOT NULL,
                tipo TEXT NOT NULL DEFAULT 'SILLA',
                estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO','INACTIVO')),
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PLANES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT NOT NULL UNIQUE,
                nombre TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                precio_mensual REAL,
                dias_prueba INTEGER NOT NULL DEFAULT 0,
                activo INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SUSCRIPCIONES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                negocio_id INTEGER NOT NULL,
                plan_id INTEGER NOT NULL,
                estado TEXT NOT NULL CHECK(estado IN ('PRUEBA','ACTIVA','PENDIENTE_PAGO','VENCIDA','SUSPENDIDA','CANCELADA')),
                fecha_inicio TEXT NOT NULL,
                fecha_fin TEXT,
                renovacion_automatica INTEGER NOT NULL DEFAULT 0,
                precio_acordado REAL,
                fecha_creacion TEXT NOT NULL,
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE,
                FOREIGN KEY(plan_id) REFERENCES $TABLE_PLANES(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PUBLICACIONES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                negocio_id INTEGER NOT NULL,
                titulo TEXT NOT NULL,
                descripcion TEXT DEFAULT '',
                tipo TEXT NOT NULL CHECK(tipo IN ('SERVICIO','PRODUCTO')),
                modalidad TEXT NOT NULL CHECK(modalidad IN ('CITA','RECOJO','AMBOS')),
                precio REAL NOT NULL CHECK(precio >= 0),
                precio_anterior REAL,
                duracion_minutos INTEGER,
                stock INTEGER,
                activa INTEGER NOT NULL DEFAULT 1,
                fecha_creacion TEXT NOT NULL,
                fecha_actualizacion TEXT NOT NULL,
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_RESERVAS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                codigo TEXT NOT NULL UNIQUE,
                negocio_id INTEGER NOT NULL,
                publicacion_id INTEGER NOT NULL,
                usuario_id INTEGER NOT NULL,
                modalidad TEXT NOT NULL CHECK(modalidad IN ('CITA','RECOJO')),
                fecha_inicio TEXT,
                fecha_fin TEXT,
                fecha_recogida TEXT,
                cantidad INTEGER NOT NULL DEFAULT 1 CHECK(cantidad > 0),
                precio_unitario REAL NOT NULL,
                total REAL NOT NULL,
                estado TEXT NOT NULL DEFAULT 'SOLICITADA' CHECK(estado IN ('SOLICITADA','CONFIRMADA','LISTA','COMPLETADA','CANCELADA','NO_SHOW')),
                modalidad_pago TEXT NOT NULL CHECK(modalidad_pago IN ('EN_LINEA','EN_LOCAL')),
                estado_pago TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK(estado_pago IN ('PENDIENTE','PAGADO','RECHAZADO','REEMBOLSADO')),
                nota TEXT DEFAULT '',
                fecha_creacion TEXT NOT NULL,
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE RESTRICT,
                FOREIGN KEY(publicacion_id) REFERENCES $TABLE_PUBLICACIONES(id) ON DELETE RESTRICT,
                FOREIGN KEY(usuario_id) REFERENCES $TABLE_USUARIOS(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_PAGOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER NOT NULL,
                reserva_id INTEGER,
                suscripcion_id INTEGER,
                concepto TEXT NOT NULL CHECK(concepto IN ('RESERVA','SUSCRIPCION')),
                monto REAL NOT NULL CHECK(monto >= 0),
                moneda TEXT NOT NULL DEFAULT 'PEN',
                canal TEXT NOT NULL CHECK(canal IN ('EN_LINEA','EN_LOCAL')),
                metodo TEXT,
                estado TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE','APROBADO','RECHAZADO','REEMBOLSADO')),
                proveedor TEXT,
                referencia_externa TEXT,
                fecha_creacion TEXT NOT NULL,
                CHECK((reserva_id IS NOT NULL AND suscripcion_id IS NULL AND concepto = 'RESERVA') OR
                      (reserva_id IS NULL AND suscripcion_id IS NOT NULL AND concepto = 'SUSCRIPCION')),
                FOREIGN KEY(usuario_id) REFERENCES $TABLE_USUARIOS(id) ON DELETE RESTRICT,
                FOREIGN KEY(reserva_id) REFERENCES $TABLE_RESERVAS(id) ON DELETE RESTRICT,
                FOREIGN KEY(suscripcion_id) REFERENCES $TABLE_SUSCRIPCIONES(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITOS (
                usuario_id INTEGER NOT NULL,
                negocio_id INTEGER NOT NULL,
                fecha_creacion TEXT NOT NULL,
                PRIMARY KEY(usuario_id, negocio_id),
                FOREIGN KEY(usuario_id) REFERENCES $TABLE_USUARIOS(id) ON DELETE CASCADE,
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_HORARIOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                negocio_id INTEGER NOT NULL,
                dia_semana INTEGER NOT NULL CHECK(dia_semana BETWEEN 1 AND 7),
                hora_inicio TEXT NOT NULL,
                hora_fin TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1,
                UNIQUE(negocio_id, dia_semana, hora_inicio, hora_fin),
                FOREIGN KEY(negocio_id) REFERENCES $TABLE_NEGOCIOS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_negocios_categoria_estado ON $TABLE_NEGOCIOS(categoria_id, estado, visible_marketplace)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_negocios_ubicacion ON $TABLE_NEGOCIOS(departamento, provincia, distrito)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_negocios_propietario ON $TABLE_NEGOCIOS(propietario_usuario_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_suscripciones_negocio_estado ON $TABLE_SUSCRIPCIONES(negocio_id, estado, fecha_fin)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_publicaciones_negocio_activa ON $TABLE_PUBLICACIONES(negocio_id, activa)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reservas_negocio_fecha ON $TABLE_RESERVAS(negocio_id, fecha_inicio, fecha_recogida)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reservas_usuario_fecha ON $TABLE_RESERVAS(usuario_id, fecha_creacion)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pagos_reserva ON $TABLE_PAGOS(reserva_id)")
    }

    private fun poblarCatalogosMarketplace(db: SQLiteDatabase) {
        val categorias = listOf(
            Triple("Barbería y peluquería", "barberia-peluqueria", "💈"),
            Triple("Belleza y uñas", "belleza-unas", "💅"),
            Triple("Salud y bienestar", "salud-bienestar", "🩺"),
            Triple("Comida y restaurantes", "comida-restaurantes", "🍽️"),
            Triple("Talleres y mantenimiento", "talleres-mantenimiento", "🛠️"),
            Triple("Tiendas y productos", "tiendas-productos", "🛍️"),
            Triple("Educación y asesorías", "educacion-asesorias", "📚"),
            Triple("Otros negocios", "otros", "🏪")
        )
        categorias.forEach { (nombre, slug, emoji) ->
            val values = ContentValues().apply {
                put("nombre", nombre); put("slug", slug); put("emoji", emoji); put("activa", 1)
            }
            db.insertWithOnConflict(TABLE_CATEGORIAS_NEGOCIO, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }

        val planes = listOf(
            arrayOf("PRUEBA", "Prueba", "Acceso inicial para configurar y probar el negocio", null, 14),
            arrayOf("BASICO", "Básico", "Plan comercial para gestionar publicaciones y reservas", null, 0),
            arrayOf("PRO", "Profesional", "Plan ampliado para negocios con mayor volumen", null, 0)
        )
        planes.forEach { plan ->
            val values = ContentValues().apply {
                put("codigo", plan[0] as String)
                put("nombre", plan[1] as String)
                put("descripcion", plan[2] as String)
                val precio = plan[3] as Double?
                if (precio == null) putNull("precio_mensual") else put("precio_mensual", precio)
                put("dias_prueba", plan[4] as Int)
                put("activo", 1)
            }
            db.insertWithOnConflict(TABLE_PLANES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun poblarServiciosIniciales(db: SQLiteDatabase?) {
        val serviciosIniciales = arrayOf(
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Corte de Cabello', '✂️', 25.0, 30, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Barbería / Barba', '🧔', 20.0, 20, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Manicura / Pedicura', '💅', 35.0, 45, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Tinte / Colorimetría', '🎨', 90.0, 90, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Tratamiento / Masaje', '💆', 70.0, 60, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Consulta Médica General', '🩺', 50.0, 30, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Atención Odontológica', '🦷', 80.0, 45, 1)",
            "INSERT INTO $TABLE_SERVICIOS ($COLUMN_SERVICIO_NOMBRE, $COLUMN_SERVICIO_EMOJI, $COLUMN_SERVICIO_PRECIO_BASE, $COLUMN_SERVICIO_DURACION, $COLUMN_SERVICIO_ACTIVO) VALUES ('Mantenimiento General', '🛠️', 60.0, 60, 1)"
        )
        for (query in serviciosIniciales) db?.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        requireNotNull(db) { "No se pudo abrir la base de datos para migrarla." }
        if (oldVersion < 6) {
            throw IllegalStateException("La base de datos está en versión $oldVersion. No existe una migración verificada desde esa versión; se detuvo la actualización para evitar pérdida de datos.")
        }
        if (oldVersion < 7 && newVersion >= 7) crearIndicesOperativos(db)
        if (oldVersion < 8 && newVersion >= 8) {
            crearEsquemaMarketplace(db)
            poblarCatalogosMarketplace(db)
        }
        if (newVersion > DATABASE_VERSION) throw IllegalStateException("La versión de base de datos $newVersion no está soportada.")
    }

    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw IllegalStateException("No se permite degradar la base de datos de $oldVersion a $newVersion porque podría dejar el esquema incompatible.")
    }
}
