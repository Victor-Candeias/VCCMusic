package pt.vcc.vccmusic.podcast.network

import java.security.MessageDigest

object PodcastIndexAuth {
    fun sha1(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
