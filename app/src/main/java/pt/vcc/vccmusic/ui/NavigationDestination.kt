package pt.vcc.vccmusic.ui

import androidx.annotation.StringRes
import pt.vcc.vccmusic.R

enum class NavigationDestination(
    val route: String,
    @StringRes val labelRes: Int,
) {
    Library("library", R.string.library),
    Playlists("playlists", R.string.playlists),
    NowPlaying("now-playing", R.string.now_playing),
}

