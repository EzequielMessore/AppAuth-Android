package net.openid.appauth.kotlin.library.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.util.InternalAPI
import net.openid.appauth.kotlin.library.LibraryComponent
import net.openid.appauth.kotlin.library.extension.formUrlEncode
import net.openid.appauth.kotlin.library.model.jwt.IdToken
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.model.response.TokenResponse
import net.openid.appauth.kotlin.library.utils.SystemClock
import org.koin.core.component.inject

class TokenServiceApi : LibraryComponent {

    private val client: HttpClient by inject<HttpClient>()

    @OptIn(InternalAPI::class)
    suspend fun performTokenRequest(
        tokenRequest: TokenRequest,
    ): Result<TokenResponse> {
        return runCatching {
            val parameters = tokenRequest.requestParameters.toMutableMap()

            client.post(tokenRequest.configuration.tokenEndpoint) {
                contentType(ContentType.Application.FormUrlEncoded)
                body = parameters.formUrlEncode()
                headers {
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
            }.body<TokenResponse>()
                .copy(tokenRequest = tokenRequest)
                .validateTokenResponse()
        }
    }

    private fun TokenResponse.validateTokenResponse() = apply {
        tokenRequest?.let {
            IdToken.from(idToken).validate(it, SystemClock.INSTANCE)
        }
    }
}
