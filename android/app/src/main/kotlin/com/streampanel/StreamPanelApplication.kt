package com.streampanel

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.PrintWriter
import java.io.StringWriter

@HiltAndroidApp
class StreamPanelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val stack = StringWriter().also { writer ->
                error.printStackTrace(PrintWriter(writer))
            }.toString()
            getSharedPreferences("diagnostic_crash", MODE_PRIVATE)
                .edit()
                .putString("last_crash", stack)
                .commit()
            defaultHandler?.uncaughtException(thread, error)
        }
    }
}
