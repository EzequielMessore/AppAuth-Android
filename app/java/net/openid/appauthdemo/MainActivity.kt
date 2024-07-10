package net.openid.appauthdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.model.request.AuthorizationRequest
import net.openid.appauth.kotlin.library.model.request.EndSessionRequest
import net.openid.appauth.kotlin.library.model.response.AuthorizationResponse
import net.openid.appauth.kotlin.library.model.response.ResponseTypeValues
import net.openid.appauth.kotlin.library.networking.NetworkDataSource
import net.openid.appauth.kotlin.library.networking.NetworkDataSourceContract
import net.openid.appauth.kotlin.library.service.AuthorizationService

class MainActivity : AppCompatActivity() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val authResponse = AuthorizationResponse.fromIntent(intent)
                val exception = AuthorizationException.fromIntent(intent)
                authStateManager.updateAfterAuthorization(authResponse, exception)

                if (exception != null) {
                    return@let
                }

                authResponse?.let { response ->
                    val request = response.createTokenExchangeRequest()
                    lifecycleScope.launch {
                        networkDataSource
                            .performTokenRequest(request)
                            .fold(
                                onSuccess = { tokenResponse ->
                                    authStateManager.updateAfterTokenResponse(tokenResponse)
                                    updateVisibility()
                                    authStateManager.current.also {
                                        Log.e("TestActivity", "AuthState: ${it.jsonSerialize()}")
                                    }
                                },
                                onFailure = { error ->
                                    Log.e("TestActivity", "Failed to fetch token from network", error)
                                },
                            )
                    }
                }

            }

            Log.e("TestActivity", "RESULT_OK")
        } else {
            Log.e("TestActivity", "RESULT_CANCELED")
        }
    }

    private val launcherLogout = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            authStateManager.logout()
            updateVisibility()
        } else {
            Log.e("TestActivity", "RESULT_CANCELED")
        }
    }

    private val networkDataSource: NetworkDataSourceContract by lazy {
        NetworkDataSource()
    }
    private val authStateManager by lazy {
        AuthStateManager.getInstance(this)
    }

    private lateinit var buttonLogin: Button
    private lateinit var buttonLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonLogin = findViewById(R.id.button)
        buttonLogout = findViewById(R.id.buttonLogout)

        updateVisibility()

        buttonLogin.setOnClickListener {
            lifecycleScope.launch {
                val intent = createAuthRequestIntent()
                launcher.launch(intent)
            }
        }

        buttonLogout.setOnClickListener {
            lifecycleScope.launch {
                val intent = createEndSessionRequestIntent() ?: return@launch
                launcherLogout.launch(intent)
            }
        }
    }

    private fun updateVisibility() {
        if (authStateManager.current.isAuthorized && authStateManager.current.getNeedsTokenRefresh()) {
            buttonLogin.isVisible = false
            buttonLogout.isVisible = true
        } else {
            buttonLogin.isVisible = true
            buttonLogout.isVisible = false
        }
    }

    private suspend fun createAuthRequestIntent(): Intent {


        val config = getConfiguration()

        val request = AuthorizationRequest.Builder(configuration = config)
            .clientId(clientId)
            .redirectUri(redirectUri)
            .responseType(ResponseTypeValues.CODE)
            .scope(scope)
            .build()

        return AuthorizationService(applicationContext)
            .getAuthorizationRequestIntent(request)
    }

    private fun createEndSessionRequestIntent(): Intent? {
        val state = authStateManager.current
        return state.authorizationServiceConfiguration?.let {
            val request = EndSessionRequest.Builder(it)
                .idTokenHint(state.lastTokenResponse?.idToken)
                .postLogoutRedirectUri(redirectUri)
                .build()

            AuthorizationService(applicationContext)
                .getEndSessionRequestIntent(request)
        }
    }

    private suspend fun getConfiguration() = suspendCancellableCoroutine { continuation ->

        lifecycleScope.launch {
            val result = networkDataSource.fetchFromUrl(issuer)
            result.fold(
                onSuccess = {
                    continuation.resumeWith(Result.success(it))
                },
                onFailure = {
                    Log.e("TestActivity", "Failed to fetch configuration from network", it)
                },
            )
        }
    }

    companion object {
        private const val issuer = ""
        private const val scope = "openid profile"
        private const val clientId = ""
        private const val redirectUri = ""
    }
}