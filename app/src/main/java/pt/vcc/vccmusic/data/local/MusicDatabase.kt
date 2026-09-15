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
        PodcastFavoriteEntity::class,
        PodcastProgressEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    /** Fornece o DAO das raízes de música. */
    abstract fun musicRootDao(): MusicRootDao
    /** Fornece o DAO das pastas de música. */
    abstract fun musicFolderDao(): MusicFolderDao
    /** Fornece o DAO das faixas. */
    abstract fun trackDao(): TrackDao
    /** Fornece o DAO das playlists. */
    abstract fun playlistDao(): PlaylistDao
    /** Fornece o DAO dos favoritos e progresso de podcasts. */
    abstract fun podcastDao(): PodcastDao
}

val MUSIC_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    /** Adiciona a coluna de favoritos das faixas durante a migração. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MUSIC_DATABASE_MIGRATION_2_3 = object : Migration(2, 3) {
    /** Adiciona a coluna de favoritos das playlists durante a migração. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE playlists ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MUSIC_DATABASE_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS podcast_favorites (
                    feedId INTEGER NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    author TEXT,
                    description TEXT,
                    image TEXT
                )
            """.trimIndent(),
        )
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS podcast_progress (
                    episodeId INTEGER NOT NULL PRIMARY KEY,
                    feedId INTEGER,
                    title TEXT NOT NULL,
                    positionMs INTEGER NOT NULL,
                    durationMs INTEGER,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent(),
        )
    }
}
