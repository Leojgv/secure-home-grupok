package cl.GrupoK.securehome.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import cl.GrupoK.securehome.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothManager(private val bluetoothAdapter: BluetoothAdapter?) {

    private var socket: BluetoothSocket? = null

    // Verifica si el socket está conectado
    fun isConnected(): Boolean = socket?.isConnected == true

    /**
     * Intenta conectar al dispositivo Bluetooth seleccionado.
     * @param address Dirección MAC del dispositivo.
     * @return true si la conexión fue exitosa.
     */
    @SuppressLint("MissingPermission") // Los permisos se deben verificar en la UI antes de llamar
    suspend fun connect(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(address)
                // Se usa el UUID inseguro genérico para mayor compatibilidad con módulos chinos
                socket = device?.createRfcommSocketToServiceRecord(Constants.BT_UUID)
                socket?.connect()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                closeConnection()
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Envía un comando (carácter) al Arduino.
     * @param command "1" para ON, "0" para OFF.
     */
    suspend fun sendCommand(command: String) {
        withContext(Dispatchers.IO) {
            try {
                if (socket != null && isConnected()) {
                    socket?.outputStream?.write(command.toByteArray())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun closeConnection() {
        try {
            socket?.close()
            socket = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}