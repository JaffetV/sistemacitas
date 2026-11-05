package com.example.comunamadsistemacitas

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comunamadsistemacitas.data.local.CitasDbHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifica que las migraciones 6 -> 8 conserven el módulo anterior y creen el marketplace. */
@RunWith(AndroidJUnit4::class)
class CitasDbHelperMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "citas_unamad.db"

    @Before
    fun prepararBaseVersion6() {
        context.deleteDatabase(databaseName)
        val db = context.openOrCreateDatabase(databaseName, 0, null)
        db.execSQL("""CREATE TABLE clientes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            telefono TEXT NOT NULL,
            fecha_registro TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE servicios (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            nombre TEXT NOT NULL,
            emoji TEXT NOT NULL,
            precio_base REAL NOT NULL,
            duracion_minutos INTEGER NOT NULL,
            activo INTEGER DEFAULT 1
        )""")
        db.execSQL("""CREATE TABLE citas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cliente_id INTEGER,
            servicio_id INTEGER,
            cliente TEXT NOT NULL,
            telefono TEXT,
            servicio TEXT NOT NULL,
            precio_cobrado REAL NOT NULL,
            monto_sena REAL DEFAULT 0.0,
            fecha_inicio TEXT NOT NULL,
            fecha_fin TEXT,
            estado TEXT NOT NULL,
            metodo_pago TEXT DEFAULT 'Efectivo',
            duracion_minutos INTEGER DEFAULT 30,
            nota TEXT,
            FOREIGN KEY(cliente_id) REFERENCES clientes(id) ON DELETE SET NULL,
            FOREIGN KEY(servicio_id) REFERENCES servicios(id) ON DELETE SET NULL
        )""")
        db.execSQL("INSERT INTO clientes(nombre, telefono, fecha_registro) VALUES ('Cliente Prueba', '51999999999', '04/11/2026')")
        db.execSQL("INSERT INTO servicios(nombre, emoji, precio_base, duracion_minutos, activo) VALUES ('Servicio Prueba', '✂️', 25, 30, 1)")
        db.execSQL("""INSERT INTO citas(
            cliente_id, servicio_id, cliente, telefono, servicio, precio_cobrado,
            monto_sena, fecha_inicio, fecha_fin, estado, metodo_pago, duracion_minutos, nota
        ) VALUES (1, 1, 'Cliente Prueba', '51999999999', 'Servicio Prueba', 25, 5,
            '05/11/2026 10:00', '05/11/2026 10:30', 'PENDIENTE', 'Efectivo', 30, 'Migración')""")
        db.version = 6
        db.close()
    }

    @After
    fun limpiar() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migracion6a8_conservaDatosYAgregaMarketplace() {
        val helper = CitasDbHelper(context)
        val db = helper.writableDatabase

        assertEquals(8, db.version)
        assertEquals(1, contar(db, "clientes"))
        assertEquals(1, contar(db, "servicios"))
        assertEquals(1, contar(db, "citas"))

        val cursor = db.rawQuery(
            "SELECT nota FROM citas WHERE id = 1", null
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("Migración", cursor.getString(0))
        cursor.close()

        val indices = mutableSetOf<String>()
        val indexCursor = db.rawQuery("PRAGMA index_list('citas')", null)
        while (indexCursor.moveToNext()) indices.add(indexCursor.getString(1))
        indexCursor.close()
        assertTrue(indices.contains("idx_citas_fecha_inicio"))
        assertTrue(indices.contains("idx_citas_estado_fecha"))

        val tablasNuevas = mutableSetOf<String>()
        val tableCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null)
        while (tableCursor.moveToNext()) tablasNuevas.add(tableCursor.getString(0))
        tableCursor.close()
        assertTrue(tablasNuevas.contains("usuarios_marketplace"))
        assertTrue(tablasNuevas.contains("negocios"))
        assertTrue(tablasNuevas.contains("publicaciones"))
        assertTrue(tablasNuevas.contains("reservas_marketplace"))

        val categorias = contar(db, "categorias_negocio")
        assertTrue(categorias >= 6)
        assertEquals(3, contar(db, "planes_suscripcion"))

        helper.close()
    }

    private fun contar(db: SQLiteDatabase, tabla: String): Int {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $tabla", null)
        cursor.moveToFirst()
        val total = cursor.getInt(0)
        cursor.close()
        return total
    }
}
