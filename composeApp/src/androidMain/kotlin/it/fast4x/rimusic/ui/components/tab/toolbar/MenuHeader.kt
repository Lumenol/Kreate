package it.fast4x.rimusic.ui.components.tab.toolbar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.typography
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.semiBold

/**
 * Groups the entries of a "⋮" menu: a small heading ("AVEC LA LISTE"), or a
 * plain line when [titleId] is `null`. Nothing in the toolbar itself. [key]
 * lets a screen add its own entries right after a heading.
 */
class MenuHeader( val key: String, @StringRes private val titleId: Int? ) : Button {

    @Composable
    override fun ToolBarButton() {}

    @Composable
    fun Render() {
        if( titleId == null )
            HorizontalDivider( Modifier.padding( vertical = 6.dp ), color = colorPalette().textDisabled.copy( alpha = .3f ) )
        else
            BasicText(
                text = stringResource( titleId ).uppercase(),
                style = typography().xxs.semiBold.color( colorPalette().textSecondary )
                                    .copy( letterSpacing = TextUnit( .08f, TextUnitType.Em ) ),
                modifier = Modifier.fillMaxWidth().padding( start = 24.dp, top = 12.dp, bottom = 4.dp )
            )
    }
}

/** Puts [button] right after the [MenuHeader] whose key is [key] (at the end if none) */
fun MutableList<Button>.addAfter( key: String, button: Button ) {
    val at = indexOfFirst { it is MenuHeader && it.key == key }
    if( at < 0 ) add( button ) else add( at + 1, button )
}
