package com.example.comunamadsistemacitas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comunamadsistemacitas.data.repository.CitaRepository
import com.example.comunamadsistemacitas.view.DetalleCitaActivity
import com.example.comunamadsistemacitas.view.ListarCitasActivity
import com.example.comunamadsistemacitas.view.RegistrarCitaActivity
import com.example.comunamadsistemacitas.view.adapter.CitaAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.util.Locale

/**
 * MainActivity: Dashboard Operativo con Botón Flotante Extendido (+ NUEVA CITA) y Tooltip Guía.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repository: CitaRepository
    private lateinit var tvProximaCitaDestacada: TextView
    private lateinit var tvIngresoReal: TextView
    private lateinit var tvIngresoProyectado: TextView
    private lateinit var rvFeedJornada: RecyclerView
    private lateinit var cardTooltipGuia: MaterialCardView
    private lateinit var adapterFeed: CitaAdapter
    private var proximaCitaActual: com.example.comunamadsistemacitas.model.Cita? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = CitaRepository(this)

        tvProximaCitaDestacada = findViewById(R.id.tvProximaCitaDestacada)
        tvIngresoReal = findViewById(R.id.tvIngresoReal)
        tvIngresoProyectado = findViewById(R.id.tvIngresoProyectado)
        rvFeedJornada = findViewById(R.id.rvFeedJornada)
        cardTooltipGuia = findViewById(R.id.cardTooltipGuia)

        val cardProximaCita = findViewById<MaterialCardView>(R.id.cardProximaCita)
        val btnIrMarketplace = findViewById<MaterialButton>(R.id.btnIrMarketplace)
        val btnVerTodas = findViewById<MaterialButton>(R.id.btnVerTodas)
        val fabAgregarCita = findViewById<ExtendedFloatingActionButton>(R.id.fabAgregarCita)

        rvFeedJornada.layoutManager = LinearLayoutManager(this)

        // Botón al Portal Marketplace Hub
        btnIrMarketplace?.setOnClickListener {
            val intent = Intent(this, com.example.comunamadsistemacitas.view.PortalMarketplaceActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Click en Tarjeta de Próxima Cita
        cardProximaCita?.setOnClickListener {
            val proxima = proximaCitaActual
            if (proxima != null) {
                val intent = Intent(this, DetalleCitaActivity::class.java)
                intent.putExtra("CITA_ID", proxima.id)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        // FAB + NUEVA CITA
        fabAgregarCita?.setOnClickListener {
            cardTooltipGuia?.visibility = View.GONE
            startActivity(Intent(this, RegistrarCitaActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnVerTodas?.setOnClickListener {
            startActivity(Intent(this, ListarCitasActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        actualizarDashboard()
    }

    override fun onResume() {
        super.onResume()
        actualizarDashboard()
    }

    private fun actualizarDashboard() {
        val proxima = repository.obtenerProximaCita()
        proximaCitaActual = proxima
        val ingresoReal = repository.obtenerTotalIngresosAtendidos()
        val ingresoProyectado = repository.obtenerIngresoProyectado()
        val listaCitas = repository.obtenerTodasLasCitas()

        // Notificación de Próxima Cita
        if (proxima != null) {
            tvProximaCitaDestacada.text = "${proxima.cliente} • ${proxima.servicio} (${proxima.fechaHora})"
        } else {
            tvProximaCitaDestacada.text = "Sin citas pendientes en agenda"
        }

        // Métricas Financieras
        tvIngresoReal.text = String.format(Locale.getDefault(), "S/ %.2f", ingresoReal)
        tvIngresoProyectado.text = String.format(Locale.getDefault(), "S/ %.2f", ingresoProyectado)

        // Feed Cronológico
        adapterFeed = CitaAdapter(listaCitas.take(5)) { cita ->
            val intent = Intent(this, DetalleCitaActivity::class.java)
            intent.putExtra("CITA_ID", cita.id)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        rvFeedJornada.adapter = adapterFeed
    }
}
