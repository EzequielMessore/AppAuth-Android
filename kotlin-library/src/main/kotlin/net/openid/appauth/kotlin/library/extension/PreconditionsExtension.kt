package net.openid.appauth.kotlin.library.extension

import android.net.Uri

internal fun checkNotEmpty(value: String?, lazyMessage: () -> Any): String {
    if (value.isNullOrEmpty()) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

internal fun checkAdditionalParams(params: Map<String, String>, builtInParams: Set<String>): Map<String, String> {
    val additionalParams = LinkedHashMap<String, String>()
    for ((key, value) in params) {
        require(!builtInParams.contains(key)) {
            "Parameter $key is directly supported via the authorization request builder, use the builder method instead"
        }
        additionalParams[key] = value
    }

    return additionalParams.toMap()
}

internal fun String?.stringToSet(): Set<String> {
    if (this == null) return emptySet()

    return LinkedHashSet(split(" "))
}

fun <K, V : Any> Map<K, V?>.filterNotNullValues(): Map<K, V> = runCatching {
    this.entries
        .filter { it.value != null }
        .associateBy({ it.key }, { it.value!! })
}.getOrElse { emptyMap() }
