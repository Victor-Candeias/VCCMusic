package pt.vcc.vccmusic.podcast.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.podcast.network.PodcastSearchResponse

internal object PodcastIndexResponseParser {
    fun parseSearch(body: String): PodcastSearchResponse {
        val root = parseObject(body)
        val feeds = root.array("feeds")
            .mapIndexedNotNull { index, element ->
                if (!element.isJsonObject) {
                    return@mapIndexedNotNull null
                }
                parseFeed(element.asJsonObject, index)
            }
        return PodcastSearchResponse(
            count = root.number("count")?.toInt() ?: feeds.size,
            feeds = feeds,
        )
    }

    fun parseEpisodes(body: String): List<PodcastEpisode> {
        val root = parseObject(body)
        return root.array("items").mapNotNull { element ->
            if (!element.isJsonObject) {
                return@mapNotNull null
            }
            parseEpisode(element.asJsonObject)
        }
    }

    private fun parseFeed(json: JsonObject, index: Int): PodcastFeed? {
        val id = json.number("id")
        val title = json.string("title")
        if (id == null || id <= 0L || title.isNullOrBlank()) {
            return null
        }
        return PodcastFeed(
            id = id,
            title = title,
            url = json.string("url"),
            originalUrl = json.string("originalUrl"),
            link = json.string("link"),
            description = json.string("description"),
            author = json.string("author"),
            image = json.string("image"),
            artwork = json.string("artwork"),
            episodeCount = json.number("episodeCount")?.toInt(),
        )
    }

    private fun parseEpisode(json: JsonObject): PodcastEpisode? {
        val id = json.number("id")
        val title = json.string("title")
        if (id == null || id <= 0L || title.isNullOrBlank()) {
            return null
        }
        return PodcastEpisode(
            id = id,
            title = title,
            description = json.string("description"),
            link = json.string("link"),
            datePublished = json.number("datePublished"),
            datePublishedPretty = json.string("datePublishedPretty"),
            enclosureUrl = json.string("enclosureUrl"),
            enclosureType = json.string("enclosureType"),
            enclosureLength = json.number("enclosureLength"),
            duration = json.number("duration"),
            image = json.string("image"),
            feedId = json.number("feedId"),
            feedTitle = json.string("feedTitle"),
            feedImage = json.string("feedImage"),
            feedAuthor = json.string("feedAuthor"),
            season = json.number("season")?.toInt(),
            episode = json.number("episode")?.toInt(),
        )
    }

    private fun parseObject(body: String): JsonObject {
        val root = try {
            JsonParser.parseString(body)
        } catch (error: JsonParseException) {
            throw JsonParseException("Podcast Index devolveu JSON inválido.", error)
        }
        if (!root.isJsonObject) {
            throw JsonParseException("Podcast Index não devolveu um objeto JSON.")
        }
        return root.asJsonObject
    }

    private fun JsonObject.array(name: String) =
        get(name)?.takeUnless(JsonElement::isJsonNull)?.takeIf { it.isJsonArray }?.asJsonArray
            ?: throw JsonParseException("Podcast Index não devolveu o campo $name como array.")

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeUnless(JsonElement::isJsonNull)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.number(name: String): Long? =
        get(name)?.takeUnless(JsonElement::isJsonNull)
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asLong
}
