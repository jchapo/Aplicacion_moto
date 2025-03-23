package com.example.moto_version.gimi

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.R
import com.example.moto_version.models.PagoRegistro
import android.view.ViewGroup
import android.view.LayoutInflater


class PagosAdapter(private val pagos: List<PagoRegistro>) :
    RecyclerView.Adapter<PagosAdapter.PagoViewHolder>() {

    class PagoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMetodo: TextView = view.findViewById(R.id.tvMetodoPago)
        val tvReceptor: TextView = view.findViewById(R.id.tvReceptor)
        val tvMonto: TextView = view.findViewById(R.id.tvMonto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pago, parent, false)
        return PagoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        val pago = pagos[position]
        holder.tvMetodo.text = pago.metodoPago
        holder.tvReceptor.text = pago.receptor
        holder.tvMonto.text = "S/ ${pago.monto}"
    }

    override fun getItemCount() = pagos.size
}