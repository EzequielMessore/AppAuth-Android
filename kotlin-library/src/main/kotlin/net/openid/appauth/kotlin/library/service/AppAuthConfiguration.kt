package net.openid.appauth.kotlin.library.service

import net.openid.appauth.kotlin.library.browser.AnyBrowserMatcher
import net.openid.appauth.kotlin.library.browser.BrowserMatcher

class AppAuthConfiguration private constructor(
    val skipIssuerHttpsCheck: Boolean,
    val browserMatcher: BrowserMatcher,
) {
    class Builder {
        private var browserMatcher: BrowserMatcher = AnyBrowserMatcher
        private var skipIssuerHttpsCheck: Boolean = false

        fun build() = AppAuthConfiguration(
            browserMatcher = browserMatcher,
            skipIssuerHttpsCheck = skipIssuerHttpsCheck
        )
    }

    companion object {
        val DEFAULT = Builder().build()
    }
}