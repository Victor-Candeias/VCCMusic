package pt.vcc.vccmusic.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

@Database(
    entities = [
        MusicRootEntity::class,
        MusicFolderEntity::class,
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicRootDao(): MusicRootDao
    abstract fun musicFolderDao(): MusicFolderDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
}

val MUSIC_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MUSIC_DATABASE_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE playlists ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}
