package net.openid.appauth.kotlin.library.model.jwt

import android.text.TextUtils
import android.util.Base64
import androidx.core.net.toUri
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.model.enum.GrantType
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.utils.Clock

@Serializable
data class IdToken(
    val issuer: String,
    val subject: String,
    val audience: List<String>,
    val expiration: Long,
    val issuedAt: Long,
    val nonce: String? = null,
    val authorizedParty: String? = null,
) {

    fun validate(tokenRequest: TokenRequest, clock: Clock) {
        validate(tokenRequest, clock, false)
    }

    private fun validate(tokenRequest: TokenRequest, clock: Clock, skipIssuerHttpsChecks: Boolean) {
        val discovery = tokenRequest.configuration.discovery
        discovery?.let {
            val expectedIssuer = discovery.issuer
            if (this.issuer != expectedIssuer) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("Issuer mismatch")
                )
            }

            val issuerUri = issuer.toUri()

            if (!skipIssuerHttpsChecks && issuerUri.scheme != "https") {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("Issuer must be an https URL")
                )
            }

            if (issuerUri.host.isNullOrEmpty()) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("Issuer host can not be empty")
                )
            }

            if (issuerUri.fragment != null || issuerUri.queryParameterNames.size > 0) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("Issuer URL should not containt query parameters or fragment components")
                )
            }

            // OpenID Connect Core Section 3.1.3.7. rule #3 & Section 2 azp Claim
            // Validates that the aud (audience) Claim contains the client ID, or that the azp
            // (authorized party) Claim matches the client ID.
            val clientId = tokenRequest.clientId
            if (!audience.contains(clientId) && clientId != authorizedParty) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("Audience mismatch")
                )
            }


            // OpenID Connect Core Section 3.1.3.7. rules #4 & #5
            // Not enforced.

            // OpenID Connect Core Section 3.1.3.7. rule #6
            // As noted above, AppAuth only supports the code flow which results in direct
            // communication of the ID Token from the Token Endpoint to the Client, and we are
            // exercising the option to use TLS server validation instead of checking the token
            // signature. Users may additionally check the token signature should they wish.

            // OpenID Connect Core Section 3.1.3.7. rules #7 & #8
            // Not enforced. See rule #6.

            // OpenID Connect Core Section 3.1.3.7. rule #9
            // Validates that the current time is before the expiry time.
            val nowInSeconds: Long = clock.currentTimeMillis / MILLIS_PER_SECOND
            if (nowInSeconds > this.expiration) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException("ID Token expired")
                )
            }


            // OpenID Connect Core Section 3.1.3.7. rule #10
            // Validates that the issued at time is not more than +/- 10 minutes on the current
            // time.
            if (abs((nowInSeconds - issuedAt).toDouble()) > TEN_MINUTES_IN_SECONDS) {
                throw AuthorizationException.fromTemplate(
                    AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                    IdTokenException(
                        "Issued at time is more than 10 minutes "
                            + "before or after the current time"
                    )
                )
            }


            // Only relevant for the authorization_code response type
            if (GrantType.AUTHORIZATION_CODE == tokenRequest.grantType) {
                // OpenID Connect Core Section 3.1.3.7. rule #11
                // Validates the nonce.
                val expectedNonce = tokenRequest.nonce
                if (!TextUtils.equals(nonce, expectedNonce)) {
                    throw AuthorizationException.fromTemplate(
                        AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR,
                        IdTokenException("Nonce mismatch")
                    )
                }
            }
        }

    }
    
    companion object {
        private const val MILLIS_PER_SECOND = 1000L
        private const val TEN_MINUTES_IN_SECONDS = 600L

        private fun parseJwtSection(jwtSection: String): JwtClaims? = runCatching {
            val decodedSection = Base64.decode(jwtSection, Base64.URL_SAFE)
            val jsonString = String(decodedSection)
            return Json.decodeFromString(JwtClaims.serializer(), jsonString)
        }.getOrNull()

        fun from(token: String?): IdToken {
            token ?: throw IdTokenException("ID token must not be null")

            val sections = token.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (sections.size <= 1)
                throw IdTokenException("ID token must have both header and claims section")


            val claims = parseJwtSection(sections[1])

            return IdToken(
                nonce = claims?.nonce,
                issuedAt = claims?.iat ?: 0L,
                authorizedParty = claims?.azp,
                issuer = claims?.iss.orEmpty(),
                expiration = claims?.exp ?: 0L,
                subject = claims?.sub.orEmpty(),
                audience = claims?.aud.orEmpty(),
            )
        }

        internal class IdTokenException(message: String?) : Exception(message)
    }
}