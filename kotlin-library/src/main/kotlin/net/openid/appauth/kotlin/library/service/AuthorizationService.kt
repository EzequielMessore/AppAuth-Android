package net.openid.appauth.kotlin.library.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import net.openid.appauth.kotlin.library.activity.AuthorizationManagementActivity
import net.openid.appauth.kotlin.library.browser.BrowserDescriptor
import net.openid.appauth.kotlin.library.browser.BrowserSelector
import net.openid.appauth.kotlin.library.browser.CustomTabManager
import net.openid.appauth.kotlin.library.internal.Logger
import net.openid.appauth.kotlin.library.model.AuthorizationManagementRequest
import net.openid.appauth.kotlin.library.model.request.AuthorizationRequest

class AuthorizationService(
    var context: Context,
    private val clientConfiguration: AppAuthConfiguration = AppAuthConfiguration.DEFAULT,
    private val browser: BrowserDescriptor? = BrowserSelector.select(context, clientConfiguration.browserMatcher),
    private val customTabManager: CustomTabManager = CustomTabManager(context),
) {
    private var disposed = false

    init {
        browser?.let {
            if (it.useCustomTab) {
                customTabManager.bind(it.packageName)
            }
        }
    }

    fun createCustomTabsIntentBuilder(vararg possibleUris: Uri): CustomTabsIntent.Builder {
        checkNotDisposed()
        return customTabManager.createTabBuilder(*possibleUris)
    }

    private fun checkNotDisposed() {
        if (disposed) {
            throw IllegalStateException("Service has been disposed and is not usable")
        }
    }

    fun dispose() {
        disposed = true
    }

    fun getAuthorizationRequestIntent(request: AuthorizationRequest): Result<Intent> {
        val customTabsIntent = createCustomTabsIntentBuilder().build()
        val authIntentResult = prepareRequestIntent(request, customTabsIntent)
        return authIntentResult.map { authIntent ->
            AuthorizationManagementActivity.createStartForResultIntent(
                context = context,
                request = request,
                authIntent = authIntent,
            )
        }
    }

    fun getEndSessionRequestIntent(request: AuthorizationManagementRequest): Result<Intent> {
        val customTabsIntent = createCustomTabsIntentBuilder().build()
        val endSessionIntentResult = prepareRequestIntent(request, customTabsIntent)
        return endSessionIntentResult.map { endSessionIntent ->
            AuthorizationManagementActivity.createStartForResultIntent(
                context = context,
                request = request,
                authIntent = endSessionIntent,
            )
        }
    }

    private fun prepareRequestIntent(request: AuthorizationManagementRequest, customTabsIntent: CustomTabsIntent): Result<Intent> {
        checkNotDisposed()

        if (browser == null) {
            Logger.warn("No browser available")
            return Result.failure(IllegalStateException("No browser available"))
        }

        val requestUri = request.toUri()
        val intent = if (browser.useCustomTab) {
            customTabsIntent.intent
        } else {
            Intent(Intent.ACTION_VIEW)
        }
        intent.setPackage(browser.packageName)
        intent.setData(requestUri)

        Logger.debug(
            "Using %s as browser for auth, custom tab = %s",
            intent.getPackage(),
            browser.useCustomTab.toString()
        )

        return Result.success(intent)
    }
}