package com.example.moto_version.gimi

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moto_version.R
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class ProveedoresFragment : Fragment() {

    private lateinit var recyclerViewProveedores: RecyclerView
    private lateinit var adapter: ProveedoresAdapter
    private lateinit var fabAgregarProveedor: FloatingActionButton
    private lateinit var listaProveedores: MutableList<Proveedor>
    private lateinit var tvEmptyView: View
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_proveedores, container, false)

        // Inicialización de vistas
        recyclerViewProveedores = view.findViewById(R.id.recyclerViewProveedores)
        tvEmptyView = view.findViewById(R.id.tvEmptyView)
        fabAgregarProveedor = view.findViewById(R.id.fabAgregarProveedor)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de Firestore
        db = FirebaseFirestore.getInstance()

        // Configuración del RecyclerView
        listaProveedores = mutableListOf()
        adapter = ProveedoresAdapter(requireContext(), listaProveedores)
        recyclerViewProveedores.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewProveedores.adapter = adapter

        // Cargar proveedores desde Firestore
        cargarProveedores()

        // Configuración del FloatingActionButton
        fabAgregarProveedor.setOnClickListener {
            val intent = Intent(requireContext(), AgregarProveedorActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar proveedores al volver a la actividad
        cargarProveedores()
    }

    private fun cargarProveedores() {
        db.collection("proveedores")
            .get()
            .addOnCompleteListener { task: Task<QuerySnapshot> ->
                if (task.isSuccessful) {
                    listaProveedores.clear()
                    for (document in task.result) {
                        val proveedor = document.toObject(Proveedor::class.java)
                        listaProveedores.add(proveedor)
                    }
                    adapter.notifyDataSetChanged()

                    // Mostrar mensaje si no hay proveedores
                    if (listaProveedores.isEmpty()) {
                        recyclerViewProveedores.visibility = View.GONE
                        tvEmptyView.visibility = View.VISIBLE
                    } else {
                        recyclerViewProveedores.visibility = View.VISIBLE
                        tvEmptyView.visibility = View.GONE
                    }
                }
            }
    }
}
