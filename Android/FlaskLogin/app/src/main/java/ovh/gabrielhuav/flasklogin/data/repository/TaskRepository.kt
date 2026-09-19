package ovh.gabrielhuav.flasklogin.data.repository

import com.google.gson.Gson
import ovh.gabrielhuav.flasklogin.data.model.MessageResponse
import ovh.gabrielhuav.flasklogin.data.model.Task
import ovh.gabrielhuav.flasklogin.data.model.TaskRequest
import ovh.gabrielhuav.flasklogin.data.remote.ApiService
import java.io.IOException

class TaskRepository(private val apiService: ApiService) {

    suspend fun getTasks(): List<Task> {
        try {
            val response = apiService.getTasks()
            if (response.isSuccessful) {
                return response.body() ?: emptyList()
            }
            throw Exception(parseError(response.code(), response.errorBody()?.string()))
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    suspend fun createTask(title: String, description: String, completed: Boolean): Task {
        try {
            val response = apiService.createTask(TaskRequest(title, description, completed))
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }
            throw Exception(parseError(response.code(), response.errorBody()?.string()))
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    suspend fun updateTask(id: Int, title: String, description: String, completed: Boolean): Task {
        try {
            val response = apiService.updateTask(id, TaskRequest(title, description, completed))
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }
            throw Exception(parseError(response.code(), response.errorBody()?.string()))
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    suspend fun deleteTask(id: Int) {
        try {
            val response = apiService.deleteTask(id)
            if (!response.isSuccessful) {
                throw Exception(parseError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: IOException) {
            throw Exception("No se pudo conectar con el servidor. Verifica que el backend esté corriendo.")
        }
    }

    private fun parseError(code: Int, errorBody: String?): String {
        val backendMessage = errorBody?.let {
            try {
                Gson().fromJson(it, MessageResponse::class.java)?.message
            } catch (e: Exception) {
                null
            }
        }
        return backendMessage ?: when (code) {
            401 -> "Tu sesión expiró, inicia sesión de nuevo"
            404 -> "Tarea no encontrada"
            400 -> "Datos inválidos"
            else -> "Ocurrió un error inesperado ($code)"
        }
    }
}
