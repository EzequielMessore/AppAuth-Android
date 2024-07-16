package net.openid.appauth.kotlin.library.internal

import android.util.Log

class Logger private constructor(private val mLog: LogWrapper) {

    private val mLogLevel: Int

    init {
        var level = Log.ASSERT
        while (level >= Log.VERBOSE && mLog.isLoggable(LOG_TAG, level)) {
            level--
        }
        mLogLevel = level + 1
    }

    fun log(level: Int, tr: Throwable?, message: String?, vararg messageParams: Any?) {
        if (mLogLevel > level) {
            return
        }
        var formattedMessage = if (messageParams.isEmpty()) {
            message
        } else {
            String.format(message!!, *messageParams)
        }
        if (tr != null) {
            formattedMessage += "\n" + mLog.getStackTraceString(tr)
        }
        mLog.println(level, LOG_TAG, formattedMessage)
    }

    interface LogWrapper {
        fun println(level: Int, tag: String?, message: String?)
        fun isLoggable(tag: String?, level: Int): Boolean
        fun getStackTraceString(tr: Throwable?): String
    }

    private class AndroidLogWrapper private constructor() : LogWrapper {
        override fun println(level: Int, tag: String?, message: String?) {
            Log.println(level, tag, message!!)
        }

        override fun isLoggable(tag: String?, level: Int): Boolean {
            return Log.isLoggable(tag, level)
        }

        override fun getStackTraceString(tr: Throwable?): String {
            return Log.getStackTraceString(tr)
        }

        companion object {
            val INSTANCE = AndroidLogWrapper()
        }
    }

    companion object {
        private const val LOG_TAG = "AppAuth-Logger"
        private var sInstance: Logger? = null

        @get:Synchronized
        @set:Synchronized
        var instance: Logger?
            get() {
                if (sInstance == null) {
                    sInstance = Logger(AndroidLogWrapper.INSTANCE)
                }
                return sInstance
            }
            set(value) {
                sInstance = value
            }

        fun verbose(message: String?, vararg messageParams: Any?) {
            instance?.log(Log.VERBOSE, null, message, *messageParams)
        }

        fun verboseWithStack(tr: Throwable?, message: String?, vararg messageParams: Any?) {
            instance?.log(Log.VERBOSE, tr, message, *messageParams)
        }

        fun debug(message: String?, vararg messageParams: Any?) {
            instance?.log(Log.DEBUG, null, message, *messageParams)
        }

        fun debugWithStack(tr: Throwable?, message: String?, vararg messageParams: Any?) {
            instance?.log(Log.DEBUG, tr, message, *messageParams)
        }

        fun info(message: String?, vararg messageParams: Any?) {
            instance?.log(Log.INFO, null, message, *messageParams)
        }

        fun infoWithStack(tr: Throwable?, message: String?, vararg messageParams: Any?) {
            instance?.log(Log.INFO, tr, message, *messageParams)
        }

        fun warn(message: String?, vararg messageParams: Any?) {
            instance?.log(Log.WARN, null, message, *messageParams)
        }

        fun warnWithStack(tr: Throwable?, message: String?, vararg messageParams: Any?) {
            instance?.log(Log.WARN, tr, message, *messageParams)
        }

        fun error(message: String?, vararg messageParams: Any?) {
            instance?.log(Log.ERROR, null, message, *messageParams)
        }

        fun errorWithStack(tr: Throwable?, message: String?, vararg messageParams: Any?) {
            instance?.log(Log.ERROR, tr, message, *messageParams)
        }
    }
}