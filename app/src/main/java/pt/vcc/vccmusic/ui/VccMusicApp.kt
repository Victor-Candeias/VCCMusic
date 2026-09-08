package pt.vcc.vccmusic.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import pt.vcc.vccmusic.ui.screen.RadioBrowserRepository
import pt.vcc.vccmusic.data.MusicRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Composable
fun VccMusicApp(
    modifier: Modifier = Modifier,
    musicRepository: MusicRepository,
    onPickRoot: () -> Unit = {},
    onReindex: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val radioRepository = remember { RadioBrowserRepository(context) }
    val favoriteStations by radioRepository.observePortugueseStations()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var configureOnlineRadios by remember { mutableStateOf(false) }
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(musicRepository) as T
        },
    )
    val favoriteTracks by libraryViewModel.favoriteTracks.collectAsStateWithLifecycle(initialValue = emptyList())
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
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                NavigationDestination.entries.filter { it.inMainNavigation }.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true || (
                        destination == NavigationDestination.OnlineRadio &&
                            currentDestination?.hierarchy?.any {
                                it.route == NavigationDestination.NowPlaying.route
                            } == true &&
                            playbackState.mediaId?.startsWith("radio:") == true
                        )
                    val label = stringResource(destination.labelRes)

                    NavigationBarItem(
                        modifier = Modifier.weight(1f).testTag("bottom-${destination.route}"),
                        selected = selected,
                        onClick = {
                            if (destination == NavigationDestination.MainMenu) {
                                if (!navController.popBackStack(NavigationDestination.MainMenu.route, false)) {
                                    navController.navigate(NavigationDestination.MainMenu.route)
                                }
                            } else if (!navController.popBackStack(destination.route, false)) {
                                navController.navigate(destination.route) {
                                    popUpTo(NavigationDestination.MainMenu.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    NavigationDestination.MainMenu -> Icons.Default.Home
                                    NavigationDestination.MusicMenu -> Icons.Default.MusicNote
                                    NavigationDestination.Library -> Icons.Default.LibraryMusic
                                    NavigationDestination.Playlists -> Icons.AutoMirrored.Filled.List
                                    NavigationDestination.Folders -> Icons.Default.Folder
                                    NavigationDestination.NowPlaying -> Icons.Default.PlayArrow
                                    NavigationDestination.OnlineRadio -> Icons.Default.Radio
                                    NavigationDestination.Settings -> Icons.Default.Settings
                                },
                                contentDescription = label,
                            )
                        },
                        label = null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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
                    onOnlineRadio = { navController.navigate(NavigationDestination.OnlineRadio.route) },
                    favoriteTracks = favoriteTracks,
                    favoriteStations = favoriteStations.filter { it.isFavorite },
                    onFavoriteTrack = { track ->
                        playbackViewModel.playTracks(listOf(track), track.id)
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                    onFavoriteRadio = { station ->
                        playbackViewModel.playRadio(
                            station.name,
                            station.streamUrl,
                            station.faviconLocalPath,
                        )
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onTrackPlayed = {
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(NavigationDestination.Folders.route) {
                LibraryScreen(
                    libraryViewModel,
                    playbackViewModel,
                    contentPadding,
                    onPickRoot,
                    onReindex,
                    onTrackPlayed = {
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
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
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onConfigureOnlineRadios = {
                        configureOnlineRadios = true
                        navController.navigate(NavigationDestination.OnlineRadio.route)
                    },
                )
            }
            composable(NavigationDestination.OnlineRadio.route) {
                OnlineRadioScreen(
                    playbackViewModel = playbackViewModel,
                    contentPadding = contentPadding,
                    repository = radioRepository,
                    forceConfiguration = configureOnlineRadios,
                    onConfigurationFinished = { configureOnlineRadios = false },
                    onStationPlayed = {
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
