package cl.GrupoK.securehome.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import cl.GrupoK.securehome.data.BluetoothManager as MyBluetoothManager
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission") // Se gestionan los permisos dinámicamente
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Configuración Bluetooth del Sistema ---
    val systemService = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val btAdapter = systemService.adapter
    val bluetoothManager = remember { MyBluetoothManager(btAdapter) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    var hasBluetoothPermission by remember { mutableStateOf(hasBluetoothPermissions(context)) }

    // --- Estados UI ---
    var isConnected by remember { mutableStateOf(false) }
    var ledState by remember { mutableStateOf(false) }
    var connectionStatusText by remember { mutableStateOf("Desconectado") }

    // Estados para el Escaneo de Dispositivos
    var showDeviceListDialog by remember { mutableStateOf(false) }
    var foundDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var isScanning by remember { mutableStateOf(false) }

    val startDiscoveryWithPermissionsGranted = {
        if (btAdapter?.isEnabled == true) {
            foundDevices = emptyList()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                btAdapter?.bondedDevices?.forEach { device ->
                    if (!foundDevices.contains(device) && !device.name.isNullOrBlank()) {
                        foundDevices = foundDevices + device
                    }
                }
            }
            btAdapter.startDiscovery()
            isScanning = true
            showDeviceListDialog = true
        } else {
            Toast.makeText(context, "Active el Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Gestión de Permisos (Android 12+) ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = hasBluetoothPermissions(context)
        val canUseBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                    permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }

        if (canUseBluetooth) {
            startDiscoveryWithPermissionsGranted()
        } else {
            Toast.makeText(context, "Permisos Bluetooth requeridos", Toast.LENGTH_SHORT).show()
        }
    }

    val requestOrStartDiscovery = {
        if (hasBluetoothPermission) {
            startDiscoveryWithPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // --- BroadcastReceiver para detectar dispositivos ---
    DisposableEffect(hasBluetoothPermission) {
        if (!hasBluetoothPermission) {
            onDispose { }
        } else {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when(intent?.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            device?.let {
                                if (!foundDevices.contains(it) && !it.name.isNullOrBlank()) {
                                    foundDevices = foundDevices + it
                                }
                            }
                        }
                    }
                }
            }
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)

            onDispose {
                context.unregisterReceiver(receiver)
                if (isScanning) btAdapter?.cancelDiscovery()
            }
        }
    }

    // --- UI Principal ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Panel de Control", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // Indicador de Estado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(if (isConnected) Color.Green else Color.Red, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = connectionStatusText)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón 1: Buscar Dispositivos (Solo habilitado si NO está conectado)
        Button(
            onClick = { requestOrStartDiscovery() },
            enabled = !isConnected, // Se deshabilita si ya estamos conectados
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar Dispositivos")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón 2: Desconectar
        Button(
            onClick = {
                bluetoothManager.closeConnection()
                isConnected = false
                connectionStatusText = "Desconectado"
                Toast.makeText(context, "Dispositivo desconectado", Toast.LENGTH_SHORT).show()
            },
            enabled = isConnected, // Solo se habilita si hay conexión activa
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error // Color rojo para indicar desconexión
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Desconectar")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Control (Switch)
        Text("Control de Iluminación", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Switch(
            checked = ledState,
            enabled = isConnected,
            onCheckedChange = { newState ->
                ledState = newState
                scope.launch {
                    bluetoothManager.sendCommand(if (newState) "1" else "0")
                }
            },
            modifier = Modifier.scale(1.5f)
        )

        Text(if (ledState) "ENCENDIDO" else "APAGADO")

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                bluetoothManager.closeConnection()
                navController.navigate("login_screen") {
                    popUpTo("login_screen") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión")
        }
    }

    // --- Diálogo de Selección de Dispositivos ---
    if (showDeviceListDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeviceListDialog = false
                if (hasBluetoothPermission) btAdapter?.cancelDiscovery()
                isScanning = false
            },
            title = { Text("Dispositivos Encontrados") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(200.dp) // Altura fija para la lista
                ) {
                    if (foundDevices.isEmpty()) {
                        item { Text("Escaneando...", modifier = Modifier.padding(8.dp)) }
                    }
                    items(foundDevices) { device ->
                        Text(
                            text = "${device.name ?: "Desconocido"} (${device.address})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!hasBluetoothPermission) {
                                        permissionLauncher.launch(requiredPermissions)
                                        Toast.makeText(context, "Permisos Bluetooth requeridos para conectar", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    btAdapter?.cancelDiscovery()
                                    isScanning = false
                                    showDeviceListDialog = false

                                    connectionStatusText = "Conectando a ${device.name}..."
                                    scope.launch {
                                        val success = bluetoothManager.connect(device.address)
                                        if (success) {
                                            isConnected = true
                                            connectionStatusText = "Conectado a ${device.name}"
                                            Toast.makeText(context, "Conectado", Toast.LENGTH_SHORT).show()
                                        } else {
                                            connectionStatusText = "Error de Conexión"
                                            Toast.makeText(context, "Falló la conexión", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp)
                        )
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeviceListDialog = false
                    if (hasBluetoothPermission) btAdapter?.cancelDiscovery()
                    isScanning = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

// Extensión para escalar UI
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)
