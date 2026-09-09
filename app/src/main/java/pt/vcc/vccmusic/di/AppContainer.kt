package pt.vcc.vccmusic.di

import android.content.Context
import androidx.room.Room
import pt.vcc.vccmusic.data.MusicRepository
import pt.vcc.vccmusic.data.RoomMusicRepository
import pt.vcc.vccmusic.data.local.MusicDatabase
import pt.vcc.vccmusic.data.local.MUSIC_DATABASE_MIGRATION_1_2
import pt.vcc.vccmusic.data.local.MUSIC_DATABASE_MIGRATION_2_3
import pt.vcc.vccmusic.data.saf.AndroidSafRootRepository
import pt.vcc.vccmusic.data.saf.SafRootRepository
import pt.vcc.vccmusic.scanner.MusicScanner
import pt.vcc.vccmusic.scanner.SafMusicScanner

interface AppContainer {
    val musicRepository: MusicRepository
    val safRootRepository: SafRootRepository
    val musicScanner: MusicScanner
}

class DefaultAppContainer(
    applicationContext: Context,
) : AppContainer {
    private val appContext = applicationContext.applicationContext

    private val database: MusicDatabase by lazy {
        Room.databaseBuilder(appContext, MusicDatabase::class.java, "music.db")
            .addMigrations(MUSIC_DATABASE_MIGRATION_1_2, MUSIC_DATABASE_MIGRATION_2_3)
            .build()
    }

    override val musicRepository: MusicRepository by lazy {
        RoomMusicRepository(database)
    }

    override val safRootRepository: SafRootRepository by lazy {
        AndroidSafRootRepository(appContext)
    }

    override val musicScanner: MusicScanner by lazy {
        SafMusicScanner(appContext, database)
    }
}
