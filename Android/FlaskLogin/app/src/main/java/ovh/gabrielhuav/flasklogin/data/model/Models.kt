package ovh.gabrielhuav.flasklogin.data.model

import com.google.gson.annotations.SerializedName

// ---- Autenticación ----

data class RegisterRequest(
    val username: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val status: String,
    val message: String,
    @SerializedName("user_id") val userId: Int?,
    val username: String?,
    val token: String?
)

data class MessageResponse(
    val message: String?
)

// ---- Recurso CRUD: Task (tarea) ----

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val completed: Boolean,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("created_at") val createdAt: String?
)

data class TaskRequest(
    val title: String,
    val description: String,
    val completed: Boolean
)
