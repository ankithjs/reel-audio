package com.ankith.reelaudio.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ankith.reelaudio.library

/**
 * Lets the user sign in to Instagram in a normal web page, then saves the session cookies
 * (on this phone only) so the extractor can fetch reels that require a login.
 */
class LoginActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        setContent {
            ReelTheme {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("Sign in to Instagram") },
                        actions = { TextButton(onClick = { saveAndFinish() }) { Text("Done") } },
                    )
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        // Instagram lands on the feed once login succeeds.
                                        if (hasSession() && url != null && "/accounts/login" !in url) saveAndFinish()
                                    }
                                }
                                loadUrl("https://www.instagram.com/accounts/login/")
                            }
                        },
                    )
                }
            }
        }
    }

    private var saved = false

    private fun hasSession(): Boolean =
        CookieManager.getInstance().getCookie("https://www.instagram.com")?.contains("sessionid=") == true

    private fun saveAndFinish() {
        if (saved) return
        val raw = CookieManager.getInstance().getCookie("https://www.instagram.com")
        if (raw.isNullOrBlank() || "sessionid=" !in raw) {
            Toast.makeText(this, "Not signed in yet.", Toast.LENGTH_SHORT).show()
            return
        }
        CookieManager.getInstance().flush()
        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val lines = buildString {
            appendLine("# Netscape HTTP Cookie File")
            raw.split(";").map { it.trim() }.filter { "=" in it }.forEach { pair ->
                val name = pair.substringBefore('=')
                val value = pair.substringAfter('=')
                appendLine(".instagram.com\tTRUE\t/\tTRUE\t$expiry\t$name\t$value")
            }
        }
        library.downloader.cookiesFile.writeText(lines)
        saved = true
        Toast.makeText(this, "Signed in. Retry any failed reels.", Toast.LENGTH_LONG).show()
        finish()
    }
}
