package app.kreate.android.themed.common.screens.details

import it.fast4x.rimusic.Database
import app.kreate.android.service.YoutubeInfo
import app.kreate.database.models.SongYoutubeInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.kreate.android.R
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.innertube.Innertube
import me.knighthat.innertube.model.InnertubeSong
import me.knighthat.innertube.model.InnertubeSongDetails
import me.knighthat.utils.Toaster

class SongDetailsViewModel(private val songId: String): ViewModel() {

    var songDetails: InnertubeSongDetails? by mutableStateOf(null)
        private set
    var songBasicInfo: InnertubeSong? by mutableStateOf(null)
        private set
    /** Views and release date, from the library filters' table (asked now if missing) */
    var youtubeInfo: SongYoutubeInfo? by mutableStateOf(null)
        private set

    init {
        fetchDetails()
        fetchBasicInfo()
        fetchYoutubeInfo()
    }

    // YouTube changed this answer ("videoPrimaryInfoRenderer" is gone): it fails for
    // most songs. The page shows without it (no description), so no error toast
    private fun fetchDetails() = CoroutineScope(Dispatchers.IO).launch {
        Innertube.songInfo( songId, CURRENT_LOCALE)
                 .onSuccess { songDetails = it }
                 .onFailure { err -> Logger.w( err ) { "no details page for $songId" } }
    }

    private fun fetchYoutubeInfo() = CoroutineScope(Dispatchers.IO).launch {
        val table = Database.songYoutubeInfoTable
        youtubeInfo = table.get( songId )
        if( youtubeInfo?.viewCount == null && YoutubeInfo.refresh( songId ) )
            youtubeInfo = table.get( songId )
    }

    private fun fetchBasicInfo() = CoroutineScope(Dispatchers.IO).launch {
        Innertube.songBasicInfo( songId, CURRENT_LOCALE)
                 .onSuccess { songBasicInfo = it }
                 .onFailure { err ->
                     Logger.e( "", err, "SongDetails" )
                     Toaster.e( R.string.error_failed_to_fetch_songs_info )
                 }
    }

    class Factory(private val songId: String): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SongDetailsViewModel(songId) as T
    }
}