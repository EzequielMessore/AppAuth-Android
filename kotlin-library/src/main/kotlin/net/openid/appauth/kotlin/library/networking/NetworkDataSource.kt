package net.openid.appauth.kotlin.library.networking

import net.openid.appauth.kotlin.library.LibraryComponent
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import net.openid.appauth.kotlin.library.model.request.TokenRequest
import net.openid.appauth.kotlin.library.model.response.TokenResponse
import org.koin.core.component.inject

interface NetworkDataSourceContract {
    suspend fun fetchFromUrl(discoveryUri: String): Result<AuthorizationServiceConfiguration>
    suspend fun performTokenRequest(tokenRequest: TokenRequest,): Result<TokenResponse>
}

class NetworkDataSource : LibraryComponent, NetworkDataSourceContract {
    private val tokenServiceApi by inject<TokenServiceApi>()
    private val authorizationServiceApi by inject<AuthorizationServiceApi>()

    override suspend fun fetchFromUrl(discoveryUri: String) =
        authorizationServiceApi.fetchFromUrl(discoveryUri)

    override suspend fun performTokenRequest(tokenRequest: TokenRequest) =
        tokenServiceApi.performTokenRequest(tokenRequest)
}
