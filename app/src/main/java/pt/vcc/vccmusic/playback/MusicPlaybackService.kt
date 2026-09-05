package pt.vcc.vccmusic.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.VccMusicApplication
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.TrackEntity

class MusicPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var libraryCallback: LibraryCallback

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.stop()
                }
            },
        )
        libraryCallback = LibraryCallback()
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            libraryCallback,
        ).build()
        loadActiveLibrary()
    }

    private fun loadActiveLibrary() {
        serviceScope.launch(Dispatchers.IO) {
            val container = (application as VccMusicApplication).container
            val root = container.musicRepository.activeRoot() ?: return@launch
            val tracks = container.musicRepository.observeAllTracks(root.id)
            tracks.collect { entities ->
                val items = QueueBuilder.build(
                    QueueRequest(QueueSource.ALL_TRACKS, entities),
                ).map(::toMediaItem)
                launch(Dispatchers.Main) {
                    if (items.isEmpty()) {
                        player.clearMediaItems()
                    } else {
                        val index = player.currentMediaItemIndex
                            .coerceIn(0, items.lastIndex)
                        player.setMediaItems(items, index, player.currentPosition)
                        player.prepare()
                    }
                }
            }
        }
    }

    private fun toMediaItem(track: TrackEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(trackId(track.id))
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build(),
            )
            .build()

    private fun toFolderItem(id: Long, name: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(folderId(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun toCategoryItem(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun toPlaylistItem(id: Long, name: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(playlistId(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.release()
        player.release()
        mediaLibrarySession = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = asyncResult {
            LibraryResult.ofItem(toCategoryItem(ROOT_ID, getString(R.string.app_name)), params)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = asyncResult {
            val repository = (application as VccMusicApplication).container.musicRepository
            val root = repository.activeRoot()
            val items = when (parentId) {
                ROOT_ID -> listOf(
                    toCategoryItem(FOLDERS_ID, getString(R.string.folders)),
                    toCategoryItem(ALL_TRACKS_ID, getString(R.string.all_music)),
                    toCategoryItem(PLAYLISTS_ID, getString(R.string.playlists)),
                    toCategoryItem(SHUFFLE_ID, getString(R.string.shuffle)),
                )
                FOLDERS_ID -> root?.let {
                    repository.observeFolders(it.id, null).first().map { folder ->
                        toFolderItem(folder.id, folder.name)
                    }
                }.orEmpty()
                ALL_TRACKS_ID -> root?.let {
                    repository.observeAllTracks(it.id).first().map(::toMediaItem)
                }.orEmpty()
                PLAYLISTS_ID -> repository.observePlaylists().first().map {
                    toPlaylistItem(it.id, it.name)
                }
                SHUFFLE_ID -> root?.let {
                    repository.observeAllTracks(it.id).first().shuffled().map(::toMediaItem)
                }.orEmpty()
                else -> when {
                    parentId.startsWith(FOLDER_PREFIX) -> {
                        val folder = repository.folder(parentId.removePrefix(FOLDER_PREFIX).toLongOrNull() ?: -1)
                        if (folder == null) emptyList() else {
                            val children = repository.observeFolders(folder.rootId, folder.id).first()
                                .map { child -> toFolderItem(child.id, child.name) }
                            children + repository.observeDirectTracks(folder.id).first().map(::toMediaItem)
                        }
                    }
                    parentId.startsWith(PLAYLIST_PREFIX) -> {
                        repository.observePlaylistTracks(
                            parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull() ?: -1,
                        ).first().map(::toMediaItem)
                    }
                    else -> emptyList()
                }
            }
            LibraryResult.ofItemList(page(items, page, pageSize), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = asyncResult {
            val repository = (application as VccMusicApplication).container.musicRepository
            val item = when {
                mediaId == ROOT_ID -> toCategoryItem(ROOT_ID, getString(R.string.app_name))
                mediaId == FOLDERS_ID -> toCategoryItem(FOLDERS_ID, getString(R.string.folders))
                mediaId == ALL_TRACKS_ID -> toCategoryItem(ALL_TRACKS_ID, getString(R.string.all_music))
                mediaId == PLAYLISTS_ID -> toCategoryItem(PLAYLISTS_ID, getString(R.string.playlists))
                mediaId == SHUFFLE_ID -> toCategoryItem(SHUFFLE_ID, getString(R.string.shuffle))
                mediaId.startsWith(FOLDER_PREFIX) -> repository.folder(
                    mediaId.removePrefix(FOLDER_PREFIX).toLongOrNull() ?: -1,
                )?.let { toFolderItem(it.id, it.name) }
                mediaId.startsWith(PLAYLIST_PREFIX) -> repository.observePlaylists().first()
                    .firstOrNull { it.id == mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull() }
                    ?.let { toPlaylistItem(it.id, it.name) }
                mediaId.startsWith(TRACK_PREFIX) -> repository.track(
                    mediaId.removePrefix(TRACK_PREFIX).toLongOrNull() ?: -1,
                )?.let(::toMediaItem)
                else -> null
            }
            item?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError<MediaItem>(SessionError.ERROR_BAD_VALUE)
        }

        private fun <T> asyncResult(block: suspend () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> {
            val future = SettableFuture.create<LibraryResult<T>>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    future.set(block())
                } catch (error: Exception) {
                    future.setException(error)
                }
            }
            return future
        }

        private fun page(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
            if (page < 0 || pageSize <= 0) return emptyList()
            val start = (page.toLong() * pageSize).coerceAtMost(items.size.toLong()).toInt()
            val end = (start + pageSize).coerceAtMost(items.size)
            return items.subList(start, end)
        }
    }

    private fun trackId(id: Long) = "$TRACK_PREFIX$id"
    private fun folderId(id: Long) = "$FOLDER_PREFIX$id"
    private fun playlistId(id: Long) = "$PLAYLIST_PREFIX$id"

    private companion object {
        const val ROOT_ID = "root"
        const val FOLDERS_ID = "folders"
        const val ALL_TRACKS_ID = "all_tracks"
        const val PLAYLISTS_ID = "playlists"
        const val SHUFFLE_ID = "shuffle"
        const val FOLDER_PREFIX = "folder:"
        const val PLAYLIST_PREFIX = "playlist:"
        const val TRACK_PREFIX = "track:"
    }
}
