package net.openid.appauth.kotlin.library.activity

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.exception.AuthorizationException.AuthorizationRequestErrors
import net.openid.appauth.kotlin.library.internal.Logger
import net.openid.appauth.kotlin.library.model.AuthorizationManagementRequest
import net.openid.appauth.kotlin.library.utils.AuthorizationManagementUtil
import org.json.JSONException

class AuthorizationManagementActivity : AppCompatActivity() {
    private var authorizationStarted = false
    private var authIntent: Intent? = null
    private var authRequest: AuthorizationManagementRequest? = null
    private var completeIntent: PendingIntent? = null
    private var cancelIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState
            ?.let { extractState(it) }
            ?: extractState(intent.extras)
    }

    override fun onResume() {
        super.onResume()
        if (!authorizationStarted) {
            runCatching {
                startActivity(authIntent)
                authorizationStarted = true
            }.onFailure {
                handleBrowserNotFound()
                finish()
            }
            return
        }

        if (intent.data != null) {
            handleAuthorizationComplete()
        } else {
            handleAuthorizationCanceled()
        }

        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(Keys.AUTHORIZATION_STARTED, authorizationStarted)
        outState.putParcelable(Keys.AUTH_INTENT, authIntent)
        outState.putString(Keys.AUTH_REQUEST, authRequest?.jsonSerializeString())
        outState.putString(Keys.AUTH_REQUEST_TYPE, AuthorizationManagementUtil.requestTypeFor(authRequest))
        outState.putParcelable(Keys.COMPLETE_INTENT, completeIntent)
        outState.putParcelable(Keys.CANCEL_INTENT, cancelIntent)
    }

    private fun extractState(state: Bundle?) {
        if (state == null) {
            Logger.warn("No stored state - unable to handle response")
            finish()
            return
        }

        authIntent = state.getParcelable(Keys.AUTH_INTENT)
        authorizationStarted = state.getBoolean(Keys.AUTHORIZATION_STARTED, false)
        completeIntent = state.getParcelable(Keys.COMPLETE_INTENT)
        cancelIntent = state.getParcelable(Keys.CANCEL_INTENT)

        runCatching {
            authRequest = AuthorizationManagementUtil.requestFrom(
                json = state.getString(Keys.AUTH_REQUEST, null),
                type = state.getString(Keys.AUTH_REQUEST_TYPE, null),
            )
        }.onFailure {
            sendResult(
                callback = cancelIntent,
                resultCode = RESULT_CANCELED,
                cancelData = AuthorizationRequestErrors.INVALID_REQUEST.toIntent(),
            )
        }

        try {
            val authRequestJson = state.getString(Keys.AUTH_REQUEST, null)
            val authRequestType = state.getString(Keys.AUTH_REQUEST_TYPE, null)
            authRequest = authRequestJson?.let {
                AuthorizationManagementUtil.requestFrom(it, authRequestType)
            }
        } catch (ex: JSONException) {
            sendResult(
                callback = cancelIntent,
                resultCode = RESULT_CANCELED,
                cancelData = AuthorizationRequestErrors.INVALID_REQUEST.toIntent(),
            )
        }
    }

    private fun handleBrowserNotFound() {
        Logger.debug("Authorization flow canceled due to missing browser")
        val cancelData: Intent = AuthorizationException.fromTemplate(
            AuthorizationException.GeneralErrors.PROGRAM_CANCELED_AUTH_FLOW,
            null
        ).toIntent()

        sendResult(cancelIntent, cancelData, RESULT_CANCELED)
    }

    private fun sendResult(callback: PendingIntent?, cancelData: Intent, resultCode: Int) {
        if (callback != null) {
            runCatching {
                callback.send(this, 0, cancelData)
            }.onFailure {
                Logger.error("Failed to send cancel intent", it)
            }
        } else {
            setResult(resultCode, cancelData)
        }
    }

    private fun handleAuthorizationComplete() {
        val responseUri = intent.data
        val responseData = extractResponseData(responseUri)
        if (responseData == null) {
            Logger.error("Failed to extract OAuth2 response from redirect")
            return
        }
        responseData.setData(responseUri)

        sendResult(completeIntent, responseData, RESULT_OK)
    }

    private fun handleAuthorizationCanceled() {
        Logger.debug("Authorization flow canceled by user")
        val cancelData: Intent = AuthorizationException.fromTemplate(
            rootCause = null,
            ex = AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW,
        ).toIntent()

        sendResult(cancelIntent, cancelData, RESULT_CANCELED)
    }

    private fun extractResponseData(responseUri: Uri?): Intent? {
        if (responseUri?.queryParameterNames?.contains(AuthorizationException.PARAM_ERROR) == true) {
            return AuthorizationException.fromOAuthRedirect(responseUri).toIntent()
        } else {
            val response = AuthorizationManagementUtil.responseWith(
                uri = responseUri,
                request = authRequest,
            )

            if (authRequest?.state == null && response.state != null ||
                (authRequest?.state != null && !authRequest?.state.equals(response.state))
            ) {
                Logger.warn(
                    "State returned in authorization response (%s) does not match state "
                        + "from request (%s) - discarding response",
                    response.state,
                    authRequest?.state
                )

                return AuthorizationRequestErrors.STATE_MISMATCH.toIntent()
            }

            return response.toIntent()
        }
    }

    companion object {
        object Keys {
            internal const val AUTH_INTENT = "authIntent"
            internal const val AUTH_REQUEST = "authRequest"
            internal const val CANCEL_INTENT = "cancelIntent"
            internal const val COMPLETE_INTENT = "completeIntent"
            internal const val AUTH_REQUEST_TYPE = "authRequestType"
            internal const val AUTHORIZATION_STARTED = "authStarted"
            internal const val REDIRECT_URI = "redirectUri"
        }

        fun createStartForResultIntent(
            context: Context,
            authIntent: Intent?,
            request: AuthorizationManagementRequest,
        ) = createStartIntent(
            context = context,
            request = request,
            cancelIntent = null,
            completeIntent = null,
            authIntent = authIntent,
        )

        private fun createStartIntent(
            context: Context,
            authIntent: Intent?,
            cancelIntent: PendingIntent?,
            completeIntent: PendingIntent?,
            request: AuthorizationManagementRequest,
        ) = createBaseIntent(context).apply {
            putExtra(Keys.AUTH_INTENT, authIntent)
            putExtra(Keys.AUTH_REQUEST, request.jsonSerializeString())
            putExtra(Keys.AUTH_REQUEST_TYPE, AuthorizationManagementUtil.requestTypeFor(request))
            putExtra(Keys.COMPLETE_INTENT, completeIntent)
            putExtra(Keys.CANCEL_INTENT, cancelIntent)
        }

        fun createResponseHandlingIntent(context: Context, responseUri: Uri?) = createBaseIntent(context).apply {
            putExtra(Keys.REDIRECT_URI, responseUri.toString())
            data = responseUri
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        private fun createBaseIntent(context: Context): Intent {
            return Intent(context, AuthorizationManagementActivity::class.java)
        }
    }
}