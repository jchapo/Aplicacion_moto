package com.example.moto_version.gimi

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class MotorizadosFragment : Fragment() {

    private lateinit var recyclerViewMotorizados: RecyclerView
    private lateinit var adapter: MotorizadosAdapter
    private lateinit var fabAgregarMotorizado: FloatingActionButton
    private lateinit var listaMotorizados: MutableList<Usuario>
    private lateinit var tvEmptyView: View
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_usuarios, container, false)

        // Inicialización de vistas
        recyclerViewMotorizados = view.findViewById(R.id.recyclerViewUsuarios)
        tvEmptyView = view.findViewById(R.id.tvEmptyViewUsuarios)
        fabAgregarMotorizado = view.findViewById(R.id.fabAgregarUsuario)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de Firestore
        db = FirebaseFirestore.getInstance()

        // Configuración del RecyclerView
        listaMotorizados = mutableListOf()
        adapter = MotorizadosAdapter(requireContext(), listaMotorizados)
        recyclerViewMotorizados.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewMotorizados.adapter = adapter

        // Cargar motorizados desde Firestore
        cargarMotorizados()

        // Configuración del FloatingActionButton
        fabAgregarMotorizado.setOnClickListener {
            val intent = Intent(requireContext(), AgregarUsuarioActivity::class.java)
            intent.putExtra("tipoUsuario", "Motorizado")
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
        // Recargar motorizados al volver a la actividad
        cargarMotorizados()
    }

    private fun cargarMotorizados() {
        val debugFlag = true // Activar o desactivar la depuración

        db.collection("usuarios")
            .whereEqualTo("rol", "Motorizado").get()
            .addOnCompleteListener { task: Task<QuerySnapshot> ->
                if (task.isSuccessful) {
                    listaMotorizados.clear()

                    for (document in task.result) {
                        val proveedor = document.toObject(Usuario::class.java)
                        listaMotorizados.add(proveedor)
                    }

                    adapter.notifyDataSetChanged()

                    // Mostrar mensaje si no hay motorizados
                    if (listaMotorizados.isEmpty()) {
                        recyclerViewMotorizados.visibility = View.GONE
                        tvEmptyView.visibility = View.VISIBLE
                    } else {
                        recyclerViewMotorizados.visibility = View.VISIBLE
                        tvEmptyView.visibility = View.GONE
                    }
                } else {
                    if (debugFlag) Log.e("DEBUG", "Error al obtener motorizados", task.exception)
                }
            }
    }

}
