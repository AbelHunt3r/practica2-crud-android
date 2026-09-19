package ovh.gabrielhuav.flasklogin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

/**
 * Guarda el token de sesión (JWT) y el nombre de usuario localmente,
 * para no tener que iniciar sesión cada vez que se abre la app.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USERNAME = stringPreferencesKey("username")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }

    suspend fun saveSession(token: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USERNAME] = username
        }
    }

    suspend fun getToken(): String? = tokenFlow.first()

    suspend fun getUsername(): String? = usernameFlow.first()

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
