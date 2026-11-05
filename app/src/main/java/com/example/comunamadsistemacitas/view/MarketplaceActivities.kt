package com.example.comunamadsistemacitas.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.comunamadsistemacitas.MainActivity
import com.example.comunamadsistemacitas.data.repository.MarketplaceRepository
import com.example.comunamadsistemacitas.model.CategoriaNegocio
import com.example.comunamadsistemacitas.model.NegocioMarketplace
import com.example.comunamadsistemacitas.model.PublicacionMarketplace
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val PREFS_MARKETPLACE = "marketplace_local_session"
private const val PREF_NEGOCIO_ID = "ultimo_negocio_id"
private const val PREF_USUARIO_ID = "usuario_cliente_id"
private const val PREF_USUARIO_NOMBRE = "usuario_cliente_nombre"
private const val PREF_USUARIO_TELEFONO = "usuario_cliente_telefono"
private const val PREF_USUARIO_EMAIL = "usuario_cliente_email"

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun crearTexto(context: Context, value: String, size: Float = 14f, color: Int = Color.rgb(51, 65, 85), bold: Boolean = false): TextView {
    return TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, context.dp(3), 0, context.dp(3))
    }
}

private fun crearCampo(context: Context, hintText: String, inputType: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
    return EditText(context).apply {
        hint = hintText
        textSize = 15f
        this.inputType = inputType
        isSingleLine = true
        setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
        background = context.getDrawable(android.R.drawable.edit_text)
    }
}

private fun crearSpinner(context: Context, opciones: List<String>): Spinner {
    return Spinner(context).apply {
        adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, opciones).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}

private fun crearBoton(context: Context, texto: String, onClick: () -> Unit): MaterialButton {
    return MaterialButton(context).apply {
        text = texto
        isAllCaps = false
        setOnClickListener { onClick() }
    }
}

private fun crearContenedor(context: Context): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(context.dp(18), context.dp(16), context.dp(18), context.dp(20))
    setBackgroundColor(Color.rgb(248, 250, 252))
}

private fun agregarCampo(parent: LinearLayout, field: EditText, context: Context) {
    parent.addView(field, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(52)).apply {
        bottomMargin = context.dp(9)
    })
}

private fun agregarTitulo(parent: LinearLayout, texto: String, context: Context) {
    parent.addView(crearTexto(context, texto, 13f, Color.rgb(71, 85, 105), true))
}

