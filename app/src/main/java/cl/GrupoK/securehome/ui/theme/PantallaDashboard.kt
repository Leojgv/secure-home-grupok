package cl.GrupoK.securehome.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.GrupoK.securehome.data.BluetoothClient
import kotlinx.coroutines.launch

/**
 * Pantalla de Control (Dashboard).
 * Permite conectar/desconectar el Bluetooth y encender/apagar el LED.
 *
 * @param bluetoothClient Instancia de BluetoothClient compartida.
 * @param onLogout Callback para cerrar sesión y volver a la pantalla de login.
 */
@Composable
fun ControlScreen(
    bluetoothClient: BluetoothClient,
    onLogout: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val scope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(bluetoothClient.isConnected()) }
    var isLedOn by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Desconectado") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SecureHome - Panel de Control",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (isConnected) "Estado: Conectado" else "Estado: Desconectado",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )

        // Botón conectar/desconectar
        Button(
            onClick = {
                if (!isConnected) {
                    scope.launch {
                        try {
                            val device = bluetoothClient.connect()
                            isConnected = true
                            statusMessage = "Conectado a ${device.name}"
                        } catch (e: Exception) {
                            isConnected = false
                            statusMessage = "Error al conectar: ${e.message}"
                        }
                    }
                } else {
                    scope.launch {
                        bluetoothClient.disconnect()
                        isConnected = false
                        isLedOn = false
                        statusMessage = "Desconectado"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isConnected) "Desconectar Bluetooth" else "Conectar Bluetooth")
        }

        // Switch para encender/apagar LED
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Luz LED")
            Switch(
                checked = isLedOn,
                onCheckedChange = { checked ->
                    if (!isConnected) {
                        statusMessage = "Conéctate al HC-05 antes de controlar el LED"
                        return@Switch
                    }
                    isLedOn = checked
                    scope.launch {
                        try {
                            // '1' para encender, '0' para apagar
                            val command = if (checked) "1" else "0"
                            bluetoothClient.sendCommand(command)
                            statusMessage = if (checked) "LED Encendido" else "LED Apagado"
                        } catch (e: Exception) {
                            statusMessage = "Error al enviar comando: ${e.message}"
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cerrar sesión
        Button(
            onClick = {
                scope.launch {
                    if (isConnected) {
                        bluetoothClient.disconnect()
                    }
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión")
        }
    }
}
