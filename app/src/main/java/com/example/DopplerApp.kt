package com.example

import android.app.Application
import com.example.crash.CrashReporter

/** App entry point: only installs the crash reporter, so any crash can be shown on the next start. */
class DopplerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