/** Portal principal del marketplace con identidad visual UNAMAD. */
class PortalMarketplaceActivity : AppCompatActivity() {
    private val navy = Color.rgb(7, 37, 72)
    private val green = Color.rgb(34, 197, 94)
    private val ink = Color.rgb(15, 23, 42)
    private val muted = Color.rgb(71, 85, 105)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Encabezado institucional.
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 28, 56), navy, Color.rgb(8, 61, 91))
            ).apply { cornerRadii = floatArrayOf(0f, 0f, this@PortalMarketplaceActivity.dp(20).toFloat(), this@PortalMarketplaceActivity.dp(20).toFloat(), this@PortalMarketplaceActivity.dp(20).toFloat(), this@PortalMarketplaceActivity.dp(20).toFloat(), 0f, 0f) }
        }
        val logo = android.widget.ImageView(this).apply {
            setImageResource(com.example.comunamadsistemacitas.R.drawable.logo_citas)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(58), dp(58)).apply { marginEnd = dp(12) })
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(crearTexto(this@PortalMarketplaceActivity, "UNAMAD", 22f, Color.WHITE, true))
        brand.addView(crearTexto(this@PortalMarketplaceActivity, "Citas Marketplace", 19f, green, true))
        brand.addView(crearTexto(this@PortalMarketplaceActivity, "Conecta · Reserva · Disfruta", 12f, Color.rgb(226, 232, 240)))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(header)

        val hero = MaterialCardView(this).apply {
            radius = dp(20).toFloat(); cardElevation = dp(3).toFloat()
            setCardBackgroundColor(navy)
        }
        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
        }
        heroContent.addView(crearTexto(this@PortalMarketplaceActivity, "Encuentra los mejores servicios cerca de ti", 25f, Color.WHITE, true))
        heroContent.addView(crearTexto(this@PortalMarketplaceActivity, "Explora negocios de tu zona, compara ofertas y reserva en pocos pasos.", 14f, Color.rgb(226, 232, 240)).apply {
            setPadding(0, dp(6), 0, dp(14))
        })
        val btnBuscar = MaterialButton(this).apply {
            text = "Buscar ahora  →"; isAllCaps = false
            setTextColor(navy); backgroundTintList = android.content.res.ColorStateList.valueOf(green)
        }
        heroContent.addView(btnBuscar, LinearLayout.LayoutParams(-2, dp(46)))
        hero.addView(heroContent)
        body.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(14), dp(14), dp(12)) })

        val search = crearCampo(this, "Buscar servicios, negocios o categorías").apply {
            setPadding(dp(14), dp(7), dp(14), dp(7))
            setBackground(android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.rgb(215, 224, 236))
            })
        }
        body.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(16), 0, dp(16), dp(14)) })
        btnBuscar.setOnClickListener { startActivity(Intent(this, MarketplaceRemotoActivity::class.java).putExtra("SEARCH_QUERY", search.text.toString())) }
        search.setOnEditorActionListener { _, _, _ ->
            startActivity(Intent(this, MarketplaceRemotoActivity::class.java).putExtra("SEARCH_QUERY", search.text.toString())); true
        }

        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        shortcuts.addView(atajo("♙", "Soy Cliente", "Explora y reserva", Color.rgb(232, 240, 252)) {
            startActivity(Intent(this, AccesoRemotoActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(112), 1f).apply { marginEnd = dp(5) })
        shortcuts.addView(atajo("▣", "Registrar mi negocio", "Publica tus servicios", Color.rgb(226, 247, 235)) {
            startActivity(Intent(this, RegistroNegocioRemotoActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(112), 1f).apply { marginStart = dp(5); marginEnd = dp(5) })
        shortcuts.addView(atajo("▦", "Mi Agenda", "Gestiona tus citas", Color.rgb(232, 240, 252)) {
            val prefs = getSharedPreferences(PREFS_MARKETPLACE, MODE_PRIVATE)
            val id = prefs.getString("active_business_uuid", null)
            if (id.isNullOrBlank()) startActivity(Intent(this, AccesoRemotoActivity::class.java).putExtra("AFTER_LOGIN", "BUSINESS"))
            else startActivity(Intent(this, PanelNegocioRemotoActivity::class.java))
        }, LinearLayout.LayoutParams(0, dp(112), 1f).apply { marginStart = dp(5) })
        body.addView(shortcuts, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), 0, dp(12), dp(18)) })

        body.addView(crearTexto(this@PortalMarketplaceActivity, "▦  Categorías populares", 18f, ink, true).apply { setPadding(dp(17), 0, dp(17), dp(10)) })
        val horizontal = android.widget.HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Belleza y estética" to "✦", "Servicios técnicos" to "🔧", "Salud y bienestar" to "♡", "Compras y retail" to "▣").forEach { (name, icon) ->
            val c = MaterialCardView(this).apply { radius = dp(14).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(220, 228, 238); strokeWidth = dp(1); isClickable = true; isFocusable = true }
            val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(14), dp(12), dp(14), dp(12)) }
            inner.addView(crearTexto(this@PortalMarketplaceActivity, icon, 24f, green, true))
            inner.addView(crearTexto(this@PortalMarketplaceActivity, name, 12f, ink, true))
            c.addView(inner)
            c.setOnClickListener { startActivity(Intent(this, MarketplaceRemotoActivity::class.java).putExtra("SEARCH_QUERY", name)) }
            categoryRow.addView(c, LinearLayout.LayoutParams(dp(132), dp(86)).apply { setMargins(dp(5), 0, dp(5), 0) })
        }
        horizontal.addView(categoryRow)
        body.addView(horizontal, LinearLayout.LayoutParams(-1, dp(94)).apply { bottomMargin = dp(16) })

        val offersTitle = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        offersTitle.addView(crearTexto(this@PortalMarketplaceActivity, "◆  Ofertas y promociones", 18f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        offersTitle.addView(crearTexto(this@PortalMarketplaceActivity, "Ver todas  →", 13f, Color.rgb(21, 128, 61), true).apply {
            setOnClickListener { startActivity(Intent(this@PortalMarketplaceActivity, MarketplaceRemotoActivity::class.java)) }
        })
        body.addView(offersTitle, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(17), 0, dp(17), dp(8)) })
        val promo = MaterialCardView(this).apply { radius = dp(16).toFloat(); cardElevation = dp(2).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1) }
        val promoBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(12), dp(15), dp(12)) }
        promoBox.addView(crearTexto(this@PortalMarketplaceActivity, "DESCUBRE OFERTAS LOCALES", 11f, Color.rgb(21, 128, 61), true))
        promoBox.addView(crearTexto(this@PortalMarketplaceActivity, "Servicios, productos y promociones de negocios cercanos.", 15f, ink, true))
        promoBox.addView(crearTexto(this@PortalMarketplaceActivity, "Consulta precios y disponibilidad directamente en cada publicación.", 12f, muted))
        promoBox.addView(crearBoton(this@PortalMarketplaceActivity, "Explorar ofertas") { startActivity(Intent(this, MarketplaceRemotoActivity::class.java)) })
        promo.addView(promoBox)
        body.addView(promo, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(18)) })

        val localMarketplace = crearTexto(this@PortalMarketplaceActivity, "Explorar catálogo local guardado en SQLite", 13f, Color.rgb(21, 91, 143), true).apply {
            gravity = Gravity.CENTER; setPadding(dp(8), dp(12), dp(8), dp(4))
            setOnClickListener { startActivity(Intent(this@PortalMarketplaceActivity, MarketplaceActivity::class.java)) }
        }
        body.addView(localMarketplace)
        val local = crearTexto(this@PortalMarketplaceActivity, "Agenda clásica · Administrar citas locales", 13f, muted, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(8), dp(8), dp(8), dp(18))
            setOnClickListener { startActivity(Intent(this@PortalMarketplaceActivity, MainActivity::class.java)) }
        }
        body.addView(local)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8)); setBackgroundColor(navy)
        }
        listOf("⌂\nInicio", "⌕\nBuscar", "◆\nOfertas", "▦\nMis reservas").forEachIndexed { index, label ->
            val item = crearTexto(this@PortalMarketplaceActivity, label, 12f, if (index == 0) green else Color.WHITE, index == 0).apply {
                gravity = Gravity.CENTER; setOnClickListener {
                    when (index) {
                        0 -> scroll.smoothScrollTo(0, 0)
                        1, 2 -> startActivity(Intent(this@PortalMarketplaceActivity, MarketplaceRemotoActivity::class.java))
                        3 -> startActivity(Intent(this@PortalMarketplaceActivity, MarketplaceRemotoActivity::class.java).putExtra("SHOW_RESERVATIONS", true))
                    }
                }
            }
            bottom.addView(item, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(bottom)
        setContentView(root)
    }

    private fun atajo(icono: String, titulo: String, subtitulo: String, fondo: Int, accion: () -> Unit): View {
        val card = MaterialCardView(this).apply {
            radius = dp(15).toFloat(); cardElevation = dp(1).toFloat(); setCardBackgroundColor(fondo)
            strokeColor = Color.rgb(213, 224, 238); strokeWidth = dp(1); isClickable = true; isFocusable = true
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(8), dp(5), dp(8)) }
        content.addView(crearTexto(this@PortalMarketplaceActivity, icono, 24f, navy, true))
        content.addView(crearTexto(this@PortalMarketplaceActivity, titulo, 12f, ink, true).apply { gravity = Gravity.CENTER })
        content.addView(crearTexto(this@PortalMarketplaceActivity, subtitulo, 10f, muted).apply { gravity = Gravity.CENTER })
        card.addView(content); card.setOnClickListener { accion() }
        return card
    }
}

