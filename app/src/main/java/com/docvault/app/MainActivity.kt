package com.docvault.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.docvault.app.core.prefs.Prefs
import com.docvault.app.ui.nav.DocVaultNav
import com.docvault.app.ui.theme.DocVaultTheme

class MainActivity : ComponentActivity() {

    private val incoming = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming.value = intent?.data

        setContent {
            val prefs = remember { Prefs(applicationContext) }
            val theme by prefs.theme.collectAsState(initial = "system")
            DocVaultTheme(theme) {
                DocVaultNav(incoming.value) { incoming.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incoming.value = intent.data
    }
}