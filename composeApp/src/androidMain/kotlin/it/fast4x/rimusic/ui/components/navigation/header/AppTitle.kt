package it.fast4x.rimusic.ui.components.navigation.header

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.kreate.android.R
import app.kreate.android.drawable.AppIcon
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.ui.components.themed.Button
import me.knighthat.utils.Toaster
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold
import androidx.compose.ui.graphics.Color

private fun appIconClickAction(
    navController: NavController,
    countToReveal: MutableIntState,
    context: Context
) {
    countToReveal.intValue++

    val message: String =
        when( countToReveal.intValue ) {
            10 -> {
                countToReveal.intValue = 0
                NavRoutes.gamePacman.navigateHere( navController )
                ""
            }
            3 -> "Do you like clicking? Then continue..."
            6 -> "Okay, you’re looking for something, keep..."
            9 -> "You are a number one, click and enjoy the surprise"
            else -> ""
        }
    if( message.isNotEmpty() )
        Toaster.n( message, Toast.LENGTH_LONG )
}

private fun appIconLongClickAction(
    navController: NavController,
    context: Context
) {
    Toaster.n( "You are a number one, click and enjoy the surprise", Toast.LENGTH_LONG )
    NavRoutes.gameSnake.navigateHere( navController )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppLogo(
    navController: NavController,
    context: Context
) {
    val countToReveal = remember { mutableIntStateOf(0) }
    val modifier = Modifier.combinedClickable(
        onClick = { appIconClickAction( navController, countToReveal, context ) },
        onLongClick = { appIconLongClickAction( navController, context ) }
    )

    Image(
        bitmap = AppIcon.Round.imageBitmap( LocalContext.current ),
        contentDescription = "App's icon",
        modifier = modifier.size( 32.dp )
    )
}

private val DEBUG_RED = Color( 0xFFF87171 )

/**
 * Debug mode must stay obvious, so it's spelled out, but as one compact
 * status pill instead of red text over two lines. Tap to open settings.
 */
@Composable
private fun DebugBadge( navController: NavController ) {
    // The palette's red (#BF4040) is too dark to read on a dark header
    val red = DEBUG_RED

    Row(
        horizontalArrangement = Arrangement.spacedBy( 4.dp ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip( CircleShape )
                           .background( red.copy( alpha = .18f ) )
                           .clickable {
                               Toaster.s( R.string.info_debug_mode_is_enabled )

                               NavRoutes.settings.navigateHere( navController )
                           }
                           .padding( horizontal = 8.dp, vertical = 4.dp )
    ) {
        Icon(
            painter = painterResource( R.drawable.bug ),
            contentDescription = null,
            tint = red,
            modifier = Modifier.size( 14.dp )
        )
        BasicText(
            text = stringResource( R.string.debug_badge ),
            style = typography().xxs.semiBold.color( red )
        )
    }
}

// START
@Composable
fun AppTitle(
    navController: NavController,
    context: Context
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy( 8.dp ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The big "Music" wordmark took a lot of room on every screen: keep the icon,
        // and turn the two-line "debug mode enabled" text into a compact badge
        AppLogo( navController, context )

        if (Preference.debugLog())
            DebugBadge( navController )

        if(Preference.parentalControl())
            Button(
                iconId = R.drawable.shield_checkmark,
                color = AppBar.contentColor(),
                padding = 0.dp,
                size = 20.dp
            ).Draw()
    }
// END
}