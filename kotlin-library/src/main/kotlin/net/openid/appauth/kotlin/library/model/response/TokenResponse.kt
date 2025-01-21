package net.openid.appauth.kotlin.library.model.response

import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.extension.checkNotEmpty
import net.openid.appauth.kotlin.library.extension.stringToSet
import net.openid.appauth.kotlin.library.json.libJson
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.utils.Clock
import net.openid.appauth.kotlin.library.utils.SystemClock

@Serializable
data class TokenResponse(
    val tokenRequest: TokenRequest? = null,
    @SerialName("token_type")
    val tokenType: String?,
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    @SerialName("expires_at")
    val expiresAt: Long? = null,
    @SerialName("id_token")
    val idToken: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("scope")
    val scope: String?,
    val additionalParameters: Map<String, String> = emptyMap(),
) {
    val scopeSet
        get() = scope?.stringToSet()

    val accessTokenExpirationTime: Long? by lazy {
        expiresAt ?: expiresIn?.let {
            SystemClock.currentTimeMillis + TimeUnit.SECONDS.toMillis(it)
        }
    }

    fun jsonSerialize() = libJson.encodeToString(value = this, serializer = serializer())

    data class Builder(
        val request: TokenRequest,
    ) {
        private var tokenType: String? = null
        private var accessToken: String? = null
        private var expiresIn: Long? = null
        private var expiresAt: Long? = null
        private var idToken: String? = null
        private var refreshToken: String? = null
        private var scope: String? = null
        private var additionalParameters: Map<String, String> = mapOf()

        fun tokenType(tokenType: String?) = apply {
            this.tokenType = tokenType
        }

        fun accessToken(accessToken: String?) = apply {
            this.accessToken = accessToken
        }

        fun expiresIn(expiresIn: Long?, clock: Clock) = apply {
            this.expiresIn = expiresIn
        }

        fun expiresAt(expiresAt: Long?) = apply {
            this.expiresAt = expiresAt
        }

        fun idToken(idToken: String?) = apply {
            this.idToken = idToken
        }

        fun refreshToken(refreshToken: String?) = apply {
            this.refreshToken = refreshToken
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

        fun build() = TokenResponse(
            scope = scope,
            idToken = idToken,
            tokenType = tokenType,
            tokenRequest = request,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            expiresIn = expiresIn,
            additionalParameters = additionalParameters,
        )
    }

    companion object {
        const val TOKEN_TYPE_BEARER = "Bearer"

        object Params {
            const val REQUEST = "request"
            const val EXPIRES_AT = "expires_at"
            const val TOKEN_TYPE = "token_type"
            const val ACCESS_TOKEN = "access_token"
            const val EXPIRES_IN = "expires_in"
            const val REFRESH_TOKEN = "refresh_token"
            const val ID_TOKEN = "id_token"
            const val SCOPE = "scope"
            const val ADDITIONAL_PARAMETERS = "additional_parameters"
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.TOKEN_TYPE,
            Params.ACCESS_TOKEN,
            Params.EXPIRES_IN,
            Params.REFRESH_TOKEN,
            Params.ID_TOKEN,
            Params.SCOPE
        )

        fun jsonDeserialize(json: String): TokenResponse? = runCatching {
            return Json { ignoreUnknownKeys = true }.decodeFromString<TokenResponse>(json)
        }.getOrNull()
    }
}
