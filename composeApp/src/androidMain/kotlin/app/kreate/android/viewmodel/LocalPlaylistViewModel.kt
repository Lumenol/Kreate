package app.kreate.android.viewmodel

import it.fast4x.rimusic.utils.joinedNames
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kreate.android.Preferences
import app.kreate.android.themed.rimusic.component.playlist.PlaylistSongsSort
import app.kreate.database.models.Playlist
import app.kreate.database.models.Song
import co.touchlab.kermit.Logger
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.relatedSongs
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.ui.components.MenuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalCoroutinesApi::class)
class LocalPlaylistViewModel(
    savedStateHandle: SavedStateHandle,
    menuState: MenuState
) : ViewModel() {

    private val _relatedSongs = MutableStateFlow(emptyMap<Song, Int>())
    private val _items = MutableStateFlow(emptyList<Song>())

    val playlistId: Long = savedStateHandle["id"]!!
    val relatedSongs: StateFlow<Map<Song, Int>> = _relatedSongs.asStateFlow()
    val items: StateFlow<List<Song>> = _items.asStateFlow()
    val sort: PlaylistSongsSort = PlaylistSongsSort(menuState)
    val playlist: StateFlow<Playlist?> =
        Database.playlistTable
                .findById( playlistId )
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )

    init {
        viewModelScope.launch {
            val isEnabled = snapshotFlow { Preferences.LOCAL_PLAYLIST_SMART_RECOMMENDATION.value }
            val maxCount = snapshotFlow { Preferences.MAX_NUMBER_OF_SMART_RECOMMENDATIONS.value }

            combine( isEnabled, maxCount ) { enabled, count ->
                if( !enabled )
                    return@combine emptyMap()

                // Usually runs before the songs are loaded: wait for them. The
                // value must be read again each time, a captured empty list
                // looped forever (and an empty playlist never gets songs).
                val items = _items.first { it.isNotEmpty() }

                withContext( Dispatchers.IO ) {
                    val requestBody = NextBody( videoId =  items.random().id )
                    Innertube.relatedSongs( requestBody )
                             ?.getOrNull()      // If result is null, all subsequence calls are cancelled
                             ?.songs
                             ?.filterNot { songItem ->
                                 // Fetched Song may not have properties like [likedAt]
                                 // so the result of [List.any] may be false.
                                 // Comparing their IDs is the most effective way
                                 items.map( Song::id )
                                     .any{ songItem.info?.endpoint?.videoId == it }
                             }
                             ?.filter { it.info?.endpoint?.videoId != null && it.info?.name != null }
                             ?.take( count.toInt() )
                             ?.associate { songItem ->
                                 with( songItem ) {
                                     Song(
                                         id = info!!.endpoint!!.videoId!!,
                                         title = info!!.name!!,
                                         artistsText = authors?.joinedNames(),
                                         durationText = durationText,
                                         thumbnailUrl = thumbnail?.url,
                                         isExplicit = explicit
                                     ) to (0..items.size).random()      // Map this song with a random position from [items]
                                 }
                             }
                             .orEmpty()
                }
            }.collectLatest { songs ->
                _relatedSongs.update { songs }
            }
        }
        viewModelScope.launch {
            // Follow the sort: it used to be read once, when the page opened (#1903)
            snapshotFlow { sort.sortBy to sort.sortOrder }
                .flatMapLatest { (sortBy, sortOrder) ->
                    Database.songPlaylistMapTable
                            .sortSongs( playlistId, sortBy, sortOrder )
                            .catch { err ->
                                Logger.e( "", err, "LocalPlaylist" )

                                // This steps fails mostly because a ghost map lingers
                                // after a failed deletion or failed attempt of insertion
                                Database.songPlaylistMapTable.clearGhostMaps()

                                emitAll( Database.songPlaylistMapTable.sortSongs( playlistId, sortBy, sortOrder ) )
                            }
                }
                .flowOn( Dispatchers.IO )
                .distinctUntilChanged()
                .collectLatest { newList ->
                    _items.update { newList }
                }
        }
    }
}