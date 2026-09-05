package pt.vcc.vccmusic.ui.screen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class RadioBrowserStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val tags: String,
    val favicon: String?,
)

class RadioBrowserRepository {
    suspend fun loadPortugueseStations(): List<RadioBrowserStation> = withContext(Dispatchers.IO) {
        val connection = URL(
            "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/PT?hidebroken=true&limit=40",
        ).openConnection() as HttpURLConnection
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
                                name = station.optString("name", "Rádio sem nome"),
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
}
