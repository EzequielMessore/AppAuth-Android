package net.openid.appauth.kotlin.library.model.request

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.appendParameter
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.extension.stringToSet
import net.openid.appauth.kotlin.library.model.AuthorizationManagementRequest
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import net.openid.appauth.kotlin.library.utils.AuthorizationManagementUtil

@Serializable
data class EndSessionRequest(
    val configuration: AuthorizationServiceConfiguration,
    val idTokenHint: String? = null,
    val postLogoutRedirectUri: String? = null,
    override val state: String? = null,
    val uiLocales: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) : AuthorizationManagementRequest {

    val uiLocalesSet
        get() = uiLocales?.stringToSet()

    override fun jsonSerializeString() =
        Json.encodeToString(serializer(), this)

    override fun toUri(): Uri? {
        return configuration.endSessionEndpoint?.toUri()?.buildUpon()?.apply {
            appendParameter(Params.STATE, state)
            appendParameter(Params.UI_LOCALES, uiLocales)
            appendParameter(Params.ID_TOKEN_HINT, idTokenHint)
            appendParameter(Params.POST_LOGOUT_REDIRECT_URI, postLogoutRedirectUri)
            additionalParameters.forEach { (key, value) ->
                appendParameter(key, value)
            }
        }?.build()
    }

    class Builder(
        private val configuration: AuthorizationServiceConfiguration,
    ) {
        private var idTokenHint: String? = null
        private var postLogoutRedirectUri: String? = null
        private var state: String? = null
        private var uiLocales: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()

        init {
            this.state = AuthorizationManagementUtil.generateRandomState()
        }


        fun idTokenHint(idTokenHint: String?) = apply {
            this.idTokenHint = idTokenHint
        }

        fun postLogoutRedirectUri(postLogoutRedirectUri: String?) = apply {
            this.postLogoutRedirectUri = postLogoutRedirectUri
        }

        fun state(state: String?) = apply {
            this.state = state
        }

        fun uiLocales(uiLocales: String?) = apply {
            this.uiLocales = uiLocales
        }

        fun uiLocalesValues(vararg uiLocales: String?) = apply {
            if (uiLocales.isEmpty()) {
                this.uiLocales = null
                return@apply
            }
            this.uiLocales = uiLocales.joinToString(" ")
        }

        fun additionalParameters(additionalParameters: Map<String, String>) = apply {
            this.additionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
        }

        fun build() = EndSessionRequest(
            state = state,
            uiLocales = uiLocales,
            idTokenHint = idTokenHint,
            configuration = configuration,
            additionalParameters = additionalParameters,
            postLogoutRedirectUri = postLogoutRedirectUri,
        )

    }

    companion object {
        object Params {
            const val STATE = "state"
            const val UI_LOCALES = "ui_locales"
            const val ID_TOKEN_HINT = "id_token_hint"
            const val POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri"
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.STATE,
            Params.UI_LOCALES,
            Params.ID_TOKEN_HINT,
            Params.POST_LOGOUT_REDIRECT_URI,
        )

        fun jsonDeserialize(json: String) = Json.decodeFromString(string = json, deserializer = serializer())
    }
}