package it.fast4x.rimusic.utils

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import it.fast4x.rimusic.ui.components.themed.TitleMiniSection
import it.fast4x.rimusic.ui.screens.settings.isYouTubeLoggedIn
import it.fast4x.rimusic.ytAccountName
import java.util.Calendar

@Composable
fun WelcomeMessage(){
    // Read again every minute: kept from the first draw, an app left open since
    // the morning still said "Bonjour" in the evening
    var hour by remember { mutableIntStateOf( Calendar.getInstance().get( Calendar.HOUR_OF_DAY ) ) }
    LaunchedEffect( Unit ) {
        while( true ) {
            kotlinx.coroutines.delay( 60_000 )
            hour = Calendar.getInstance().get( Calendar.HOUR_OF_DAY )
        }
    }

    val message = when (hour) {
        in 6..12 -> {
            stringResource(R.string.good_morning)
        }

        in 13..17 -> {
            stringResource(R.string.good_afternoon)
        }

        in 18..23 -> {
            stringResource(R.string.good_evening)
        }

        else -> {
            stringResource(R.string.good_night)
        }
    }.let {
        if (isYouTubeLoggedIn()) "$it, ${ytAccountName()}"
        else it
    }

    TitleMiniSection(
        title = message,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}