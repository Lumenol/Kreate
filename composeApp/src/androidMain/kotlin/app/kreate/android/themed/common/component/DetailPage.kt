package app.kreate.android.themed.common.component

import androidx.compose.runtime.getValue
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.ui.components.Skeleton
import it.fast4x.rimusic.ui.components.navigation.header.AppHeader
import it.fast4x.rimusic.ui.components.tab.toolbar.Button
import it.fast4x.rimusic.ui.components.tab.toolbar.EllipsisMenuComponent
import it.fast4x.rimusic.ui.components.tab.toolbar.MenuIcon
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.medium
import it.fast4x.rimusic.utils.semiBold

/** The 3 main tabs (Home · Discover · Library), shared by home and detail pages */
object MainTabs {

    const val HOME = 0
    const val DISCOVER = 6
    /** Songs, artists, albums and playlists: chips inside the Library tab */
    val LIBRARY = 1..4

    @Composable
    fun Items( item: @Composable (Int, String, Int) -> Unit, libraryTab: Int ) {
        if ( Preferences.QUICK_PICKS_PAGE.value ) {
            item( HOME, stringResource( R.string.home ), R.drawable.home )
            item( DISCOVER, stringResource( R.string.discover ), R.drawable.globe )
        }
        item( libraryTab, stringResource( R.string.library ), R.drawable.library )
    }
}

/**
 * Scaffold of album, artist and playlist pages: "back" and [actions] on top,
 * the main tabs at the bottom (tapping one goes back home on that tab), so
 * these pages don't get a bottom bar of their own.
 */
