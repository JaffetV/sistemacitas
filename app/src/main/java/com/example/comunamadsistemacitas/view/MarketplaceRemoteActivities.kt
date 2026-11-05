package com.example.comunamadsistemacitas.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.comunamadsistemacitas.data.remote.MarketplaceApiClient
import com.example.comunamadsistemacitas.data.remote.MarketplaceApiConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val REMOTE_PREFS = "marketplace_local_session"
private const val ACTIVE_BUSINESS_UUID = "active_business_uuid"
private const val ACTIVE_USER_ID = "remote_user_id"
private const val ACTIVE_USER_NAME = "remote_user_name"

private fun Context.rdp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
private fun Context.rText(value: String, size: Float = 14f, color: Int = Color.rgb(51, 65, 85), bold: Boolean = false): TextView =
    TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, rdp(3), 0, rdp(3))
    }
private fun Context.rField(hintText: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(this).apply {
    hint = hintText; textSize = 15f; inputType = type; isSingleLine = true
    setPadding(rdp(12), rdp(8), rdp(12), rdp(8))
}
private fun Context.rButton(label: String, action: () -> Unit): MaterialButton = MaterialButton(this).apply {
    text = label; isAllCaps = false; setOnClickListener { action() }
}
private fun Context.rColumn(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL; setPadding(rdp(18), rdp(16), rdp(18), rdp(22)); setBackgroundColor(Color.rgb(246, 248, 252))
}
private fun Context.rCard(): MaterialCardView = MaterialCardView(this).apply {
    radius = rdp(14).toFloat(); cardElevation = rdp(1).toFloat(); setCardBackgroundColor(Color.WHITE)
    strokeColor = Color.rgb(226, 232, 240); strokeWidth = rdp(1)
}
private fun LinearLayout.rAddField(field: EditText, context: Context) {
    addView(field, LinearLayout.LayoutParams(-1, context.rdp(52)).apply { bottomMargin = context.rdp(8) })
}
private fun showRemoteError(context: Context, error: Throwable) {
    val message = error.message?.takeIf { it.isNotBlank() } ?: "No se pudo conectar con el servidor."
    AlertDialog.Builder(context).setTitle("UNAMAD Citas").setMessage(message).setPositiveButton("Aceptar", null).show()
}

/** Inicio de sesión y alta de cliente contra el backend REST. */
class AccesoRemotoActivity : AppCompatActivity() {
    private lateinit var api: MarketplaceApiClient
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = MarketplaceApiConfig.crearCliente(this)
        val root = rColumn()
        root.addView(rText("Accede a UNAMAD Citas", 25f, Color.rgb(15, 35, 65), true))
        root.addView(rText("Inicia sesión con tu cuenta para reservar o administrar un negocio.", 14f))
        email = rField("Correo electrónico", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        password = rField("Contraseña", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        root.rAddField(email, this); root.rAddField(password, this)
        status = rText("La sesión se almacena cifrada mediante Android Keystore.", 12f, Color.rgb(71, 85, 105))
        root.addView(status)
        root.addView(rButton("Iniciar sesión") { iniciarSesion() }, LinearLayout.LayoutParams(-1, rdp(52)))
        root.addView(rButton("Crear cuenta gratuita de cliente") { mostrarRegistroCliente() }, LinearLayout.LayoutParams(-1, rdp(52)))
        root.addView(rButton("Volver al inicio") { finish() }, LinearLayout.LayoutParams(-1, rdp(48)))
        setContentView(root)
        if (api.haySesion()) cargarNegociosDeCuenta()
    }

    private fun iniciarSesion() {
        val mail = email.text.toString().trim()
        val pass = password.text.toString()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches() || pass.length < 10) {
            email.error = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) "Correo no válido" else null
            password.error = if (pass.length < 10) "La contraseña debe tener al menos 10 caracteres" else null
            return
        }
        status.text = "Conectando con el servidor…"
        api.ejecutarAsync({ api.iniciarSesion(mail, pass) }) { result ->
            result.onSuccess { user ->
                getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit()
                    .putString(ACTIVE_USER_ID, user.optString("id"))
                    .putString(ACTIVE_USER_NAME, user.optString("full_name"))
                    .apply()
                cargarNegociosDeCuenta()
            }.onFailure { status.text = "No se pudo iniciar sesión."; showRemoteError(this, it) }
        }
    }

    private fun cargarNegociosDeCuenta() {
        api.ejecutarAsync({ api.obtenerMisNegocios() }) { result ->
            result.onSuccess { businesses ->
                if (businesses.length() == 0) {
                    getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit().remove(ACTIVE_BUSINESS_UUID).apply()
                    val next = if (intent.getStringExtra("AFTER_LOGIN") == "BUSINESS") RegistroNegocioRemotoActivity::class.java else MarketplaceRemotoActivity::class.java
                    startActivity(Intent(this, next)); finish()
                } else {
                    val labels = (0 until businesses.length()).map { i ->
                        val item = businesses.getJSONObject(i)
                        "${item.optString("name")} · ${item.optString("district")}" 
                    }.toTypedArray()
                    AlertDialog.Builder(this).setTitle("Selecciona tu negocio activo")
                        .setItems(labels) { _, which ->
                            val chosen = businesses.getJSONObject(which)
                            getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit()
                                .putString(ACTIVE_BUSINESS_UUID, chosen.getString("id")).apply()
                            startActivity(Intent(this, PanelNegocioRemotoActivity::class.java)); finish()
                        }.setOnCancelListener { status.text = "Sesión iniciada. Selecciona tu negocio para continuar." }.show()
                }
            }.onFailure { showRemoteError(this, it) }
        }
    }

    private fun mostrarRegistroCliente() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(8), rdp(4), rdp(8), rdp(4)) }
        val name = rField("Nombre y apellidos")
        val mail = rField("Correo electrónico", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val phone = rField("Teléfono", InputType.TYPE_CLASS_PHONE)
        val pass = rField("Contraseña (mínimo 10 caracteres)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        listOf(name, mail, phone, pass).forEach { box.rAddField(it, this) }
        val dialog = AlertDialog.Builder(this).setTitle("Crear cuenta de cliente")
            .setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Crear cuenta", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (name.text.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail.text.toString()).matches() || phone.text.length < 7 || pass.text.length < 10) {
                    Toast.makeText(this, "Completa los datos y usa una contraseña de al menos 10 caracteres.", Toast.LENGTH_LONG).show(); return@setOnClickListener
                }
                api.ejecutarAsync({ api.registrarCliente(name.text.toString(), mail.text.toString(), phone.text.toString(), pass.text.toString()) }) { result ->
                    result.onSuccess { user ->
                        getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit()
                            .putString(ACTIVE_USER_ID, user.optString("id"))
                            .putString(ACTIVE_USER_NAME, user.optString("full_name")).apply()
                        dialog.dismiss(); startActivity(Intent(this, MarketplaceRemotoActivity::class.java)); finish()
                    }.onFailure { showRemoteError(this, it) }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroy() { super.onDestroy(); api.liberar() }
}

/** Alta de negocio directamente en PostgreSQL; el UUID devuelto se usa en el panel. */
class RegistroNegocioRemotoActivity : AppCompatActivity() {
    private lateinit var api: MarketplaceApiClient
    private lateinit var categories: JSONArray
    private lateinit var categorySpinner: Spinner
    private lateinit var ownerName: EditText
    private lateinit var ownerEmail: EditText
    private lateinit var ownerPhone: EditText
    private lateinit var ownerPassword: EditText
    private lateinit var businessName: EditText
    private lateinit var description: EditText
    private lateinit var department: EditText
    private lateinit var province: EditText
    private lateinit var district: EditText
    private lateinit var address: EditText
    private lateinit var modeCita: CheckBox
    private lateinit var modeRecojo: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = MarketplaceApiConfig.crearCliente(this)
        categories = JSONArray()
        val root = rColumn()
        val scroll = ScrollView(this)
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(form); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        form.addView(rText("Registra tu negocio", 25f, Color.rgb(15, 35, 65), true))
        form.addView(rText("El servidor crea tu cuenta de propietario y activa una prueba comercial de 14 días.", 13f))
        ownerName = rField("Nombre del responsable")
        ownerEmail = rField("Correo electrónico", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        ownerPhone = rField("Teléfono del responsable", InputType.TYPE_CLASS_PHONE)
        ownerPassword = rField("Contraseña (mínimo 10 caracteres)", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        businessName = rField("Nombre comercial")
        description = rField("Descripción del negocio")
        department = rField("Departamento (ej. Madre de Dios)")
        province = rField("Provincia (ej. Tambopata)")
        district = rField("Distrito (ej. Puerto Maldonado)")
        address = rField("Dirección del establecimiento")
        listOf(ownerName, ownerEmail, ownerPhone, ownerPassword, businessName, description, department, province, district, address).forEach { form.rAddField(it, this) }
        form.addView(rText("Categoría", 13f, Color.rgb(71, 85, 105), true))
        categorySpinner = Spinner(this); form.addView(categorySpinner, LinearLayout.LayoutParams(-1, rdp(50)))
        modeCita = CheckBox(this).apply { text = "Atención mediante citas"; isChecked = true }
        modeRecojo = CheckBox(this).apply { text = "Pedidos para recoger" }
        form.addView(modeCita); form.addView(modeRecojo)
        status = rText("Cargando categorías del servidor…", 12f)
        form.addView(status)
        root.addView(rButton("Registrar negocio") { registrar() }, LinearLayout.LayoutParams(-1, rdp(52)))
        root.addView(rButton("Volver") { finish() }, LinearLayout.LayoutParams(-1, rdp(46)))
        setContentView(root)
        api.ejecutarAsync({ api.obtenerCategorias() }) { result ->
            result.onSuccess { json ->
                categories = json
                val labels = (0 until json.length()).map { i -> val c = json.getJSONObject(i); "${c.optString("emoji", "🏪")}  ${c.optString("name")}" }
                categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                status.text = "${json.length()} categorías disponibles."
            }.onFailure { status.text = "No se cargaron categorías."; showRemoteError(this, it) }
        }
        // Si el propietario ya inició sesión, reutiliza sus datos de perfil y evita pedir otra contraseña.
        if (api.haySesion()) {
            ownerPassword.visibility = View.GONE
            ownerName.hint = "Nombre del responsable (cuenta actual)"
            ownerEmail.hint = "Correo electrónico (cuenta actual)"
            api.ejecutarAsync({ api.obtenerMiPerfil() }) { result ->
                result.onSuccess { profile ->
                    ownerName.setText(profile.optString("full_name"))
                    ownerEmail.setText(profile.optString("email"))
                    ownerPhone.setText(profile.optString("phone"))
                }.onFailure { showRemoteError(this, it) }
            }
        }
    }

    private fun registrar() {
        val modes = JSONArray()
        if (modeCita.isChecked) modes.put("CITA")
        if (modeRecojo.isChecked) modes.put("RECOJO")
        if (categories.length() == 0 || categorySpinner.selectedItemPosition !in 0 until categories.length()) {
            Toast.makeText(this, "Selecciona una categoría válida.", Toast.LENGTH_LONG).show(); return
        }
        val existingSession = api.haySesion()
        val ownerFieldsValid = existingSession || (ownerName.text.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(ownerEmail.text.toString()).matches() &&
            ownerPassword.text.length >= 10)
        val businessFieldsValid = businessName.text.isNotBlank() && ownerPhone.text.length >= 7 &&
            department.text.isNotBlank() && province.text.isNotBlank() && district.text.isNotBlank() &&
            address.text.isNotBlank() && modes.length() > 0
        if (!ownerFieldsValid || !businessFieldsValid) {
            Toast.makeText(this, "Completa nombre, teléfono y ubicación del negocio. Para una cuenta nueva, agrega también los datos del propietario y una contraseña de 10 caracteres.", Toast.LENGTH_LONG).show(); return
        }
        val categoryId = categories.getJSONObject(categorySpinner.selectedItemPosition).getString("id")
        val business = JSONObject().put("name", businessName.text.toString().trim())
            .put("categoryId", categoryId).put("description", description.text.toString().trim())
            .put("phone", ownerPhone.text.toString().trim()).put("department", department.text.toString().trim())
            .put("province", province.text.toString().trim()).put("district", district.text.toString().trim())
            .put("address", address.text.toString().trim()).put("modes", modes)
        val payload = JSONObject().put("fullName", ownerName.text.toString().trim())
            .put("email", ownerEmail.text.toString().trim()).put("ownerPhone", ownerPhone.text.toString().trim())
            .put("password", ownerPassword.text.toString()).put("business", business)
        status.text = "Registrando negocio…"
        api.ejecutarAsync({ if (existingSession) api.crearOtroNegocio(business) else api.registrarNegocio(payload) }) { result ->
            result.onSuccess { registered ->
                getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit()
                    .putString(ACTIVE_USER_ID, registered.optString("ownerUserId"))
                    .putString(ACTIVE_USER_NAME, ownerName.text.toString().trim())
                    .putString(ACTIVE_BUSINESS_UUID, registered.getString("id")).apply()
                Toast.makeText(this, "Negocio registrado en el servidor.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, PanelNegocioRemotoActivity::class.java)); finish()
            }.onFailure { status.text = "No se pudo registrar el negocio."; showRemoteError(this, it) }
        }
    }

    override fun onDestroy() { super.onDestroy(); api.liberar() }
}

/** Exploración remota con identificadores UUID y solicitudes de reserva en servidor. */
class MarketplaceRemotoActivity : AppCompatActivity() {
    private lateinit var api: MarketplaceApiClient
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private lateinit var district: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = MarketplaceApiConfig.crearCliente(this)
        val root = rColumn()
        root.addView(rText("Explora negocios", 25f, Color.rgb(15, 35, 65), true))
        root.addView(rText("Resultados del marketplace conectado al servidor.", 13f))
        search = rField("Buscar negocio, servicio o categoría").apply { setText(intent.getStringExtra("SEARCH_QUERY") ?: "") }
        district = rField("Distrito (opcional)").apply { setText("Puerto Maldonado") }
        root.rAddField(search, this); root.rAddField(district, this)
        root.addView(rButton("Buscar") { cargarNegocios() }, LinearLayout.LayoutParams(-1, rdp(48)))
        root.addView(rButton("Mis reservas") { cargarMisReservas() }, LinearLayout.LayoutParams(-1, rdp(46)))
        status = rText("Cargando…", 12f); root.addView(status)
        val scroller = ScrollView(this); list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.addView(list); root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(rButton("Volver al inicio") { finish() }, LinearLayout.LayoutParams(-1, rdp(44)))
        setContentView(root)
        if (intent.getBooleanExtra("SHOW_RESERVATIONS", false)) cargarMisReservas() else cargarNegocios()
    }

    private fun cargarNegocios() {
        status.text = "Consultando negocios publicados…"
        api.ejecutarAsync({ api.explorarNegocios(search.text.toString(), null, district.text.toString()) }) { result ->
            result.onSuccess { items ->
                list.removeAllViews(); status.text = "${items.length()} negocios encontrados."
                if (items.length() == 0) list.addView(rText("No hay negocios publicados con esos filtros. Prueba otro distrito o término.", 14f))
                for (i in 0 until items.length()) {
                    val business = items.getJSONObject(i)
                    val card = rCard(); val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(14), rdp(12), rdp(14), rdp(12)) }
                    content.addView(rText("${business.optString("emoji", "🏪")}  ${business.optString("name")}", 17f, Color.rgb(15, 35, 65), true))
                    content.addView(rText("${business.optString("category")} · ${business.optString("district")}, ${business.optString("province")}", 12f))
                    content.addView(rText(business.optString("description"), 13f))
                    content.addView(rText("Modalidades: ${business.optJSONArray("modes")?.toString()?.replace("[", "")?.replace("]", "") ?: ""}", 12f, Color.rgb(21, 128, 61), true))
                    content.addView(rButton("Ver servicios y ofertas") { mostrarPublicaciones(business) })
                    card.addView(content); list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = rdp(9) })
                }
            }.onFailure { status.text = "No se pudo consultar el marketplace."; showRemoteError(this, it) }
        }
    }

    private fun mostrarPublicaciones(business: JSONObject) {
        val businessId = business.optString("id")
        api.ejecutarAsync({ api.obtenerPublicaciones(businessId) }) { result ->
            result.onSuccess { listings ->
                val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(8), rdp(4), rdp(8), rdp(4)) }
                if (listings.length() == 0) box.addView(rText("Este negocio todavía no publica servicios ni productos."))
                for (i in 0 until listings.length()) {
                    val item = listings.getJSONObject(i)
                    box.addView(rText(item.optString("title"), 16f, Color.rgb(15, 35, 65), true))
                    box.addView(rText("${item.optString("kind")} · S/ ${item.optString("price")} · ${item.optString("mode")}", 13f))
                    if (item.optDouble("compare_at_price", 0.0) > item.optDouble("price", 0.0)) box.addView(rText("Precio anterior: S/ ${item.optString("compare_at_price")}", 12f, Color.rgb(21, 128, 61)))
                    if (item.optString("description").isNotBlank()) box.addView(rText(item.optString("description"), 12f))
                    box.addView(rButton("Reservar / pedir") { mostrarFormularioReserva(businessId, item) })
                    val line = View(this).apply { setBackgroundColor(Color.rgb(226, 232, 240)) }
                    box.addView(line, LinearLayout.LayoutParams(-1, rdp(1)))
                }
                AlertDialog.Builder(this).setTitle(business.optString("name")).setView(ScrollView(this).apply { addView(box) }).setPositiveButton("Cerrar", null).show()
            }.onFailure { showRemoteError(this, it) }
        }
    }

    private fun mostrarFormularioReserva(businessId: String, listing: JSONObject) {
        if (!api.haySesion()) {
            AlertDialog.Builder(this).setTitle("Inicia sesión")
                .setMessage("Para enviar una reserva, necesitas una cuenta de cliente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Iniciar sesión") { _, _ -> startActivity(Intent(this, AccesoRemotoActivity::class.java)) }.show()
            return
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(8), rdp(4), rdp(8), rdp(4)) }
        val kind = listing.optString("kind")
        val mode = if (kind == "PRODUCTO") "RECOJO" else "CITA"
        val count = rField("Cantidad (para recogida)", InputType.TYPE_CLASS_NUMBER).apply { setText("1") }
        val note = rField("Nota para el negocio (opcional)")
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) }
        val dateText = rText("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)}", 14f, Color.rgb(15, 35, 65), true)
        box.addView(rText("Modalidad: ${if (mode == "CITA") "Cita" else "Recogida"}", 13f))
        box.addView(dateText)
        box.addView(rButton("Elegir fecha y hora") {
            DatePickerDialog(this, { _, y, m, d ->
                TimePickerDialog(this, { _, h, min ->
                    calendar.set(y, m, d, h, min)
                    dateText.text = "Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)}"
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        })
        if (mode == "RECOJO") box.rAddField(count, this)
        box.rAddField(note, this)
        val dialog = AlertDialog.Builder(this).setTitle("Reservar: ${listing.optString("title")}")
            .setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Enviar reserva", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(calendar.time)
                val quantity = if (mode == "RECOJO") count.text.toString().toIntOrNull() ?: 0 else 1
                if (quantity <= 0 || calendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Selecciona una fecha futura y una cantidad válida.", Toast.LENGTH_LONG).show(); return@setOnClickListener
                }
                val payload = JSONObject().put("businessId", businessId).put("listingId", listing.getString("id"))
                    .put("mode", mode).put("dateTime", time).put("quantity", quantity)
                    .put("paymentMethod", "EN_LOCAL").put("note", note.text.toString().trim())
                api.ejecutarAsync({ api.crearReserva(payload) }) { result ->
                    result.onSuccess { reservation ->
                        dialog.dismiss()
                        AlertDialog.Builder(this).setTitle("Reserva enviada")
                            .setMessage("Código: ${reservation.optString("code", "registrado")}\nEstado: SOLICITADA\nEl negocio debe confirmar la solicitud.")
                            .setPositiveButton("Aceptar", null).show()
                    }.onFailure { showRemoteError(this, it) }
                }
            }
        }
        dialog.show()
    }

    private fun cargarMisReservas() {
        if (!api.haySesion()) {
            startActivity(Intent(this, AccesoRemotoActivity::class.java)); return
        }
        status.text = "Cargando tus reservas…"
        api.ejecutarAsync({ api.obtenerMisReservas() }) { result ->
            result.onSuccess { items ->
                list.removeAllViews(); status.text = "${items.length()} reservas de tu cuenta."
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val card = rCard(); val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(12), rdp(10), rdp(12), rdp(10)) }
                    content.addView(rText("${item.optString("code")} · ${item.optString("status")}", 15f, Color.rgb(15, 35, 65), true))
                    content.addView(rText("${item.optString("business_name")} · ${item.optString("listing_title")}", 13f))
                    content.addView(rText("${item.optString("mode")} · ${item.optString("scheduled_at")} · S/ ${item.optString("total")}", 12f))
                    if (item.optString("status") in listOf("SOLICITADA", "CONFIRMADA")) content.addView(rButton("Cancelar reserva") { cancelarReserva(item.optString("id")) })
                    card.addView(content); list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = rdp(8) })
                }
            }.onFailure { showRemoteError(this, it) }
        }
    }

    private fun cancelarReserva(id: String) {
        api.ejecutarAsync({ api.cancelarMiReserva(id) }) { result ->
            result.onSuccess { Toast.makeText(this, "Reserva cancelada.", Toast.LENGTH_SHORT).show(); cargarMisReservas() }
            .onFailure { showRemoteError(this, it) }
        }
    }

    override fun onDestroy() { super.onDestroy(); api.liberar() }
}

