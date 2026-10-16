package it.fast4x.rimusic.ui.components.navigation.header

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.size
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.coil3.ImageFactory
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.extensions.pip.rememberPipHandler
import it.fast4x.rimusic.thumbnailShape
import it.fast4x.rimusic.ui.components.themed.DropdownMenu
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.utils.isAtLeastAndroid8
import it.fast4x.rimusic.ytAccountThumbnail

@Composable
private fun HamburgerMenu(
    expanded: Boolean,
    onItemClick: (NavRoutes) -> Unit,
    onDismissRequest: () -> Unit
) {
    val enablePictureInPicture by Preferences.IS_PIP_ENABLED
    val pipHandler = rememberPipHandler()

    val menu = DropdownMenu(
        expanded = expanded,
        // Opaque: the see-through menu let the chips behind show through its text
        containerColor = colorPalette().background1,
        onDismissRequest = onDismissRequest
    )
    // History button
    menu.add(
        DropdownMenu.Item(
            R.drawable.history,
            R.string.history
        ) { onItemClick( NavRoutes.history ) }
    )
    // Statistics button
    menu.add(
        DropdownMenu.Item(
            R.drawable.stats_chart,
            R.string.statistics
        ) { onItemClick( NavRoutes.statistics ) }
    )
    // Profiles button
    menu.add(
        DropdownMenu.Item(
            R.drawable.person,
            R.string.profiles
        ) { onItemClick( NavRoutes.PROFILES ) }
    )
    // Picture in picture button
    val context = LocalContext.current
    val isPipSupported = if( isAtLeastAndroid8 )
        context.packageManager.hasSystemFeature( PackageManager.FEATURE_PICTURE_IN_PICTURE )
    else
        false
    if (isPipSupported && enablePictureInPicture)
        menu.add(
            DropdownMenu.Item(
                R.drawable.images_sharp,
                R.string.menu_go_to_picture_in_picture
            ) { pipHandler.enterPictureInPictureMode() }
        )
    menu.add { HorizontalDivider() }
    // Settings button
    menu.add(
        DropdownMenu.Item(
            R.drawable.settings,
            R.string.settings
        ) { onItemClick( NavRoutes.settings ) }
    )

    menu.Draw()
}

// START
@Composable
fun ActionBar(
    navController: NavController,
) {
    var expanded by remember { mutableStateOf(false) }

    // Downloads running: a small "↓ 2" pill, on every page, leading to them
    val downloads by it.fast4x.rimusic.service.MyDownloadHelper.instance.downloads.collectAsState()
    val running = downloads.values.count {
        it.state == androidx.media3.exoplayer.offline.Download.STATE_QUEUED
                || it.state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
                || it.state == androidx.media3.exoplayer.offline.Download.STATE_RESTARTING
    }
    if( running > 0 )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy( 2.dp ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.clip( androidx.compose.foundation.shape.CircleShape )
                               .background( colorPalette().background2 )
                               .clickable { NavRoutes.DOWNLOADS.navigateHere( navController ) }
                               .padding( horizontal = 10.dp, vertical = 6.dp )
        ) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource( R.drawable.download ),
                contentDescription = stringResource( R.string.downloads_in_progress_title ),
                tint = colorPalette().text,
                modifier = Modifier.size( 14.dp )
            )
            androidx.compose.foundation.text.BasicText(
                text = running.toString(),
                style = typography().xs.semiBold.color( colorPalette().text )
            )
        }

    // History in sight: in the menu alone, behind the account picture, nobody found it
    HeaderIcon( R.drawable.history, description = stringResource( R.string.history ) ) { NavRoutes.history.navigateHere( navController ) }

    // Search Icon
    HeaderIcon( R.drawable.search, description = stringResource( R.string.word_search ) ) {
        navController.navigate( NavRoutes.search.name ) {
            if( Preferences.SINGLE_BACK_FROM_SEARCH.value )
                popUpTo( NavRoutes.search.name )
            launchSingleTop = true
        }
    }

    if (isYouTubeLoggedIn()) {
        if (ytAccountThumbnail() != "")
            ImageFactory.AsyncImage(
                thumbnailUrl = ytAccountThumbnail(),
                contentScale = ContentScale.Fit,
                modifier = Modifier.height( 40.dp )
                                   .padding( end = 10.dp )
                                   .clip( thumbnailShape() )
                                   .clickable { expanded = !expanded }
            )
        else HeaderIcon( R.drawable.ytmusic, size = 30.dp ) { expanded = !expanded }
    } else HeaderIcon( R.drawable.burger ) { expanded = !expanded }

    // Define actions for when item inside menu clicked,
    // and when user clicks on places other than the menu (dismiss)
    val onItemClick: (NavRoutes) -> Unit = {
        expanded = false
        it.navigateHere( navController )
    }
    val onDismissRequest: () -> Unit = { expanded = false }

    // Hamburger menu
    HamburgerMenu(
        expanded = expanded,
        onItemClick = onItemClick,
        onDismissRequest = onDismissRequest
    )
// END
}