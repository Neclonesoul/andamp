package app.andamp.dev.bridge

import android.webkit.JavascriptInterface
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.serialization.json.*

class AndampBridge(
    private val controller: () -> MediaController?,
    private val emit: (String) -> Unit
) {
    private val json = Json { ignoreUnknownKeys = false }

    @JavascriptInterface
    fun postMessage(payload: String) {
        val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return
        if (obj["protocolVersion"]?.jsonPrimitive?.intOrNull != 1) return
        val player = controller() ?: return
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "requestSnapshot" -> emitSnapshot(player)
            "play" -> player.play()
            "pause" -> player.pause()
            "togglePlayback" -> if (player.isPlaying) player.pause() else player.play()
            "seekTo" -> obj["positionMs"]?.jsonPrimitive?.longOrNull?.takeIf { it >= 0 }?.let(player::seekTo)
            "seekBy" -> obj["deltaMs"]?.jsonPrimitive?.longOrNull?.let { player.seekTo((player.currentPosition + it).coerceAtLeast(0)) }
            "next" -> player.seekToNextMediaItem()
            "previous" -> if (player.currentPosition > 3000) player.seekTo(0) else player.seekToPreviousMediaItem()
            "setShuffle" -> obj["enabled"]?.jsonPrimitive?.booleanOrNull?.let { player.shuffleModeEnabled = it }
            "setRepeat" -> when(obj["mode"]?.jsonPrimitive?.contentOrNull) {
                "off" -> player.repeatMode = Player.REPEAT_MODE_OFF
                "all" -> player.repeatMode = Player.REPEAT_MODE_ALL
                "one" -> player.repeatMode = Player.REPEAT_MODE_ONE
            }
            "playTrack" -> obj["mediaId"]?.jsonPrimitive?.contentOrNull?.let { id ->
                val index = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == id }
                if (index != null) { player.seekToDefaultPosition(index); player.play() }
            }
        }
        emitSnapshot(player)
    }

    fun emitSnapshot(player: Player) {
        val current = player.currentMediaItem
        val md = current?.mediaMetadata
        val queue = buildJsonArray {
            for (i in 0 until player.mediaItemCount) {
                val item = player.getMediaItemAt(i); val m=item.mediaMetadata
                add(buildJsonObject {
                    put("id","native:$i:${item.mediaId}")
                    putJsonObject("track") {
                        put("id",item.mediaId); put("title",m.title?.toString()?:"Unknown Track")
                        put("artist",m.artist?.toString()?:"Unknown Artist"); put("album",m.albumTitle?.toString()?:"Unknown Album")
                        put("durationMs",0)
                    }
                })
            }
        }
        val payload = buildJsonObject {
            put("type","snapshot")
            putJsonObject("payload") {
                put("protocolVersion",1)
                put("status", when { player.playerError!=null -> "error"; player.playbackState==Player.STATE_BUFFERING -> "loading"; player.isPlaying -> "playing"; player.playbackState==Player.STATE_ENDED -> "ended"; player.currentMediaItem!=null -> "paused"; else -> "idle" })
                put("currentMediaId", current?.mediaId)
                if(current==null) put("currentTrack",JsonNull) else putJsonObject("currentTrack"){
                    put("id",current.mediaId); put("title",md?.title?.toString()?:"Unknown Track");put("artist",md?.artist?.toString()?:"Unknown Artist");put("album",md?.albumTitle?.toString()?:"Unknown Album");put("durationMs",player.duration.coerceAtLeast(0))
                }
                put("queue",queue);put("queueIndex",player.currentMediaItemIndex);put("positionMs",player.currentPosition.coerceAtLeast(0));put("bufferedPositionMs",player.bufferedPosition.coerceAtLeast(0));put("durationMs",player.duration.coerceAtLeast(0))
                put("repeatMode",when(player.repeatMode){Player.REPEAT_MODE_ONE->"one";Player.REPEAT_MODE_ALL->"all";else->"off"});put("shuffle",player.shuffleModeEnabled)
                if(player.playerError==null) put("error",JsonNull) else put("error","Playback failed")
                putJsonArray("availableActions"){add("play");add("pause");add("seek");add("next");add("previous")}
            }
        }
        emit(payload.toString())
    }
}
