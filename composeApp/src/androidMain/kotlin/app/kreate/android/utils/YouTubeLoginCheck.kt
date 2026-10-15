package app.kreate.android.utils

import android.content.Context
import android.widget.Toast
import app.kreate.android.R
import co.touchlab.kermit.Logger
import com.metrolist.innertube.YouTube
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.utils.isNetworkConnected
import me.knighthat.utils.Toaster

/**
 * Tells when the saved YouTube login stopped working: likes, dislikes and
 * history sent to YouTube then fail without much explanation (idea from N-Zik).
 *
 * The account menu comes back without an account when the cookie expired. A
 * network failure says nothing about the login, so it's ignored.
 */
suspend fun warnIfYouTubeLoginExpired( context: Context ) {
    if( !isYouTubeLoggedIn() || !isNetworkConnected( context ) ) return

    YouTube.accountInfo().onFailure { err ->
        if( err is NullPointerException || err is IndexOutOfBoundsException ) {
            Logger.w( err, "YouTubeLogin" ) { "logged in, but YouTube returned no account" }
            Toaster.w( R.string.youtube_login_expired, Toast.LENGTH_LONG )
        }
    }
}