/** Registro autoservicio de proveedor y activación automática del período inicial. */
class RegistroNegocioActivity : AppCompatActivity() {
    private lateinit var repository: MarketplaceRepository
    private lateinit var categorias: List<CategoriaNegocio>
    private lateinit var responsable: EditText
    private lateinit var email: EditText
    private lateinit var telefono: EditText
    private lateinit var nombre: EditText
    private lateinit var descripcion: EditText
    private lateinit var departamento: EditText
    private lateinit var provincia: EditText
    private lateinit var distrito: EditText
    private lateinit var direccion: EditText
    private lateinit var categoriaSpinner: Spinner
    private lateinit var modalidadGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MarketplaceRepository(this)
        categorias = repository.obtenerCategorias()
        val root = crearContenedor(this)
        val scroll = ScrollView(this)
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(form)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        form.addView(crearTexto(this, "Registra tu negocio", 25f, Color.rgb(30, 41, 59), true))
        form.addView(crearTexto(this, "El alta es automática. Tendrás un período de prueba de configuración de 14 días. El precio de los planes comerciales se definirá antes de activar cobros.", 14f))

        responsable = campo(form, "Nombre del responsable")
        email = campo(form, "Correo electrónico (opcional)", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT)
        telefono = campo(form, "Teléfono de contacto", android.text.InputType.TYPE_CLASS_PHONE)
        nombre = campo(form, "Nombre comercial del negocio")
        descripcion = campo(form, "Descripción breve del negocio")
        agregarTitulo(form, "Categoría", this)
        categoriaSpinner = crearSpinner(this, categorias.map { "${it.emoji}  ${it.nombre}" })
        form.addView(categoriaSpinner, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(10) })
        agregarTitulo(form, "Ubicación local", this)
        departamento = campo(form, "Departamento (ej. Madre de Dios)")
        provincia = campo(form, "Provincia")
        distrito = campo(form, "Distrito")
        direccion = campo(form, "Dirección del establecimiento")
        agregarTitulo(form, "Modalidades disponibles", this)
        modalidadGroup = RadioGroup(this).apply { orientation = LinearLayout.VERTICAL }
        listOf("Solo citas" to "cita", "Solo pedidos para recoger" to "recoger", "Ambas modalidades" to "ambas").forEach { (label, tag) ->
            modalidadGroup.addView(RadioButton(this).apply { text = label; this.tag = tag; id = View.generateViewId() })
        }
        modalidadGroup.check(modalidadGroup.getChildAt(2).id)
        form.addView(modalidadGroup)
        form.addView(crearBoton(this, "Crear negocio y continuar") { guardarNegocio() }, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(14)
        })
        form.addView(crearTexto(this, "El registro se guarda en este dispositivo en esta fase. Para operar en varios teléfonos se integrará autenticación y servidor en la fase cloud.", 12f).apply {
            setPadding(0, dp(10), 0, 0)
        })
        setContentView(root)
    }

    private fun campo(parent: LinearLayout, hint: String, type: Int = android.text.InputType.TYPE_CLASS_TEXT): EditText {
        return crearCampo(this, hint, type).also { agregarCampo(parent, it, this) }
    }

    private fun guardarNegocio() {
        val categoria = categorias.getOrNull(categoriaSpinner.selectedItemPosition) ?: run {
            Toast.makeText(this, "Selecciona una categoría.", Toast.LENGTH_LONG).show(); return
        }
        val selected = modalidadGroup.findViewById<RadioButton>(modalidadGroup.checkedRadioButtonId)?.tag?.toString() ?: "ambas"
        val modalidades = when (selected) {
            "cita" -> setOf("CITA")
            "recoger" -> setOf("RECOJO")
            else -> setOf("CITA", "RECOJO")
        }
        try {
            val id = repository.registrarNegocio(
                responsable.text.toString(), email.text.toString(), telefono.text.toString(),
                nombre.text.toString(), categoria.id, descripcion.text.toString(), departamento.text.toString(),
                provincia.text.toString(), distrito.text.toString(), direccion.text.toString(), modalidades
            )
            getSharedPreferences(PREFS_MARKETPLACE, MODE_PRIVATE).edit().putInt(PREF_NEGOCIO_ID, id).apply()
            Toast.makeText(this, "Negocio registrado correctamente.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PanelNegocioActivity::class.java).putExtra("NEGOCIO_ID", id))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "No se pudo registrar el negocio.", Toast.LENGTH_LONG).show()
        }
    }
}

