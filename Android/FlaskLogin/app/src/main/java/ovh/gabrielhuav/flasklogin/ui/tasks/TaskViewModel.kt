package ovh.gabrielhuav.flasklogin.ui.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.data.model.Task
import ovh.gabrielhuav.flasklogin.data.remote.RetrofitClient
import ovh.gabrielhuav.flasklogin.data.repository.TaskRepository
import ovh.gabrielhuav.flasklogin.util.UiState

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(RetrofitClient.getApiService(application))

    private val _tasksState = MutableStateFlow<UiState<List<Task>>>(UiState.Idle)
    val tasksState: StateFlow<UiState<List<Task>>> = _tasksState.asStateFlow()

    // Estado de una operación puntual (crear/editar/borrar), para mostrar
    // loaders o errores en el diálogo sin perder la lista ya cargada.
    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    // Se activa cuando el backend responde 401: la sesión ya no es válida.
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    fun loadTasks() {
        _tasksState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val tasks = repository.getTasks()
                _tasksState.value = UiState.Success(tasks)
            } catch (e: Exception) {
                handleError(e) { _tasksState.value = UiState.Error(it) }
            }
        }
    }

    fun createTask(title: String, description: String) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repository.createTask(title, description, completed = false)
                _actionState.value = UiState.Success(Unit)
                loadTasks()
            } catch (e: Exception) {
                handleError(e) { _actionState.value = UiState.Error(it) }
            }
        }
    }

    fun updateTask(task: Task, title: String, description: String, completed: Boolean) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                repository.updateTask(task.id, title, description, completed)
                _actionState.value = UiState.Success(Unit)
                loadTasks()
            } catch (e: Exception) {
                handleError(e) { _actionState.value = UiState.Error(it) }
            }
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task.id, task.title, task.description ?: "", !task.completed)
                loadTasks()
            } catch (e: Exception) {
                handleError(e) { _tasksState.value = UiState.Error(it) }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task.id)
                loadTasks()
            } catch (e: Exception) {
                handleError(e) { _tasksState.value = UiState.Error(it) }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }

    fun consumeSessionExpired() {
        _sessionExpired.value = false
    }

    private fun handleError(e: Exception, onError: (String) -> Unit) {
        val message = e.message ?: "Error desconocido"
        if (message.contains("sesión expiró", ignoreCase = true) || message.contains("401")) {
            _sessionExpired.value = true
        }
        onError(message)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TaskViewModel(application) as T
                }
            }
    }
}
