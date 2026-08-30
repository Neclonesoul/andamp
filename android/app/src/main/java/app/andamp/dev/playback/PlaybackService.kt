package app.andamp.dev.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.SessionError
import app.andamp.dev.MainActivity
import app.andamp.dev.library.MediaStoreRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var library: MediaStoreRepository

    override fun onCreate() {
        super.onCreate()
        library = MediaStoreRepository(contentResolver)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true
            )
        }
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaLibrarySession.Builder(this, player, Callback(library))
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() { session.release(); player.release(); super.onDestroy() }

    private class Callback(private val library: MediaStoreRepository) : MediaLibrarySession.Callback {
        private fun item(t: app.andamp.dev.library.LibraryTrack) = MediaItem.Builder()
            .setMediaId(t.id).setUri(t.uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).setIsPlayable(true).setIsBrowsable(false).build())
            .build()

        override fun onGetLibraryRoot(session: MediaLibrarySession, browser: ControllerInfo, params: LibraryParams?): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(
                MediaItem.Builder().setMediaId("root").setMediaMetadata(MediaMetadata.Builder().setTitle("Andamp").setIsBrowsable(true).setIsPlayable(false).build()).build(), params
            ))

        override fun onGetChildren(session: MediaLibrarySession, browser: ControllerInfo, parentId: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val tracks=library.tracks()
            val children = when(parentId){
                "root" -> listOf("tracks" to "Tracks","albums" to "Albums","artists" to "Artists","playlists" to "Playlists","favorites" to "Favorites","recent" to "Recently Played").map{(id,title)->
                    MediaItem.Builder().setMediaId(id).setMediaMetadata(MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false).build()).build()
                }
                "tracks" -> tracks.map(::item)
                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
        }

        override fun onSetMediaItems(session: MediaSession, controller: ControllerInfo, mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val byId=library.tracks().associateBy{it.id}
            val resolved=mediaItems.mapNotNull{m->byId[m.mediaId]?.let(::item)}
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(resolved,startIndex.coerceAtLeast(0),startPositionMs))
        }
    }
}
