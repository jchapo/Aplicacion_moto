package com.example.moto_version.gimi

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.moto_version.LoginActivity
import com.example.moto_version.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth


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
                    FirebaseAuth.getInstance().signOut()  // Cerrar sesión en Firebase
                    val intent = Intent(this, LoginActivity::class.java)  // Redirigir a la pantalla de login
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK  // Eliminar historial de actividades
                    startActivity(intent)
                    finish()  // Cerrar la actividad actual
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtra la lista del RecyclerView en el fragmento
                mapaPedidoFragment?.filterList(newText)
                return true
            }
        })

        return true
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
