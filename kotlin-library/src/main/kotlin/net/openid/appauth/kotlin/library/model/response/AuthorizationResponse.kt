package net.openid.appauth.kotlin.library.model.response

import android.content.Intent
import android.net.Uri
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.extension.checkNotEmpty
import net.openid.appauth.kotlin.library.extension.filterNotNullValues
import net.openid.appauth.kotlin.library.extension.stringToSet
import net.openid.appauth.kotlin.library.model.AuthorizationManagementResponse
import net.openid.appauth.kotlin.library.model.enum.GrantType
import net.openid.appauth.kotlin.library.model.request.AuthorizationRequest
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.utils.Clock
import net.openid.appauth.kotlin.library.utils.SystemClock

@Serializable
data class AuthorizationResponse(
    val request: AuthorizationRequest,
    override val state: String? = null,
    val tokenType: String? = null,
    val authorizationCode: String? = null,
    val accessToken: String? = null,
    val accessTokenExpirationTime: Long? = null,
    val idToken: String? = null,
    val scope: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) : AuthorizationManagementResponse {
    val scopeSet: Set<String>?
        get() = scope?.stringToSet()

    override fun toIntent() = Intent().apply {
        putExtra(EXTRA_RESPONSE, jsonSerialize())
    }

    override fun jsonSerialize() = Json.encodeToString(serializer(), this)

    fun hasAccessTokenExpired(): Boolean {
        return hasAccessTokenExpired(SystemClock.INSTANCE)
    }

    private fun hasAccessTokenExpired(clock: Clock): Boolean {
        return accessTokenExpirationTime != null
            && clock.currentTimeMillis > accessTokenExpirationTime
    }

    fun createTokenExchangeRequest(): TokenRequest {
        return createTokenExchangeRequest(emptyMap())
    }

    fun createTokenExchangeRequest(additionalExchangeParameters: Map<String, String>): TokenRequest {
        if (authorizationCode == null) {
            throw IllegalStateException("authorizationCode not available for exchange request");
        }

        return TokenRequest.Builder(clientId = request.clientId, configuration = request.configuration)
            .grantType(GrantType.AUTHORIZATION_CODE)
            .redirectUri(request.redirectUri)
            .codeVerifier(request.codeVerifier)
            .authorizationCode(authorizationCode)
            .additionalParameters(additionalExchangeParameters)
            .nonce(request.nonce)
            .build()
    }

    class Builder(
        private var request: AuthorizationRequest,
    ) {
        private var state: String? = null
        private var tokenType: String? = null
        private var authorizationCode: String? = null
        private var accessToken: String? = null
        private var accessTokenExpirationTime: Long? = null
        private var idToken: String? = null
        private var scope: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()

        fun request(request: AuthorizationRequest) = apply {
            this.request = request
        }

        fun state(state: String?) = apply {
            this.state = state
        }

        fun tokenType(tokenType: String?) = apply {
            this.tokenType = tokenType
        }

        fun authorizationCode(authorizationCode: String?) = apply {
            this.authorizationCode = authorizationCode
        }

        fun accessToken(accessToken: String?) = apply {
            this.accessToken = accessToken
        }

        fun accessTokenExpiresIn(expiresIn: Long?, clock: Clock) = apply {
            accessTokenExpirationTime = expiresIn?.let {
                clock.currentTimeMillis + TimeUnit.SECONDS.toMillis(it)
            }
        }

        fun idToken(idToken: String?) = apply {
            this.idToken = idToken
        }

        fun scope(scope: String?) = apply {
            val split = scope?.split(" +")
            split?.forEach {
                checkNotEmpty(it) { "individual scopes cannot be null or empty" }
            }
            this.scope = split?.joinToString(" ")
        }

        fun additionalParameters(additionalParameters: Map<String, String>) = apply {
            this.additionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
        }

        fun build() = AuthorizationResponse(
            scope = scope,
            state = state,
            idToken = idToken,
            request = request,
            tokenType = tokenType,
            accessToken = accessToken,
            authorizationCode = authorizationCode,
            additionalParameters = additionalParameters,
            accessTokenExpirationTime = accessTokenExpirationTime,
        )

        fun fromUri(uri: Uri?) = apply {
            fromUri(uri, SystemClock.INSTANCE)
        }

        fun fromUri(uri: Uri?, clock: Clock) = apply {
            state(uri?.getQueryParameter(Params.STATE))
            tokenType(uri?.getQueryParameter(Params.TOKEN_TYPE))
            authorizationCode(uri?.getQueryParameter(Params.AUTHORIZATION_CODE))
            accessToken(uri?.getQueryParameter(Params.ACCESS_TOKEN))
            accessTokenExpiresIn(uri?.getQueryParameter(Params.EXPIRES_IN)?.toLongOrNull(), clock)
            idToken(uri?.getQueryParameter(Params.ID_TOKEN))
            scope(uri?.getQueryParameter(Params.SCOPE))

            uri?.queryParameterNames
                ?.minus(BUILT_IN_PARAMS)
                ?.associateWith(uri::getQueryParameter)
                ?.filterNotNullValues()
                ?.let(::additionalParameters)
        }
    }

    companion object {

        object Params {
            const val STATE = "state"
            const val TOKEN_TYPE = "token_type"
            const val AUTHORIZATION_CODE = "code"
            const val ACCESS_TOKEN = "access_token"
            const val EXPIRES_IN = "expires_in"
            const val ID_TOKEN = "id_token"
            const val SCOPE = "scope"
        }

        private const val EXTRA_RESPONSE = "net.openid.appauth.AuthorizationResponse"

        private val BUILT_IN_PARAMS = setOf(
            Params.SCOPE,
            Params.STATE,
            Params.ID_TOKEN,
            Params.TOKEN_TYPE,
            Params.EXPIRES_IN,
            Params.ACCESS_TOKEN,
            Params.AUTHORIZATION_CODE,
        )

        fun jsonDeserialize(json: String): AuthorizationResponse {
            return Json.decodeFromString<AuthorizationResponse>(json)
        }

        fun fromIntent(dataIntent: Intent?): AuthorizationResponse? {
            requireNotNull(dataIntent) { "dataIntent must not be null" }
            if (!contains(dataIntent)) return null

            return runCatching {
                Json.decodeFromString<AuthorizationResponse>(dataIntent.getStringExtra(EXTRA_RESPONSE)!!)
            }.getOrElse {
                throw IllegalArgumentException("Intent contains malformed auth response", it)
            }
        }

        fun contains(intent: Intent): Boolean {
            return intent.hasExtra(EXTRA_RESPONSE)
        }
    }
}