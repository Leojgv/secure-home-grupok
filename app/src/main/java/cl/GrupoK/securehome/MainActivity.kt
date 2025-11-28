package cl.GrupoK.securehome

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Constantes de configuración del sistema
 * UUID estándar para módulos Bluetooth HC-05/HC-06
 */
object AppConstants {
    const val BT_MODULE_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    const val COMANDO_LED_ON = "1"
    const val COMANDO_LED_OFF = "0"

    // Credenciales de prueba (en producción usar base de datos encriptada)
    const val USER_DEMO = "grupoK"
    const val PASS_DEMO = "1234"
}

/**
 * Activity principal que gestiona la navegación y permisos
 */
class MainActivity : ComponentActivity() {

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permisos Bluetooth concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permisos Bluetooth denegados", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos Bluetooth en Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecureHomeApp()
                }
            }
        }
    }

    /**
     * Solicita permisos Bluetooth
     */
    private fun requestBluetoothPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        bluetoothPermissionLauncher.launch(permissions)
    }
}

/**
 * Componente principal de navegación de la aplicación
 */
@Composable
fun SecureHomeApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("control") {
            ControlScreen(navController = navController)
        }
    }
}

/**
 * Pantalla de Login - Primera pantalla de seguridad OT
 * Valida credenciales antes de permitir acceso al control de actuadores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Campo Usuario
        OutlinedTextField(
            value = usuario,
            onValueChange = {
                usuario = it
                errorMessage = ""
            },
            label = { Text("Usuario") },
            leadingIcon = { Icon(Icons.Default.Person, "Usuario") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Campo Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = ""
            },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, "Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Mensaje de error
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de Login
        Button(
            onClick = {
                isLoading = true
                // Validación de credenciales
                if (usuario == AppConstants.USER_DEMO && password == AppConstants.PASS_DEMO) {
                    errorMessage = ""
                    navController.navigate("control") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    errorMessage = "❌ Credenciales incorrectas"
                }
                isLoading = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && usuario.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Iniciar Sesión", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ayuda de credenciales demo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Credenciales:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text("Usuario: grupok", fontSize = 12.sp)
                Text("Contraseña: 1234", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Pantalla de Control - Dashboard principal
 * Gestiona la conexión Bluetooth y control del actuador (LED)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var isConnected by remember { mutableStateOf(false) }
    var isLedOn by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Desconectado") }
    var isConnecting by remember { mutableStateOf(false) }

    // Socket Bluetooth (se mantiene mientras la pantalla esté activa)
    var bluetoothSocket by remember { mutableStateOf<BluetoothSocket?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // Card de Estado de Conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected)
                    Color(0xFF4CAF50)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = "Estado",
                    tint = if (isConnected) Color.White else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Estado de Conexión",
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color.White else Color.Black
                    )
                    Text(
                        text = statusMessage,
                        fontSize = 14.sp,
                        color = if (isConnected) Color.White else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Conectar/Desconectar Bluetooth
        Button(
            onClick = {
                if (isConnected) {
                    // Desconectar
                    scope.launch {
                        try {
                            bluetoothSocket?.close()
                            bluetoothSocket = null
                            isConnected = false
                            isLedOn = false
                            statusMessage = "Desconectado"
                        } catch (e: Exception) {
                            statusMessage = "Error al desconectar: ${e.message}"
                        }
                    }
                } else {
                    // Conectar
                    isConnecting = true
                    scope.launch {
                        val result = conectarBluetooth(context)
                        bluetoothSocket = result.first

                        if (result.first != null) {
                            isConnected = true
                            statusMessage = "✅ Conectado a HC-05"
                        } else {
                            isConnected = false
                            statusMessage = "❌ ${result.second}"
                        }
                        isConnecting = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ),
            enabled = !isConnecting
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isConnected) "Desconectar" else "Conectar Bluetooth",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Control del LED (solo habilitado si está conectado)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isLedOn) Color(0xFFFFEB3B) else Color(0xFF424242)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "LED",
                    modifier = Modifier.size(64.dp),
                    tint = if (isLedOn) Color(0xFFFF9800) else Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLedOn) "LED ENCENDIDO" else "LED APAGADO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLedOn) Color.Black else Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Switch de control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (isConnected && bluetoothSocket != null) {
                                scope.launch {
                                    val success = enviarComando(
                                        bluetoothSocket!!,
                                        AppConstants.COMANDO_LED_OFF
                                    )
                                    if (success) {
                                        isLedOn = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error al enviar comando",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("APAGAR")
                    }

                    Button(
                        onClick = {
                            if (isConnected && bluetoothSocket != null) {
                                scope.launch {
                                    val success = enviarComando(
                                        bluetoothSocket!!,
                                        AppConstants.COMANDO_LED_ON
                                    )
                                    if (success) {
                                        isLedOn = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error al enviar comando",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = isConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text("ENCENDER")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Función suspendida para conectar con el módulo Bluetooth
 * Busca dispositivo HC-05 emparejado y establece conexión
 *
 * @param context Contexto de la aplicación
 * @return Pair con el socket conectado y mensaje de estado
 */
suspend fun conectarBluetooth(context: android.content.Context): Pair<BluetoothSocket?, String> {
    return withContext(Dispatchers.IO) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                return@withContext Pair(null, "Dispositivo no soporta Bluetooth")
            }

            if (!bluetoothAdapter.isEnabled) {
                return@withContext Pair(null, "Bluetooth desactivado")
            }

            // Verificar permisos
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext Pair(null, "Permisos Bluetooth no concedidos")
            }

            // Buscar dispositivo HC-05
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            val hc05 = pairedDevices.find {
                it.name?.contains("HC-05", ignoreCase = true) == true ||
                        it.name?.contains("HC-06", ignoreCase = true) == true
            }

            if (hc05 == null) {
                return@withContext Pair(null, "HC-05 no emparejado")
            }

            // Crear socket y conectar
            val uuid = UUID.fromString(AppConstants.BT_MODULE_UUID)
            val socket = hc05.createRfcommSocketToServiceRecord(uuid)

            bluetoothAdapter.cancelDiscovery()
            socket.connect()

            Pair(socket, "Conectado exitosamente")

        } catch (e: IOException) {
            Pair(null, "Error de conexión: ${e.message}")
        } catch (e: Exception) {
            Pair(null, "Error: ${e.message}")
        }
    }
}

/**
 * Envía un comando al Arduino a través del socket Bluetooth
 *
 * @param socket Socket Bluetooth conectado
 * @param comando Comando a enviar ('1' o '0')
 * @return true si el envío fue exitoso
 */
suspend fun enviarComando(socket: BluetoothSocket, comando: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            socket.outputStream.write(comando.toByteArray())
            socket.outputStream.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
}