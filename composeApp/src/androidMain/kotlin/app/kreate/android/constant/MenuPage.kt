package app.kreate.android.constant

import app.kreate.android.themed.common.component.menu.SongDownloadButton
import app.kreate.android.service.DownloadCacheState
import it.fast4x.rimusic.service.MyDownloadHelper
import androidx.media3.datasource.cache.Cache
import app.kreate.di.CacheType
import org.koin.java.KoinJavaComponent.get
import it.fast4x.rimusic.service.modern.isLocal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import app.kreate.android.R
import app.kreate.android.themed.common.component.BottomMenu
import app.kreate.android.themed.common.component.menu.AddSongToPlaylistButton
import app.kreate.android.themed.common.component.menu.DeletePlaylistButton
import app.kreate.android.themed.common.component.menu.DeleteSongButton
import app.kreate.android.themed.common.component.menu.ExportCacheButton
import app.kreate.android.themed.common.component.menu.GoToAlbumButton
import app.kreate.android.themed.common.component.menu.GoToArtistButton
import app.kreate.android.themed.common.component.menu.MenuButton
import app.kreate.android.themed.common.component.menu.PinPlaylistButton
import app.kreate.android.themed.common.component.menu.PlaylistEnqueueButton
import app.kreate.android.themed.common.component.menu.PlaylistPlayNextButton
import app.kreate.android.themed.common.component.menu.PlaylistShuffleButton
import app.kreate.android.themed.common.component.menu.RemoveSongFromPlaylist
import app.kreate.android.themed.common.component.menu.ResetSongButton
import app.kreate.android.themed.common.component.menu.SongEditThumbnailButton
import app.kreate.android.themed.common.component.menu.SongEnqueueButton
import app.kreate.android.themed.common.component.menu.SongPlayNextButton
import app.kreate.android.themed.common.component.menu.SongRadioButton
import app.kreate.android.themed.common.component.menu.SongRenameAuthorButton
import app.kreate.android.themed.common.component.menu.SongRenameButton
import app.kreate.android.themed.common.component.menu.ViewSongDetailsButton
import app.kreate.database.models.Playlist
import app.kreate.database.models.PlaylistPreview
import app.kreate.util.cleanPrefix
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import me.knighthat.utils.Toaster


sealed interface MenuPage {

    suspend fun getButtons(): List<MenuButton<*>> = emptyList()

    /**
     * Holds and shows nothing. Shouldn't be used for anything other than
     * being a placeholder for other pages.
     */
    object Empty : MenuPage

    data class Song(override val song: MediaItem) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOf(
                // Common actions first, rare ones (editing, export) after,
                // the destructive delete last so it isn't hit by mistake
                // Play next and add to playlist are quick actions on top of the menu
                SongEnqueueButton(),
                SongRadioButton(), GoToArtistButton(), GoToAlbumButton(),
                ViewSongDetailsButton(),
                SongRenameButton(), SongRenameAuthorButton(), SongEditThumbnailButton(),
                ResetSongButton(), ExportCacheButton(), DeleteSongButton()
            )
    }

    /**
     * The player's ⋮: the same sheet as a long press on a song, with the player's
     * own actions (equalizer, sleep timer is a quick action) and editing grouped.
     */
    data class PlayerSong(override val song: MediaItem) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOfNotNull(
                SongRadioButton(),
                // Download or remove the download, as the song stands now (not for local files)
                if( song.isLocal ) null else SongDownloadButton(
                    DownloadCacheState.isDownloaded(
                        MyDownloadHelper.instance.downloads.value[song.mediaId]?.state,
                        get<Cache>( Cache::class.java, CacheType.DOWNLOAD ),
                        song.mediaId
                    )
                ),
                app.kreate.android.themed.common.component.menu.EqualizerButton(),
                GoToArtistButton(), GoToAlbumButton(), ViewSongDetailsButton(),
                app.kreate.android.themed.common.component.menu.EditSongButton()
            )
    }

    /** "Modifier le titre…": rename, authors, cover */
    data class EditSong(override val song: MediaItem) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOf( SongRenameButton(), SongRenameAuthorButton(), SongEditThumbnailButton() )
    }

    data class LocalSong(override val song: MediaItem) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOf(
                // Play next and add to playlist are quick actions on top of the menu
                SongEnqueueButton(),
                SongRenameButton(), SongRenameAuthorButton(), SongEditThumbnailButton()
            )
    }

    data class LocalPlaylistSong(
        val playlist: Playlist,
        override val song: MediaItem
    ) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOf(
                // Play next and add to playlist are quick actions on top of the menu
                SongEnqueueButton(),
                SongRadioButton(), GoToArtistButton(), GoToAlbumButton(),
                ViewSongDetailsButton(),
                SongRenameButton(), SongRenameAuthorButton(), SongEditThumbnailButton(),
                ResetSongButton(), ExportCacheButton(),
                RemoveSongFromPlaylist(playlist), DeleteSongButton()
            )
    }

    data class LocalPlaylistLocalSong(
        val playlist: Playlist,
        override val song: MediaItem
    ) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> =
            listOf(
                // Play next and add to playlist are quick actions on top of the menu
                SongEnqueueButton(),
                SongRenameButton(), SongRenameAuthorButton(), SongEditThumbnailButton(),
                RemoveSongFromPlaylist(playlist)
            )
    }

    data class AddToPlaylist(override val song: MediaItem) : MenuPage, SongMenu {

        override suspend fun getButtons(): List<MenuButton<*>> = withContext( Dispatchers.IO ) {
            Database.playlistTable
                    .allAsPreview()
                    .firstOrNull()
                    ?.map {
                        val isAdded = Database.songPlaylistMapTable.isMapped( song.mediaId, it.playlist.id ).first()

                        object : MenuButton<MediaItem>() {

                            override var iconId: Int by mutableIntStateOf( R.drawable.playlist )
                            override val tooltipMessageId: Int = R.string.add_to_playlist
                            override val title: String
                                @Composable
                                get() = cleanPrefix( it.playlist.name )

                            init {
                                iconId = if( isAdded ) R.drawable.checkmark else R.drawable.playlist
                            }

                            override fun onClick( menu: BottomMenu, item: MediaItem ) {
                                val songName = song.mediaMetadata.title?.toString().orEmpty()
                                val playlistName = it.playlist.name

                                if( iconId == R.drawable.checkmark ) {
                                    Toaster.i( R.string.info_song_already_in_playlist, songName, playlistName )
                                    return
                                }

                                Database.asyncTransaction {
                                    iconId = R.drawable.checkmark

                                    mapIgnore( it.playlist, item )
                                    Toaster.s( R.string.success_added_song_to_playlist, songName, playlistName )
                                }
                            }
                        }
                    }
                    .orEmpty()
        }
    }

    data class NavRedirect(
        val destination: NavRoutes,
        val path: String
    ) : MenuPage

    data class LocalPlaylist(val playlist: PlaylistPreview) : MenuPage {

        override suspend fun getButtons(): List<MenuButton<PlaylistPreview>> =
            listOf(
                PinPlaylistButton(playlist), PlaylistPlayNextButton(), PlaylistEnqueueButton(),
                PlaylistShuffleButton(), DeletePlaylistButton()
            )
    }

    sealed interface SongMenu {

        val song: MediaItem
    }
}