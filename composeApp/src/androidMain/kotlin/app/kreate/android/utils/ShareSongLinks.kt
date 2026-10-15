package app.kreate.android.utils

import android.content.Context
import app.kreate.android.R
import app.kreate.android.themed.common.component.shareUrl
import app.kreate.database.models.Song
import me.knighthat.utils.Toaster

/** Above that, sharing a whole list is more likely a mistake than a wish */
private const val MAX_UNSELECTED = 50

/**
 * Shares the YouTube Music links of [songs], one per line ("title - artist: link").
 * Songs on the phone have no link and are left out. Without a selection, a long
 * list asks to select the songs first.
 *
 * @return `true` when the share sheet was opened
 */
fun Context.shareSongLinks( songs: List<Song>, isSelection: Boolean ): Boolean {
    val online = songs.filterNot( Song::isLocal )
    if( online.isEmpty() ) {
        Toaster.i( R.string.no_song_to_share )
        return false
    }
    if( !isSelection && online.size > MAX_UNSELECTED ) {
        Toaster.i( R.string.select_songs_to_share )
        return false
    }

    online.joinToString( "\n" ) { song ->
              val name = listOfNotNull( song.cleanTitle(), song.cleanArtistsText().takeIf( String::isNotBlank ) )
                  .joinToString( " - " )
              "$name: https://music.youtube.com/watch?v=${song.id}"
          }
          .also( ::shareUrl )
    return true
}
