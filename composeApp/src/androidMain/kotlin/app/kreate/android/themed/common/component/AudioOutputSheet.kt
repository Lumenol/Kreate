package app.kreate.android.themed.common.component

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.kreate.android.R
import app.kreate.android.service.player.AudioOutputs
import app.kreate.android.utils.AudioOutput
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.ui.components.themed.Menu
import it.fast4x.rimusic.ui.components.themed.MenuEntry

/**
 * Kreate's output panel: the outputs connected now, kept up to date as they come and
 * go, and the one Kreate plays on (ticked). Only Kreate's sound moves; "All apps…"
 * opens Android's picker for the whole phone, "Connect a device" Bluetooth settings.
 */
@Composable
fun AudioOutputSheet( onDone: () -> Unit ) {
    val context = LocalContext.current
    val systemName = AudioOutput.rememberName()
    val selected = AudioOutputs.selected

    @Composable
    fun Check( on: Boolean ) {
        if( on ) Icon( painterResource( R.drawable.checkmark ), null, tint = colorPalette().accent, modifier = Modifier.size( 18.dp ) )
    }

    Menu {
        MenuEntry(
            icon = R.drawable.volume_up,
            text = stringResource( R.string.audio_output_default ),
            secondaryText = systemName.takeIf { it.isNotBlank() },
            onClick = { AudioOutputs.select( context, null ); onDone() },
            trailingContent = { Check( selected == null ) }
        )
        AudioOutputs.outputs.forEach { output ->
            MenuEntry(
                icon = R.drawable.volume_up,
                text = output.name,
                onClick = { AudioOutputs.select( context, output ); onDone() },
                trailingContent = { Check( selected?.key == output.key ) }
            )
        }
        HorizontalDivider( color = colorPalette().background3 )
        MenuEntry(
            icon = R.drawable.settings,
            text = stringResource( R.string.audio_output_connect ),
            onClick = {
                onDone()
                runCatching {
                    context.startActivity( Intent( Settings.ACTION_BLUETOOTH_SETTINGS ).addFlags( Intent.FLAG_ACTIVITY_NEW_TASK ) )
                }
            }
        )
        MenuEntry(
            icon = R.drawable.link,
            text = stringResource( R.string.audio_output_all_apps ),
            secondaryText = stringResource( R.string.audio_output_all_apps_description ),
            onClick = { onDone(); AudioOutput.showPicker( context ) }
        )
    }
}
