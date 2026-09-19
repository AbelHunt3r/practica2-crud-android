package ovh.gabrielhuav.flasklogin.data.remote

import ovh.gabrielhuav.flasklogin.data.model.LoginRequest
import ovh.gabrielhuav.flasklogin.data.model.LoginResponse
import ovh.gabrielhuav.flasklogin.data.model.MessageResponse
import ovh.gabrielhuav.flasklogin.data.model.RegisterRequest
import ovh.gabrielhuav.flasklogin.data.model.Task
import ovh.gabrielhuav.flasklogin.data.model.TaskRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("tasks")
    suspend fun getTasks(): Response<List<Task>>

    @POST("tasks")
    suspend fun createTask(@Body request: TaskRequest): Response<Task>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body request: TaskRequest): Response<Task>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<MessageResponse>
}
