package net.openid.appauth.kotlin.library.browser

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import net.openid.appauth.kotlin.library.extension.toCustomTabUriBundle
import net.openid.appauth.kotlin.library.internal.Logger

class CustomTabManager(context: Context) {

    companion object {
        private const val CLIENT_WAIT_TIME = 1L
    }

    private val contextRef = WeakReference(context)
    private val client = AtomicReference<CustomTabsClient>()
    private val clientLatch = CountDownLatch(1)
    private var connection: CustomTabsServiceConnection? = null

    @Synchronized
    fun bind(browserPackage: String) {
        if (connection != null) return

        connection = object : CustomTabsServiceConnection() {
            override fun onServiceDisconnected(componentName: ComponentName) {
                Logger.debug("CustomTabsService is disconnected")
                setClient(null)
            }

            override fun onCustomTabsServiceConnected(componentName: ComponentName,
                                                      customTabsClient: CustomTabsClient) {
                Logger.debug("CustomTabsService is connected")
                customTabsClient.warmup(0)
                setClient(customTabsClient)
            }

            private fun setClient(client: CustomTabsClient?) {
                this@CustomTabManager.client.set(client)
                clientLatch.countDown()
            }
        }

        val context = contextRef.get()
        if (context == null || !CustomTabsClient.bindCustomTabsService(
                context,
                browserPackage,
                connection!!)) {
            Logger.info("Unable to bind custom tabs service")
            clientLatch.countDown()
        }
    }

    fun createTabBuilder(vararg possibleUris: Uri?): CustomTabsIntent.Builder {
        return CustomTabsIntent.Builder(createSession(null, *possibleUris))
    }

    @Synchronized
    fun dispose() {
        if (connection == null) {
            return
        }

        val context = contextRef.get()
        context?.unbindService(connection!!)

        client.set(null)
        Logger.debug("CustomTabsService is disconnected")
    }

    fun createSession(callbacks: CustomTabsCallback?, vararg possibleUris: Uri?): CustomTabsSession? {
        val client = getClient() ?: return null

        val session = client.newSession(callbacks)
        if (session == null) {
            Logger.warn("Failed to create custom tabs session through custom tabs client")
            return null
        }

        if (possibleUris.isNotEmpty()) {
            val additionalUris = possibleUris.toList().toCustomTabUriBundle(1)
            session.mayLaunchUrl(possibleUris.firstOrNull(), null, additionalUris)
        }

        return session
    }

    fun getClient(): CustomTabsClient? {
        try {
            clientLatch.await(CLIENT_WAIT_TIME, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Logger.info("Interrupted while waiting for browser connection")
            clientLatch.countDown()
        }

        return client.get()
    }
}