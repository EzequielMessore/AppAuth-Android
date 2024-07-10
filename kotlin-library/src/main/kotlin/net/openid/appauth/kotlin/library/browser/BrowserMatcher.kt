package net.openid.appauth.kotlin.library.browser

interface BrowserMatcher {
    /**
     * @return true if the browser matches some set of criteria.
     */
    fun matches(descriptor: BrowserDescriptor?): Boolean
}
