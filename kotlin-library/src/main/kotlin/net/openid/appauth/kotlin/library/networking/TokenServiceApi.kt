package net.openid.appauth.kotlin.library.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import net.openid.appauth.kotlin.library.LibraryComponent
import net.openid.appauth.kotlin.library.model.jwt.IdToken
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.model.response.TokenResponse
import net.openid.appauth.kotlin.library.utils.SystemClock
import org.koin.core.component.inject

class TokenServiceApi : LibraryComponent {

    private val client: HttpClient by inject<HttpClient>()

    suspend fun performTokenRequest(
        tokenRequest: TokenRequest,
    ): Result<TokenResponse> {
        return runCatching {
            val parameters = tokenRequest.requestParameters.toMutableMap()

            client.post(tokenRequest.configuration.tokenEndpoint) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    parameters.forEach { (key, value) -> append(key, value) }
                }))
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
