package cl.GrupoK.securehome

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.GrupoK.securehome.data.BluetoothClient
import cl.GrupoK.securehome.ui.theme.ControlScreen
import cl.GrupoK.securehome.ui.theme.LoginScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Pedir permisos BT en Android 12+
        requestBluetoothPermissionsIfNeeded()

        setContent {
            val bluetoothClient = remember { BluetoothClient(this@MainActivity) }
            val navController = rememberNavController()

            SecureHomeApp(navController, bluetoothClient)
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    100
                )
            }
        }
    }
}

@Composable
fun SecureHomeApp(
    navController: NavHostController,
    bluetoothClient: BluetoothClient
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
                }
            )
        }
        composable("control") {
            ControlScreen(
                bluetoothClient = bluetoothClient,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("control") { inclusive = true }
                    }
                }
            )
        }
    }
}
