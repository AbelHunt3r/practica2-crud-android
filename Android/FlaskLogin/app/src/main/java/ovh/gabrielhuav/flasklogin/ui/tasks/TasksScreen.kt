package ovh.gabrielhuav.flasklogin.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ovh.gabrielhuav.flasklogin.data.model.Task
import ovh.gabrielhuav.flasklogin.ui.auth.AuthViewModel
import ovh.gabrielhuav.flasklogin.ui.navigation.AppMenu
import ovh.gabrielhuav.flasklogin.ui.navigation.Screen
import ovh.gabrielhuav.flasklogin.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    authViewModel: AuthViewModel,
    onNavigate: (Screen) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val tasksState by taskViewModel.tasksState.collectAsState()
    val sessionExpired by taskViewModel.sessionExpired.collectAsState()
    val currentUser by authViewModel.username.collectAsState()

    var showDialogFor by remember { mutableStateOf<Task?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        taskViewModel.loadTasks()
    }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            taskViewModel.consumeSessionExpired()
            onNavigateToLogin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Screen.Tasks.title) },
                actions = {
                    AppMenu(
                        isLoggedIn = currentUser != null,
                        onNavigate = onNavigate,
                        onLogout = { authViewModel.logout(onDone = onNavigateToLogin) }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tarea")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = tasksState) {
                is UiState.Loading, UiState.Idle -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { taskViewModel.loadTasks() }) {
                            Text("Reintentar")
                        }
                    }
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "Aún no tienes tareas. Toca + para crear la primera.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            items(state.data, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    onToggle = { taskViewModel.toggleCompleted(task) },
                                    onEdit = { showDialogFor = task },
                                    onDelete = { taskViewModel.deleteTask(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TaskFormDialog(
            task = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, description, completed ->
                taskViewModel.createTask(title, description)
                showCreateDialog = false
            }
        )
    }

    showDialogFor?.let { task ->
        TaskFormDialog(
            task = task,
            onDismiss = { showDialogFor = null },
            onSave = { title, description, completed ->
                taskViewModel.updateTask(task, title, description, completed)
                showDialogFor = null
            }
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null
                    )
                    if (!task.description.isNullOrBlank()) {
                        Text(text = task.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar")
            }
        }
    }
}
