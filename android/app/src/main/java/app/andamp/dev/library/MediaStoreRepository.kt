package app.andamp.dev.library

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore

data class LibraryTrack(
    val id: String, val uri: String, val title: String, val artist: String, val album: String,
    val albumId: Long?, val artistId: Long?, val durationMs: Long, val trackNumber: Int?,
    val year: Int?, val mimeType: String?, val sizeBytes: Long?, val dateAdded: Long?
)

class MediaStoreRepository(private val resolver: ContentResolver) {
    fun tracks(): List<LibraryTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED
        )
        val result = mutableListOf<LibraryTrack>()
        resolver.query(collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            fun idx(name:String)=c.getColumnIndexOrThrow(name)
            while(c.moveToNext()){
                val id=c.getLong(idx(MediaStore.Audio.Media._ID))
                result += LibraryTrack(
                    id=id.toString(), uri=ContentUris.withAppendedId(collection,id).toString(),
                    title=c.getString(idx(MediaStore.Audio.Media.TITLE))?.trim().takeUnless{it.isNullOrBlank()} ?: "Unknown Track",
                    artist=c.getString(idx(MediaStore.Audio.Media.ARTIST))?.trim().takeUnless{it.isNullOrBlank()} ?: "Unknown Artist",
                    album=c.getString(idx(MediaStore.Audio.Media.ALBUM))?.trim().takeUnless{it.isNullOrBlank()} ?: "Unknown Album",
                    albumId=c.getLong(idx(MediaStore.Audio.Media.ALBUM_ID)),
                    artistId=c.getLong(idx(MediaStore.Audio.Media.ARTIST_ID)),
                    durationMs=c.getLong(idx(MediaStore.Audio.Media.DURATION)),
                    trackNumber=c.getInt(idx(MediaStore.Audio.Media.TRACK)).takeIf{it>0},
                    year=c.getInt(idx(MediaStore.Audio.Media.YEAR)).takeIf{it>0},
                    mimeType=c.getString(idx(MediaStore.Audio.Media.MIME_TYPE)),
                    sizeBytes=c.getLong(idx(MediaStore.Audio.Media.SIZE)),
                    dateAdded=c.getLong(idx(MediaStore.Audio.Media.DATE_ADDED))
                )
            }
        }
        return result
    }
}
