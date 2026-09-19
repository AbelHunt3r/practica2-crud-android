package ovh.gabrielhuav.flasklogin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Menú desplegable de navegación, requerido por la práctica: permite llegar
 * a Inicio de Sesión, Registro de Usuario y Operaciones CRUD desde
 * cualquier pantalla. Si hay sesión iniciada, también ofrece cerrarla.
 */
@Composable
fun AppMenu(
    isLoggedIn: Boolean,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.Menu, contentDescription = "Menú de navegación")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(Screen.Login.title) },
            onClick = {
                expanded = false
                onNavigate(Screen.Login)
            }
        )
        DropdownMenuItem(
            text = { Text(Screen.Register.title) },
            onClick = {
                expanded = false
                onNavigate(Screen.Register)
            }
        )
        DropdownMenuItem(
            text = { Text(Screen.Tasks.title) },
            onClick = {
                expanded = false
                onNavigate(Screen.Tasks)
            }
        )
        if (isLoggedIn) {
            DropdownMenuItem(
                text = { Text("Cerrar sesión") },
                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}
