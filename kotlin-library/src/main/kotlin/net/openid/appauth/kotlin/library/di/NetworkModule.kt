package net.openid.appauth.kotlin.library.di

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import net.openid.appauth.kotlin.library.networking.AuthorizationServiceApi
import net.openid.appauth.kotlin.library.networking.TokenServiceApi
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            installJson()
        }
    }

    factoryOf(::TokenServiceApi)
    factoryOf(::AuthorizationServiceApi)
}

internal fun HttpClientConfig<*>.installJson(block: (JsonBuilder.() -> Unit)? = null) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                block?.invoke(this)
            }
        )
    }
}
