package pt.vcc.vccmusic

import android.app.Application
import pt.vcc.vccmusic.di.AppContainer
import pt.vcc.vccmusic.di.DefaultAppContainer

class VccMusicApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            pt.vcc.vccmusic.diagnostics.DiagnosticLogger.log(
                applicationContext,
                "Crash",
                "Exceção não capturada na thread=${thread.name}",
                error,
            )
            previousHandler?.uncaughtException(thread, error)
        }
    }
}
