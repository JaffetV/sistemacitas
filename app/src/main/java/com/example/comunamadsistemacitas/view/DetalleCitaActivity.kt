package com.example.comunamadsistemacitas.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.comunamadsistemacitas.MainActivity
import com.example.comunamadsistemacitas.R
import com.example.comunamadsistemacitas.data.repository.CitaRepository
import com.example.comunamadsistemacitas.model.Cita
import com.example.comunamadsistemacitas.model.EstadoCita
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * DetalleCitaActivity: Gestión avanzada de cita con despliegue de Seña abonada, Saldo pendiente,
 * Semáforo de Confiabilidad del cliente y disparo directo a WhatsApp.
 */
class DetalleCitaActivity : AppCompatActivity() {

    private lateinit var repository: CitaRepository
    private var citaActual: Cita? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_cita)

        repository = CitaRepository(this)

        val btnAtras = findViewById<MaterialButton>(R.id.btnAtras)
        val btnInicio = findViewById<MaterialButton>(R.id.btnInicio)

        val tvDetalleCita = findViewById<TextView>(R.id.tvDetalleCita)
        val btnNotificarWhatsapp = findViewById<MaterialButton>(R.id.btnNotificarWhatsapp)
        val btnAtendido = findViewById<MaterialButton>(R.id.btnAtendido)
        val btnCancelado = findViewById<MaterialButton>(R.id.btnCancelado)
        val btnEliminar = findViewById<MaterialButton>(R.id.btnEliminar)

        val citaId = intent.getIntExtra("CITA_ID", -1)

        // Botón Atrás
        btnAtras?.setOnClickListener {
            finish()
        }

        // Botón Inicio
        btnInicio?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        if (citaId != -1) {
            citaActual = repository.obtenerCitaPorId(citaId)
            val cita = citaActual
            if (cita != null) {
                val semaforo = repository.obtenerSemaforoCliente(cita.cliente)
                val telefonoMostrar = if (cita.telefono.isNotBlank()) cita.telefono else "No especificado"
                val senaMostrar = String.format(Locale.getDefault(), "S/ %.2f", cita.montoSena)
                val saldoMostrar = String.format(Locale.getDefault(), "S/ %.2f", cita.saldoPendiente)

                tvDetalleCita?.text = """
                    👤 Cliente: ${cita.cliente}
                    ${semaforo.etiqueta}
                    📞 Teléfono: $telefonoMostrar
                    🏷️ Servicio: ${cita.servicio}
                    💰 Precio Total: S/ ${String.format(Locale.getDefault(), "%.2f", cita.precio)}
                    💳 Seña Abonada: $senaMostrar
                    💵 Saldo Pendiente: $saldoMostrar
                    📅 Fecha y Hora: ${cita.fechaHora}
                    📌 Estado Actual: ${cita.estado.name}
                    💳 Método de Cobro: ${cita.metodoPago}
                """.trimIndent()

                // Transición dinámica de estados en UI
                if (cita.estado == EstadoCita.ATENDIDO || cita.estado == EstadoCita.CANCELADO || cita.estado == EstadoCita.NO_SHOW) {
                    btnAtendido?.text = "REABRIR CITA (PENDIENTE)"
                }
            } else {
                tvDetalleCita?.text = "No se encontraron los datos de la cita."
            }
        } else {
            tvDetalleCita?.text = "ID de cita no válido."
        }

        // Acción: Avisar por WhatsApp con Deep Linking Directo (whatsapp://send?phone=...)
        btnNotificarWhatsapp?.setOnClickListener {
            val cita = citaActual
            if (cita != null) {
                val infoSena = if (cita.montoSena > 0.0) {
                    "Seña abonada: S/ ${String.format(Locale.getDefault(), "%.2f", cita.montoSena)}. Saldo restante: S/ ${String.format(Locale.getDefault(), "%.2f", cita.saldoPendiente)}."
                } else {
                    "Total a pagar: S/ ${String.format(Locale.getDefault(), "%.2f", cita.precio)}."
                }

                val plantillaMensaje = when (cita.estado) {
                    EstadoCita.PENDIENTE -> """
                        Hola *${cita.cliente}*, te recordamos tu cita de *${cita.servicio}* el día *${cita.fechaHora}*.
                        $infoSena
                        📌 *Política:* Puedes cancelar o solicitar reagendamiento con al menos 4 horas de anticipación respondiendo a este mensaje. ¡Te esperamos!
                    """.trimIndent()
                    EstadoCita.ATENDIDO -> "Hola *${cita.cliente}*, ¡muchas gracias por tu visita! Tu atención de *${cita.servicio}* ha sido registrada con éxito. ¡Esperamos verte pronto!"
                    EstadoCita.CANCELADO, EstadoCita.NO_SHOW -> "Hola *${cita.cliente}*, tu reserva de *${cita.servicio}* del *${cita.fechaHora}* fue registrada como CANCELADA / NO ASISTIÓ. Si deseas reagendar, avísanos."
                }

                val numeroRaw = cita.telefono.replace("[^0-9]".toRegex(), "")
                val numCompleto = if (numeroRaw.startsWith("51")) numeroRaw else "51$numeroRaw"

                val intentWhatsapp = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("whatsapp://send?phone=$numCompleto&text=${Uri.encode(plantillaMensaje)}")
                }

                try {
                    startActivity(intentWhatsapp)
                } catch (e: Exception) {
                    try {
                        val intentWeb = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?phone=$numCompleto&text=${Uri.encode(plantillaMensaje)}")
                        }
                        startActivity(intentWeb)
                    } catch (ex: Exception) {
                        Toast.makeText(this, "No se pudo abrir la aplicación de WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "No hay datos de la cita para enviar", Toast.LENGTH_SHORT).show()
            }
        }

        // Acción Primaria: Marcar como Atendido / Reabrir cita con Modal de Método de Pago
        btnAtendido?.setOnClickListener {
            val cita = citaActual
            if (citaId != -1 && cita != null) {
                if (cita.estado == EstadoCita.ATENDIDO || cita.estado == EstadoCita.CANCELADO || cita.estado == EstadoCita.NO_SHOW) {
                    // Reabrir cita
                    repository.actualizarEstadoYMetodoPago(citaId, EstadoCita.PENDIENTE, "Pendiente de cobro")
                    Toast.makeText(this, "Cita reabierta a PENDIENTE", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Modal de Selección de Método de Pago para Arqueo de Caja
                    val metodosPago = arrayOf("Efectivo", "Yape / Plin", "Transferencia Bancaria", "Tarjeta Débito / Crédito")
                    AlertDialog.Builder(this)
                        .setTitle("Seleccionar Método de Cobro")
                        .setMessage("Saldo a cobrar: S/ ${String.format(Locale.getDefault(), "%.2f", cita.saldoPendiente)}")
                        .setItems(metodosPago) { _, which ->
                            val metodoSeleccionado = metodosPago[which]
                            repository.actualizarEstadoYMetodoPago(citaId, EstadoCita.ATENDIDO, metodoSeleccionado)
                            Toast.makeText(this, "Cita ATENDIDA (Pago: $metodoSeleccionado)", Toast.LENGTH_SHORT).show()

                            enviarAgradecimientoWhatsapp(cita.cliente, cita.telefono, cita.servicio)
                            finish()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }

        // Acción Secundaria Neutra: Cancelar Cita (UPDATE)
        btnCancelado?.setOnClickListener {
            if (citaId != -1) {
                repository.actualizarEstadoCita(citaId, EstadoCita.CANCELADO)
                Toast.makeText(this, "Estado actualizado: CANCELADO", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Acción Alerta Destructiva: Eliminar Cita (DELETE)
        btnEliminar?.setOnClickListener {
            val cita = citaActual
            if (citaId != -1) {
                val nombreCliente = cita?.cliente ?: "esta cita"
                AlertDialog.Builder(this)
                    .setTitle("Confirmar eliminación")
                    .setMessage("¿Deseas eliminar la cita de $nombreCliente? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar cita") { _, _ ->
                        repository.eliminarCita(citaId)
                        Toast.makeText(this, "Cita eliminada correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun enviarAgradecimientoWhatsapp(cliente: String, telefono: String, servicio: String) {
        val mensaje = "Hola *$cliente*, ¡muchas gracias por atenderte con nosotros en *$servicio*! Esperamos que tu experiencia haya sido excelente. ¡Hasta pronto!"
        val numeroRaw = telefono.replace("[^0-9]".toRegex(), "")
        val numCompleto = if (numeroRaw.startsWith("51")) numeroRaw else "51$numeroRaw"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?phone=$numCompleto&text=${Uri.encode(mensaje)}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Ignorar si el usuario no desea abrir WhatsApp tras cobrar
        }
    }
}
