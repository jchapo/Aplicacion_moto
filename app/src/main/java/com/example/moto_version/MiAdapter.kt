package com.example.moto_version

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.models.Recojo

class MiAdapter(private var listaRecojos: List<Recojo>) : RecyclerView.Adapter<MiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagenRecojoItem: ImageView = view.findViewById(R.id.imagenRecojoItem)
        val tvClienteNombre: TextView = view.findViewById(R.id.tvClienteNombre)
        val tvProveedorNombre: TextView = view.findViewById(R.id.tvProveedorNombre)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val itemCard: CardView = view.findViewById(R.id.itemCard)
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
        holder.imagenRecojoItem.setImageResource(R.drawable.imagen)

        // Verificar si la fecha de recojo es diferente de null
        if (recojo.fechaRecojoPedidoMotorizado != null) {
            // Si es distinto de null, cambiar el color de fondo del CardView usando backgroundTint
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.purple_200)
        } else {
            // Si es null, usar otro color (o el color predeterminado)
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(holder.itemView.context, R.color.purple_700)
        }

        // Agregar clic para abrir una nueva actividad
        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, DetalleRecojoActivity::class.java).apply {
                putExtra("id", recojo.id)
                putExtra("clienteNombre", recojo.clienteNombre)
                putExtra("proveedorNombre", recojo.proveedorNombre)
                putExtra("pedidoCantidadCobrar", recojo.pedidoCantidadCobrar)
                putExtra("pedidoMetodoPago", recojo.pedidoMetodoPago)
                putExtra("fechaRecojoPedidoMotorizado", recojo.fechaRecojoPedidoMotorizado?.seconds)
                putExtra("fechaEntregaPedidoMotorizado", recojo.fechaEntregaPedidoMotorizado?.seconds)
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

