package app.kreate.android.utils

import android.content.Context
import android.content.Intent

/** Opens Android's share sheet with the song's YouTube Music link */
fun shareSong( context: Context, songId: String ) {
    val intent = Intent( Intent.ACTION_SEND ).apply {
        type = "text/plain"
        putExtra( Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$songId" )
    }
    context.startActivity(
        Intent.createChooser( intent, null ).addFlags( Intent.FLAG_ACTIVITY_NEW_TASK )
    )
}