/** Panel remoto del propietario: negocio activo, catálogo, horarios y reservas con UUID. */
class PanelNegocioRemotoActivity : AppCompatActivity() {
    private lateinit var api: MarketplaceApiClient
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var businessId: String = ""
    private var allowedBusinessModes: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = MarketplaceApiConfig.crearCliente(this)
        businessId = getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).getString(ACTIVE_BUSINESS_UUID, "").orEmpty()
        val root = rColumn()
        root.addView(rText("Panel de mi negocio", 25f, Color.rgb(15, 35, 65), true))
        status = rText("Conectando…", 12f); root.addView(status)
        root.addView(rButton("Cambiar negocio activo") { seleccionarNegocio() }, LinearLayout.LayoutParams(-1, rdp(46)))
        root.addView(rButton("Nueva publicación") { mostrarNuevaPublicacion() }, LinearLayout.LayoutParams(-1, rdp(46)))
        root.addView(rButton("Configurar horario semanal") { configurarHorario() }, LinearLayout.LayoutParams(-1, rdp(46)))
        val scroll = ScrollView(this); content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(rButton("Cerrar sesión en este dispositivo") {
            api.cerrarSesion()
            getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit().remove(ACTIVE_BUSINESS_UUID).remove(ACTIVE_USER_ID).apply()
            startActivity(Intent(this, AccesoRemotoActivity::class.java)); finish()
        }, LinearLayout.LayoutParams(-1, rdp(46)))
        setContentView(root)
        if (!api.haySesion()) {
            startActivity(Intent(this, AccesoRemotoActivity::class.java).putExtra("AFTER_LOGIN", "BUSINESS"))
            finish()
        } else if (businessId.isBlank()) seleccionarNegocio() else cargarPanel()
    }

    private fun seleccionarNegocio() {
        api.ejecutarAsync({ api.obtenerMisNegocios() }) { result ->
            result.onSuccess { businesses ->
                if (businesses.length() == 0) { showRemoteError(this, IllegalStateException("Esta cuenta todavía no tiene negocios asociados.")); return@onSuccess }
                val labels = (0 until businesses.length()).map { i -> val b = businesses.getJSONObject(i); "${b.optString("name")} · ${b.optString("district")}" }.toTypedArray()
                AlertDialog.Builder(this).setTitle("Selecciona el negocio activo").setItems(labels) { _, which ->
                    businessId = businesses.getJSONObject(which).getString("id")
                    getSharedPreferences(REMOTE_PREFS, MODE_PRIVATE).edit().putString(ACTIVE_BUSINESS_UUID, businessId).apply()
                    cargarPanel()
                }.show()
            }.onFailure { showRemoteError(this, it) }
        }
    }

    private fun cargarPanel() {
        if (businessId.isBlank()) return
        status.text = "Sincronizando datos del negocio…"
        api.ejecutarAsync({
            val business = api.obtenerNegocio(businessId)
            val listings = api.obtenerPublicaciones(businessId)
            val reservations = api.obtenerReservasNegocio(businessId)
            val hours = api.obtenerHorarios(businessId)
            JSONObject().put("business", business).put("listings", listings).put("reservations", reservations).put("hours", hours)
        }) { result ->
            result.onSuccess { data ->
                val business = data.getJSONObject("business")
                val listings = data.getJSONArray("listings")
                val reservations = data.getJSONArray("reservations")
                val hours = data.getJSONArray("hours")
                allowedBusinessModes = buildSet {
                    val configured = business.optJSONArray("modes") ?: JSONArray()
                    for (modeIndex in 0 until configured.length()) add(configured.getString(modeIndex))
                }
                content.removeAllViews()
                content.addView(rText(business.optString("name"), 20f, Color.rgb(15, 35, 65), true))
                content.addView(rText("${business.optString("district")}, ${business.optString("province")} · ${business.optString("subscriptionStatus")}", 12f))
                content.addView(rText("Publicaciones (${listings.length()})", 17f, Color.rgb(15, 35, 65), true).apply { setPadding(0, rdp(14), 0, rdp(6)) })
                for (i in 0 until listings.length()) {
                    val item = listings.getJSONObject(i)
                    val card = rCard(); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(12), rdp(10), rdp(12), rdp(10)) }
                    box.addView(rText(item.optString("title"), 15f, Color.rgb(15, 35, 65), true))
                    box.addView(rText("${item.optString("kind")} · ${item.optString("mode")} · S/ ${item.optString("price")}", 12f))
                    card.addView(box); content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = rdp(6) })
                }
                content.addView(rText("Horario semanal (${hours.length()} bloques)", 17f, Color.rgb(15, 35, 65), true).apply { setPadding(0, rdp(14), 0, rdp(6)) })
                if (hours.length() == 0) content.addView(rText("Aún no has configurado horarios de atención.", 12f))
                for (i in 0 until hours.length()) {
                    val h = hours.getJSONObject(i)
                    content.addView(rText("Día ${h.optInt("day_of_week")}: ${h.optString("opens_at")} – ${h.optString("closes_at")} ${if (h.optBoolean("active")) "" else "(inactivo)"}", 12f))
                }
                content.addView(rText("Reservas recibidas (${reservations.length()})", 17f, Color.rgb(15, 35, 65), true).apply { setPadding(0, rdp(14), 0, rdp(6)) })
                for (i in 0 until reservations.length()) {
                    val reservation = reservations.getJSONObject(i)
                    val card = rCard(); val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(12), rdp(10), rdp(12), rdp(10)) }
                    box.addView(rText("${reservation.optString("code")} · ${reservation.optString("status")}", 14f, Color.rgb(15, 35, 65), true))
                    box.addView(rText("${reservation.optString("customer_name")} · ${reservation.optString("customer_phone")}", 12f))
                    box.addView(rText("${reservation.optString("listing_title")} · ${reservation.optString("mode")} · ${reservation.optString("scheduled_at")}", 12f))
                    box.addView(rText("Cantidad: ${reservation.optInt("quantity")} · Total: S/ ${reservation.optString("total")}", 12f))
                    val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
                    when (reservation.optString("status")) {
                        "SOLICITADA" -> actions.addView(rButton("Confirmar") { cambiarEstado(reservation.optString("id"), "CONFIRMADA") })
                        "CONFIRMADA" -> actions.addView(rButton(if (reservation.optString("mode") == "RECOJO") "Lista" else "Completar") {
                            cambiarEstado(reservation.optString("id"), if (reservation.optString("mode") == "RECOJO") "LISTA" else "COMPLETADA")
                        })
                        "LISTA" -> actions.addView(rButton("Entregada") { cambiarEstado(reservation.optString("id"), "COMPLETADA") })
                    }
                    if (actions.childCount > 0) box.addView(actions)
                    card.addView(box); content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = rdp(7) })
                }
                status.text = "Datos sincronizados desde PostgreSQL · UUID: $businessId"
            }.onFailure { status.text = "No se pudo sincronizar el panel."; showRemoteError(this, it) }
        }
    }

    private fun cambiarEstado(reservationId: String, state: String) {
        api.ejecutarAsync({ api.cambiarEstadoReserva(businessId, reservationId, state) }) { result ->
            result.onSuccess { Toast.makeText(this, "Reserva actualizada: $state", Toast.LENGTH_SHORT).show(); cargarPanel() }
                .onFailure { showRemoteError(this, it) }
        }
    }

    private fun mostrarNuevaPublicacion() {
        if (businessId.isBlank()) { seleccionarNegocio(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(8), rdp(4), rdp(8), rdp(4)) }
        val title = rField("Nombre del servicio o producto")
        val description = rField("Descripción breve")
        val price = rField("Precio en soles", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val compare = rField("Precio anterior (opcional)", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val kind = Spinner(this).apply { adapter = ArrayAdapter(this@PanelNegocioRemotoActivity, android.R.layout.simple_spinner_item, listOf("SERVICIO", "PRODUCTO")) }
        val mode = Spinner(this)
        fun updateModes() {
            val choices = if (kind.selectedItemPosition == 1) {
                if ("RECOJO" in allowedBusinessModes) listOf("RECOJO") else listOf("RECOGIDA NO HABILITADA")
            } else {
                buildList {
                    if ("CITA" in allowedBusinessModes) add("CITA")
                    if ("RECOJO" in allowedBusinessModes) add("RECOJO")
                    if ("CITA" in allowedBusinessModes && "RECOJO" in allowedBusinessModes) add("AMBOS")
                }.ifEmpty { listOf("SIN MODALIDAD HABILITADA") }
            }
            mode.adapter = ArrayAdapter(this@PanelNegocioRemotoActivity, android.R.layout.simple_spinner_item, choices).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        kind.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = updateModes()
        }
        updateModes()
        val duration = rField("Duración en minutos (servicios)", InputType.TYPE_CLASS_NUMBER)
        val stock = rField("Stock (productos, opcional)", InputType.TYPE_CLASS_NUMBER)
        box.addView(rText("Tipo")); box.addView(kind); box.addView(rText("Modalidad")); box.addView(mode)
        listOf(title, description, price, compare, duration, stock).forEach { box.rAddField(it, this) }
        val dialog = AlertDialog.Builder(this).setTitle("Nueva publicación").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = price.text.toString().replace(',', '.').toDoubleOrNull()
                val old = compare.text.toString().replace(',', '.').toDoubleOrNull()
                if (title.text.isBlank() || amount == null || amount < 0) { Toast.makeText(this, "Ingresa título y precio válido.", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                val selectedKind = kind.selectedItem.toString()
                val selectedMode = mode.selectedItem?.toString().orEmpty()
                if (selectedMode !in listOf("CITA", "RECOJO", "AMBOS")) {
                    Toast.makeText(this, "Habilita una modalidad válida para este negocio antes de publicar.", Toast.LENGTH_LONG).show(); return@setOnClickListener
                }
                val payload = JSONObject().put("title", title.text.toString().trim()).put("description", description.text.toString().trim())
                    .put("kind", selectedKind).put("mode", selectedMode).put("price", amount)
                if (old != null) payload.put("compareAtPrice", old)
                duration.text.toString().toIntOrNull()?.let { payload.put("durationMinutes", it) }
                stock.text.toString().toIntOrNull()?.let { payload.put("stock", it) }
                api.ejecutarAsync({ api.publicar(businessId, payload) }) { result ->
                    result.onSuccess { dialog.dismiss(); Toast.makeText(this, "Publicación guardada en PostgreSQL.", Toast.LENGTH_SHORT).show(); cargarPanel() }
                        .onFailure { showRemoteError(this, it) }
                }
            }
        }
        dialog.show()
    }

    private fun configurarHorario() {
        if (businessId.isBlank()) { seleccionarNegocio(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(rdp(8), rdp(4), rdp(8), rdp(4)) }
        box.addView(rText("Se aplicará el mismo bloque de atención de lunes a sábado. Puedes ajustarlo después por día desde el panel web.", 12f))
        val open = rField("Apertura (HH:mm)").apply { setText("09:00") }
        val close = rField("Cierre (HH:mm)").apply { setText("18:00") }
        box.rAddField(open, this); box.rAddField(close, this)
        val dialog = AlertDialog.Builder(this).setTitle("Horario semanal").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
                if (!pattern.matches(open.text) || !pattern.matches(close.text) || open.text.toString() >= close.text.toString()) {
                    Toast.makeText(this, "Usa horas HH:mm y una hora de cierre posterior.", Toast.LENGTH_LONG).show(); return@setOnClickListener
                }
                val hours = JSONArray()
                (1..6).forEach { day -> hours.put(JSONObject().put("dayOfWeek", day).put("opensAt", open.text.toString()).put("closesAt", close.text.toString()).put("active", true)) }
                api.ejecutarAsync({ api.configurarHorarios(businessId, hours) }) { result ->
                    result.onSuccess { dialog.dismiss(); Toast.makeText(this, "Horario actualizado.", Toast.LENGTH_SHORT).show(); cargarPanel() }
                        .onFailure { showRemoteError(this, it) }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroy() { super.onDestroy(); api.liberar() }
}
