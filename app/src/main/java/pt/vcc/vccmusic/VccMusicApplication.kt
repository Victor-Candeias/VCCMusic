package pt.vcc.vccmusic

import android.app.Application
import pt.vcc.vccmusic.di.AppContainer
import pt.vcc.vccmusic.di.DefaultAppContainer

class VccMusicApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(applicationContext) }
}

