package net.openid.appauth.kotlin.library.model.jwt

import kotlinx.serialization.Serializable

@Serializable
data class JwtClaims(
    val exp: Long?,
    val iat: Long?,
    val iss: String?,
    val aud: List<String>?,
    val sub: String?,
    val azp: String?,
    val nonce: String?,
)
