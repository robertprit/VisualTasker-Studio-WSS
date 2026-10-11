package com.visualtasker.wss.emscript.runtime

import android.app.Application

class VisualTaskerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeProcessHost.initialize(this)
    }
}
