package pt.vcc.vccmusic.playback

import pt.vcc.vccmusic.data.local.TrackEntity

enum class QueueSource {
    ALL_TRACKS,
    FOLDER,
    SELECTION,
}

data class QueueRequest(
    val source: QueueSource,
    val tracks: List<TrackEntity>,
    val selectedTrackId: Long? = null,
)

object QueueBuilder {
    fun build(request: QueueRequest): List<TrackEntity> {
        val uniqueTracks = request.tracks
            .asSequence()
            .filter { it.uri.isNotBlank() }
            .distinctBy { it.uri }
            .sortedWith(compareBy<TrackEntity> { it.title.lowercase() }.thenBy { it.uri })
            .toList()

        val selected = request.selectedTrackId?.let { id ->
            uniqueTracks.firstOrNull { it.id == id }
        } ?: return uniqueTracks

        return listOf(selected) + uniqueTracks.filterNot { it.uri == selected.uri }
    }
}
