package app.andamp.dev.bridge

import android.webkit.JavascriptInterface
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import app.andamp.dev.library.LibraryTrack
import app.andamp.dev.library.MediaStoreRepository
import kotlinx.serialization.json.*

class AndampBridge(
    private val controller: () -> MediaController?,
    private val library: MediaStoreRepository,
    private val emit: (String) -> Unit
) {
    private val json = Json { ignoreUnknownKeys = false }

    @Volatile
    private var cachedTracks: List<LibraryTrack> = emptyList()

    @JavascriptInterface
    fun postMessage(payload: String) {
        val obj = runCatching {
            json.parseToJsonElement(payload).jsonObject
        }.getOrNull() ?: return

        if (obj["protocolVersion"]?.jsonPrimitive?.intOrNull != 1) return

        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "requestLibrary" -> {
                cachedTracks = runCatching { library.tracks() }.getOrDefault(emptyList())
                emitLibrary()
                return
            }
        }

        val player = controller() ?: return

        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "requestSnapshot" -> emitSnapshot(player)

            "play" -> player.play()

            "pause" -> player.pause()

            "togglePlayback" ->
                if (player.isPlaying) player.pause() else player.play()

            "seekTo" ->
                obj["positionMs"]
                    ?.jsonPrimitive
                    ?.longOrNull
                    ?.takeIf { it >= 0 }
                    ?.let(player::seekTo)

            "seekBy" ->
                obj["deltaMs"]
                    ?.jsonPrimitive
                    ?.longOrNull
                    ?.let {
                        player.seekTo(
                            (player.currentPosition + it).coerceAtLeast(0)
                        )
                    }

            "next" -> player.seekToNextMediaItem()

            "previous" ->
                if (player.currentPosition > 3000) {
                    player.seekTo(0)
                } else {
                    player.seekToPreviousMediaItem()
                }

            "setShuffle" ->
                obj["enabled"]
                    ?.jsonPrimitive
                    ?.booleanOrNull
                    ?.let { player.shuffleModeEnabled = it }

            "setRepeat" ->
                when (obj["mode"]?.jsonPrimitive?.contentOrNull) {
                    "off" -> player.repeatMode = Player.REPEAT_MODE_OFF
                    "all" -> player.repeatMode = Player.REPEAT_MODE_ALL
                    "one" -> player.repeatMode = Player.REPEAT_MODE_ONE
                }

            "playTrack" -> {
                val id =
                    obj["mediaId"]?.jsonPrimitive?.contentOrNull
                        ?: return

                if (cachedTracks.isEmpty()) {
                    cachedTracks =
                        runCatching { library.tracks() }
                            .getOrDefault(emptyList())
                }

                val track =
                    cachedTracks.firstOrNull { it.id == id }
                        ?: return

                val item = mediaItem(track)

                player.setMediaItem(item)
                player.prepare()
                player.play()
            }
        }

        emitSnapshot(player)
    }

    private fun mediaItem(track: LibraryTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()

    private fun emitLibrary() {
        val payload = buildJsonObject {
            put("type", "librarySnapshot")

            putJsonObject("payload") {
                put("protocolVersion", 1)

                putJsonArray("tracks") {
                    cachedTracks.forEach { track ->
                        add(
                            buildJsonObject {
                                put("id", track.id)
                                put("uri", track.uri)
                                put("title", track.title)
                                put("artist", track.artist)
                                put("album", track.album)

                                track.albumId?.let {
                                    put("albumId", it.toString())
                                }

                                track.artistId?.let {
                                    put("artistId", it.toString())
                                }

                                put("durationMs", track.durationMs)

                                track.trackNumber?.let {
                                    put("trackNumber", it)
                                }

                                track.year?.let {
                                    put("year", it)
                                }

                                track.mimeType?.let {
                                    put("mimeType", it)
                                }

                                track.sizeBytes?.let {
                                    put("sizeBytes", it)
                                }

                                track.dateAdded?.let {
                                    put("dateAdded", it)
                                }
                            }
                        )
                    }
                }

                put("scannedAt", System.currentTimeMillis())
            }
        }

        emit(payload.toString())
    }

    fun emitSnapshot(player: Player) {
        val current = player.currentMediaItem
        val md = current?.mediaMetadata

        val duration =
            player.duration.takeIf { it > 0 } ?: 0

        val queue = buildJsonArray {
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i)
                val metadata = item.mediaMetadata

                val known =
                    cachedTracks.firstOrNull {
                        it.id == item.mediaId
                    }

                add(
                    buildJsonObject {
                        put("id", "native:$i:${item.mediaId}")

                        putJsonObject("track") {
                            put("id", item.mediaId)
                            put(
                                "title",
                                metadata.title?.toString()
                                    ?: known?.title
                                    ?: "Unknown Track"
                            )
                            put(
                                "artist",
                                metadata.artist?.toString()
                                    ?: known?.artist
                                    ?: "Unknown Artist"
                            )
                            put(
                                "album",
                                metadata.albumTitle?.toString()
                                    ?: known?.album
                                    ?: "Unknown Album"
                            )
                            put(
                                "durationMs",
                                known?.durationMs ?: 0
                            )
                        }
                    }
                )
            }
        }

        val payload = buildJsonObject {
            put("type", "snapshot")

            putJsonObject("payload") {
                put("protocolVersion", 1)

                put(
                    "status",
                    when {
                        player.playerError != null -> "error"
                        player.playbackState == Player.STATE_BUFFERING ->
                            "loading"
                        player.isPlaying -> "playing"
                        player.playbackState == Player.STATE_ENDED ->
                            "ended"
                        player.currentMediaItem != null -> "paused"
                        else -> "idle"
                    }
                )

                if (current == null) {
                    put("currentMediaId", JsonNull)
                    put("currentTrack", JsonNull)
                } else {
                    put("currentMediaId", current.mediaId)

                    val known =
                        cachedTracks.firstOrNull {
                            it.id == current.mediaId
                        }

                    putJsonObject("currentTrack") {
                        put("id", current.mediaId)
                        put(
                            "title",
                            md?.title?.toString()
                                ?: known?.title
                                ?: "Unknown Track"
                        )
                        put(
                            "artist",
                            md?.artist?.toString()
                                ?: known?.artist
                                ?: "Unknown Artist"
                        )
                        put(
                            "album",
                            md?.albumTitle?.toString()
                                ?: known?.album
                                ?: "Unknown Album"
                        )
                        put(
                            "durationMs",
                            known?.durationMs ?: duration
                        )
                    }
                }

                put("queue", queue)
                put("queueIndex", player.currentMediaItemIndex)
                put(
                    "positionMs",
                    player.currentPosition.coerceAtLeast(0)
                )
                put(
                    "bufferedPositionMs",
                    player.bufferedPosition.coerceAtLeast(0)
                )
                put("durationMs", duration)

                put(
                    "repeatMode",
                    when (player.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "one"
                        Player.REPEAT_MODE_ALL -> "all"
                        else -> "off"
                    }
                )

                put("shuffle", player.shuffleModeEnabled)

                if (player.playerError == null) {
                    put("error", JsonNull)
                } else {
                    put("error", "Playback failed")
                }

                putJsonArray("availableActions") {
                    add("play")
                    add("pause")
                    add("seek")
                    add("next")
                    add("previous")
                }
            }
        }

        emit(payload.toString())
    }
}
