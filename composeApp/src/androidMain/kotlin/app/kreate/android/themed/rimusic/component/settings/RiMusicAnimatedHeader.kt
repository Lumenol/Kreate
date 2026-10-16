package app.kreate.android.themed.rimusic.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kreate.android.themed.common.component.AbstractSearch
import app.kreate.android.themed.common.component.settings.SettingEntrySearch

@Composable
private fun SettingEntrySearch.AnimatedHeader() {
    AnimatedContent(
        targetState = isVisible,
        transitionSpec = {
            if ( targetState ) {
                slideInVertically { height -> height } + expandHorizontally() + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + shrinkHorizontally() + fadeOut()
            } else {
                slideInVertically { height -> -height } + expandHorizontally() + fadeIn() togetherWith
                        slideOutVertically { height -> height } + shrinkHorizontally() + fadeOut()
            }.using(
                // Disable clipping since the faded slide-in/out should
                // be displayed out of bounds.
                SizeTransform(clip = false)
            )
        },
        label = "animated header and search bar"
    ) { target ->
        if (target)
            SearchBar()
        else
            SearchHint()
    }
}

/**
 * A quiet "Rechercher dans Général" field instead of a big centred "Général":
 * the tab chip above already names the page, and the title took a tenth of
 * the screen. Touching it opens the search, like the icon did.
 */
@Composable
private fun SettingEntrySearch.SearchHint() =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy( 8.dp ),
        modifier = Modifier.fillMaxWidth()
                           .height( 36.dp )
                           .clip( CircleShape )
                           .background( colorPalette().background1 )
                           .clickable( role = androidx.compose.ui.semantics.Role.Button, onClick = ::onShortClick )
                           .padding( horizontal = 14.dp )
    ) {
        Icon(
            painter = painterResource( R.drawable.search ),
            contentDescription = null,
            tint = colorPalette().textSecondary,
            modifier = Modifier.size( 16.dp )
        )
        // A search closed but still filtering shows its words: an empty-looking
        // field over a filtered list made settings look gone
        val active = input.text.isNotBlank()
        BasicText(
            text = if( active ) input.text else stringResource( R.string.settings_search_in, stringResource( titleId ) ),
            style = typography().xs.color( if( active ) colorPalette().text else colorPalette().textSecondary ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }

@Composable
fun SettingEntrySearch.RiMusicAnimatedHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
                           .height( 44.dp )
                           .padding( horizontal = 16.dp, vertical = 4.dp )
    ) {
        // The page's icon opens and closes the search, as before: once open,
        // the hint is gone and it's the way back
        HeaderIcon( Modifier.padding( end = AbstractSearch.DECO_BOX_ITEM_SPACING.dp ) )
        AnimatedHeader()
    }
}
