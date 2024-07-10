package net.openid.appauth.kotlin.library.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import net.openid.appauth.kotlin.library.internal.Logger

class RedirectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AuthorizationManagementActivity
            .createResponseHandlingIntent(context = this, responseUri = intent.data)
            .let(::startActivity)
        finish()
    }
}