package net.openid.appauth.kotlin.library.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.errors.IOException
import net.openid.appauth.kotlin.library.LibraryComponent
import net.openid.appauth.kotlin.library.exception.AuthorizationException
import net.openid.appauth.kotlin.library.internal.Logger
import net.openid.appauth.kotlin.library.model.AuthorizationServiceConfiguration
import org.koin.core.component.inject

class AuthorizationServiceApi : LibraryComponent {

     private val client: HttpClient by inject<HttpClient>()

    suspend fun fetchFromUrl(
        discoveryUri: String,
    ): Result<AuthorizationServiceConfiguration> {
        return runCatching {
            client.get(discoveryUri)
                .body<AuthorizationServiceConfiguration>()
        }
    }
}