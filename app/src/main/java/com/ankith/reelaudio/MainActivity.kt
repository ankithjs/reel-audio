package com.ankith.reelaudio

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.ankith.reelaudio.ui.AppRoot
import com.ankith.reelaudio.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    /** Text shared from Instagram, waiting for the user to pick a group and confirm. */
    private val pendingShare = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            AppRoot(
                vm = vm,
                pendingShare = pendingShare.value,
                onShareHandled = { pendingShare.value = null },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        vm.visible.value = true
    }

    override fun onStop() {
        vm.visible.value = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val type = intent.type.orEmpty()
        if (type.startsWith("text/")) {
            val text = listOfNotNull(
                intent.getStringExtra(Intent.EXTRA_TEXT),
                intent.getStringExtra(Intent.EXTRA_SUBJECT),
            ).joinToString("\n")
            if (text.isNotBlank()) pendingShare.value = text
        } else {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            uri?.let { vm.importFile(it) }
        }
    }
}
