package me.knighthat.component.artist

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.kreate.database.models.Artist
import it.fast4x.innertube.YtMusic
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.tab.toolbar.Descriptive
import it.fast4x.rimusic.ui.screens.settings.isYouTubeSyncEnabled
import it.fast4x.rimusic.utils.conditional
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.semiBold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import org.koin.java.KoinJavaComponent.inject

class FollowButton private constructor(
    private val getArtist: () -> Artist?
): Button, Descriptive {

    companion object {
        @Composable
        operator fun invoke( getArtist: () -> Artist? ): FollowButton =
            FollowButton(getArtist)
    }

    override val messageId: Int = R.string.follow

    override fun onShortClick() {
        val context: Context by inject(Context::class.java)
        if ( isYouTubeSyncEnabled() && !isNetworkConnected( context ) ){
            Toaster.noInternet()
        } else {
            val artist = getArtist() ?: return

            Database.asyncTransaction {
                artistTable.toggleFollow( artist.id )
            }

            CoroutineScope( Dispatchers.IO ).launch {
                if( !isYouTubeSyncEnabled() ) return@launch

                if ( artist.bookmarkedAt != null )
                    YtMusic.unsubscribeChannel( artist.id )
                else
                    YtMusic.subscribeChannel( artist.id )
            }
        }
    }

    @Composable
    override fun ToolBarButton() {
        val artist = getArtist() ?: return
        val isFollowed by remember {
            Database.artistTable
                    .isFollowing( artist.id )
        }.collectAsState( false, Dispatchers.IO )
        // Neutral like the rest of the page: outlined when not subscribed,
        // grey once subscribed (the accent color clashed with the palette)
        val colorPalette = colorPalette()
        val shape = RoundedCornerShape(50)

        Box(
            modifier = Modifier.height( 36.dp )
                               .clip( shape )
                               .conditional( isFollowed ) { background( colorPalette.background2 ) }
                               .conditional( !isFollowed ) { border( 1.dp, colorPalette.textDisabled, shape ) }
                               .clickable( onClick = ::onShortClick )
                               .padding( horizontal = 16.dp ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = stringResource( if( isFollowed ) R.string.following else R.string.follow ),
                style = typography().xs.semiBold.copy( color = colorPalette.text ),
                maxLines = 1
            )
        }
    }
}