package cl.GrupoK.securehome.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.GrupoK.securehome.utils.Constants

@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "SecureHome Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        if (isError) {
            Text(
                text = "Credenciales incorrectas o campos vacíos",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Validación de Integridad (Campos no vacíos)
                if (username.isBlank() || password.isBlank()) {
                    isError = true
                    return@Button
                }

                // Validación de Credenciales (Seguridad OT)
                if (username == Constants.USER_ADMIN && password == Constants.PASS_ADMIN) {
                    isError = false
                    Toast.makeText(context, "Acceso Autorizado", Toast.LENGTH_SHORT).show()
                    // Navegar al Dashboard y eliminar Login del historial (para que 'Atrás' cierre la app)
                    navController.navigate("dashboard_screen") {
                        popUpTo("login_screen") { inclusive = true }
                    }
                } else {
                    isError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("INGRESAR")
        }
    }
}