package app.kreate.android.themed.common.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.kreate.android.R
import app.kreate.android.themed.common.component.settings.SettingComponents
import app.kreate.android.themed.common.component.settings.SettingEntrySearch
import app.kreate.android.themed.common.component.settings.entry
import app.kreate.android.themed.common.component.settings.header

/** The YouTube addresses Kreate opens (AndroidManifest): each one is ticked in Android's settings */
private val YOUTUBE_HOSTS = listOf( "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be" )

/**
 * The hosts Android lets Kreate open when a link is touched. Since Android 12 a link
 * goes to the app owning its domain (YouTube) unless the user adds Kreate for it:
 * no app can do it for itself. `null` before Android 12, where Android asks each time.
 */
private fun enabledHosts( context: Context ): Set<String>? {
    if( Build.VERSION.SDK_INT < Build.VERSION_CODES.S ) return null
    val state = context.getSystemService<DomainVerificationManager>()
                       ?.getDomainVerificationUserState( context.packageName )
                       ?: return emptySet()
    return state.hostToStateMap
                .filterValues { it != DomainVerificationUserState.DOMAIN_STATE_NONE }
                .keys
}

/** Opens the page where the links are added: Kreate's "Open by default" */
private fun openLinkSettings( context: Context ) {
    val uri = "package:${context.packageName}".toUri()
    val intent = if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S )
        Intent( Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri )
    else
        Intent( Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri )
    context.startActivity( intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK ) )
}

/**
 * "Open YouTube links with Kreate": says how many of the 5 YouTube addresses Android
 * gives to Kreate, and opens the page to add the others. Read again when coming back
 * from Android's settings.
 */
fun LazyListScope.openLinksSection( search: SettingEntrySearch ) {
    header( R.string.open_links )
    entry( search, R.string.open_links_with_kreate ) {
        val context = LocalContext.current
        var enabled by remember { mutableStateOf( enabledHosts( context ) ) }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect( lifecycleOwner ) {
            val observer = LifecycleEventObserver { _, event ->
                if( event == Lifecycle.Event.ON_RESUME ) enabled = enabledHosts( context )
            }
            lifecycleOwner.lifecycle.addObserver( observer )
            onDispose { lifecycleOwner.lifecycle.removeObserver( observer ) }
        }

        val hosts = enabled
        val subtitle = when {
            hosts == null -> stringResource( R.string.open_links_asked )
            YOUTUBE_HOSTS.all { it in hosts } -> stringResource( R.string.open_links_all )
            YOUTUBE_HOSTS.none { it in hosts } -> stringResource( R.string.open_links_none )
            else -> stringResource(
                R.string.open_links_some,
                YOUTUBE_HOSTS.count { it in hosts },
                YOUTUBE_HOSTS.size,
                YOUTUBE_HOSTS.filterNot { it in hosts }.joinToString( ", " )
            )
        }

        SettingComponents.Text(
            title = stringResource( R.string.open_links_with_kreate ),
            subtitle = subtitle,
            onClick = { openLinkSettings( context ) }
        )
    }
}
