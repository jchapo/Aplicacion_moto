package com.example.moto_version.gimi

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
import com.bumptech.glide.Glide
import com.example.moto_version.moto.DetalleRecojoActivity
import com.example.moto_version.R
import com.example.moto_version.models.ClienteRecojo

class GimiMiAdapter(private var listaRecojos: List<ClienteRecojo>) : RecyclerView.Adapter<GimiMiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagenRecojoItem: ImageView = view.findViewById(R.id.imagenRecojoItem)
        val tvGimiNombre: TextView = view.findViewById(R.id.tvClienteNombre)
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

        holder.tvGimiNombre.text = recojo.clienteNombre
        holder.tvProveedorNombre.text = recojo.proveedorNombre
        holder.tvPrecio.text = "S/ ${recojo.pedidoCantidadCobrar}"
        Log.d("GlideDebug", "URL de la imagen: ${recojo.thumbnailFotoRecojo}")
        if (recojo.fechaRecojoPedidoMotorizado != null) {
            // Cargar la imagen desde thumbnailFotoRecojo si la fecha no es null
            Glide.with(holder.itemView.context)
                .load(recojo.thumbnailFotoRecojo)
                //.placeholder(R.drawable.loading_image) // Imagen de carga opcional
                .error(R.drawable.fondo_gris_nanpi) // Imagen de error si falla la carga
                .into(holder.imagenRecojoItem)
        } else {
            // Usar imagen por defecto si fechaRecojoPedidoMotorizado es null
            holder.imagenRecojoItem.setImageResource(R.drawable.fondo_gris_nanpi)
        }


        // Verificar si el pedido está anulado
        if (recojo.fechaAnulacionPedido != null) {
            // Si está anulado, cambiar el color de fondo del CardView a rojo
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(
                holder.itemView.context, R.color.md_theme_errorContainer
            )
        }
        // Verificar si el pedido ha sido entregado
        else if (recojo.fechaEntregaPedidoMotorizado != null) {
            // Si ha sido entregado, cambiar el color de fondo del CardView a verde
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(
                holder.itemView.context, R.color.green
            )
        }
        // Verificar si el pedido ha sido recogido pero aún no entregado
        else if (recojo.fechaRecojoPedidoMotorizado != null) {
            // Si ha sido recogido, pero no entregado, cambiar el color a azul
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(
                holder.itemView.context, R.color.md_theme_primaryFixedDim
            )
        }
    // Pedido aún no recogido
        else {
            // Si el pedido no ha sido recogido, cambiar el color a amarillo
            holder.itemCard.backgroundTintList = ContextCompat.getColorStateList(
                holder.itemView.context, R.color.amarillo
            )
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

    fun actualizarLista(nuevaLista: List<ClienteRecojo>) {
        listaRecojos = nuevaLista
        notifyDataSetChanged()
    }
}