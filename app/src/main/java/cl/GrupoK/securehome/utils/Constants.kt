package cl.GrupoK.securehome.utils

import java.util.UUID

object Constants {
    // UUID Estándar para módulos HC-05, HC-06
    val BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Credenciales (Simulación)
    const val USER_ADMIN = "admin"
    const val PASS_ADMIN = "1234"
}