/** Registro sin costo de clientes. El perfil se reutiliza al reservar desde el mismo dispositivo. */
class RegistroClienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = crearContenedor(this)
        root.addView(crearTexto(this, "Tu cuenta gratuita", 25f, Color.rgb(30, 41, 59), true))
        root.addView(crearTexto(this, "Explorar y crear una cuenta de cliente no tiene mensualidad. Solo pagas el servicio o pedido que decidas contratar.", 14f))
        val name = crearCampo(this, "Nombre y apellidos")
        val phone = crearCampo(this, "Teléfono", android.text.InputType.TYPE_CLASS_PHONE)
        val email = crearCampo(this, "Correo electrónico (opcional)", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        agregarCampo(root, name, this); agregarCampo(root, phone, this); agregarCampo(root, email, this)
        root.addView(crearBoton(this, "Crear cuenta gratuita") {
            try {
                val repository = MarketplaceRepository(this)
                val id = repository.registrarCliente(name.text.toString(), phone.text.toString(), email.text.toString())
                getSharedPreferences(PREFS_MARKETPLACE, MODE_PRIVATE).edit()
                    .putInt(PREF_USUARIO_ID, id)
                    .putString(PREF_USUARIO_NOMBRE, name.text.toString().trim())
                    .putString(PREF_USUARIO_TELEFONO, phone.text.toString().trim())
                    .putString(PREF_USUARIO_EMAIL, email.text.toString().trim())
                    .apply()
                startActivity(Intent(this, MarketplaceActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, e.message ?: "No se pudo crear la cuenta.", Toast.LENGTH_LONG).show()
            }
        }, LinearLayout.LayoutParams(-1, dp(54)))
        setContentView(root)
    }
}

