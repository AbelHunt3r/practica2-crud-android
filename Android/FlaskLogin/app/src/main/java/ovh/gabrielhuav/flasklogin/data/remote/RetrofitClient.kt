package ovh.gabrielhuav.flasklogin.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ovh.gabrielhuav.flasklogin.data.local.SessionManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * IMPORTANTE: en el emulador de Android, "localhost" apunta al propio
     * emulador, no a la máquina anfitriona. Para llegar al backend Flask que
     * corre en Docker en tu computadora hay que usar 10.0.2.2 (alias especial
     * del emulador hacia el host) con el puerto publicado por docker-compose.
     *
     * Si en algún momento pruebas en un dispositivo físico conectado a la
     * misma red Wi-Fi, cambia esto por la IP local de tu computadora, por
     * ejemplo "http://192.168.1.50:5000/".
     */
    private const val BASE_URL = "http://10.0.2.2:5001/"

    @Volatile
    private var instance: ApiService? = null

    fun getApiService(context: Context): ApiService {
        return instance ?: synchronized(this) {
            instance ?: buildApiService(context.applicationContext).also { instance = it }
        }
    }

    private fun buildApiService(context: Context): ApiService {
        val sessionManager = SessionManager(context)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
