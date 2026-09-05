package pt.vcc.vccmusic.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pt.vcc.vccmusic.ui.screen.LibraryScreen
import pt.vcc.vccmusic.ui.screen.LibraryViewModel
import pt.vcc.vccmusic.ui.screen.MainMenuScreen
import pt.vcc.vccmusic.ui.screen.MusicMenuScreen
import pt.vcc.vccmusic.ui.screen.PlaylistScreen
import pt.vcc.vccmusic.ui.screen.PlaylistViewModel
import pt.vcc.vccmusic.ui.screen.PlaybackViewModel
import pt.vcc.vccmusic.ui.screen.LibraryStart
import pt.vcc.vccmusic.ui.screen.NowPlayingScreen
import pt.vcc.vccmusic.ui.screen.SettingsScreen
import pt.vcc.vccmusic.ui.screen.OnlineRadioScreen
import pt.vcc.vccmusic.ui.screen.RadioStation
import pt.vcc.vccmusic.ui.screen.defaultRadioStations
import pt.vcc.vccmusic.data.MusicRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.mutableStateListOf

@Composable
fun VccMusicApp(
    modifier: Modifier = Modifier,
    musicRepository: MusicRepository,
    onPickRoot: () -> Unit = {},
    onReindex: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val radioStations = remember {
        mutableStateListOf<RadioStation>().also { it.addAll(defaultRadioStations) }
    }
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(musicRepository) as T
        },
    )
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlaylistViewModel(musicRepository) as T
        },
    )
    val playbackViewModel: PlaybackViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlaybackViewModel(context) as T
        },
    )
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.filter { it.inMainNavigation }.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    val label = stringResource(destination.labelRes)

                    NavigationBarItem(
                        modifier = Modifier.testTag("bottom-${destination.route}"),
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(NavigationDestination.MainMenu.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    NavigationDestination.MainMenu -> Icons.Default.Home
                                    NavigationDestination.MusicMenu -> Icons.Default.Home
                                    NavigationDestination.Library -> Icons.Default.Home
                                    NavigationDestination.Playlists -> Icons.AutoMirrored.Filled.List
                                    NavigationDestination.Folders -> Icons.Default.Home
                                    NavigationDestination.NowPlaying -> Icons.Default.PlayArrow
                                    NavigationDestination.OnlineRadio -> Icons.Default.PlayArrow
                                    NavigationDestination.Settings -> Icons.Default.Home
                                },
                                contentDescription = label,
                            )
                        },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationDestination.MainMenu.route,
        ) {
            composable(NavigationDestination.MainMenu.route) {
                MainMenuScreen(
                    contentPadding = contentPadding,
                    onMusic = { navController.navigate(NavigationDestination.Library.route) },
                    onPlaylists = { navController.navigate(NavigationDestination.Playlists.route) },
                    onFolders = { navController.navigate(NavigationDestination.Folders.route) },
                    onNowPlaying = { navController.navigate(NavigationDestination.NowPlaying.route) },
                    onOnlineRadio = { navController.navigate(NavigationDestination.OnlineRadio.route) },
                    onSettings = { navController.navigate(NavigationDestination.Settings.route) },
                    onExit = onExit,
                )
            }
            composable(NavigationDestination.MusicMenu.route) {
                MusicMenuScreen(
                    contentPadding = contentPadding,
                    onTracks = { navController.navigate(NavigationDestination.Library.route) },
                    onFolders = { navController.navigate(NavigationDestination.Library.route) },
                    onPlaylists = { navController.navigate(NavigationDestination.Playlists.route) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(NavigationDestination.Library.route) {
                LibraryScreen(
                    libraryViewModel,
                    playbackViewModel,
                    contentPadding,
                    onPickRoot,
                    onReindex,
                    start = LibraryStart.ALL_TRACKS,
                )
            }
            composable(NavigationDestination.Folders.route) {
                LibraryScreen(
                    libraryViewModel,
                    playbackViewModel,
                    contentPadding,
                    onPickRoot,
                    onReindex,
                )
            }
            composable(NavigationDestination.Playlists.route) {
                val activeRoot by musicRepository.observeActiveRoot()
                    .collectAsStateWithLifecycle(initialValue = null)
                PlaylistScreen(
                    playlistViewModel,
                    activeRoot?.id,
                    contentPadding,
                    playbackViewModel,
                )
            }
            composable(NavigationDestination.NowPlaying.route) {
                NowPlayingScreen(playbackViewModel, contentPadding)
            }
            composable(NavigationDestination.Settings.route) {
                SettingsScreen(
                    contentPadding = contentPadding,
                    onPickRoot = onPickRoot,
                    onReindex = onReindex,
                    radioStations = radioStations,
                    onRadioStationEnabledChanged = { name, enabled ->
                        val index = radioStations.indexOfFirst { it.name == name }
                        if (index >= 0) radioStations[index] = radioStations[index].copy(enabled = enabled)
                    },
                )
            }
            composable(NavigationDestination.OnlineRadio.route) {
                OnlineRadioScreen(playbackViewModel, contentPadding)
            }
        }
    }
}
