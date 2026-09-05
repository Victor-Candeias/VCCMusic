package pt.vcc.vccmusic.ui

import androidx.annotation.StringRes
import pt.vcc.vccmusic.R

enum class NavigationDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val inMainNavigation: Boolean = true,
) {
    MainMenu("main-menu", R.string.main_menu),
    MusicMenu("music-menu", R.string.music, false),
    Library("library", R.string.library),
    Playlists("playlists", R.string.playlists),
    NowPlaying("now-playing", R.string.now_playing),
    OnlineRadio("online-radio", R.string.online_radio),
    Settings("settings", R.string.settings),
}
