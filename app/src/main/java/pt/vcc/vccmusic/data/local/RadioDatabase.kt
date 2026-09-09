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
    val faviconLocalPath: String?,
    val isFavorite: Boolean = false,
)

@Dao
interface RadioStationDao {
    /** Observa todas as estações ordenadas pelo nome. */
    @Query("SELECT * FROM radio_stations ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RadioStationEntity>>

    /** Obtém todas as estações configuradas para operações de sincronização. */
    @Query("SELECT * FROM radio_stations")
    suspend fun findAll(): List<RadioStationEntity>

    /** Insere ou atualiza várias estações, preservando os identificadores. */
    @Upsert
    suspend fun upsertAll(stations: List<RadioStationEntity>)

    /** Remove todas as estações configuradas. */
    @Query("DELETE FROM radio_stations")
    suspend fun deleteAll()

    /** Atualiza o estado de favorito de uma estação. */
    @Query("UPDATE radio_stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun setFavorite(stationId: String, isFavorite: Boolean)

    /** Guarda o caminho local da imagem de uma estação. */
    @Query("UPDATE radio_stations SET faviconLocalPath = :path WHERE id = :stationId")
    suspend fun setFaviconLocalPath(stationId: String, path: String)
}

@Database(entities = [RadioStationEntity::class], version = 3, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {
    /** Fornece o DAO das estações de rádio. */
    abstract fun radioStationDao(): RadioStationDao
}
