package net.openid.appauth.kotlin.library.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.browser.customtabs.CustomTabsService

object BrowserSelector {

    private const val SCHEME_HTTP = "http"
    private const val SCHEME_HTTPS = "https"

    private const val ACTION_CUSTOM_TABS_CONNECTION = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION

    private val BROWSER_INTENT = Intent()
        .setAction(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData(Uri.fromParts("http", "", null))

    @SuppressLint("PackageManagerGetSignatures")
    private fun getAllBrowsers(context: Context): List<BrowserDescriptor> {
        val pm = context.packageManager
        val browsers = mutableListOf<BrowserDescriptor>()
        var defaultBrowserPackage: String? = null

        var queryFlag = PackageManager.GET_RESOLVED_FILTER
        queryFlag = queryFlag or PackageManager.MATCH_ALL

        val resolvedDefaultActivity = pm.resolveActivity(BROWSER_INTENT, 0)
        resolvedDefaultActivity?.let {
            defaultBrowserPackage = it.activityInfo.packageName
        }

        val resolvedActivityList = pm.queryIntentActivities(BROWSER_INTENT, queryFlag)

        for (info in resolvedActivityList) {
            if (!isFullBrowser(info)) {
                continue
            }

            try {
                var defaultBrowserIndex = 0
                val packageInfo = pm.getPackageInfo(
                    info.activityInfo.packageName,
                    PackageManager.GET_SIGNATURES
                )

                if (hasWarmupService(pm, info.activityInfo.packageName)) {
                    val customTabBrowserDescriptor = BrowserDescriptor(packageInfo, true)
                    if (info.activityInfo.packageName == defaultBrowserPackage) {
                        browsers.add(defaultBrowserIndex, customTabBrowserDescriptor)
                        defaultBrowserIndex++
                    } else {
                        browsers.add(customTabBrowserDescriptor)
                    }
                }

                val fullBrowserDescriptor = BrowserDescriptor(packageInfo, false)
                if (info.activityInfo.packageName == defaultBrowserPackage) {
                    browsers.add(defaultBrowserIndex, fullBrowserDescriptor)
                } else {
                    browsers.add(fullBrowserDescriptor)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // a descriptor cannot be generated without the package info
            }
        }

        return browsers
    }

    @SuppressLint("PackageManagerGetSignatures")
    fun select(context: Context, browserMatcher: BrowserMatcher): BrowserDescriptor? {
        val allBrowsers = getAllBrowsers(context)
        var bestMatch: BrowserDescriptor? = null
        for (browser in allBrowsers) {
            if (!browserMatcher.matches(browser)) {
                continue
            }

            if (browser.useCustomTab) {
                return browser
            }

            if (bestMatch == null) {
                bestMatch = browser
            }
        }

        return bestMatch
    }

    private fun hasWarmupService(pm: PackageManager, packageName: String): Boolean {
        val serviceIntent = Intent()
        serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
        serviceIntent.`package` = packageName
        return pm.resolveService(serviceIntent, 0) != null
    }

    private fun isFullBrowser(resolveInfo: ResolveInfo): Boolean {
        val filter = resolveInfo.filter ?: return false

        if (!filter.hasAction(Intent.ACTION_VIEW) ||
            !filter.hasCategory(Intent.CATEGORY_BROWSABLE) ||
            filter.schemesIterator() == null
        ) {
            return false
        }

        if (filter.authoritiesIterator() != null) {
            return false
        }

        var supportsHttp = false
        var supportsHttps = false
        val schemeIter = filter.schemesIterator()
        while (schemeIter.hasNext()) {
            val scheme = schemeIter.next()
            supportsHttp = supportsHttp or (SCHEME_HTTP == scheme)
            supportsHttps = supportsHttps or (SCHEME_HTTPS == scheme)

            if (supportsHttp && supportsHttps) {
                return true
            }
        }

        return false
    }
}