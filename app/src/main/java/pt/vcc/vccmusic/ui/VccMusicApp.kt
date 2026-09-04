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
import pt.vcc.vccmusic.ui.screen.PlaceholderScreen
import pt.vcc.vccmusic.ui.screen.LibraryScreen
import pt.vcc.vccmusic.ui.screen.LibraryViewModel
import pt.vcc.vccmusic.ui.screen.PlaylistScreen
import pt.vcc.vccmusic.ui.screen.PlaylistViewModel
import pt.vcc.vccmusic.data.MusicRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@Composable
fun VccMusicApp(
    modifier: Modifier = Modifier,
    musicRepository: MusicRepository,
    onPickRoot: () -> Unit = {},
    onReindex: () -> Unit = {},
) {
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
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    val label = stringResource(destination.labelRes)

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(NavigationDestination.Library.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    NavigationDestination.Library -> Icons.Default.Home
                                    NavigationDestination.Playlists -> Icons.AutoMirrored.Filled.List
                                    NavigationDestination.NowPlaying -> Icons.Default.PlayArrow
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
            startDestination = NavigationDestination.Library.route,
        ) {
            NavigationDestination.entries.forEach { destination ->
                composable(destination.route) {
                    if (destination == NavigationDestination.Library) {
                        LibraryScreen(libraryViewModel, contentPadding, onPickRoot, onReindex)
                    } else if (destination == NavigationDestination.Playlists) {
                        val activeRoot by musicRepository.observeActiveRoot()
                            .collectAsStateWithLifecycle(initialValue = null)
                        PlaylistScreen(
                            playlistViewModel,
                            activeRoot?.id,
                            contentPadding,
                        )
                    } else {
                        PlaceholderScreen(
                            titleRes = destination.labelRes,
                            contentPadding = contentPadding,
                            testTag = "screen-${destination.route}",
                        )
                    }
                }
            }
        }
    }
}
