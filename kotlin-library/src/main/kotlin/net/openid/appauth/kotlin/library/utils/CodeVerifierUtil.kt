package net.openid.appauth.kotlin.library.utils

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.regex.Pattern
import net.openid.appauth.kotlin.library.internal.Logger
import net.openid.appauth.kotlin.library.model.request.AuthorizationRequest

/**
 * Generates code verifiers and challenges for PKCE exchange.
 *
 * @see "Proof Key for Code Exchange by OAuth Public Clients
 */
object CodeVerifierUtil {

    /**
     * The minimum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients
     */
    private const val MIN_CODE_VERIFIER_LENGTH: Int = 43

    /**
     * The maximum permitted length for a code verifier.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients
     */
    private const val MAX_CODE_VERIFIER_LENGTH: Int = 128

    /**
     * The default entropy (in bytes) used for the code verifier.
     */
    private const val DEFAULT_CODE_VERIFIER_ENTROPY: Int = 64

    /**
     * The minimum permitted entropy (in bytes) for use with
     * [.generateRandomCodeVerifier].
     */
    private const val MIN_CODE_VERIFIER_ENTROPY: Int = 32

    /**
     * The maximum permitted entropy (in bytes) for use with
     * [.generateRandomCodeVerifier].
     */
    private const val MAX_CODE_VERIFIER_ENTROPY: Int = 96

    /**
     * Base64 encoding settings used for generated code verifiers.
     */
    private const val PKCE_BASE64_ENCODE_SETTINGS = Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE

    /**
     * Regex for legal code verifier strings, as defined in the spec.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients
     */
    private val REGEX_CODE_VERIFIER: Pattern = Pattern.compile("^[0-9a-zA-Z\\-._~]{43,128}$")


    /**
     * Throws an IllegalArgumentException if the provided code verifier is invalid.
     *
     * @see "Proof Key for Code Exchange by OAuth Public Clients
     */
    fun checkCodeVerifier(codeVerifier: String) {
        check(
            value = MIN_CODE_VERIFIER_LENGTH <= codeVerifier.length,
            lazyMessage = { "codeVerifier length is shorter than allowed by the PKCE specification" },
        )
        check(
            value = codeVerifier.length <= MAX_CODE_VERIFIER_LENGTH,
            lazyMessage = { "codeVerifier length is longer than allowed by the PKCE specification" },
        )
        check(
            value = REGEX_CODE_VERIFIER.matcher(codeVerifier).matches(),
            lazyMessage = { "codeVerifier string contains illegal characters" },
        )
    }

    /**
     * Generates a random code verifier string using the provided entropy source and the specified
     * number of bytes of entropy.
     */
    /**
     * Generates a random code verifier string using [SecureRandom] as the source of
     * entropy, with the default entropy quantity as defined by
     * [.DEFAULT_CODE_VERIFIER_ENTROPY].
     */
    fun generateRandomCodeVerifier(): String {
        return generateRandomCodeVerifier(SecureRandom(), DEFAULT_CODE_VERIFIER_ENTROPY)
    }


    /**
     * Generates a random code verifier string using [SecureRandom] as the source of
     * entropy, with the default entropy quantity as defined by
     * [.DEFAULT_CODE_VERIFIER_ENTROPY].
     */
    @JvmOverloads
    fun generateRandomCodeVerifier(entropySource: SecureRandom?, entropyBytes: Int = DEFAULT_CODE_VERIFIER_ENTROPY): String {
        checkNotNull(entropySource) { "entropySource cannot be null" }

        check(
            value = MIN_CODE_VERIFIER_ENTROPY <= entropyBytes,
            lazyMessage = { "entropyBytes is less than the minimum permitted" },
        )
        check(
            value = entropyBytes <= MAX_CODE_VERIFIER_ENTROPY,
            lazyMessage = { "entropyBytes is greater than the maximum permitted" },
        )

        val randomBytes = ByteArray(entropyBytes)
        entropySource.nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, PKCE_BASE64_ENCODE_SETTINGS)
    }

    /**
     * Produces a challenge from a code verifier, using SHA-256 as the challenge method if the
     * system supports it (all Android devices _should_ support SHA-256), and falls back
     * to the [&quot;plain&quot; challenge type][AuthorizationRequest.CODE_CHALLENGE_METHOD_PLAIN] if
     * unavailable.
     */
    fun deriveCodeVerifierChallenge(codeVerifier: String): String {
        try {
            val sha256Digester = MessageDigest.getInstance("SHA-256")
            sha256Digester.update(codeVerifier.toByteArray(charset("ISO_8859_1")))
            val digestBytes = sha256Digester.digest()
            return Base64.encodeToString(digestBytes, PKCE_BASE64_ENCODE_SETTINGS)
        } catch (e: NoSuchAlgorithmException) {
            Logger.warn("SHA-256 is not supported on this device! Using plain challenge", e)
            return codeVerifier
        } catch (e: UnsupportedEncodingException) {
            Logger.error("ISO-8859-1 encoding not supported on this device!", e)
            throw IllegalStateException("ISO-8859-1 encoding not supported", e)
        }
    }

    val codeVerifierChallengeMethod: String
        /**
         * Returns the challenge method utilized on this system: typically
         * [SHA-256][AuthorizationRequest.CODE_CHALLENGE_METHOD_S256] if supported by
         * the system, [plain][AuthorizationRequest.CODE_CHALLENGE_METHOD_PLAIN] otherwise.
         */
        get() {
            try {
                MessageDigest.getInstance("SHA-256")
                // no exception, so SHA-256 is supported
                return AuthorizationRequest.CODE_CHALLENGE_METHOD_S256
            } catch (e: NoSuchAlgorithmException) {
                return AuthorizationRequest.CODE_CHALLENGE_METHOD_PLAIN
            }
        }
}
