package net.openid.appauth.kotlin.library.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Base64
import java.security.SecureRandom
import net.openid.appauth.kotlin.library.model.AuthorizationManagementRequest
import net.openid.appauth.kotlin.library.model.AuthorizationManagementResponse
import net.openid.appauth.kotlin.library.model.request.AuthorizationRequest
import net.openid.appauth.kotlin.library.model.request.EndSessionRequest
import net.openid.appauth.kotlin.library.model.response.AuthorizationResponse
import net.openid.appauth.kotlin.library.model.response.EndSessionResponse
import org.json.JSONException

internal object AuthorizationManagementUtil {
    private const val STATE_LENGTH = 16
    private const val REQUEST_TYPE_AUTHORIZATION: String = "authorization"
    private const val REQUEST_TYPE_END_SESSION: String = "end_session"

    fun generateRandomState(): String {
        val sr = SecureRandom()
        val random = ByteArray(STATE_LENGTH)
        sr.nextBytes(random)
        return Base64.encodeToString(random, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
    }

    fun requestTypeFor(request: AuthorizationManagementRequest?): String? {
        return when (request) {
            is AuthorizationRequest -> REQUEST_TYPE_AUTHORIZATION
            is EndSessionRequest -> REQUEST_TYPE_END_SESSION
            else -> null
        }
    }

    /**
     * Reads an authorization request from a JSON string representation produced by either
     * [AuthorizationRequest.jsonSerialize] or [EndSessionRequest.jsonSerialize].
     * @throws JSONException if the provided JSON does not match the expected structure.
     */
    @Throws(JSONException::class)
    fun requestFrom(json: String?, type: String): AuthorizationManagementRequest {
        checkNotNull(json) { "json can not be null" }

        if (REQUEST_TYPE_AUTHORIZATION == type) {
            return AuthorizationRequest.jsonDeserialize(json)
        }

        if (REQUEST_TYPE_END_SESSION == type) {
            return EndSessionRequest.jsonDeserialize(json)
        }

        throw IllegalArgumentException(
            "No AuthorizationManagementRequest found matching to this json schema"
        )
    }

    /**
     * Builds an AuthorizationManagementResponse from
     * [AuthorizationManagementRequest] and [Uri]
     */
    @SuppressLint("VisibleForTests")
    fun responseWith(
        request: AuthorizationManagementRequest?, uri: Uri?,
    ): AuthorizationManagementResponse {
        if (request is AuthorizationRequest) {
            return AuthorizationResponse.Builder(request)
                .fromUri(uri)
                .build()
        }
        if (request is EndSessionRequest) {
            return EndSessionResponse.Builder(request)
                .fromUri(uri)
                .build()
        }
        throw IllegalArgumentException("Malformed request or uri")
    }

    /**
     * Extracts response from an intent produced by [.toIntent]. This is
     * used to extract the response from the intent data passed to an activity registered as the
     * handler for [AuthorizationService.performEndSessionRequest]
     * or [AuthorizationService.performAuthorizationRequest].
     */
    fun responseFrom(dataIntent: Intent): AuthorizationManagementResponse? {
        if (EndSessionResponse.contains(dataIntent)) {
            return EndSessionResponse.fromIntent(dataIntent)
        }

        if (AuthorizationResponse.contains(dataIntent)) {
            return AuthorizationResponse.fromIntent(dataIntent)
        }

        throw IllegalArgumentException("Malformed intent")
    }
}
