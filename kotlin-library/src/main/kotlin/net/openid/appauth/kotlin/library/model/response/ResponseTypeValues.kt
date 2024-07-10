package net.openid.appauth.kotlin.library.model.response

object ResponseTypeValues {

    /**
     * For requesting an authorization code.
     *
     * @see "The OAuth 2.0 Authorization Framework
     */
    const val CODE = "code"

    /**
     * For requesting an access token via an implicit grant.
     *
     * @see "The OAuth 2.0 Authorization Framework
     */
    const val TOKEN = "token"

    /**
     * For requesting an OpenID Conenct ID Token.
     *
     * @see "The OAuth 2.0 Authorization Framework
     */
    const val ID_TOKEN = "id_token"
}
