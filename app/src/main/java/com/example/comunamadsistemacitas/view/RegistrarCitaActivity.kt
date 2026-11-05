package com.example.comunamadsistemacitas.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * RegistrarCitaActivity: Conexión relacional con tablas Clientes, Servicios y Citas,
 * sanitización E.164 para WhatsApp, validación por rango completo e interfaz responsiva.
 */
class RegistrarCitaActivity : AppCompatActivity() {

    private lateinit var repository: CitaRepository
    private var duracionMinutosSeleccionada = 30
    private var fechaBaseSeleccionada = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_cita)

        repository = CitaRepository(this)

        val btnAtras = findViewById<MaterialButton>(R.id.btnAtras)
        val btnInicio = findViewById<MaterialButton>(R.id.btnInicio)

        val tilCliente = findViewById<TextInputLayout>(R.id.tilCliente)
        val tilServicio = findViewById<TextInputLayout>(R.id.tilServicio)
        val tilPrecio = findViewById<TextInputLayout>(R.id.tilPrecio)
        val tilFechaHora = findViewById<TextInputLayout>(R.id.tilFechaHora)

        val etCliente = findViewById<AutoCompleteTextView>(R.id.etCliente)
        val tvSemaforoCliente = findViewById<TextView>(R.id.tvSemaforoCliente)
        val etTelefono = findViewById<TextInputEditText>(R.id.etTelefono)
        val etServicio = findViewById<AutoCompleteTextView>(R.id.etServicio)
        val etPrecio = findViewById<TextInputEditText>(R.id.etPrecio)
        val etMontoSena = findViewById<TextInputEditText>(R.id.etMontoSena)
        val etFechaHora = findViewById<TextInputEditText>(R.id.etFechaHora)

        val chipDia1 = findViewById<Chip>(R.id.chipDia1)
        val chipDia2 = findViewById<Chip>(R.id.chipDia2)
        val chipDia3 = findViewById<Chip>(R.id.chipDia3)
        val chipDia4 = findViewById<Chip>(R.id.chipDia4)
        val chipDia5 = findViewById<Chip>(R.id.chipDia5)

        val tvEstimacionFin = findViewById<TextView>(R.id.tvEstimacionFin)
        val tvAlertaConflicto = findViewById<TextView>(R.id.tvAlertaConflicto)

        val cbNotificarWhatsapp = findViewById<MaterialCheckBox>(R.id.cbNotificarWhatsapp)
        val etMensajeWhatsappPreview = findViewById<TextInputEditText>(R.id.etMensajeWhatsappPreview)
        val btnGuardar = findViewById<MaterialButton>(R.id.btnGuardar)

        val tvPreviewCliente = findViewById<TextView>(R.id.tvPreviewCliente)
        val tvPreviewServicio = findViewById<TextView>(R.id.tvPreviewServicio)
        val tvPreviewCobro = findViewById<TextView>(R.id.tvPreviewCobro)

        // Botón Atrás
        btnAtras?.setOnClickListener { finish() }

        // Botón Inicio
        btnInicio?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Directorio de clientes relacional
        val directorioClientes = repository.obtenerDirectorioClientes()
        val nombresClientes = directorioClientes.map { it.nombre }.toTypedArray()
        val adapterClientes = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresClientes)
        etCliente?.setAdapter(adapterClientes)

        val actualizarSemaforoCliente = { nombre: String ->
            if (nombre.isNotBlank()) {
                val semaforo = repository.obtenerSemaforoCliente(nombre)
                tvSemaforoCliente?.text = "${semaforo.etiqueta}\n💡 ${semaforo.recomendacion}"
                when (semaforo.nivel) {
                    "ROJO" -> tvSemaforoCliente?.setBackgroundColor(0xFFFFEBEE.toInt())
                    "AMARILLO" -> tvSemaforoCliente?.setBackgroundColor(0xFFFFF8E1.toInt())
                    "VERDE" -> tvSemaforoCliente?.setBackgroundColor(0xFFE8F5E9.toInt())
                    else -> tvSemaforoCliente?.setBackgroundColor(0xFFF5F5F5.toInt())
                }
            } else {
                tvSemaforoCliente?.text = "⚪ Ingrese el cliente para evaluar su confiabilidad"
                tvSemaforoCliente?.setBackgroundColor(0xFFF5F5F5.toInt())
            }
        }

        etCliente?.setOnItemClickListener { _, _, position, _ ->
            val clienteSeleccionado = nombresClientes.getOrNull(position)
            if (clienteSeleccionado != null) {
                actualizarSemaforoCliente(clienteSeleccionado)
                val match = directorioClientes.find { it.nombre == clienteSeleccionado }
                if (match != null && match.telefono.isNotEmpty()) {
                    etTelefono?.setText(match.telefono)
                }
            }
        }

        // Actualización de Vista Previa en Tiempo Real y Plantilla WhatsApp
        val actualizarVistaPreviaYPlantilla = {
            val cliente = etCliente?.text?.toString()?.trim().orEmpty().ifEmpty { "(Por completar)" }
            val telefono = etTelefono?.text?.toString()?.trim().orEmpty()
            val servicio = etServicio?.text?.toString()?.trim().orEmpty().ifEmpty { "(Sin seleccionar)" }
            val precio = etPrecio?.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val sena = etMontoSena?.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            val fechaHora = etFechaHora?.text?.toString()?.trim().orEmpty().ifEmpty { "(Sin definir)" }
            val saldo = (precio - sena).coerceAtLeast(0.0)

            tvPreviewCliente?.text = "Cliente: $cliente ${if (telefono.isNotBlank()) "($telefono)" else ""}"
            tvPreviewServicio?.text = "Servicio: $servicio ($duracionMinutosSeleccionada min)"
            tvPreviewCobro?.text = String.format(Locale.getDefault(), "Total: S/ %.2f | Seña: S/ %.2f | Saldo: S/ %.2f", precio, sena, saldo)

            // Plantilla de WhatsApp
            val infoSenaMsg = if (sena > 0.0) {
                "Seña abonada: S/ ${String.format(Locale.getDefault(), "%.2f", sena)}. Saldo restante: S/ ${String.format(Locale.getDefault(), "%.2f", saldo)}."
            } else {
                "Total a pagar: S/ ${String.format(Locale.getDefault(), "%.2f", precio)}."
            }

            val plantillaEdit = """
                Hola *$cliente*, confirmamos tu cita de *$servicio* para el *$fechaHora*.
                $infoSenaMsg
                📌 *Política:* Puedes cancelar o solicitar reagendamiento con al menos 4 horas de anticipación respondiendo a este mensaje. ¡Te esperamos!
            """.trimIndent()

            etMensajeWhatsappPreview?.setText(plantillaEdit)

            // Detección de traslape por rango en tiempo real
            if (fechaHora != "(Sin definir)" && repository.existeTraslapeHorario(fechaHora, duracionMinutosSeleccionada)) {
                tvAlertaConflicto?.visibility = View.VISIBLE
                tvAlertaConflicto?.text = "⚠️ Alerta de Traslape: El rango asignado para $fechaHora se cruza con otra reserva activa."
            } else {
                tvAlertaConflicto?.visibility = View.GONE
            }
        }

        etCliente?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarSemaforoCliente(s?.toString() ?: "")
                actualizarVistaPreviaYPlantilla()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etTelefono?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actualizarVistaPreviaYPlantilla() }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPrecio?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actualizarVistaPreviaYPlantilla() }
            override fun afterTextChanged(s: Editable?) {}
        })

        etMontoSena?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actualizarVistaPreviaYPlantilla() }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Selector de Servicio u Oficio relacional
        val serviciosLista = repository.obtenerServiciosActivos()
        val nombresServicios = serviciosLista.map { "${it.emoji} ${it.nombre} (${it.duracionMinutos} min)" }.toTypedArray()
        val adapterServicios = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresServicios)
        etServicio?.setAdapter(adapterServicios)
        etServicio?.setOnItemClickListener { _, _, position, _ ->
            val servicioObj = serviciosLista.getOrNull(position)
            if (servicioObj != null) {
                if (etPrecio?.text.isNullOrEmpty()) {
                    etPrecio?.setText(servicioObj.precioBase.toString())
                }
                duracionMinutosSeleccionada = servicioObj.duracionMinutos
            }
            calcularHoraEstimadaFin(etFechaHora?.text?.toString().orEmpty(), tvEstimacionFin)
            actualizarVistaPreviaYPlantilla()
        }

        // Tira Horizontal de Días Abiertos/Cerrados
        val configurarTiraDias = {
            val chipsDias = arrayOf(chipDia1, chipDia2, chipDia3, chipDia4, chipDia5)
            val sdfLabel = SimpleDateFormat("EEE dd/MM", Locale.getDefault())
            val sdfValue = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            for (i in 0..4) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, i)
                val chip = chipsDias.getOrNull(i)
                val fechaVal = sdfValue.format(cal.time)
                val label = sdfLabel.format(cal.time)

                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    chip?.text = "🔒 $label (CERRADO)"
                    chip?.setOnClickListener {
                        Toast.makeText(this, "⚠️ El local permanece CERRADO los domingos.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    chip?.text = "🟢 $label"
                    chip?.setOnClickListener {
                        fechaBaseSeleccionada = fechaVal
                        val horaActual = etFechaHora?.text?.toString()?.split(" ")?.getOrNull(1) ?: "10:00"
                        val fhCompleta = "$fechaBaseSeleccionada $horaActual"
                        etFechaHora?.setText(fhCompleta)
                        calcularHoraEstimadaFin(fhCompleta, tvEstimacionFin)
                        actualizarVistaPreviaYPlantilla()
                    }
                }
            }
        }
        configurarTiraDias()

        // Selector de Bloques de Horarios por Chips
        val asignarHoraBloque = { horaBloque: String ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            if (fechaBaseSeleccionada.isBlank()) {
                fechaBaseSeleccionada = sdf.format(Calendar.getInstance().time)
            }
            val fechaHoraBloque = "$fechaBaseSeleccionada $horaBloque"
            etFechaHora?.setText(fechaHoraBloque)
            calcularHoraEstimadaFin(fechaHoraBloque, tvEstimacionFin)
            actualizarVistaPreviaYPlantilla()
        }

        findViewById<Chip>(R.id.chip0900)?.setOnClickListener { asignarHoraBloque("09:00") }
        findViewById<Chip>(R.id.chip1000)?.setOnClickListener { asignarHoraBloque("10:00") }
        findViewById<Chip>(R.id.chip1100)?.setOnClickListener { asignarHoraBloque("11:00") }
        findViewById<Chip>(R.id.chip1400)?.setOnClickListener { asignarHoraBloque("14:00") }
        findViewById<Chip>(R.id.chip1500)?.setOnClickListener { asignarHoraBloque("15:00") }
        findViewById<Chip>(R.id.chip1600)?.setOnClickListener { asignarHoraBloque("16:00") }
        findViewById<Chip>(R.id.chip1700)?.setOnClickListener { asignarHoraBloque("17:00") }

        // Configuración de DatePicker y TimePicker
        etFechaHora?.keyListener = null
        etFechaHora?.isFocusable = false
        etFechaHora?.isFocusableInTouchMode = false
        etFechaHora?.isClickable = true

        val abrirFechaHoraPicker = {
            mostrarDatePickerDialog { fechaSeleccionada ->
                mostrarTimePickerDialog { horaSeleccionada ->
                    val fhStr = String.format(Locale.getDefault(), "%s %s", fechaSeleccionada, horaSeleccionada)
                    etFechaHora?.setText(fhStr)
                    calcularHoraEstimadaFin(fhStr, tvEstimacionFin)
                    actualizarVistaPreviaYPlantilla()
                }
            }
        }

        etFechaHora?.setOnClickListener { abrirFechaHoraPicker() }
        tilFechaHora?.setOnClickListener { abrirFechaHoraPicker() }

        // Evento Guardar Reserva con Deshabilitación Anti-Doble Clic
        btnGuardar?.setOnClickListener {
            btnGuardar.isEnabled = false

            val cliente = etCliente?.text.toString().trim()
            val telefonoRaw = etTelefono?.text.toString().trim()
            val servicio = etServicio?.text.toString().trim()
            val precioRaw = etPrecio?.text.toString().trim().replace(',', '.')
            val senaRaw = etMontoSena?.text.toString().trim().replace(',', '.')
            val fechaHora = etFechaHora?.text.toString().trim()
            val mensajeCustom = etMensajeWhatsappPreview?.text.toString().trim()

            val telefonoSanitizado = CitaRepository.sanitizarTelefonoWhatsapp(telefonoRaw)

            var esValido = true

            if (cliente.isEmpty()) {
                tilCliente?.error = "Ingrese el nombre del cliente"
                esValido = false
            } else {
                tilCliente?.error = null
            }

            if (servicio.isEmpty()) {
                tilServicio?.error = "Seleccione un servicio"
                esValido = false
            } else {
                tilServicio?.error = null
            }

            val precioDouble = precioRaw.toDoubleOrNull()
            if (precioRaw.isEmpty()) {
                tilPrecio?.error = "Ingrese el precio del servicio"
                esValido = false
            } else if (precioDouble == null || precioDouble <= 0.0) {
                tilPrecio?.error = "Ingrese un precio válido mayor a 0"
                esValido = false
            } else {
                tilPrecio?.error = null
            }

            val senaDouble = senaRaw.toDoubleOrNull() ?: 0.0

            if (fechaHora.isEmpty()) {
                tilFechaHora?.error = "Seleccione fecha y hora"
                esValido = false
            } else {
                tilFechaHora?.error = null
            }

            if (esValido) {
                if (repository.existeTraslapeHorario(fechaHora, duracionMinutosSeleccionada)) {
                    AlertDialog.Builder(this)
                        .setTitle("Alerta de Traslape de Horario")
                        .setMessage("Existe una cita activa cuyo rango de atención ($duracionMinutosSeleccionada min) colisiona con $fechaHora. ¿Deseas agendar de todos modos?")
                        .setPositiveButton("Agendar de todos modos") { _, _ ->
                            procesarGuardado(cliente, telefonoSanitizado, servicio, precioDouble ?: 0.0, senaDouble, fechaHora, cbNotificarWhatsapp?.isChecked == true, mensajeCustom)
                        }
                        .setNegativeButton("Cambiar Horario") { _, _ ->
                            btnGuardar.isEnabled = true
                        }
                        .setOnCancelListener { btnGuardar.isEnabled = true }
                        .show()
                } else {
                    procesarGuardado(cliente, telefonoSanitizado, servicio, precioDouble ?: 0.0, senaDouble, fechaHora, cbNotificarWhatsapp?.isChecked == true, mensajeCustom)
                }
            } else {
                btnGuardar.isEnabled = true
            }
        }
    }

    private fun calcularHoraEstimadaFin(fechaHoraStr: String, tvEstimacion: TextView?) {
        if (fechaHoraStr.isBlank()) {
            tvEstimacion?.text = "⏰ Seleccione horario y servicio para calcular hora fin"
            return
        }
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaInicio = sdf.parse(fechaHoraStr)
            if (fechaInicio != null) {
                val cal = Calendar.getInstance()
                cal.time = fechaInicio

                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    tvEstimacion?.text = "⚠️ ATENCIÓN: El local permanece CERRADO los domingos."
                    return
                }

                cal.add(Calendar.MINUTE, duracionMinutosSeleccionada)
                val horaFinStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
                tvEstimacion?.text = "⏰ Cita: $fechaHoraStr → Estima finalizar: $horaFinStr ($duracionMinutosSeleccionada min)"
            }
        } catch (e: Exception) {
            tvEstimacion?.text = "⏰ Duración estimada: $duracionMinutosSeleccionada min"
        }
    }

    private fun procesarGuardado(
        cliente: String,
        telefono: String,
        servicio: String,
        precio: Double,
        sena: Double,
        fechaHora: String,
        notificarWhatsapp: Boolean,
        mensajePersonalizado: String
    ) {
        try {
            val nuevaCita = Cita(
                cliente = cliente,
                telefono = telefono,
                servicio = servicio,
                precio = precio,
                montoSena = sena,
                fechaHora = fechaHora,
                estado = EstadoCita.PENDIENTE,
                duracionMinutos = duracionMinutosSeleccionada
            )
            val id = repository.insertarCita(nuevaCita)
            if (id != -1L) {
                Toast.makeText(this, "Reserva registrada con éxito", Toast.LENGTH_SHORT).show()

                if (notificarWhatsapp) {
                    enviarWhatsappPersonalizado(telefono, mensajePersonalizado)
                }

                finish()
            } else {
                Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
                findViewById<MaterialButton>(R.id.btnGuardar)?.isEnabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error inesperado: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            findViewById<MaterialButton>(R.id.btnGuardar)?.isEnabled = true
        }
    }

    private fun enviarWhatsappPersonalizado(telefono: String, mensajeText: String) {
        try {
            val numeroLimpio = CitaRepository.sanitizarTelefonoWhatsapp(telefono)
            val mensajeEncoded = Uri.encode(mensajeText)
            val url = if (numeroLimpio.length >= 9) {
                "whatsapp://send?phone=$numeroLimpio&text=$mensajeEncoded"
            } else {
                "https://api.whatsapp.com/send?text=$mensajeEncoded"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp nativo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDatePickerDialog(onFechaSeleccionada: (String) -> Unit) {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val fechaFormateada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year)
            val checkCal = Calendar.getInstance()
            checkCal.set(year, month, dayOfMonth)

            if (checkCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                Toast.makeText(this, "⚠️ El local está CERRADO los domingos. Elija de Lunes a Sábado.", Toast.LENGTH_LONG).show()
            }

            fechaBaseSeleccionada = fechaFormateada
            onFechaSeleccionada(fechaFormateada)
        }, anio, mes, dia).show()
    }

    private fun mostrarTimePickerDialog(onHoraSeleccionada: (String) -> Unit) {
        val calendario = Calendar.getInstance()
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            val horaFormateada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            onHoraSeleccionada(horaFormateada)
        }, hora, minuto, true).show()
    }
}