/** Catálogo local de negocios publicados. */
class MarketplaceActivity : AppCompatActivity() {
    private lateinit var repository: MarketplaceRepository
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private lateinit var category: Spinner
    private lateinit var categories: List<CategoriaNegocio>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MarketplaceRepository(this)
        categories = repository.obtenerCategorias()
        val root = crearContenedor(this)
        root.addView(crearTexto(this, "Explora tu zona", 25f, Color.rgb(30, 41, 59), true))
        root.addView(crearTexto(this, "Negocios con citas, reservas y pedidos para recoger.", 14f))
        search = crearCampo(this, "Buscar negocio, servicio o distrito")
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10); bottomMargin = dp(8) })
        val categoryOptions = listOf("Todas las categorías") + categories.map { "${it.emoji} ${it.nombre}" }
        category = crearSpinner(this, categoryOptions)
        root.addView(category, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(10) })
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = cargarNegocios()
            override fun afterTextChanged(s: Editable?) {}
        })
        category.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = cargarNegocios()
        }
        cargarNegocios()
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) cargarNegocios()
    }

    private fun cargarNegocios() {
        if (!::list.isInitialized) return
        val selected = category.selectedItemPosition
        val categoryId = if (selected > 0) categories.getOrNull(selected - 1)?.id else null
        val businesses = repository.obtenerNegociosActivos(search.text.toString(), categoryId)
        list.removeAllViews()
        if (businesses.isEmpty()) {
            val message = if (search.text.isNullOrBlank()) "Todavía no hay negocios publicados en esta zona. Puedes ser el primero en registrar tu negocio." else "No encontramos negocios con esos filtros. Prueba otra búsqueda."
            list.addView(crearTexto(this, message, 15f).apply { setPadding(0, dp(18), 0, dp(18)) })
            list.addView(crearBoton(this, "Registrar un negocio") { startActivity(Intent(this, RegistroNegocioActivity::class.java)) })
            return
        }
        businesses.forEach { business ->
            val card = MaterialCardView(this).apply {
                radius = dp(15).toFloat(); cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1)
            }
            val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)) }
            inner.addView(crearTexto(this, "${business.emoji}  ${business.nombre}", 18f, Color.rgb(30, 41, 59), true))
            inner.addView(crearTexto(this, "${business.categoria}  ·  ${business.distrito}, ${business.provincia}", 12f, Color.rgb(71, 85, 105)))
            if (business.descripcion.isNotBlank()) inner.addView(crearTexto(this, business.descripcion, 13f))
            val modes = business.modalidades.joinToString(" y ") { if (it == "CITA") "Citas" else "Recogida" }
            inner.addView(crearTexto(this, "Modalidades: $modes", 12f, Color.rgb(59, 111, 112), true))
            inner.addView(crearBoton(this, "Ver ofertas y reservar") { mostrarPublicaciones(business) })
            card.addView(inner)
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun mostrarPublicaciones(business: NegocioMarketplace) {
        val items = repository.obtenerPublicaciones(business.id)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        content.addView(crearTexto(this, business.nombre, 19f, Color.rgb(30, 41, 59), true))
        content.addView(crearTexto(this, "${business.direccion}, ${business.distrito}", 12f))
        if (items.isEmpty()) content.addView(crearTexto(this, "Este negocio todavía no ha publicado servicios o productos.", 14f))
        val dialog = AlertDialog.Builder(this).setTitle("Catálogo del negocio").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("Cerrar", null).create()
        items.forEach { item ->
            val offer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, dp(10)) }
            offer.addView(crearTexto(this, item.titulo, 16f, Color.rgb(30, 41, 59), true))
            offer.addView(crearTexto(this, "${item.tipo.lowercase().replaceFirstChar { it.uppercase() }} · ${item.modalidad} · S/ ${String.format(Locale.US, "%.2f", item.precio)}", 13f))
            if (item.precioAnterior != null) offer.addView(crearTexto(this, "Antes: S/ ${String.format(Locale.US, "%.2f", item.precioAnterior)}", 12f, Color.rgb(22, 131, 74)))
            if (item.descripcion.isNotBlank()) offer.addView(crearTexto(this, item.descripcion, 13f))
            offer.addView(crearBoton(this, "Reservar / pedir") {
                dialog.dismiss()
                mostrarFormularioReserva(business, item)
            })
            content.addView(offer)
            val line = View(this).apply { setBackgroundColor(Color.rgb(226, 232, 240)) }
            content.addView(line, LinearLayout.LayoutParams(-1, dp(1)))
        }
        dialog.show()
    }

    private fun mostrarFormularioReserva(business: NegocioMarketplace, item: PublicacionMarketplace) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        val prefs = getSharedPreferences(PREFS_MARKETPLACE, MODE_PRIVATE)
        val name = crearCampo(this, "Nombre y apellidos").apply { setText(prefs.getString(PREF_USUARIO_NOMBRE, "") ?: "") }
        val phone = crearCampo(this, "Teléfono de contacto", android.text.InputType.TYPE_CLASS_PHONE).apply { setText(prefs.getString(PREF_USUARIO_TELEFONO, "") ?: "") }
        val email = crearCampo(this, "Correo (opcional)", android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT).apply { setText(prefs.getString(PREF_USUARIO_EMAIL, "") ?: "") }
        val possibleModes = when (item.modalidad) {
            "CITA" -> listOf("CITA")
            "RECOJO" -> listOf("RECOJO")
            else -> listOf("CITA", "RECOJO").filter { it in business.modalidades }
        }.filter { mode -> mode in business.modalidades && (mode != "CITA" || item.tipo == "SERVICIO") }
        if (possibleModes.isEmpty()) {
            Toast.makeText(this, "Este servicio no tiene una modalidad reservable configurada.", Toast.LENGTH_LONG).show(); return
        }
        agregarTitulo(box, "Tus datos", this); agregarCampo(box, name, this); agregarCampo(box, phone, this); agregarCampo(box, email, this)
        agregarTitulo(box, "Tipo de reserva", this)
        val modeSpinner = crearSpinner(this, possibleModes.map { if (it == "CITA") "Cita en el negocio" else "Pedido para recoger" })
        box.addView(modeSpinner, LinearLayout.LayoutParams(-1, dp(48)))
        agregarTitulo(box, "Fecha y hora", this)
        val dateTime = crearTexto(this, "Selecciona fecha y hora", 14f, Color.rgb(30, 41, 59)).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(241, 245, 249))
        }
        box.addView(dateTime, LinearLayout.LayoutParams(-1, dp(48)))
        val selectDate = crearBoton(this, "Elegir fecha y hora") {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    calendar.set(year, month, day, hour, minute)
                    dateTime.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        box.addView(selectDate)
        val qty = crearCampo(this, "Cantidad", android.text.InputType.TYPE_CLASS_NUMBER).apply { setText("1") }
        agregarTitulo(box, "Cantidad (para pedidos)", this); agregarCampo(box, qty, this)
        val payment = crearSpinner(this, listOf("Pagar en el establecimiento", "Pago en línea (pendiente de pasarela)"))
        agregarTitulo(box, "Cómo deseas pagar", this)
        box.addView(payment, LinearLayout.LayoutParams(-1, dp(48)))
        val note = crearCampo(this, "Nota para el negocio (opcional)")
        agregarTitulo(box, "Nota", this); agregarCampo(box, note, this)

        AlertDialog.Builder(this)
            .setTitle("Reservar: ${item.titulo}")
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar reserva", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try {
                            val mode = possibleModes[modeSpinner.selectedItemPosition]
                            val count = if (mode == "CITA") 1 else (qty.text.toString().toIntOrNull() ?: 0)
                            val paymentMode = if (payment.selectedItemPosition == 0) "EN_LOCAL" else "EN_LINEA"
                            val reservation = repository.crearReserva(
                                business.id, item, name.text.toString(), phone.text.toString(), email.text.toString(),
                                mode, dateTime.text.toString().takeIf { it != "Selecciona fecha y hora" } ?: "",
                                count, paymentMode, note.text.toString()
                            )
                            prefs.edit().putInt(PREF_USUARIO_ID, repository.registrarCliente(name.text.toString(), phone.text.toString(), email.text.toString()))
                                .putString(PREF_USUARIO_NOMBRE, name.text.toString().trim())
                                .putString(PREF_USUARIO_TELEFONO, phone.text.toString().trim())
                                .putString(PREF_USUARIO_EMAIL, email.text.toString().trim()).apply()
                            dialog.dismiss()
                            val message = "Reserva ${reservation.codigo} creada. Total: S/ ${String.format(Locale.US, "%.2f", reservation.total)}. Estado: SOLICITADA."
                            AlertDialog.Builder(this).setTitle("Solicitud registrada").setMessage(
                                "$message\n\nEl negocio debe confirmarla. " + if (paymentMode == "EN_LINEA") "El pago en línea queda pendiente hasta integrar una pasarela real." else "El pago se realiza directamente en el establecimiento."
                            ).setPositiveButton("Aceptar", null).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message ?: "No se pudo crear la reserva.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                dialog.show()
            }
    }
}

