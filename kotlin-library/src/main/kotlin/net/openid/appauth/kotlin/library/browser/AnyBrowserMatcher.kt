package net.openid.appauth.kotlin.library.browser

object AnyBrowserMatcher : BrowserMatcher {
    override fun matches(descriptor: BrowserDescriptor?) = true
}