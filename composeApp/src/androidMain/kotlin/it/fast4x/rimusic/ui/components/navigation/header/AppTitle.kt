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
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.ui.components.themed.Button
import me.knighthat.utils.Toaster
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape

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
        // The big "Music" wordmark and the red "debug mode enabled" text took a lot of
        // room on every screen: keep the icon, and flag debug mode with a dot on it
        Box {
            AppLogo( navController, context )

            if (Preference.debugLog())
                Box(
                    Modifier.align( Alignment.TopEnd )
                            .size( 10.dp )
                            .background( colorPalette().red, CircleShape )
                            .clickable {
                                Toaster.s( R.string.info_debug_mode_is_enabled )

                                NavRoutes.settings.navigateHere( navController )
                            }
                )
        }

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