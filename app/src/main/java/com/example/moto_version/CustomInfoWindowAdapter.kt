package com.example.moto_version

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.example.moto_version.R

class CustomInfoWindowAdapter(private val inflater: LayoutInflater) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        return null // Usar el diseño personalizado en getInfoContents
    }

    override fun getInfoContents(marker: Marker): View {
        val view = inflater.inflate(R.layout.custom_info_window, null)

        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val tvDescripcion = view.findViewById<TextView>(R.id.tvDescripcion)

        val lines = marker.title?.split("\n") ?: listOf("Sin título")
        tvTitulo.text = lines.getOrNull(0) ?: ""
        tvDescripcion.text = lines.getOrNull(1) ?: ""

        return view
    }
}
