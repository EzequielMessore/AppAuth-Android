package net.openid.appauth.kotlin.library.model.response

import android.content.Intent
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.openid.appauth.kotlin.library.model.AuthorizationManagementResponse
import net.openid.appauth.kotlin.library.model.request.EndSessionRequest

@Serializable
data class EndSessionResponse(
    val request: EndSessionRequest,
    override val state: String? = null,
) : AuthorizationManagementResponse {

    override fun toIntent() = Intent().apply {
        putExtra(EXTRA_RESPONSE, jsonSerialize())
    }

    override fun jsonSerialize(): String =
        Json.encodeToString(value = this, serializer = serializer())

    class Builder(
        private val request: EndSessionRequest,
    ) {
        private var state: String? = null

        fun state(state: String?) = apply {
            this.state = state
        }

        fun fromUri(uri: Uri?) = apply {
            state(uri?.getQueryParameter(Params.STATE))
        }

        fun build() = EndSessionResponse(
            request = request,
            state = state,
        )
    }

    companion object {
        const val EXTRA_RESPONSE = "net.openid.appauth.EndSessionResponse"

        object Params {
            const val STATE = "state"
        }

        fun jsonDeserialize(json: String): EndSessionResponse {
            return Json.decodeFromString(string = json, deserializer = serializer())
        }

        fun fromIntent(intent: Intent?): EndSessionResponse? {
            requireNotNull(intent) { "intent must not be null" }
            if (!contains(intent)) return null

            return runCatching {
                jsonDeserialize(intent.getStringExtra(EXTRA_RESPONSE)!!)
            }.getOrElse {
                throw IllegalArgumentException("Intent contains malformed auth response", it)
            }
        }

        fun contains(intent: Intent): Boolean {
            return intent.hasExtra(EXTRA_RESPONSE)
        }
    }
}
