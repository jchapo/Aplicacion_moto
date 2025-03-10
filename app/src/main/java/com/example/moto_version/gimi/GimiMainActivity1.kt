package com.example.moto_version.gimi

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.moto_version.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView


class GimiMainActivity1 : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var navigationView: NavigationView
    private var mapaPedidoFragment: MapaPedidoFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gimi_activity_main_1)

        // Inicializar vistas
        drawerLayout = findViewById(R.id.uno_gimi_drawer_layout)
        toolbar = findViewById(R.id.uno_gimi_toolbar)
        navigationView = findViewById(R.id.uno_gimi_nav_view)

        // Configurar Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Manejar clic en el ícono del menú
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Manejar clics en el menú lateral
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_map_pedidos -> configurarFragmentoYActualizar("pedidos")
                R.id.nav_map_recojos -> configurarFragmentoYActualizar("recojos")
                R.id.nav_map_entregas -> configurarFragmentoYActualizar("entregas")
                R.id.nav_proveedores -> {
                    toolbar.title = "Proveedores"
                    val proveedoresFragment = ProveedoresFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.uno_contenedor_principal, proveedoresFragment)
                        .commit()
                }
                R.id.nav_motorizados -> {
                    toolbar.title = "Motorizados"
                    val motorizadosFragment = MotorizadosFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.uno_contenedor_principal, motorizadosFragment)
                        .commit()
                }
                R.id.nav_cerrar_sesion -> {
                    // Lógica para cerrar sesión
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Cargar el fragmento por defecto
        if (mapaPedidoFragment == null) {
            toolbar.title = "Mapa Pedidos"
            mapaPedidoFragment = MapaPedidoFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.uno_contenedor_principal, mapaPedidoFragment!!)
                .commit()
        }

        // Manejar el botón de retroceso para cerrar el menú si está abierto
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed() // Llama al comportamiento predeterminado
                    isEnabled = true
                }
            }
        })
    }

    private fun configurarFragmentoYActualizar(tipo: String) {
        // Obtener el fragmento actual si ya existe
        val fragment = supportFragmentManager.findFragmentById(R.id.uno_contenedor_principal) as? MapaPedidoFragment

        if (fragment == null) {
            // Si no existe, crear una nueva instancia
            val newFragment = MapaPedidoFragment().apply {
                arguments = Bundle().apply {
                    putString("tipo", tipo)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.uno_contenedor_principal, newFragment)
                .commit()
        } else {
            // Si ya existe, actualizar el fragmento actual
            fragment.actualizarMapaSegunSeleccion(tipo)
        }

        // Actualizar título de la Toolbar
        toolbar.title = when (tipo) {
            "pedidos" -> "Mapa de Pedidos"
            "recojos" -> "Mapa de Recojos"
            "entregas" -> "Mapa de Entregas"
            "proveedores" -> "Proveedores"
            else -> "Mapa"
        }
    }

}
