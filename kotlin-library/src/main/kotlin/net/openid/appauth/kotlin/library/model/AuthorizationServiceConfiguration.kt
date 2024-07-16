package net.openid.appauth.kotlin.library.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthorizationServiceConfiguration(
    val issuer: String? = null,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: String,
    @SerialName("token_endpoint")
    val tokenEndpoint: String,
    @SerialName("end_session_endpoint")
    val endSessionEndpoint: String? = null,
    @SerialName("registration_endpoint")
    val registrationEndpoint: String? = null,
    @SerialName("userinfo_endpoint")
    val userInfoEndpoint: String? = null,
) {

    fun toJson(): String {
        return Json { ignoreUnknownKeys = true }.encodeToString(value = this, serializer = serializer())
    }

    fun fromJson(json: String): AuthorizationServiceConfiguration {
        return Json { ignoreUnknownKeys = true }.decodeFromString(string = json, deserializer = serializer())
    }

    companion object {
        private const val WELL_KNOWN_PATH = ".well-known"
        private const val OPENID_CONFIGURATION_RESOURCE: String = "openid-configuration"
    }
}
