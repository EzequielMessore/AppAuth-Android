package net.openid.appauth.kotlin.library.browser

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.util.Base64
import androidx.annotation.NonNull
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Represents a browser that may be used for an authorization flow.
 */
class BrowserDescriptor(
    val packageName: String,
    val signatureHashes: Set<String>,
    val version: String,
    val useCustomTab: Boolean,
) {
    constructor(packageInfo: PackageInfo, useCustomTab: Boolean) : this(
        packageInfo.packageName,
        generateSignatureHashes(packageInfo.signatures),
        packageInfo.versionName,
        useCustomTab
    )

    fun changeUseCustomTab(newUseCustomTabValue: Boolean): BrowserDescriptor {
        return BrowserDescriptor(
            packageName,
            signatureHashes,
            version,
            newUseCustomTabValue
        )
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }

        if (obj == null || obj !is BrowserDescriptor) {
            return false
        }

        val other = obj
        return this.packageName == other.packageName
            && (this.version == other.version)
            && (this.useCustomTab == other.useCustomTab)
            && signatureHashes == other.signatureHashes
    }

    override fun hashCode(): Int {
        var hash = packageName.hashCode()

        hash = PRIME_HASH_FACTOR * hash + version.hashCode()
        hash = PRIME_HASH_FACTOR * hash + (if (useCustomTab) 1 else 0)

        for (signatureHash in signatureHashes) {
            hash = PRIME_HASH_FACTOR * hash + signatureHash.hashCode()
        }

        return hash
    }

    companion object {
        // See: http://stackoverflow.com/a/2816747
        private const val PRIME_HASH_FACTOR = 92821

        private const val DIGEST_SHA_512 = "SHA-512"

        /**
         * Generates a SHA-512 hash, Base64 url-safe encoded, from a [Signature].
         */
        fun generateSignatureHash(signature: Signature): String {
            try {
                val digest = MessageDigest.getInstance(DIGEST_SHA_512)
                val hashBytes = digest.digest(signature.toByteArray())
                return Base64.encodeToString(hashBytes, Base64.URL_SAFE or Base64.NO_WRAP)
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException(
                    "Platform does not support $DIGEST_SHA_512 hashing"
                )
            }
        }

        /**
         * Generates a set of SHA-512, Base64 url-safe encoded signature hashes from the provided
         * array of signatures.
         */
        fun generateSignatureHashes(signatures: Array<Signature?>) = signatures
            .filterNotNull()
            .map(::generateSignatureHash)
            .toSet()
    }
}
