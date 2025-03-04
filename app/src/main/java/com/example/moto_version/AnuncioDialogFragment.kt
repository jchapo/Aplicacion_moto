package com.example.moto_version

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class AnuncioDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_anuncio) // Usa el layout del modal
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) // Pantalla completa
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Fondo transparente

        val btnCerrar = dialog.findViewById<Button>(R.id.btnCerrar)
        btnCerrar.setOnClickListener { dismiss() } // Cerrar el modal

        return dialog
    }
}
