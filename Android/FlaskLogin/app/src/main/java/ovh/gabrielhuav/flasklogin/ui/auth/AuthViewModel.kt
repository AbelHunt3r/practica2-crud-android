package ovh.gabrielhuav.flasklogin.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ovh.gabrielhuav.flasklogin.data.local.SessionManager
import ovh.gabrielhuav.flasklogin.data.remote.RetrofitClient
import ovh.gabrielhuav.flasklogin.data.repository.AuthRepository
import ovh.gabrielhuav.flasklogin.util.UiState

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val repository = AuthRepository(
        apiService = RetrofitClient.getApiService(application),
        sessionManager = sessionManager
    )

    private val _loginState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val loginState: StateFlow<UiState<String>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val registerState: StateFlow<UiState<String>> = _registerState.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    init {
        viewModelScope.launch {
            _username.value = sessionManager.getUsername()
        }
    }

    suspend fun isLoggedIn(): Boolean = repository.isLoggedIn()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("Usuario y contraseña son obligatorios")
            return
        }
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.login(username, password)
                _username.value = response.username
                _loginState.value = UiState.Success(response.message)
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        if (username.isBlank() || password.isBlank()) {
            _registerState.value = UiState.Error("Usuario y contraseña son obligatorios")
            return
        }
        if (password != confirmPassword) {
            _registerState.value = UiState.Error("Las contraseñas no coinciden")
            return
        }
        _registerState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val message = repository.register(username, password)
                _registerState.value = UiState.Success(message)
            } catch (e: Exception) {
                _registerState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            _username.value = null
            resetStates()
            onDone()
        }
    }

    fun resetStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(application) as T
                }
            }
    }
}
