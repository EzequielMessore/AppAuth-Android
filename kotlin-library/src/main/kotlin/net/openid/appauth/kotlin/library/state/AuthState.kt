package net.openid.appauth.kotlin.library.state

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import net.openid.appauth.kotlin.library.model.enum.GrantType
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.model.response.AuthorizationResponse
import net.openid.appauth.kotlin.library.model.response.RegistrationResponse
import net.openid.appauth.kotlin.library.model.response.TokenResponse
import net.openid.appauth.kotlin.library.utils.Clock
import net.openid.appauth.kotlin.library.utils.SystemClock

@Serializable
data class AuthState(
    val scope: String? = null,
    val refreshToken: String? = null,
    val lastTokenResponse: TokenResponse? = null,
    val config: AuthorizationServiceConfiguration? = null,
    val lastRegistrationResponse: RegistrationResponse? = null,
    val lastAuthorizationResponse: AuthorizationResponse? = null,
) {
    constructor(
        authError: AuthorizationException? = null,
        authResponse: AuthorizationResponse?,
    ) : this(lastAuthorizationResponse = authResponse) {
        authorizationException = authError
        update(authResponse, authError)
    }

    constructor(
        registrationResponse: RegistrationResponse,
    ) : this(lastRegistrationResponse = registrationResponse) {
        update(registrationResponse)
    }

    constructor(
        authResponse: AuthorizationResponse?,
        tokenResponse: TokenResponse?,
        authError: AuthorizationException? = null,
    ) : this(lastAuthorizationResponse = authResponse, lastTokenResponse = tokenResponse) {
        authorizationException = authError
        update(tokenResponse, authError)
    }

    constructor(
        tokenResponse: TokenResponse?,
        authException: AuthorizationException? = null,
    ) : this(lastTokenResponse = tokenResponse) {
        authorizationException = authException
        update(tokenResponse, authException)
    }


    val accessToken =
        lastTokenResponse?.accessToken ?: lastAuthorizationResponse?.accessToken

    val accessTokenExpirationTime =
        lastTokenResponse?.accessTokenExpirationTime ?: lastAuthorizationResponse?.accessTokenExpirationTime

    val idToken =
        lastTokenResponse?.idToken ?: lastAuthorizationResponse?.idToken

    // todo implements getParsedIdToken

    val clientSecret =
        lastRegistrationResponse?.clientSecret

    val clientSecretExpirationTime =
        lastRegistrationResponse?.clientSecretExpiresAt

    val isAuthorized: Boolean
        get() = authorizationException == null && (accessToken ?: idToken) != null

    private var needsTokenRefreshOverride = false
    var authorizationException: AuthorizationException? = null

    fun setNeedsTokenRefresh(needsTokenRefresh: Boolean) {
        needsTokenRefreshOverride = needsTokenRefresh
    }

    val needsTokenRefresh: Boolean
        get() = getNeedsTokenRefresh(SystemClock.INSTANCE)

    private fun getNeedsTokenRefresh(clock: Clock): Boolean {
        if (needsTokenRefreshOverride) return true

        val expirationTime = accessTokenExpirationTime
            ?: return accessToken == null

        return expirationTime <= clock.currentTimeMillis + EXPIRY_TIME_TOLERANCE_MS
    }

    fun hasClientSecretExpired(): Boolean {
        return hasClientSecretExpired(SystemClock.INSTANCE)
    }

    private fun hasClientSecretExpired(clock: Clock): Boolean {
        val expirationTime = clientSecretExpirationTime ?: return false
        return expirationTime != 0L && expirationTime <= clock.currentTimeMillis
    }

    fun update(
        tokenResponse: TokenResponse?,
        authException: AuthorizationException?,
    ) = apply {
        require((tokenResponse != null) xor (authException != null)) {
            "exactly one of tokenResponse or authException should be non-null"
        }

        authException
            ?.takeIf { it.type == AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR }
            ?.let {
                authorizationException = it
                return this
            }

        return copy(
            scope = tokenResponse?.scope,
            lastTokenResponse = tokenResponse,
            refreshToken = tokenResponse?.refreshToken,
        )
    }

    fun update(
        authResponse: AuthorizationResponse?,
        authException: AuthorizationException?,
    ) = apply {
        require((authResponse != null) xor (authException != null)) {
            "exactly one of authResponse or authException should be non-null"
        }

        authException
            ?.takeIf { it.type == AuthorizationException.TYPE_OAUTH_AUTHORIZATION_ERROR }
            ?.let {
                authorizationException = it
                return this
            }

        return copy(
            lastAuthorizationResponse = authResponse,
            scope = authResponse?.scope ?: authResponse?.request?.scope,
        )
    }

    fun update(
        registrationResponse: RegistrationResponse?,
    ): AuthState {
        return copy(
            config = authorizationServiceConfiguration,
            lastRegistrationResponse = registrationResponse,
        )
    }

    val authorizationServiceConfiguration =
        lastAuthorizationResponse?.request?.configuration ?: config

    fun createTokenRefresh(): TokenRequest {
        return createTokenRefresh(emptyMap())
    }

    private fun createTokenRefresh(
        additionalParameters: Map<String, String> = emptyMap(),
    ): TokenRequest {
        checkNotNull(refreshToken) { "No refresh token available for refresh request" }
        checkNotNull(lastAuthorizationResponse) { "No authorization configuration available for refresh request" }

        return TokenRequest.Builder(
            clientId = lastAuthorizationResponse.request.clientId,
            configuration = lastAuthorizationResponse.request.configuration,
        )
            .scope(scope)
            .refreshToken(refreshToken)
            .grantType(GrantType.REFRESH_TOKEN)
            .additionalParameters(additionalParameters)
            .build()
    }

    fun jsonSerialize(): String {
        return Json.encodeToString(value = this, serializer = serializer())
    }

    companion object {
        const val EXPIRY_TIME_TOLERANCE_MS = 60000

        fun jsonDeserialize(serializedState: String): AuthState {
            return Json.decodeFromString(string = serializedState, deserializer = serializer())
        }
    }
}
