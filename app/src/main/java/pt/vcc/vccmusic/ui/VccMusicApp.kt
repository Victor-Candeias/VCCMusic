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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
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
import pt.vcc.vccmusic.ui.theme.appBackgroundBrush
import pt.vcc.vccmusic.ui.screen.RadioBrowserRepository
import pt.vcc.vccmusic.scanner.ScanProgress
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import pt.vcc.vccmusic.R
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
/** Monta a navegação principal, barras globais e os ViewModels da aplicação. */
fun VccMusicApp(
    modifier: Modifier = Modifier,
    musicRepository: MusicRepository,
    onPickRoot: () -> Unit = {},
    onReindex: () -> Unit = {},
    reindexing: Boolean = false,
    reindexProgress: ScanProgress = ScanProgress(0, 0, 0),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onExportDiagnosticLog: () -> Unit = {},
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
            /** Cria o ViewModel responsável pela biblioteca musical. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LibraryViewModel(musicRepository) as T
        },
    )
    val favoriteTracks by libraryViewModel.favoriteTracks.collectAsStateWithLifecycle(initialValue = emptyList())
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            /** Cria o ViewModel responsável pelas playlists. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlaylistViewModel(musicRepository) as T
        },
    )
    val favoritePlaylists by playlistViewModel.playlists
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val favoritePlaylistIds = favoritePlaylists.map { it.id }
    val favoritePlaylistTracks by remember(favoritePlaylistIds) {
        if (favoritePlaylistIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyMap<Long, List<pt.vcc.vccmusic.data.local.TrackEntity>>())
        } else {
            combine(
                favoritePlaylistIds.map { musicRepository.observePlaylistTracks(it) },
            ) { tracksByPlaylist ->
                favoritePlaylistIds.zip(tracksByPlaylist).toMap()
            }
        }
    }.collectAsStateWithLifecycle(initialValue = emptyMap())
    val coroutineScope = rememberCoroutineScope()
    val playbackViewModel: PlaybackViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            /** Cria o ViewModel que controla a reprodução multimédia. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlaybackViewModel(context) as T
        },
    )
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier.background(appBackgroundBrush(isDarkTheme)),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp,
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
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
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
                        favoritePlaylists = favoritePlaylists.filter { it.isFavorite },
                        favoritePlaylistTrackCounts = favoritePlaylistTracks
                            .filterKeys { id -> favoritePlaylists.any { it.id == id && it.isFavorite } }
                            .mapValues { it.value.size },
                        favoriteStations = favoriteStations.filter { it.isFavorite },
                        onFavoriteTrack = { track ->
                            playbackViewModel.playTracks(listOf(track), track.id)
                            navController.navigate(NavigationDestination.NowPlaying.route) {
                                launchSingleTop = true
                            }
                        },
                        onFavoritePlaylist = { playlist, shuffle ->
                            coroutineScope.launch {
                                val tracks = musicRepository.observePlaylistTracks(playlist.id).first()
                                if (tracks.isNotEmpty()) {
                                    playbackViewModel.playTracks(
                                        tracks,
                                        source = pt.vcc.vccmusic.playback.QueueSource.SELECTION,
                                        shuffle = shuffle,
                                    )
                                    navController.navigate(NavigationDestination.NowPlaying.route) {
                                        launchSingleTop = true
                                    }
                                }
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
                    onTrackPlayed = {
                        navController.navigate(NavigationDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onExportDiagnosticLog = onExportDiagnosticLog,
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
            if (reindexing) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.reindex_in_progress)) },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(
                                    R.string.reindex_progress,
                                    reindexProgress.folders,
                                    reindexProgress.tracks,
                                    reindexProgress.errors,
                                ),
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    },
                    confirmButton = {},
                )
        }
        }
    }
}
