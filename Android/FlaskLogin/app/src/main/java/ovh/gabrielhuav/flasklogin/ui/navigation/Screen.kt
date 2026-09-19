package ovh.gabrielhuav.flasklogin.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "Inicio de Sesión")
    data object Register : Screen("register", "Registro de Usuario")
    data object Tasks : Screen("tasks", "Operaciones CRUD")
}
