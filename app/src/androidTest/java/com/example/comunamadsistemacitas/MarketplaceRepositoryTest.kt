package com.example.comunamadsistemacitas

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comunamadsistemacitas.data.repository.MarketplaceRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Prueba el flujo base: alta de negocio, publicación, reserva y bloqueo de doble cita. */
@RunWith(AndroidJUnit4::class)
class MarketplaceRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var repository: MarketplaceRepository
    private var negocioId: Int = -1

    @Before
    fun preparar() {
        context.deleteDatabase("citas_unamad.db")
        repository = MarketplaceRepository(context)
        val categoria = repository.obtenerCategorias().first()
        negocioId = repository.registrarNegocio(
            propietarioNombre = "Responsable de prueba",
            propietarioEmail = "responsable@test.local",
            propietarioTelefono = "999888777",
            nombre = "Negocio de prueba",
            categoriaId = categoria.id,
            descripcion = "Prueba de marketplace",
            departamento = "Madre de Dios",
            provincia = "Tambopata",
            distrito = "Tambopata",
            direccion = "Av. de prueba 123",
            modalidades = setOf("CITA", "RECOJO")
        )
    }

    @After
    fun limpiar() {
        repository.cerrar()
        context.deleteDatabase("citas_unamad.db")
    }

    @Test
    fun altaPublicacionYReserva_seGuardanYNoPermitenCruceDeHorario() {
        val publicacionId = repository.crearPublicacion(
            negocioId = negocioId,
            titulo = "Corte clásico",
            descripcion = "Servicio de prueba",
            tipo = "SERVICIO",
            modalidad = "CITA",
            precio = 25.0,
            precioAnterior = null,
            duracionMinutos = 30
        )
        val publicacion = repository.obtenerPublicaciones(negocioId).first { it.id == publicacionId.toInt() }
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) }
        val hora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)

        val reserva = repository.crearReserva(
            negocioId = negocioId,
            publicacion = publicacion,
            nombreCliente = "Cliente de prueba",
            telefonoCliente = "988777666",
            emailCliente = "cliente@test.local",
            modalidad = "CITA",
            fechaHora = hora,
            cantidad = 1,
            modalidadPago = "EN_LOCAL",
            nota = "Prueba de extremo a extremo"
        )

        assertTrue(reserva.id > 0)
        assertEquals("SOLICITADA", reserva.estado)
        assertEquals(1, repository.obtenerReservasNegocio(negocioId).size)

        var solapamientoRechazado = false
        try {
            repository.crearReserva(
                negocioId, publicacion, "Otro cliente", "977666555", "otro@test.local",
                "CITA", hora, 1, "EN_LOCAL", "Debe ser rechazado"
            )
        } catch (_: IllegalArgumentException) {
            solapamientoRechazado = true
        }
        assertTrue("El sistema debe rechazar dos citas que se cruzan", solapamientoRechazado)
        assertEquals(1, repository.obtenerReservasNegocio(negocioId).size)
    }
}
