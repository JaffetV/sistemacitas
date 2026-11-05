package com.example.comunamadsistemacitas.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.comunamadsistemacitas.R
import com.example.comunamadsistemacitas.model.Cita
import com.example.comunamadsistemacitas.model.EstadoCita
import java.util.Locale

/**
 * CitaAdapter: Adaptador de RecyclerView con soporte para visualización de badges de estado y seña abonada.
 */
class CitaAdapter(
    private var listaCitas: List<Cita>,
    private val onItemClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    class CitaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCliente: TextView = itemView.findViewById(R.id.tvCliente)
        val tvServicio: TextView = itemView.findViewById(R.id.tvServicio)
        val tvFechaHora: TextView = itemView.findViewById(R.id.tvFechaHora)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = listaCitas[position]
        val context = holder.itemView.context

        holder.tvCliente.text = cita.cliente
        holder.tvServicio.text = cita.servicio
        holder.tvFechaHora.text = cita.fechaHora

        if (cita.montoSena > 0.0) {
            holder.tvPrecio.text = String.format(Locale.getDefault(), "S/ %.2f (Seña: S/ %.2f)", cita.precio, cita.montoSena)
        } else {
            holder.tvPrecio.text = String.format(Locale.getDefault(), "S/ %.2f", cita.precio)
        }

        holder.tvEstado.text = cita.estado.name

        // Configuración dinámica de colores de tarjetas según estado
        when (cita.estado) {
            EstadoCita.PENDIENTE -> {
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.estado_pendiente_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.estado_pendiente_text))
            }
            EstadoCita.ATENDIDO -> {
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.estado_atendido_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.estado_atendido_text))
            }
            EstadoCita.CANCELADO, EstadoCita.NO_SHOW -> {
                holder.tvEstado.setBackgroundColor(ContextCompat.getColor(context, R.color.estado_cancelado_bg))
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.estado_cancelado_text))
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(cita)
        }
    }

    override fun getItemCount(): Int = listaCitas.size

    fun actualizarLista(nuevaLista: List<Cita>) {
        listaCitas = nuevaLista
        notifyDataSetChanged()
    }
}
