package cl.GrupoK.securehome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.GrupoK.securehome.data.BluetoothClient
import cl.GrupoK.securehome.ui.theme.ControlScreen
import cl.GrupoK.securehome.ui.theme.LoginScreen

/**
 * Activity principal de SecureHome.
 * Configura la navegación entre Login y Panel de Control.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Instancia única de BluetoothClient para toda la app
            val bluetoothClient = remember { BluetoothClient(this) }
            val navController = rememberNavController()

            SecureHomeApp(
                navController = navController,
                bluetoothClient = bluetoothClient
            )
        }
    }
}

/**
 * Composable raíz de la aplicación SecureHome.
 */
@Composable
fun SecureHomeApp(
    navController: NavHostController,
    bluetoothClient: BluetoothClient
) {
    Scaffold { innerPadding ->
        SecureHomeNavHost(
            navController = navController,
            bluetoothClient = bluetoothClient,
            contentPadding = innerPadding
        )
    }
}

/**
 * Define el NavHost con las rutas de la app.
 *
 * Rutas:
 * - "login": pantalla inicial
 * - "control": dashboard de control
 */
@Composable
fun SecureHomeNavHost(
    navController: NavHostController,
    bluetoothClient: BluetoothClient,
    contentPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("control") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                contentPadding = contentPadding
            )
        }

        composable("control") {
            ControlScreen(
                bluetoothClient = bluetoothClient,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("control") { inclusive = true }
                    }
                },
                contentPadding = contentPadding
            )
        }
    }
}
