package net.openid.appauth.kotlin.library

import android.content.Context
import androidx.startup.Initializer
import net.openid.appauth.kotlin.library.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent

object KoinLibrary {
    internal val koinApplication by lazy { KoinApplication.init() }
    fun init(context: Context) {
        koinApplication.apply {
            androidContext(context)
            modules(networkModule)
        }
    }
}

interface LibraryComponent : KoinComponent {
    override fun getKoin(): Koin {
        return KoinLibrary.koinApplication.koin
    }
}

class KoinInitializer : Initializer<Any> {
    override fun create(context: Context): Any {
        KoinLibrary.init(context)
        return ""
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
