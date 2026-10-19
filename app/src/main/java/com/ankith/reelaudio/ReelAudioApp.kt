package com.ankith.reelaudio

import android.app.Application
import android.content.Context
import com.ankith.reelaudio.data.Library
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReelAudioApp : Application() {
    lateinit var library: Library
        private set

    override fun onCreate() {
        super.onCreate()
        library = Library(this)
        autoUpdateExtractor()
    }

    /** Instagram changes often; refresh the extractor every few days in the background. */
    private fun autoUpdateExtractor() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val last = prefs.getLong("extractor_updated", 0)
        if (System.currentTimeMillis() - last < 3L * 24 * 3600 * 1000) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { library.downloader.updateExtractor() }
                .onSuccess { prefs.edit().putLong("extractor_updated", System.currentTimeMillis()).apply() }
        }
    }
}

val Context.library: Library get() = (applicationContext as ReelAudioApp).library
