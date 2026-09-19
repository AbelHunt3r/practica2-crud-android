package ovh.gabrielhuav.flasklogin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import ovh.gabrielhuav.flasklogin.ui.auth.AuthViewModel
import ovh.gabrielhuav.flasklogin.ui.auth.LoginScreen
import ovh.gabrielhuav.flasklogin.ui.auth.RegisterScreen
import ovh.gabrielhuav.flasklogin.ui.tasks.TasksScreen
import ovh.gabrielhuav.flasklogin.ui.tasks.TaskViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(application))
    val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.factory(application))

    // Al abrir la app, si ya había una sesión guardada, saltamos directo a CRUD.
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (authViewModel.isLoggedIn()) Screen.Tasks.route else Screen.Login.route
    }

    if (startDestination == null) {
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { navigateSingleTop(navController, Screen.Tasks.route) },
                onNavigate = { screen -> navigateSingleTop(navController, screen.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigate = { screen -> navigateSingleTop(navController, screen.route) }
            )
        }
        composable(Screen.Tasks.route) {
            TasksScreen(
                taskViewModel = taskViewModel,
                authViewModel = authViewModel,
                onNavigate = { screen -> navigateSingleTop(navController, screen.route) },
                onNavigateToLogin = { navigateSingleTop(navController, Screen.Login.route) }
            )
        }
    }
}

private fun navigateSingleTop(navController: NavHostController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
        popUpTo(route) { inclusive = false }
    }
}
