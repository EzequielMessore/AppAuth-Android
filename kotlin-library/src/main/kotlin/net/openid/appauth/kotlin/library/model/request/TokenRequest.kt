package net.openid.appauth.kotlin.library.model.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.extension.stringToSet
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import net.openid.appauth.kotlin.library.model.enum.GrantType
import net.openid.appauth.kotlin.library.utils.CodeVerifierUtil

@Serializable
data class TokenRequest(
    val configuration: AuthorizationServiceConfiguration,
    val nonce: String? = null,
    val clientId: String,
    val grantType: String,
    val redirectUri: String? = null,
    val authorizationCode: String? = null,
    val scope: String? = null,
    val refreshToken: String? = null,
    val codeVerifier: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) {
    val scopeSet: Set<String> = scope?.stringToSet().orEmpty()

    val requestParameters: Map<String, String>
        get() {
            val params = mutableMapOf<String, String>()
            params[Params.CLIENT_ID] = clientId
            params[Params.GRANT_TYPE] = grantType
            scope?.let { params[Params.SCOPE] = it }
            authorizationCode?.let { params[Params.CODE] = it }
            redirectUri?.let { params[Params.REDIRECT_URI] = it }
            refreshToken?.let { params[Params.REFRESH_TOKEN] = it }
            codeVerifier?.let { params[Params.CODE_VERIFIER] = it }
            additionalParameters.forEach { (key, value) ->
                params[key] = value
            }
            return params
        }

    fun jsonSerialize() = Json.encodeToString(value = this, serializer = serializer())

    class Builder(
        private var configuration: AuthorizationServiceConfiguration,
        private var clientId: String,
    ) {
        private var nonce: String? = null
        private var grantType: String
        private var redirectUri: String? = null
        private var authorizationCode: String? = null
        private var scope: String? = null
        private var refreshToken: String? = null
        private var codeVerifier: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()

        init {
            this.grantType = ""
        }

        fun nonce(nonce: String?) = apply {
            this.nonce = nonce?.ifEmpty { null }
        }

        fun grantType(grantType: String) = apply {
            this.grantType = grantType
        }

        fun redirectUri(redirectUri: String?) = apply {
            this.redirectUri = redirectUri
        }

        fun authorizationCode(authorizationCode: String?) = apply {
            this.authorizationCode = authorizationCode
        }

        fun scope(scope: String?) = apply {
            this.scope = scope?.split(" +")?.joinToString(" ")
        }

        fun refreshToken(refreshToken: String?) = apply {
            this.refreshToken = refreshToken
        }

        fun codeVerifier(codeVerifier: String?) = apply {
            codeVerifier?.let { CodeVerifierUtil.checkCodeVerifier(it) }
            this.codeVerifier = codeVerifier
        }

        fun additionalParameters(additionalParameters: Map<String, String>) = apply {
            checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
            this.additionalParameters = additionalParameters
        }

        private fun inferGrantType(): String {
            return when {
                grantType.isNotBlank() -> grantType
                authorizationCode != null -> GrantType.AUTHORIZATION_CODE
                refreshToken != null -> GrantType.REFRESH_TOKEN
                else -> throw IllegalStateException("grant type not specified and cannot be inferred")
            }
        }

        fun build(): TokenRequest {
            val grantType = inferGrantType()

            if (grantType == "authorization_code" && authorizationCode == null) {
                throw IllegalStateException("authorization code must be specified for grant_type = authorization_code")
            }

            if (grantType == "refresh_token" && refreshToken == null) {
                throw IllegalStateException("refresh token must be specified for grant_type = refresh_token")
            }

            if (grantType == "authorization_code" && redirectUri == null) {
                throw IllegalStateException("no redirect URI specified on token request for code exchange")
            }

            return TokenRequest(
                configuration = configuration,
                nonce = nonce,
                clientId = clientId,
                grantType = grantType,
                redirectUri = redirectUri,
                authorizationCode = authorizationCode,
                scope = scope,
                refreshToken = refreshToken,
                codeVerifier = codeVerifier,
                additionalParameters = additionalParameters
            )
        }
    }

    companion object {

        fun jsonDeserialize(json: String) = Json.decodeFromString(string = json, deserializer = serializer())

        const val PARAM_CLIENT_ID = "client_id"

        object Params {
            const val CODE = "code"
            const val SCOPE = "scope"
            const val CLIENT_ID = "client_id"
            const val GRANT_TYPE = "grant_type"
            const val REDIRECT_URI = "redirect_uri"
            const val CODE_VERIFIER = "code_verifier"
            const val REFRESH_TOKEN = "refresh_token"
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.CODE,
            Params.SCOPE,
            Params.CLIENT_ID,
            Params.GRANT_TYPE,
            Params.REDIRECT_URI,
            Params.CODE_VERIFIER,
            Params.REFRESH_TOKEN,
        )
    }
}