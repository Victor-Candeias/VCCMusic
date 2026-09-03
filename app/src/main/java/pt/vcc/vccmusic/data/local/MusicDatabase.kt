package pt.vcc.vccmusic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MusicRootEntity::class,
        MusicFolderEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicRootDao(): MusicRootDao
    abstract fun musicFolderDao(): MusicFolderDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
}
