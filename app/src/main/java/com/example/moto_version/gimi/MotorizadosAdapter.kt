package com.example.moto_version.gimi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.R


class MotorizadosAdapter(private val context: Context, private var motorizados: List<Usuario>?) :
    RecyclerView.Adapter<MotorizadosAdapter.ProveedorViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProveedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
        val proveedor = motorizados!![position]
        holder.tvNombreEmpresa.text = proveedor.nombreEmpresa
        holder.tvNombreCompleto.text = proveedor.nombreCompleto
        holder.tvEmail.text = proveedor.email

        // Acción para el botón de llamada
        holder.btnLlamar.setOnClickListener {
            val phone = proveedor.phone
            if (!phone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$phone")
                context.startActivity(intent)
            }
        }

        // Acción para el clic en el resto del item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AgregarUsuarioActivity::class.java)
            intent.putExtra("userId", proveedor.phone)
            intent.putExtra("tipoUsuario", "Motorizado")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return if (motorizados != null) motorizados!!.size else 0
    }

    fun updateMotorizados(newMotorizados: List<Usuario>?) {
        this.motorizados = newMotorizados
        notifyDataSetChanged()
    }

    class ProveedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvNombreEmpresa: TextView = itemView.findViewById<TextView>(R.id.tvNombreEmpresa)
        var tvNombreCompleto: TextView = itemView.findViewById<TextView>(R.id.tvNombreCompleto)
        var tvEmail: TextView = itemView.findViewById<TextView>(R.id.tvEmail)
        var btnLlamar: ImageButton = itemView.findViewById<ImageButton>(R.id.btnLlamar)
    }
}