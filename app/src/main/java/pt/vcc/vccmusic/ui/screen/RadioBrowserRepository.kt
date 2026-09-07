package pt.vcc.vccmusic.ui.screen

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

private val Context.radioPreferences by preferencesDataStore(name = "radio_preferences")
private val radioApiUrlKey = stringPreferencesKey("api_url")
private const val MAX_FAVICON_SIZE_BYTES = 2 * 1024 * 1024

private val RADIO_DATABASE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE radio_stations ADD COLUMN faviconLocalPath TEXT",
        )
    }
}

private val RADIO_DATABASE_MIGRATION_2_3 = object : Migration(2, 3) {
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

    val apiUrl: Flow<String> = appContext.radioPreferences.data.map { preferences ->
        preferences[radioApiUrlKey] ?: DEFAULT_RADIO_BROWSER_API_URL
    }

    fun observePortugueseStations(): Flow<List<RadioBrowserStation>> =
        dao.observeAll().map { stations -> stations.map { it.toDomain() } }

    suspend fun refreshPortugueseStations() {
        val configuredIds = dao.findAll().map { it.id }.toSet()
        val stations = fetchAvailablePortugueseStations()
            .filter { it.id in configuredIds }
        saveStations(stations)
    }

    suspend fun validateAndSaveApiUrl(url: String) {
        val normalizedUrl = url.trim()
        require(normalizedUrl.startsWith("https://") || normalizedUrl.startsWith("http://")) {
            "O URL da API deve começar por http:// ou https://."
        }
        val stations = fetchPortugueseStations(normalizedUrl)
        check(stations.isNotEmpty()) { "A API não devolveu nenhuma rádio." }
        saveApiUrl(normalizedUrl)
    }

    suspend fun fetchAvailablePortugueseStations(): List<RadioBrowserStation> {
        val url = apiUrl.first()
        return fetchPortugueseStations(url)
    }

    suspend fun replaceConfiguredStations(stations: List<RadioBrowserStation>) {
        database.withTransaction {
            dao.deleteAll()
            saveStations(stations)
        }
    }

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

    private suspend fun saveApiUrl(url: String) {
        appContext.radioPreferences.edit { preferences ->
            preferences[radioApiUrlKey] = url
        }
    }

    suspend fun setFavorite(stationId: String, isFavorite: Boolean) {
        dao.setFavorite(stationId, isFavorite)
    }

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

    private fun RadioStationEntity.toDomain() = RadioBrowserStation(
        id = id,
        name = name,
        streamUrl = streamUrl,
        tags = tags,
        favicon = favicon,
        faviconLocalPath = faviconLocalPath,
        isFavorite = isFavorite,
    )

    private fun String.toSha256(): String =
        MessageDigest.getInstance("SHA-256").digest(toByteArray()).joinToString("") {
            "%02x".format(it)
        }
}
