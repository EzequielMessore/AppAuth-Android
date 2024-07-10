package net.openid.appauth.kotlin.library.model

import android.net.Uri
import org.json.JSONObject

/**
 * A base request for session management models
 * [AuthorizationRequest]
 * [EndSessionRequest]
 */
interface AuthorizationManagementRequest {
    /**
     * Produces a JSON string representation of the request for persistent storage or
     * local transmission (e.g. between activities). This method is just a convenience wrapper
     * for [.jsonSerialize], converting the JSON object to its string form.
     */
    fun jsonSerializeString(): String?

    /**
     * An opaque value used by the client to maintain state between the request and callback.
     */
    val state: String?

    /**
     * Produces a request URI, that can be used to dispatch the request.
     */
    fun toUri(): Uri?
}
