package net.openid.appauth.kotlin.library.model.request

import kotlinx.serialization.Serializable
import net.openid.appauth.kotlin.library.extension.checkAdditionalParams
import net.openid.appauth.kotlin.library.json.libJson
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration

@Serializable
data class RegistrationRequest(
    val configuration: AuthorizationServiceConfiguration,
    val redirectUris: List<String>,
    val applicationType: String,
    val responseTypes: List<String>? = null,
    val grantTypes: List<String>? = null,
    val subjectType: String? = null,
    val jwksUri: String? = null,
    val jwks: String? = null,
    val tokenEndpointAuthenticationMethod: String? = null,
    val additionalParameters: Map<String, String> = emptyMap(),
) {

    fun jsonSerialize() = libJson.encodeToString(value = this, serializer = serializer())

    class Builder(
        val redirectUris: List<String>,
        val configuration: AuthorizationServiceConfiguration,
    ) {
        private var responseTypes: List<String>? = null
        private var grantTypes: List<String>? = null
        private var subjectType: String? = null
        private var jwksUri: String? = null
        private var jwks: String? = null
        private var tokenEndpointAuthenticationMethod: String? = null
        private var additionalParameters: Map<String, String> = emptyMap()

        fun responseTypes(vararg responseTypes: String) = apply {
            responseTypes(responseTypes.toList())
        }

        fun responseTypes(responseTypes: List<String>) = apply {
            this.responseTypes = responseTypes
        }

        fun grantTypes(vararg grantTypes: String) = apply {
            grantTypes(grantTypes.toList())
        }

        fun grantTypes(grantTypes: List<String>) = apply {
            this.grantTypes = grantTypes
        }

        fun subjectType(subjectType: String) = apply {
            this.subjectType = subjectType
        }

        fun jwksUri(jwksUri: String) = apply {
            this.jwksUri = jwksUri
        }

        fun jwks(jwks: String) = apply {
            this.jwks = jwks
        }

        fun tokenEndpointAuthenticationMethod(tokenEndpointAuthenticationMethod: String) = apply {
            this.tokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod
        }

        fun additionalParameters(additionalParameters: Map<String, String>) = apply {
            this.additionalParameters = checkAdditionalParams(additionalParameters, BUILT_IN_PARAMS)
        }

        fun build() = RegistrationRequest(
            jwks = jwks,
            jwksUri = jwksUri,
            grantTypes = grantTypes,
            subjectType = subjectType,
            redirectUris = redirectUris,
            configuration = configuration,
            responseTypes = responseTypes,
            applicationType = APPLICATION_TYPE_NATIVE,
            additionalParameters = additionalParameters,
            tokenEndpointAuthenticationMethod = tokenEndpointAuthenticationMethod,
        )
    }

    companion object {
        const val SUBJECT_TYPE_PUBLIC = "public"
        const val SUBJECT_TYPE_PAIRWISE = "pairwise"
        const val APPLICATION_TYPE_NATIVE = "native"

        object Params {
            const val REDIRECT_URIS = "redirect_uris"
            const val RESPONSE_TYPES = "response_types"
            const val GRANT_TYPES = "grant_types"
            const val APPLICATION_TYPE = "application_type"
            const val SUBJECT_TYPE = "subject_type"
            const val JWKS_URI = "jwks_uri"
            const val JWKS = "jwks"
            const val TOKEN_ENDPOINT_AUTHENTICATION_METHOD = "token_endpoint_auth_method"
        }

        private val BUILT_IN_PARAMS = setOf(
            Params.REDIRECT_URIS,
            Params.RESPONSE_TYPES,
            Params.GRANT_TYPES,
            Params.APPLICATION_TYPE,
            Params.SUBJECT_TYPE,
            Params.JWKS_URI,
            Params.JWKS,
            Params.TOKEN_ENDPOINT_AUTHENTICATION_METHOD
        )
    }
}