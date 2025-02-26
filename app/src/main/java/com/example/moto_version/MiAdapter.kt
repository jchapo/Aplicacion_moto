package com.example.moto_version

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.models.Recojo

class MiAdapter(private var listaRecojos: List<Recojo>) : RecyclerView.Adapter<MiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagenRecojo: ImageView = view.findViewById(R.id.imagenRecojo)
        val tvClienteNombre: TextView = view.findViewById(R.id.tvClienteNombre)
        val tvProveedorNombre: TextView = view.findViewById(R.id.tvProveedorNombre)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recojo = listaRecojos[position]

        holder.tvClienteNombre.text = recojo.clienteNombre
        holder.tvProveedorNombre.text = recojo.proveedorNombre
        holder.tvPrecio.text = "S/ ${recojo.pedidoCantidadCobrar}"
        holder.imagenRecojo.setImageResource(R.drawable.imagen)

        // Agregar clic para abrir una nueva actividad
        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, DetalleRecojoActivity::class.java).apply {
                putExtra("id", recojo.id)
                putExtra("clienteNombre", recojo.clienteNombre)
                putExtra("proveedorNombre", recojo.proveedorNombre)
                putExtra("pedidoCantidadCobrar", recojo.pedidoCantidadCobrar)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listaRecojos.size

    fun actualizarLista(nuevaLista: List<Recojo>) {
        listaRecojos = nuevaLista
        notifyDataSetChanged()
    }
}

