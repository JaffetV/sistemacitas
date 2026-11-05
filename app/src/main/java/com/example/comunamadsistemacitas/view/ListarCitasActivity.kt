package com.example.comunamadsistemacitas.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comunamadsistemacitas.MainActivity
import com.example.comunamadsistemacitas.R
import com.example.comunamadsistemacitas.data.repository.CitaRepository
import com.example.comunamadsistemacitas.view.adapter.CitaAdapter
import com.google.android.material.button.MaterialButton

/**
 * ListarCitasActivity: Muestra la lista de citas almacenadas en SQLite con tarjetas interactivas.
 * Incluye navegación directa Atrás e Inicio.
 */
class ListarCitasActivity : AppCompatActivity() {

    private lateinit var rvCitas: RecyclerView
    private lateinit var adapter: CitaAdapter
    private lateinit var repository: CitaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_citas)

        repository = CitaRepository(this)

        val btnAtras = findViewById<MaterialButton>(R.id.btnAtras)
        val btnInicio = findViewById<MaterialButton>(R.id.btnInicio)

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

        rvCitas = findViewById(R.id.rvCitas)
        rvCitas.layoutManager = LinearLayoutManager(this)

        val listaCitas = repository.obtenerTodasLasCitas()
        adapter = CitaAdapter(listaCitas) { cita ->
            val intent = Intent(this, DetalleCitaActivity::class.java)
            intent.putExtra("CITA_ID", cita.id)
            startActivity(intent)
        }
        rvCitas.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.actualizarLista(repository.obtenerTodasLasCitas())
        }
    }
}
