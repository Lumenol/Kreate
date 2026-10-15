package it.fast4x.rimusic.ui.components

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.kreate.android.Preferences
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.BuiltInPlaylist
import it.fast4x.rimusic.enums.ColorPaletteMode
import androidx.compose.foundation.shape.CircleShape

@Composable
fun <E> ButtonsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorPaletteMode by Preferences.THEME_MODE
    // Keep the selected chip on screen when it starts off screen
    // (e.g. settings opened straight on "Miscellaneous")
    val scrollState = rememberScrollState()
    val chipStarts = remember { mutableStateMapOf<E, Int>() }
    val margin = with( LocalDensity.current ) { 12.dp.roundToPx() }
    LaunchedEffect( currentValue, chipStarts[currentValue] ) {
        val start = chipStarts[currentValue] ?: return@LaunchedEffect
        if( start < scrollState.value || start > scrollState.value + scrollState.viewportSize / 2 )
            scrollState.animateScrollTo( (start - margin).coerceAtLeast( 0 ) )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { (value, label) ->
            FilterChip(
                modifier = Modifier.onGloballyPositioned {
                    chipStarts[value] = it.positionInParent().x.toInt()
                },
                label = { Text(label) },
                selected = currentValue == value,
                // Filled pills, the selected one inverted (white on dark), like the
                // filters of YouTube Music and Spotify, instead of outlined chips
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults
                    .filterChipColors(
                        containerColor = colorPalette().background2,
                        labelColor = colorPalette().text,
                        selectedContainerColor = colorPalette().text,
                        selectedLabelColor = colorPalette().background0,
                    ),
                onClick = { onValueUpdate(value) }
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun ButtonsRow(
    chips: List<BuiltInPlaylist>,
    currentValue: BuiltInPlaylist,
    onValueUpdate: (BuiltInPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorPaletteMode by Preferences.THEME_MODE
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.width(12.dp))

        chips.forEach { playlistType ->
            FilterChip(
                label = { Text( playlistType.text ) },
                selected = currentValue == playlistType,
                // Filled pills, the selected one inverted (white on dark), like the
                // filters of YouTube Music and Spotify, instead of outlined chips
                shape = CircleShape,
                border = null,
                colors = FilterChipDefaults
                    .filterChipColors(
                        containerColor = colorPalette().background2,
                        labelColor = colorPalette().text,
                        selectedContainerColor = colorPalette().text,
                        selectedLabelColor = colorPalette().background0,
                    ),
                onClick = { onValueUpdate(playlistType) }
            )

            Spacer(Modifier.width(8.dp))
        }
    }
}