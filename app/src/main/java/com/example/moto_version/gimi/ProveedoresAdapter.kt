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


class ProveedoresAdapter(private val context: Context, private var proveedores: List<Proveedor>?) :
    RecyclerView.Adapter<ProveedoresAdapter.ProveedorViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proveedor, parent, false)
        return ProveedorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
        val proveedor = proveedores!![position]
        holder.tvNombreEmpresa.text = proveedor.nombreEmpresa
        holder.tvNombreCompleto.text = proveedor.nombreCompleto
        holder.tvEmail.text = proveedor.email

        holder.btnLlamar.setOnClickListener { v: View? ->
            val phone = proveedor.phone
            if (phone != null && !phone.isEmpty()) {
                val intent =
                    Intent(Intent.ACTION_DIAL)
                intent.setData(Uri.parse("tel:$phone"))
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (proveedores != null) proveedores!!.size else 0
    }

    fun updateProveedores(newProveedores: List<Proveedor>?) {
        this.proveedores = newProveedores
        notifyDataSetChanged()
    }

    class ProveedorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvNombreEmpresa: TextView = itemView.findViewById<TextView>(R.id.tvNombreEmpresa)
        var tvNombreCompleto: TextView = itemView.findViewById<TextView>(R.id.tvNombreCompleto)
        var tvEmail: TextView = itemView.findViewById<TextView>(R.id.tvEmail)
        var btnLlamar: ImageButton = itemView.findViewById<ImageButton>(R.id.btnLlamar)
    }
}