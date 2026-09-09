package pt.vcc.vccmusic.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import pt.vcc.vccmusic.data.local.MusicDatabase
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.PlaylistTrackEntity
import pt.vcc.vccmusic.data.local.TrackEntity

private const val MAX_ARTWORK_BYTES = 512 * 1024

data class ScanProgress(
    val folders: Int,
    val tracks: Int,
    val errors: Int,
)

data class ScanResult(
    val completed: Boolean,
    val folders: Int,
    val tracks: Int,
    val errors: List<ScanError>,
)

data class ScanError(
    val uri: String,
    val message: String,
)

interface MusicScanner {
    suspend fun scan(
        root: Uri,
        rootId: Long,
        onProgress: (ScanProgress) -> Unit = {},
    ): ScanResult
}

private data class DocumentInfo(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
)

private data class ScannedFolder(
    val entity: MusicFolderEntity,
    val parentUri: String?,
)

private data class ScannedTrack(
    val entity: TrackEntity,
    val folderUri: String,
)

class SafMusicScanner(
    context: Context,
    private val database: MusicDatabase,
) : MusicScanner {
    private val resolver: ContentResolver = context.applicationContext.contentResolver

    override suspend fun scan(
        root: Uri,
        rootId: Long,
        onProgress: (ScanProgress) -> Unit,
    ): ScanResult = withContext(Dispatchers.IO) {
        scanOnIo(root, rootId, onProgress)
    }

    private suspend fun scanOnIo(
        root: Uri,
        rootId: Long,
        onProgress: (ScanProgress) -> Unit,
    ): ScanResult {
        val folders = mutableListOf<ScannedFolder>()
        val tracksByUri = linkedMapOf<String, ScannedTrack>()
        val errors = mutableListOf<ScanError>()
        val pending = ArrayDeque<Pair<DocumentInfo, String?>>()
        val visited = mutableSetOf<String>()
        var enumerationComplete = true
        val rootInfo = DocumentInfo(root, root.lastPathSegment ?: "Música", DocumentsContract.Document.MIME_TYPE_DIR)
        pending.add(rootInfo to null)
        var scannedFolders = 0

        try {
            while (pending.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val (folder, parentUri) = pending.removeFirst()
                if (!visited.add(folder.uri.toString())) continue
                val folderEntity = MusicFolderEntity(
                    id = 0,
                    rootId = rootId,
                    parentId = null,
                    name = folder.name,
                    uri = folder.uri.toString(),
                )
                folders += ScannedFolder(folderEntity.copy(name = folder.name), parentUri)
                scannedFolders++

                val children = try {
                    listChildren(folder.uri)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    errors += ScanError(folder.uri.toString(), error.message ?: "Não foi possível listar a pasta.")
                    enumerationComplete = false
                    continue
                }

                for (child in children) {
                    currentCoroutineContext().ensureActive()
                    if (child.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(child to folder.uri.toString())
                    } else if (isAudio(child)) {
                        readTrack(child, rootId)
                            .onSuccess {
                                tracksByUri.putIfAbsent(
                                    it.uri,
                                    ScannedTrack(it, folder.uri.toString()),
                                )
                            }
                            .onFailure { error ->
                                errors += ScanError(child.uri.toString(), error.message ?: "Não foi possível ler a faixa.")
                            }
                    }
                }
                onProgress(ScanProgress(scannedFolders, tracksByUri.size, errors.size))
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            return ScanResult(false, scannedFolders, tracksByUri.size, errors)
        }

        if (!enumerationComplete) {
            return ScanResult(false, scannedFolders, tracksByUri.size, errors)
        }

        database.withTransaction {
            val playlistSnapshot = database.playlistDao().snapshotTracksForRoot(rootId)
            database.trackDao().deleteForRoot(rootId)
            database.musicFolderDao().deleteForRoot(rootId)

            val folderIds = mutableMapOf<String, Long>()
            folders.forEach { scanned ->
                val id = database.musicFolderDao().insert(
                    scanned.entity.copy(id = 0, parentId = null),
                )
                folderIds[scanned.entity.uri] = id
            }
            folders.forEach { scanned ->
                val folderId = folderIds[scanned.entity.uri] ?: return@forEach
                val parentId = scanned.parentUri?.let(folderIds::get)
                database.musicFolderDao().updateParent(folderId, parentId)
            }
            val rebuiltTracks = tracksByUri.values.mapNotNull { scanned ->
                folderIds[scanned.folderUri]?.let { folderId ->
                    scanned.entity.copy(id = 0, folderId = folderId)
                }
            }
            database.trackDao().insertAll(rebuiltTracks)

            if (playlistSnapshot.isNotEmpty()) {
                val rebuiltByUri = database.trackDao()
                    .findByUris(rootId, playlistSnapshot.map { it.trackUri }.distinct())
                    .associateBy { it.uri }
                database.playlistDao().insertTracks(
                    playlistSnapshot
                        .groupBy { it.playlistId }
                        .values
                        .flatMap { entries ->
                            entries
                                .mapNotNull { snapshot ->
                                    rebuiltByUri[snapshot.trackUri]?.let { track ->
                                        snapshot to track.id
                                    }
                                }
                                .mapIndexed { position, (snapshot, trackId) ->
                                    PlaylistTrackEntity(
                                        playlistId = snapshot.playlistId,
                                        trackId = trackId,
                                        position = position,
                                    )
                                }
                        },
                )
            }
        }
        return ScanResult(true, folders.size, tracksByUri.size, errors)
    }

    private fun listChildren(folderUri: Uri): List<DocumentInfo> {
        val documentId = if (folderUri.pathSegments.contains("document")) {
            DocumentsContract.getDocumentId(folderUri)
        } else {
            DocumentsContract.getTreeDocumentId(folderUri)
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            documentId,
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        return resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    add(
                        DocumentInfo(
                            DocumentsContract.buildDocumentUriUsingTree(folderUri, id),
                            cursor.getString(nameColumn) ?: id,
                            cursor.getString(mimeColumn),
                        ),
                    )
                }
            }
        } ?: throw IOException("O provider não devolveu documentos.")
    }

    private fun isAudio(document: DocumentInfo): Boolean {
        val mime = document.mimeType?.lowercase()
        if (mime?.startsWith("audio/") == true) return true
        if (!mime.isNullOrBlank() && mime != "application/octet-stream") return false
        return AUDIO_EXTENSIONS.any { document.name.endsWith(it, ignoreCase = true) }
    }

    private fun readTrack(
        document: DocumentInfo,
        rootId: Long,
    ): Result<TrackEntity> = try {
        val retriever = MediaMetadataRetriever()
        try {
            resolver.openFileDescriptor(document.uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf(String::isNotBlank)
                    ?: document.name.substringBeforeLast('.', document.name)
                return Result.success(
                    TrackEntity(
                        id = 0,
                        rootId = rootId,
                        folderId = 0,
                        title = title,
                        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                        album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                        uri = document.uri.toString(),
                        artworkUri = null,
                        artwork = retriever.embeddedPicture?.takeIf { it.size <= MAX_ARTWORK_BYTES },
                        isFavorite = false,
                    ),
                )
            } ?: error("O documento não está acessível.")
        } finally {
            retriever.release()
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        val AUDIO_EXTENSIONS = setOf(".mp3", ".m4a", ".aac", ".flac", ".ogg", ".oga", ".wav", ".opus", ".wma")
    }
}
