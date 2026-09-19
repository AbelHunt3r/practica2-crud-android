package ovh.gabrielhuav.flasklogin.util

/**
 * Representa el estado de la UI para una operación asíncrona (llamada de red).
 * Con esto la pantalla siempre sabe si debe mostrar: nada, un loader, un error
 * o el resultado exitoso — tal como pide la práctica.
 */
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
