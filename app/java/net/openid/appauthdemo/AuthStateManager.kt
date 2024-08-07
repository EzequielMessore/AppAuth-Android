package net.openid.appauthdemo

import android.content.Context
import android.util.Log
import androidx.annotation.AnyThread
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.model.response.AuthorizationResponse
import net.openid.appauth.kotlin.library.model.response.RegistrationResponse
import net.openid.appauth.kotlin.library.model.response.TokenResponse
import net.openid.appauth.kotlin.library.state.AuthState
import org.json.JSONException

class AuthStateManager private constructor(context: Context) {
    private val preferences by lazy {
        context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)
    }
    private val preferencesLock = ReentrantLock()
    private val currentAuthState = AtomicReference<AuthState>()

    @get:AnyThread
    val current: AuthState
        get() {
            if (currentAuthState.get() != null) {
                return currentAuthState.get()
            }

            val state: AuthState = readState()
            return if (currentAuthState.compareAndSet(null, state)) state
            else currentAuthState.get()
        }

    @AnyThread
    fun replace(state: AuthState): AuthState {
        writeState(state)
        currentAuthState.set(state)
        return state
    }

    @AnyThread
    fun updateAfterAuthorization(
        response: AuthorizationResponse?,
        ex: AuthorizationException?,
    ): AuthState {
        val update = current.update(response, ex)
        return replace(update)
    }

    @AnyThread
    fun updateAfterTokenResponse(
        response: TokenResponse?,
        ex: AuthorizationException? = null,
    ): AuthState {
        val update = current.update(response, ex)
        return replace(update)
    }

    @AnyThread
    fun updateAfterRegistration(
        response: RegistrationResponse?,
        ex: AuthorizationException?,
    ): AuthState {
        val current = current
        if (ex != null) return current

        return replace(current.update(response))
    }

    fun logout(): AuthState {
        return replace(AuthState())
    }

    @AnyThread
    private fun readState(): AuthState {
        preferencesLock.lock()
        try {
            val currentState = preferences.getString(KEY_STATE, null) ?: return AuthState()

            try {
                return AuthState.jsonDeserialize(currentState)
            } catch (ex: JSONException) {
                Log.w(TAG, "Failed to deserialize stored auth state - discarding")
                return AuthState()
            }
        } finally {
            preferencesLock.unlock()
        }
    }

    @AnyThread
    private fun writeState(state: AuthState?) {
        preferencesLock.lock()
        try {
            val editor = preferences.edit()
            if (state == null) {
                editor.remove(KEY_STATE)
            } else {
                editor.putString(KEY_STATE, state.jsonSerialize())
            }

            check(editor.commit()) { "Failed to write state to shared prefs" }
        } finally {
            preferencesLock.unlock()
        }
    }

    companion object {
        private val INSTANCE_REF = AtomicReference(WeakReference<AuthStateManager?>(null))

        private const val TAG = "AuthStateManager"

        private const val STORE_NAME = "AuthState"
        private const val KEY_STATE = "state"

        @AnyThread
        fun getInstance(context: Context): AuthStateManager {
            var manager = INSTANCE_REF.get().get()
            if (manager == null) {
                manager = AuthStateManager(context.applicationContext)
                INSTANCE_REF.set(WeakReference(manager))
            }

            return manager
        }
    }
}