@Composable
fun DetailSkeleton(
    navController: NavController,
    miniPlayer: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    title: (@Composable () -> Unit)? = null,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    // Read once: the scaffold animates its content when the index changes, and
    // that content must stay put while the page is closing
    val homeTab = remember { Preferences.HOME_TAB_INDEX.value }
    val libraryTab = if( homeTab in MainTabs.LIBRARY ) homeTab else MainTabs.LIBRARY.first

    Skeleton(
        navController = navController,
        tabIndex = homeTab,
        onTabChanged = {
            Preferences.HOME_TAB_INDEX.value = it
            navController.popBackStack( NavRoutes.home.name, false )
        },
        miniPlayer = miniPlayer,
        navBarContent = { MainTabs.Items( it, libraryTab ) },
        detailActions = actions,
        detailTitle = title,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    navController: NavController,
    actions: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit = {}
) =
    TopAppBar(
        title = title,
        navigationIcon = {
            if( navController.previousBackStackEntry != null )
                IconButton( onClick = navController::navigateUp ) {
                    Icon(
                        painter = painterResource( R.drawable.chevron_back ),
                        contentDescription = null,
                        tint = colorPalette().text,
                        modifier = Modifier.size( 24.dp )
                    )
                }
        },
        actions = actions,
        expandedHeight = if( isLandscape ) 48.dp else TopAppBarDefaults.TopAppBarExpandedHeight,
        colors = AppHeader.colors()
    )

/**
 * [text] in the top bar once the page's header has scrolled away, so a long
 * list still says which playlist or album it is (only ← and ⋯ were left).
 */
@Composable
fun ScrolledTitle( text: String, listState: androidx.compose.foundation.lazy.LazyListState ) {
    val scrolled by remember( listState ) { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    androidx.compose.animation.AnimatedVisibility(
        visible = scrolled,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = typography().m.semiBold.color( colorPalette().text ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/** "⋮" listing [buttons] with their names; buttons that can't be menu items stay icons */
@Composable
fun DetailMenu( buttons: List<Button> ) {
    val menuItems = buttons.filterIsInstance<MenuIcon>()
    val ellipsisMenu = EllipsisMenuComponent.init { menuItems }

    buttons.filterNot { it is MenuIcon }.forEach { it.ToolBarButton() }
    if( menuItems.isNotEmpty() )
        ellipsisMenu.ToolBarButton()
}

/** A link in the header, e.g. an artist of the album; [onClick] null = plain text */
data class HeaderLink( val text: String, val onClick: (() -> Unit)? )

/**
 * Header of album and playlist pages: square cover, full title (2 lines, no
 * scrolling text), clickable artists, details, "Play" / "Shuffle" and labeled
 * [actions]. The cover is left out in landscape, where it sits on the side.
 */
@Composable
fun DetailHeader(
    cover: Painter?,
    title: String,
    links: List<HeaderLink>,
    details: String,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    /** Instead of [cover], e.g. the 4-cover mosaic of a playlist */
    coverContent: (@Composable (Modifier) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth().padding( horizontal = 20.dp )
) {
    val coverModifier = Modifier.padding( top = 4.dp, bottom = 18.dp )
                                .size( 208.dp )
                                .clip( RoundedCornerShape(10.dp) )
                                .background( colorPalette().background1 )
    if( isLandscape ) {
        // The cover sits on the side in landscape
    } else if( coverContent != null )
        coverContent( coverModifier )
    else if( cover != null )
        Image(
            painter = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = coverModifier
        )

    BasicText(
        text = title,
        style = typography().l.semiBold.copy(
            color = colorPalette().text,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )

    if( links.isNotEmpty() )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding( top = 4.dp )
        ) {
            links.forEachIndexed { index, link ->
                if( index > 0 )
                    BasicText( ", ", style = typography().s.semiBold.color( colorPalette().text ) )
                BasicText(
                    text = link.text,
                    style = typography().s.semiBold.color( colorPalette().text ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clip( RoundedCornerShape(4.dp) )
                                       .clickable( enabled = link.onClick != null ) { link.onClick?.invoke() }
                                       .padding( horizontal = 2.dp, vertical = 4.dp )
                )
            }
        }

    if( details.isNotBlank() )
        BasicText(
            text = details,
            style = typography().xs.medium.color( colorPalette().textSecondary ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

    Row(
        horizontalArrangement = Arrangement.spacedBy( 10.dp ),
        modifier = Modifier.fillMaxWidth().padding( top = 16.dp )
    ) {
        WidePill( stringResource( R.string.word_play ), R.drawable.play, true, onPlay )
        WidePill( stringResource( R.string.shuffle ), R.drawable.shuffle, false, onShuffle )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth().padding( top = 14.dp, bottom = 8.dp ),
        content = actions
    )
}

@Composable
private fun RowScope.WidePill( text: String, @DrawableRes iconId: Int, filled: Boolean, onClick: () -> Unit ) =
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight( 1f )
                           .height( 44.dp )
                           .clip( CircleShape )
                           .background( if( filled ) colorPalette().text else colorPalette().background2 )
                           .clickable( onClick = onClick )
    ) {
        val content = if( filled ) colorPalette().background0 else colorPalette().text
        Icon(
            painter = painterResource( iconId ),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size( 18.dp )
        )
        BasicText(
            text = text,
            style = typography().s.semiBold.color( content ),
            maxLines = 1,
            modifier = Modifier.padding( start = 6.dp )
        )
    }

/** Round icon with its name underneath, instead of an unlabeled toolbar icon; [selected] = switched on */
@Composable
fun LabeledAction( icon: Painter, label: String, selected: Boolean = false, onClick: () -> Unit ) =
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width( 76.dp )
                           .clip( RoundedCornerShape(12.dp) )
                           .clickable( onClick = onClick )
                           .padding( vertical = 4.dp )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size( 44.dp )
                               .clip( CircleShape )
                               .background( if( selected ) colorPalette().text else colorPalette().background2 )
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if( selected ) colorPalette().background0 else colorPalette().text,
                modifier = Modifier.size( 20.dp )
            )
        }
        BasicText(
            text = label,
            style = typography().xxs.medium.color( colorPalette().text ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding( top = 6.dp )
        )
    }

@Composable
fun LabeledAction( button: it.fast4x.rimusic.ui.components.tab.toolbar.Icon, label: String ) =
    LabeledAction( button.icon, label, onClick = button::onShortClick )

/** Opens Android's share sheet with [url] */
fun android.content.Context.shareUrl( url: String ) =
    startActivity(
        android.content.Intent.createChooser(
            android.content.Intent( android.content.Intent.ACTION_SEND ).apply {
                type = "text/plain"
                putExtra( android.content.Intent.EXTRA_TEXT, url )
            },
            null
        )
    )
