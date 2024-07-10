package net.openid.appauth.kotlin.library.model

import android.content.Intent
import org.json.JSONObject

interface AuthorizationManagementResponse {
    val state: String?
    fun toIntent(): Intent?
    fun jsonSerialize(): String
}
