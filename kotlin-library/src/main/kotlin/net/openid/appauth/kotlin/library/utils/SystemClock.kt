package net.openid.appauth.kotlin.library.utils

interface Clock {
    val currentTimeMillis: Long
}

object SystemClock : Clock {
    val INSTANCE: SystemClock = this
    override val currentTimeMillis: Long
        get() = System.currentTimeMillis()
}
