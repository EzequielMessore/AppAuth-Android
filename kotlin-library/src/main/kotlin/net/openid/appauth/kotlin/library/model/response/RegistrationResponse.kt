package net.openid.appauth.kotlin.library.model.response

import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.json.libJson
import net.openid.appauth.kotlin.library.model.request.RegistrationRequest
import net.openid.appauth.kotlin.library.utils.Clock
import net.openid.appauth.kotlin.library.utils.SystemClock

@Serializable
data class RegistrationResponse(
    val request: RegistrationRequest,
    val clientId: String,
    val clientIdIssuedAt: Long? = null,
    val clientSecret: String? = null,
    val clientSecretExpiresAt: Long? = null,
    val registrationAccessToken: String? = null,
    val registrationClientUri: String? = null,
    val tokenEndpointAuthMethod: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) {

    fun jsonSerialize() =
        libJson.encodeToString(value = this, serializer = serializer())

    fun hasClientSecretExpired(): Boolean {
        return hasClientSecretExpired(SystemClock.INSTANCE)
    }

    private fun hasClientSecretExpired(clock: Clock): Boolean {
        val now = TimeUnit.MILLISECONDS.toSeconds(clock.currentTimeMillis)
        return clientSecretExpiresAt != null && now > clientSecretExpiresAt
    }

    class Builder(
        private val request: RegistrationRequest,
    ) {
        private var clientId: String? = null
        private var clientIdIssuedAt: Long? = null
        private var clientSecret: String? = null
        private var clientSecretExpiresAt: Long? = null
        private var registrationAccessToken: String? = null
        private var registrationClientUri: String? = null
        private var tokenEndpointAuthMethod: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()

        fun setClientId(clientId: String) = apply {
            this.clientId = clientId
        }

        fun setClientIdIssuedAt(clientIdIssuedAt: Long) = apply {
            this.clientIdIssuedAt = clientIdIssuedAt
        }

        fun setClientSecret(clientSecret: String) = apply {
            this.clientSecret = clientSecret
        }

        fun setClientSecretExpiresAt(clientSecretExpiresAt: Long) = apply {
            this.clientSecretExpiresAt = clientSecretExpiresAt
        }

        fun setRegistrationAccessToken(registrationAccessToken: String) = apply {
            this.registrationAccessToken = registrationAccessToken
        }

        fun setRegistrationClientUri(registrationClientUri: String) = apply {
            this.registrationClientUri = registrationClientUri
        }

        fun setTokenEndpointAuthMethod(tokenEndpointAuthMethod: String) = apply {
            this.tokenEndpointAuthMethod = tokenEndpointAuthMethod
        }

        fun setAdditionalParameters(additionalParameters: Map<String, String>) = apply {
            this.additionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
        }

        fun build() = RegistrationResponse(
            request = request,
            clientId = clientId ?: throw IllegalStateException("clientId is required"),
            clientSecret = clientSecret,
            clientIdIssuedAt = clientIdIssuedAt,
            clientSecretExpiresAt = clientSecretExpiresAt,
            registrationAccessToken = registrationAccessToken,
            registrationClientUri = registrationClientUri,
            tokenEndpointAuthMethod = tokenEndpointAuthMethod,
            additionalParameters = additionalParameters
        )
    }

    companion object {
        object Params {
            const val CLIENT_ID = "client_id"
            const val CLIENT_SECRET = "client_secret"
            const val CLIENT_SECRET_EXPIRES_AT = "client_secret_expires_at"
            const val REGISTRATION_ACCESS_TOKEN = "registration_access_token"
            const val REGISTRATION_CLIENT_URI = "registration_client_uri"
            const val CLIENT_ID_ISSUED_AT = "client_id_issued_at"
            const val TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method"
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.CLIENT_ID,
            Params.CLIENT_SECRET,
            Params.CLIENT_SECRET_EXPIRES_AT,
            Params.REGISTRATION_ACCESS_TOKEN,
            Params.REGISTRATION_CLIENT_URI,
            Params.CLIENT_ID_ISSUED_AT,
            Params.TOKEN_ENDPOINT_AUTH_METHOD
        )

        fun jsonDeserialize(json: String) =
            libJson.decodeFromString<RegistrationResponse>(json)
    }
}