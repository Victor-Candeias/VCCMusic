package pt.vcc.vccmusic.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "radio_stations")
data class RadioStationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val tags: String,
    val favicon: String?,
    val isFavorite: Boolean = false,
)

@Dao
interface RadioStationDao {
    @Query("SELECT * FROM radio_stations ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RadioStationEntity>>

    @Query("SELECT * FROM radio_stations")
    suspend fun findAll(): List<RadioStationEntity>

    @Upsert
    suspend fun upsertAll(stations: List<RadioStationEntity>)

    @Query("UPDATE radio_stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun setFavorite(stationId: String, isFavorite: Boolean)
}

@Database(entities = [RadioStationEntity::class], version = 1, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioStationDao(): RadioStationDao
}
