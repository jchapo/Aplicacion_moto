package com.example.moto_version

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MiAdapter(private var lista: List<String>) : RecyclerView.Adapter<MiAdapter.MiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MiViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiViewHolder, position: Int) {
        val documentoId = lista[position]
        holder.textView.text = "ID: $documentoId"

        // Alternar colores para cada elemento
        if (position % 2 == 0) {
            holder.cardView.setCardBackgroundColor(0xFFE3F2FD.toInt()) // Azul claro
        } else {
            holder.cardView.setCardBackgroundColor(0xFFFFF9C4.toInt()) // Amarillo claro
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<String>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    class MiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val cardView: CardView = itemView as CardView
    }
}