/** Panel básico del proveedor registrado. */
class PanelNegocioActivity : AppCompatActivity() {
    private lateinit var repository: MarketplaceRepository
    private var negocioId: Int = -1
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MarketplaceRepository(this)
        negocioId = intent.getIntExtra("NEGOCIO_ID", -1).takeIf { it > 0 }
            ?: getSharedPreferences(PREFS_MARKETPLACE, MODE_PRIVATE).getInt(PREF_NEGOCIO_ID, -1)
        if (negocioId <= 0) {
            Toast.makeText(this, "No se encontró un negocio registrado en este dispositivo.", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val root = crearContenedor(this)
        root.addView(crearTexto(this, "Panel del negocio", 24f, Color.rgb(30, 41, 59), true))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this); scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(crearBoton(this, "+ Publicar servicio o producto") { mostrarDialogoPublicacion() })
        root.addView(crearBoton(this, "+ Añadir trabajador (Personal)") { mostrarDialogoPersonal() })
        root.addView(crearBoton(this, "+ Añadir recurso (Silla/Cabina)") { mostrarDialogoRecurso() })
        root.addView(crearBoton(this, "Abrir marketplace") { startActivity(Intent(this, MarketplaceActivity::class.java)) })
        root.addView(crearBoton(this, "Abrir agenda clásica") { startActivity(Intent(this, MainActivity::class.java)) })
        root.addView(crearBoton(this, "Volver al inicio") { finish() })
        setContentView(root)
        cargarPanel()
    }

    override fun onResume() { super.onResume(); if (::content.isInitialized) cargarPanel() }

