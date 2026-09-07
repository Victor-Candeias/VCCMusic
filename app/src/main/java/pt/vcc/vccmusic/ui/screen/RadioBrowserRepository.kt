package pt.vcc.vccmusic.ui.screen

import android.content.Context
import androidx.room.Room
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import pt.vcc.vccmusic.data.local.RadioDatabase
import pt.vcc.vccmusic.data.local.RadioStationEntity

const val DEFAULT_RADIO_BROWSER_API_URL =
    "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/PT?hidebroken=true&limit=100"

private val Context.radioPreferences by preferencesDataStore(name = "radio_preferences")
private val radioApiUrlKey = stringPreferencesKey("api_url")

data class RadioBrowserStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tags: String,
    val favicon: String?,
    val isFavorite: Boolean = false,
)

class RadioBrowserRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        RadioDatabase::class.java,
        "radio.db",
    ).build()
    private val dao = database.radioStationDao()

    val apiUrl: Flow<String> = appContext.radioPreferences.data.map { preferences ->
        preferences[radioApiUrlKey] ?: DEFAULT_RADIO_BROWSER_API_URL
    }

    fun observePortugueseStations(): Flow<List<RadioBrowserStation>> =
        dao.observeAll().map { stations -> stations.map { it.toDomain() } }

    suspend fun refreshPortugueseStations() {
        val url = apiUrl.first()
        val stations = fetchPortugueseStations(url)
        saveStations(stations)
        saveApiUrl(url)
    }

    suspend fun validateAndSaveApiUrl(url: String) {
        val normalizedUrl = url.trim()
        require(normalizedUrl.startsWith("https://") || normalizedUrl.startsWith("http://")) {
            "O URL da API deve começar por http:// ou https://."
        }
        val stations = fetchPortugueseStations(normalizedUrl)
        check(stations.isNotEmpty()) { "A API não devolveu nenhuma rádio." }
        saveStations(stations)
        saveApiUrl(normalizedUrl)
    }

    private suspend fun saveStations(stations: List<RadioBrowserStation>) {
        val favorites = dao.findAll().filter { it.isFavorite }.map { it.id }.toSet()
        dao.upsertAll(stations.map { it.toEntity(it.id in favorites) })
    }

    private suspend fun saveApiUrl(url: String) {
        appContext.radioPreferences.edit { preferences ->
            preferences[radioApiUrlKey] = url
        }
    }

    suspend fun setFavorite(stationId: String, isFavorite: Boolean) {
        dao.setFavorite(stationId, isFavorite)
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

    private fun RadioBrowserStation.toEntity(isFavorite: Boolean) = RadioStationEntity(
        id = id,
        name = name,
        streamUrl = streamUrl,
        tags = tags,
        favicon = favicon,
        isFavorite = isFavorite,
    )

    private fun RadioStationEntity.toDomain() = RadioBrowserStation(
        id = id,
        name = name,
        streamUrl = streamUrl,
        tags = tags,
        favicon = favicon,
        isFavorite = isFavorite,
    )
}
