package cl.GrupoK.securehome.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import cl.GrupoK.securehome.Constantes.BluetoothConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Cliente Bluetooth encargado de manejar la conexión con el módulo HC-05
 * y el envío de comandos al Arduino.
 *
 * Esta clase NO es composable, se usa desde la capa de UI con corrutinas.
 */
class BluetoothClient(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null

    /**
     * Intenta conectar con el dispositivo HC-05 emparejado.
     * Se ejecuta en Dispatchers.IO para no bloquear el hilo principal.
     *
     * @throws IllegalStateException si el bluetooth está desactivado o no existe.
     * @throws IOException si falla la conexión al socket.
     */
    suspend fun connect(): BluetoothDevice = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null) {
            throw IllegalStateException("Este dispositivo no soporta Bluetooth")
        }

        if (bluetoothAdapter.isEnabled.not()) {
            throw IllegalStateException("Bluetooth desactivado. Actívalo e inténtalo de nuevo.")
        }

        // Buscar el dispositivo emparejado por nombre
        val device = bluetoothAdapter.bondedDevices.firstOrNull {
            it.name == BluetoothConstants.HC05_DEVICE_NAME
        } ?: throw IllegalStateException("No se encontró el dispositivo emparejado ${BluetoothConstants.HC05_DEVICE_NAME}")

        // Crear socket RFCOMM
        val uuid = UUID.fromString(BluetoothConstants.SERIAL_UUID)
        val socket = device.createRfcommSocketToServiceRecord(uuid)

        try {
            bluetoothAdapter.cancelDiscovery()
            socket.connect()
            bluetoothSocket = socket
        } catch (e: IOException) {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            throw IOException("Error al conectar con el dispositivo: ${e.message}")
        }

        device
    }

    /**
     * Desconecta el socket Bluetooth si está conectado.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            bluetoothSocket?.close()
        } catch (_: IOException) {
        } finally {
            bluetoothSocket = null
        }
    }

    /**
     * Envía un comando de texto al Arduino mediante el socket Bluetooth.
     * Importante: llamar SOLO cuando ya esté conectado.
     *
     * @param command Cadena a enviar (por ejemplo "1" o "0").
     */
    suspend fun sendCommand(command: String) = withContext(Dispatchers.IO) {
        val socket = bluetoothSocket ?: throw IllegalStateException("No hay conexión Bluetooth activa")
        try {
            socket.outputStream.write(command.toByteArray())
            socket.outputStream.flush()
        } catch (e: IOException) {
            throw IOException("Error al enviar comando: ${e.message}")
        }
    }

    /**
     * Indica si el socket está conectado actualmente.
     */
    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true
}
