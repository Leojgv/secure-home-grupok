package cl.GrupoK.securehome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.GrupoK.securehome.ui.DashboardScreen
import cl.GrupoK.securehome.ui.LoginScreen
import cl.GrupoK.securehome.ui.theme.SecureHomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureHomeTheme {
                // Configuración de Navegación
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "login_screen") {

                    // Definición de Pantalla 1: Login
                    composable("login_screen") {
                        LoginScreen(navController = navController)
                    }

                    // Definición de Pantalla 2: Dashboard
                    composable("dashboard_screen") {
                        DashboardScreen(navController = navController)
                    }
                }
            }
        }
    }
}