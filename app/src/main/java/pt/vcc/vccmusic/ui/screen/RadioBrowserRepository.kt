package pt.vcc.vccmusic.ui.screen

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import pt.vcc.vccmusic.data.local.RadioDatabase
import pt.vcc.vccmusic.data.local.RadioStationEntity

const val DEFAULT_RADIO_BROWSER_API_URL =
    "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/PT?hidebroken=true&limit=100"

private const val MAX_FAVICON_SIZE_BYTES = 2 * 1024 * 1024

private val RADIO_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    /** Adiciona o caminho local opcional do favicon. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE radio_stations ADD COLUMN faviconLocalPath TEXT",
        )
    }
}

private val RADIO_DATABASE_MIGRATION_2_3 = object : Migration(2, 3) {
    /** Limpa estações antigas após a alteração do modelo remoto. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM radio_stations")
    }
}

data class RadioBrowserStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tags: String,
    val favicon: String?,
    val faviconLocalPath: String? = null,
    val isFavorite: Boolean = false,
)

class RadioBrowserRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        RadioDatabase::class.java,
        "radio.db",
    ).addMigrations(RADIO_DATABASE_MIGRATION_1_2, RADIO_DATABASE_MIGRATION_2_3).build()
    private val dao = database.radioStationDao()

    /** Observa as estações configuradas e converte-as para o modelo de domínio. */
    fun observePortugueseStations(): Flow<List<RadioBrowserStation>> =
        dao.observeAll().map { stations -> stations.map { it.toDomain() } }

    /** Atualiza apenas as estações remotas que já estavam configuradas. */
    suspend fun refreshPortugueseStations() {
        val configuredIds = dao.findAll().map { it.id }.toSet()
        val stations = fetchAvailablePortugueseStations()
            .filter { it.id in configuredIds }
        saveStations(stations)
    }

    /** Obtém a lista atual de estações portuguesas no Radio Browser. */
    suspend fun fetchAvailablePortugueseStations(): List<RadioBrowserStation> {
        return fetchPortugueseStations(DEFAULT_RADIO_BROWSER_API_URL)
    }

    /** Substitui atomicamente a configuração local de estações. */
    suspend fun replaceConfiguredStations(stations: List<RadioBrowserStation>) {
        database.withTransaction {
            dao.deleteAll()
            saveStations(stations)
        }
    }

    /** Guarda estações preservando favoritos e ícones ainda válidos. */
    private suspend fun saveStations(stations: List<RadioBrowserStation>) {
        val existingStations = dao.findAll().associateBy { it.id }
        dao.upsertAll(
            stations.map { station ->
                val existing = existingStations[station.id]
                station.toEntity(
                    isFavorite = existing?.isFavorite == true,
                    faviconLocalPath = existing?.faviconLocalPath
                        ?.takeIf { existing.favicon == station.favicon },
                )
            },
        )
    }

    /** Atualiza o estado de favorito de uma estação configurada. */
    suspend fun setFavorite(stationId: String, isFavorite: Boolean) {
        dao.setFavorite(stationId, isFavorite)
    }

    /** Descarrega e armazena localmente o favicon, respeitando o limite de tamanho. */
    suspend fun cacheFavicon(station: RadioBrowserStation): String? =
        withContext(Dispatchers.IO) {
            station.favicon ?: return@withContext null

            val iconDirectory = File(appContext.filesDir, "radio-favicons").apply { mkdirs() }
            val iconFile = File(iconDirectory, "${station.id.toSha256()}.img")
            if (iconFile.exists() && iconFile.length() > 0) {
                dao.setFaviconLocalPath(station.id, iconFile.absolutePath)
                return@withContext iconFile.absolutePath
            }

            val connection = URL(station.favicon).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5_000
                connection.readTimeout = 10_000
                connection.setRequestProperty("User-Agent", "VCCMusic/1.0 Android")
                if (connection.responseCode !in 200..299) {
                    return@withContext null
                }
                val bytes = connection.inputStream.use { input ->
                    input.readBytes().also {
                        require(it.size <= MAX_FAVICON_SIZE_BYTES) {
                            "O ícone da rádio excede o limite permitido."
                        }
                    }
                }
                val temporaryFile = File(iconDirectory, "${iconFile.name}.tmp")
                temporaryFile.writeBytes(bytes)
                if (!temporaryFile.renameTo(iconFile)) {
                    temporaryFile.delete()
                    error("Não foi possível guardar o ícone da rádio.")
                }
                dao.setFaviconLocalPath(station.id, iconFile.absolutePath)
                iconFile.absolutePath
            } finally {
                connection.disconnect()
            }
        }

    /** Consulta o endpoint Radio Browser e converte o JSON em estações. */
    private suspend fun fetchPortugueseStations(apiUrl: String): List<RadioBrowserStation> =
        withContext(Dispatchers.IO) {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "VCCMusic/1.0 Android")
            if (connection.responseCode !in 200..299) {
                error("Radio Browser HTTP ${connection.responseCode}")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val stations = JSONArray(json)
            buildList {
                for (index in 0 until stations.length()) {
                    val station = stations.getJSONObject(index)
                    val stream = station.optString("url_resolved").ifBlank {
                        station.optString("url")
                    }
                    if (stream.isNotBlank()) {
                        add(
                            RadioBrowserStation(
                                id = station.optString("stationuuid", stream),
                                name = station.optString("name", "Rádio sem nome")
                                    .replace(
                                        Regex("\\s*\\([^)]*(Portugal|PT)[^)]*\\)\\s*$", RegexOption.IGNORE_CASE),
                                        "",
                                    )
                                    .trim(),
                                streamUrl = stream,
                                tags = station.optString("tags"),
                                favicon = station.optString("favicon").takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Converte uma estação de domínio em entidade persistível. */
    private fun RadioBrowserStation.toEntity(
        isFavorite: Boolean,
        faviconLocalPath: String?,
    ) = RadioStationEntity(
        id = id,
        name = name,
        streamUrl = streamUrl,
        tags = tags,
        favicon = favicon,
        faviconLocalPath = faviconLocalPath,
        isFavorite = isFavorite,
    )

    /** Converte uma entidade persistida no modelo usado pela interface. */
    private fun RadioStationEntity.toDomain() = RadioBrowserStation(
        id = id,
        name = name,
        streamUrl = streamUrl,
        tags = tags,
        favicon = favicon,
        faviconLocalPath = faviconLocalPath,
        isFavorite = isFavorite,
    )

    /** Gera um nome hexadecimal estável para cache local de favicons. */
    private fun String.toSha256(): String =
        MessageDigest.getInstance("SHA-256").digest(toByteArray()).joinToString("") {
            "%02x".format(it)
        }
}
