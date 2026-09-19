package ovh.gabrielhuav.flasklogin.data.repository

import ovh.gabrielhuav.flasklogin.data.local.SessionManager
import ovh.gabrielhuav.flasklogin.data.model.LoginRequest
import ovh.gabrielhuav.flasklogin.data.model.LoginResponse
import ovh.gabrielhuav.flasklogin.data.model.RegisterRequest
import ovh.gabrielhuav.flasklogin.data.remote.ApiService
import com.google.gson.Gson
import ovh.gabrielhuav.flasklogin.data.model.MessageResponse
import java.io.IOException

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    /** Devuelve un mensaje de éxito, o lanza una excepción con un mensaje legible. */
    suspend fun register(username: String, password: String): String {
        try {
            val response = apiService.register(RegisterRequest(username, password))
            if (response.isSuccessful) {
                return response.body()?.message ?: "Usuario creado exitosamente"
            }
            throw Exception(parseError(response.errorBody()?.string()))
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    /** Inicia sesión y, si es exitoso, guarda el token para las siguientes peticiones. */
    suspend fun login(username: String, password: String): LoginResponse {
        try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.token != null) {
                    sessionManager.saveSession(body.token, body.username ?: username)
                }
                return body
            }
            throw Exception(parseError(response.errorBody()?.string(), default = "Credenciales inválidas"))
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun currentUsername(): String? = sessionManager.getUsername()

    suspend fun isLoggedIn(): Boolean = sessionManager.getToken() != null

    private fun parseError(errorBody: String?, default: String = "Ocurrió un error inesperado"): String {
        if (errorBody.isNullOrBlank()) return default
        return try {
            Gson().fromJson(errorBody, MessageResponse::class.java)?.message ?: default
        } catch (e: Exception) {
            default
        }
    }
}