    private fun cargarPanel() {
        val business = repository.obtenerNegocioParaGestion(negocioId)
        content.removeAllViews()
        if (business == null) {
            content.addView(crearTexto(this, "No fue posible cargar el negocio. Vuelve a registrarlo o verifica la base local."))
            return
        }
        content.addView(crearTexto(this, business.nombre, 20f, Color.rgb(30, 41, 59), true))
        content.addView(crearTexto(this, "${business.categoria} · ${business.distrito}, ${business.provincia}", 13f))
        content.addView(crearTexto(this, "Suscripción: ${repository.obtenerEstadoSuscripcion(negocioId)}", 13f, Color.rgb(37, 99, 235), true))
        content.addView(crearTexto(this, "Modalidades: ${business.modalidades.joinToString(" + ")}", 13f))
        content.addView(crearTexto(this, "Tus publicaciones", 17f, Color.rgb(30, 41, 59), true).apply { setPadding(0, dp(16), 0, dp(6)) })
        val items = repository.obtenerPublicacionesNegocioDePropietario(negocioId)
        if (items.isEmpty()) content.addView(crearTexto(this, "Todavía no has publicado nada. Utiliza el botón para crear tu primer servicio o producto."))
        items.forEach { item ->
            val card = MaterialCardView(this).apply { radius = dp(12).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1) }
            val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)) }
            inner.addView(crearTexto(this, item.titulo, 16f, Color.rgb(30, 41, 59), true))
            inner.addView(crearTexto(this, "${item.tipo} · ${item.modalidad} · S/ ${String.format(Locale.US, "%.2f", item.precio)}", 13f))
            if (item.descripcion.isNotBlank()) inner.addView(crearTexto(this, item.descripcion, 12f))
            card.addView(inner)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        // Personal / Trabajadores
        content.addView(crearTexto(this, "Personal / Trabajadores", 17f, Color.rgb(30, 41, 59), true).apply { setPadding(0, dp(14), 0, dp(6)) })
        val personalList = repository.obtenerPersonalNegocio(negocioId)
        if (personalList.isEmpty()) {
            content.addView(crearTexto(this, "No hay trabajadores registrados. Añade personal (ej. Barbero 1)."))
        } else {
            personalList.forEach { p ->
                val pCard = MaterialCardView(this).apply { radius = dp(8).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1) }
                val pInner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), dp(8), dp(12), dp(8)); gravity = Gravity.CENTER_VERTICAL }
                val pInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                pInfo.addView(crearTexto(this, p.nombre, 15f, Color.rgb(30, 41, 59), true))
                if (p.especialidad.isNotBlank()) pInfo.addView(crearTexto(this, p.especialidad, 12f))
                pInfo.addView(crearTexto(this, "Estado: ${p.estado}", 11f, if (p.estado == "ACTIVO") Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68), true))
                pInner.addView(pInfo)
                val btnToggle = crearBoton(this, if (p.estado == "ACTIVO") "Inactivar" else "Activar") {
                    val nuevo = if (p.estado == "ACTIVO") "INACTIVO" else "ACTIVO"
                    repository.cambiarEstadoPersonal(p.id, nuevo)
                    cargarPanel()
                }
                pInner.addView(btnToggle)
                pCard.addView(pInner)
                content.addView(pCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }

        // Recursos Físicos
        content.addView(crearTexto(this, "Recursos Físicos (Sillas / Equipos)", 17f, Color.rgb(30, 41, 59), true).apply { setPadding(0, dp(14), 0, dp(6)) })
        val recursosList = repository.obtenerRecursosNegocio(negocioId)
        if (recursosList.isEmpty()) {
            content.addView(crearTexto(this, "No hay recursos registrados. Añade sillas, cabinas o equipos (ej. Silla 1)."))
        } else {
            recursosList.forEach { r ->
                val rCard = MaterialCardView(this).apply { radius = dp(8).toFloat(); setCardBackgroundColor(Color.WHITE); strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1) }
                val rInner = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(12), dp(8), dp(12), dp(8)); gravity = Gravity.CENTER_VERTICAL }
                val rInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
                rInfo.addView(crearTexto(this, r.nombre, 15f, Color.rgb(30, 41, 59), true))
                rInfo.addView(crearTexto(this, "Tipo: ${r.tipo} · Estado: ${r.estado}", 12f))
                rInner.addView(rInfo)
                val btnToggleR = crearBoton(this, if (r.estado == "ACTIVO") "Inactivar" else "Activar") {
                    val nuevo = if (r.estado == "ACTIVO") "INACTIVO" else "ACTIVO"
                    repository.cambiarEstadoRecurso(r.id, nuevo)
                    cargarPanel()
                }
                rInner.addView(btnToggleR)
                rCard.addView(rInner)
                content.addView(rCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }
        content.addView(crearTexto(this, "Reservas recibidas", 17f, Color.rgb(30, 41, 59), true).apply { setPadding(0, dp(18), 0, dp(6)) })
        val reservations = repository.obtenerReservasNegocio(negocioId)
        if (reservations.isEmpty()) content.addView(crearTexto(this, "Todavía no hay reservas. Cuando un cliente reserve desde el marketplace, aparecerá aquí."))
        reservations.forEach { reservation ->
            val bookingCard = MaterialCardView(this).apply {
                radius = dp(12).toFloat(); setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.rgb(226, 232, 240); strokeWidth = dp(1)
            }
            val bookingContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)) }
            bookingContent.addView(crearTexto(this, "${reservation.codigo} · ${reservation.estado}", 15f, Color.rgb(30, 41, 59), true))
            bookingContent.addView(crearTexto(this, "${reservation.cliente} · ${reservation.telefono}", 13f))
            bookingContent.addView(crearTexto(this, "${reservation.publicacion} · ${reservation.modalidad} · ${reservation.fechaHora}", 12f))
            bookingContent.addView(crearTexto(this, "Cantidad: ${reservation.cantidad} · Total: S/ ${String.format(Locale.US, "%.2f", reservation.total)}", 13f))
            bookingContent.addView(crearTexto(this, "Pago: ${reservation.modalidadPago} · ${reservation.estadoPago}", 12f, Color.rgb(71, 85, 105)))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            when (reservation.estado) {
                "SOLICITADA" -> {
                    actions.addView(crearBoton(this, "Confirmar") { cambiarEstadoReserva(reservation.id, "CONFIRMADA") })
                    actions.addView(crearBoton(this, "Cancelar") { cambiarEstadoReserva(reservation.id, "CANCELADA") })
                }
                "CONFIRMADA" -> {
                    val next = if (reservation.modalidad == "RECOJO") "LISTA" else "COMPLETADA"
                    actions.addView(crearBoton(this, if (next == "LISTA") "Marcar lista" else "Completar") { cambiarEstadoReserva(reservation.id, next) })
                    actions.addView(crearBoton(this, "Cancelar") { cambiarEstadoReserva(reservation.id, "CANCELADA") })
                }
                "LISTA" -> actions.addView(crearBoton(this, "Entregada / completada") { cambiarEstadoReserva(reservation.id, "COMPLETADA") })
            }
            if (actions.childCount > 0) bookingContent.addView(actions)
            bookingCard.addView(bookingContent)
            content.addView(bookingCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        content.addView(crearTexto(this, "Nota: en esta etapa las reservas se guardan localmente. La sincronización multiusuario y los cobros reales se habilitarán al conectar el backend y la pasarela.", 12f).apply { setPadding(0, dp(14), 0, 0) })
    }

    private fun cambiarEstadoReserva(id: Int, estado: String) {
        try {
            if (repository.actualizarEstadoReserva(id, estado)) {
                cargarPanel()
                Toast.makeText(this, "Reserva actualizada: $estado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo cambiar el estado. Puede que la reserva ya haya cambiado.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Error al actualizar la reserva.", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoPublicacion() {
        val business = repository.obtenerNegocioParaGestion(negocioId) ?: return
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        val title = crearCampo(this, "Nombre del servicio o producto")
        val description = crearCampo(this, "Descripción breve")
        val price = crearCampo(this, "Precio en soles", android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val previous = crearCampo(this, "Precio anterior para oferta (opcional)", android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val duration = crearCampo(this, "Duración en minutos (para citas)", android.text.InputType.TYPE_CLASS_NUMBER)
        val stock = crearCampo(this, "Stock disponible (opcional)", android.text.InputType.TYPE_CLASS_NUMBER)
        val type = crearSpinner(this, listOf("Servicio", "Producto"))
        fun modesFor(selectedType: Int): List<String> {
            if (selectedType == 1) return if ("RECOJO" in business.modalidades) listOf("RECOJO") else emptyList()
            val available = mutableListOf<String>()
            if ("CITA" in business.modalidades) available.add("CITA")
            if ("RECOJO" in business.modalidades) available.add("RECOJO")
            if (available.size == 2) available.add("AMBOS")
            return available
        }
        var modes = modesFor(0)
        val mode = crearSpinner(this, modes)
        type.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                modes = modesFor(position)
                val shown = modes.ifEmpty { listOf("No disponible: habilita recogida") }
                mode.adapter = ArrayAdapter(this@PanelNegocioActivity, android.R.layout.simple_spinner_item, shown).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }
        }
        box.addView(crearTexto(this, "Tipo de publicación")); box.addView(type)
        box.addView(crearTexto(this, "Modalidad habilitada")); box.addView(mode)
        listOf(title, description, price, previous, duration, stock).forEach { agregarCampo(box, it, this) }
        AlertDialog.Builder(this).setTitle("Nueva publicación").setView(box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        try {
                            val listingType = if (type.selectedItemPosition == 0) "SERVICIO" else "PRODUCTO"
                            require(modes.isNotEmpty()) { "Para publicar productos, habilita primero los pedidos para recoger." }
                            val listingMode = modes.getOrNull(mode.selectedItemPosition) ?: throw IllegalArgumentException("Selecciona una modalidad.")
                            val amount = price.text.toString().replace(',', '.').toDoubleOrNull() ?: throw IllegalArgumentException("Ingresa un precio válido.")
                            val prior = previous.text.toString().replace(',', '.').toDoubleOrNull()
                            val mins = duration.text.toString().toIntOrNull()
                            val availableStock = stock.text.toString().toIntOrNull()
                            repository.crearPublicacion(negocioId, title.text.toString(), description.text.toString(), listingType, listingMode, amount, prior, mins, availableStock)
                            dialog.dismiss(); cargarPanel()
                            Toast.makeText(this, "Publicación guardada.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message ?: "No se pudo guardar la publicación.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun mostrarDialogoPersonal() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        val nameField = crearCampo(this, "Nombre del trabajador (ej. Barbero 1)")
        val specField = crearCampo(this, "Especialidad o rol (opcional)")
        listOf(nameField, specField).forEach { agregarCampo(box, it, this) }
        AlertDialog.Builder(this).setTitle("Añadir Trabajador").setView(box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    repository.agregarPersonalNegocio(negocioId, nameField.text.toString(), specField.text.toString())
                    cargarPanel()
                    Toast.makeText(this, "Trabajador añadido.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message ?: "Error al añadir trabajador.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun mostrarDialogoRecurso() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)) }
        val nameField = crearCampo(this, "Nombre del recurso (ej. Silla de barbería 1)")
        val typeField = crearCampo(this, "Tipo (SILLA, CABINA, EQUIPO)")
        listOf(nameField, typeField).forEach { agregarCampo(box, it, this) }
        AlertDialog.Builder(this).setTitle("Añadir Recurso").setView(box)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    repository.agregarRecursoNegocio(negocioId, nameField.text.toString(), typeField.text.toString())
                    cargarPanel()
                    Toast.makeText(this, "Recurso añadido.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message ?: "Error al añadir recurso.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }
}
