package net.openid.appauth.kotlin.library.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthorizationServiceDiscovery(
    val issuer: String,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("jwks_uri")
    val jwksUri: String,
    @SerialName("response_types_supported")
    val responseTypesSupported: List<String>,
    @SerialName("subject_types_supported")
    val subjectTypesSupported: List<String>,
    @SerialName("id_token_signing_alg_values_supported")
    val idTokenSigningAlgorithmValuesSupported: List<String>,
)
