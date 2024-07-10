package net.openid.appauth.kotlin.library.extension

import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsService
import java.net.URLDecoder
import java.net.URLEncoder

private val charset = charset("UTF-8")

internal fun Uri.Builder.appendParameter(key: String, value: String?) {
    value?.let {
        appendQueryParameter(key, it)
    }
}

fun String?.parseUriIfAvailable(): Uri? {
    return this?.let { Uri.parse(it) }
}

fun Uri.Builder.appendQueryParameterIfNotNull(paramName: String, value: Any?) {
    value?.let { appendQueryParameter(paramName, it.toString()) }
}

fun Uri.getLongQueryParameter(param: String): Long? {
    return this.getQueryParameter(param)?.toLongOrNull()
}

fun List<Uri?>.toCustomTabUriBundle(startIndex: Int): List<Bundle> {
    require(startIndex >= 0) { "startIndex must be positive" }
    if (this.isEmpty() || this.size <= startIndex) {
        return emptyList()
    }

    return this.drop(startIndex).filterNotNull().map { uri ->
        Bundle().apply { putParcelable(CustomTabsService.KEY_URL, uri) }
    }
}

internal fun Map<String, String>?.formUrlEncode(): String {
    return this
        ?.map { "${it.key}=${it.value}" }
        ?.joinToString("&")
        .orEmpty()
}

internal fun String.formUrlEncodeValue(): String {
    return URLEncoder.encode(this, charset.name())
}

fun String.formUrlDecode(): List<Pair<String, String>> {
    if (this.isEmpty()) {
        return emptyList()
    }

    return this.split("&").mapNotNull { part ->
        val paramAndValue = part.split("=")
        if (paramAndValue.size == 2) {
            Pair(paramAndValue[0], URLDecoder.decode(paramAndValue[1], charset.name()))
        } else {
            null
        }
    }
}

fun String.formUrlDecodeUnique(): Map<String, String> {
    return this.formUrlDecode().associate { it.first to it.second }
}
