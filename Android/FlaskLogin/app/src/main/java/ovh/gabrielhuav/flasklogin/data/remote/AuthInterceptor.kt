package ovh.gabrielhuav.flasklogin.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import ovh.gabrielhuav.flasklogin.data.local.SessionManager

/**
 * Agrega automáticamente el header "Authorization: Bearer <token>" a cada
 * petición saliente, usando el token guardado tras el login. Las rutas
 * /register y /login no necesitan token, así que se dejan pasar tal cual.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val path = original.url.encodedPath
        if (path.endsWith("/login") || path.endsWith("/register")) {
            return chain.proceed(original)
        }

        val token = runBlocking { sessionManager.getToken() }

        val requestWithAuth = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(requestWithAuth)
    }
}
