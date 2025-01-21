package net.openid.appauth.kotlin.library.model.request

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.extension.appendParameter
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.json.libJson
import net.openid.appauth.kotlin.library.model.AuthorizationManagementRequest
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import net.openid.appauth.kotlin.library.utils.AuthorizationManagementUtil
import net.openid.appauth.kotlin.library.utils.CodeVerifierUtil

@Serializable
data class AuthorizationRequest(
    // required
    val configuration: AuthorizationServiceConfiguration,
    val clientId: String,
    val redirectUri: String,
    val responseType: String,

    // optional
    val scope: String? = null,
    val nonce: String? = null,
    val prompt: String? = null,
    val claims: String? = null,
    val display: String? = null,
    val loginHint: String? = null,
    val uiLocales: String? = null,
    val codeVerifier: String? = null,
    val responseMode: String? = null,
    val claimsLocales: String? = null,
    override val state: String? = null,
    val codeVerifierChallenge: String? = null,
    val codeVerifierChallengeMethod: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) : AuthorizationManagementRequest {
    override fun jsonSerializeString() = libJson.encodeToString(value = this, serializer = serializer())

    override fun toUri(): Uri? {
        return configuration.authorizationEndpoint.toUri().buildUpon()?.apply {
            appendParameter(Params.REDIRECT_URI, redirectUri)
            appendParameter(Params.CLIENT_ID, clientId)
            appendParameter(Params.RESPONSE_TYPE, responseType)

            appendParameter(Params.DISPLAY, display)
            appendParameter(Params.LOGIN_HINT, loginHint)
            appendParameter(Params.PROMPT, prompt)
            appendParameter(Params.UI_LOCALES, uiLocales)
            appendParameter(Params.STATE, state)
            appendParameter(Params.NONCE, nonce)
            appendParameter(Params.SCOPE, scope)
            appendParameter(Params.RESPONSE_MODE, responseMode)

            if (codeVerifier != null) {
                appendParameter(Params.CODE_CHALLENGE, codeVerifierChallenge)
                appendParameter(Params.CODE_CHALLENGE_METHOD, codeVerifierChallengeMethod)
            }

            appendParameter(Params.CLAIMS, claims)
            appendParameter(Params.CLAIMS_LOCALES, claimsLocales)

            additionalParameters.forEach { (key, value) ->
                appendParameter(key, value)
            }
        }?.build()
    }

    class Builder(
        private val configuration: AuthorizationServiceConfiguration,
    ) {

        init {
            state(AuthorizationManagementUtil.generateRandomState())
            nonce(AuthorizationManagementUtil.generateRandomState())
            codeVerifier(CodeVerifierUtil.generateRandomCodeVerifier())
        }

        private lateinit var clientId: String
        private lateinit var redirectUri: String
        private lateinit var responseType: String
        private var scope: String? = null
        private var nonce: String? = null
        private var prompt: String? = null
        private var claims: String? = null
        private var display: String? = null
        private var loginHint: String? = null
        private var uiLocales: String? = null
        private var codeVerifier: String? = null
        private var responseMode: String? = null
        private var claimsLocales: String? = null
        private var state: String? = null
        private var codeVerifierChallenge: String? = null
        private var codeVerifierChallengeMethod: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()


        fun clientId(clientId: String) = apply {
            this.clientId = clientId
        }

        fun redirectUri(redirectUri: String) = apply {
            this.redirectUri = redirectUri
        }

        fun responseType(responseType: String) = apply {
            this.responseType = responseType
        }

        fun scope(scope: String?) = apply {
            this.scope = scope
        }

        fun nonce(nonce: String?) = apply {
            this.nonce = nonce
        }

        fun prompt(prompt: String?) = apply {
            this.prompt = prompt
        }

        fun claims(claims: String?) = apply {
            this.claims = claims
        }

        fun display(display: String?) = apply {
            this.display = display
        }

        fun loginHint(loginHint: String?) = apply {
            this.loginHint = loginHint
        }

        fun uiLocales(uiLocales: String?) = apply {
            this.uiLocales = uiLocales
        }

        fun codeVerifier(codeVerifier: String?) = apply {
            if (codeVerifier != null) {
                CodeVerifierUtil.checkCodeVerifier(codeVerifier)
                this.codeVerifier = codeVerifier
                this.codeVerifierChallenge = CodeVerifierUtil.deriveCodeVerifierChallenge(codeVerifier)
                this.codeVerifierChallengeMethod = CodeVerifierUtil.codeVerifierChallengeMethod
            } else {
                this.codeVerifier = null
                this.codeVerifierChallenge = null
                this.codeVerifierChallengeMethod = null
            }
        }

        fun responseMode(responseMode: String?) = apply {
            this.responseMode = responseMode
        }

        fun claimsLocales(claimsLocales: String?) = apply {
            this.claimsLocales = claimsLocales
        }

        fun state(state: String?) = apply {
            this.state = state
        }

        fun codeVerifierChallenge(codeVerifierChallenge: String?) = apply {
            this.codeVerifierChallenge = codeVerifierChallenge
        }

        fun codeVerifierChallengeMethod(codeVerifierChallengeMethod: String?) = apply {
            this.codeVerifierChallengeMethod = codeVerifierChallengeMethod
        }

        fun additionalParameters(additionalParameters: Map<String, String>) = apply {
            this.additionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
        }

        fun build() = AuthorizationRequest(
            configuration = configuration,
            clientId = clientId,
            redirectUri = redirectUri,
            responseType = responseType,
            scope = scope,
            nonce = nonce,
            prompt = prompt,
            claims = claims,
            display = display,
            loginHint = loginHint,
            uiLocales = uiLocales,
            codeVerifier = codeVerifier,
            responseMode = responseMode,
            claimsLocales = claimsLocales,
            state = state,
            codeVerifierChallenge = codeVerifierChallenge,
            codeVerifierChallengeMethod = codeVerifierChallengeMethod,
            additionalParameters = additionalParameters
        )
    }

    companion object {
        const val CODE_CHALLENGE_METHOD_S256 = "S256"
        const val CODE_CHALLENGE_METHOD_PLAIN = "plain"

        object Params {
            const val CLIENT_ID = "client_id";
            const val CODE_CHALLENGE = "code_challenge";
            const val CODE_CHALLENGE_METHOD = "code_challenge_method";
            const val DISPLAY = "display";
            const val LOGIN_HINT = "login_hint";
            const val PROMPT = "prompt";
            const val UI_LOCALES = "ui_locales";
            const val REDIRECT_URI = "redirect_uri";
            const val RESPONSE_MODE = "response_mode";
            const val RESPONSE_TYPE = "response_type";
            const val SCOPE = "scope";
            const val STATE = "state";
            const val NONCE = "nonce";
            const val CLAIMS = "claims";
            const val CLAIMS_LOCALES = "claims_locales";
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.CLIENT_ID,
            Params.CODE_CHALLENGE,
            Params.CODE_CHALLENGE_METHOD,
            Params.DISPLAY,
            Params.LOGIN_HINT,
            Params.PROMPT,
            Params.UI_LOCALES,
            Params.REDIRECT_URI,
            Params.RESPONSE_MODE,
            Params.RESPONSE_TYPE,
            Params.SCOPE,
            Params.STATE,
            Params.CLAIMS,
            Params.CLAIMS_LOCALES
        )

        fun jsonDeserialize(json: String) = Json { ignoreUnknownKeys = true }.decodeFromString(deserializer = serializer(), string = json)
    }
}