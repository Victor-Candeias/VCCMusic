package pt.vcc.vccmusic.ui.screen

data class RadioStation(
    val name: String,
    val streamUrl: String,
    val enabled: Boolean = true,
)

val defaultRadioStations = listOf(
    RadioStation("Radio Paradise", "https://stream.radioparadise.com/aac-320"),
    RadioStation("SomaFM Groove Salad", "https://ice1.somafm.com/groovesalad-128-mp3"),
)
