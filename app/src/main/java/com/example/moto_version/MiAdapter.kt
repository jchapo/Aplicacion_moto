package com.example.moto_version

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

        // Imagen por defecto (puedes cambiar esto para cargar imágenes dinámicamente)
        holder.imagenRecojo.setImageResource(R.drawable.imagen)
    }

    override fun getItemCount(): Int = listaRecojos.size

    fun actualizarLista(nuevaLista: List<Recojo>) {
        listaRecojos = nuevaLista
        notifyDataSetChanged()
    }

    fun obtenerNombreCliente(index: Int): String? {
        return if (index in listaRecojos.indices) listaRecojos[index].clienteNombre else null
    }

}
