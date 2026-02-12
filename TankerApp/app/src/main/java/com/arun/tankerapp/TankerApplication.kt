package com.arun.tankerapp

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.arun.tankerapp.core.logging.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TankerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant Timber logging tree
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(FileLoggingTree(this))

        // Set up global exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught Exception on thread: ${thread.name}")
            
            // Show a toast message on the UI thread before crashing
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Something went wrong. Check logs.", Toast.LENGTH_LONG).show()
            }
            
            // Let the default handler take over after a short delay to allow toast/logs
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
