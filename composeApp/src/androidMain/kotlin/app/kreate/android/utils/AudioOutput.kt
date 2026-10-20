package app.kreate.android.utils

import android.content.Context
import android.content.Intent
import android.media.MediaRouter
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

/**
 * Where the sound goes (phone, Bluetooth speaker, headphones…) and Android's own
 * picker to change it: connecting devices stays Android's job, Kreate only shows the
 * output and opens the picker without going through the notification shade.
 */
object AudioOutput {

    private val logger = Logger.withTag( "AudioOutput" )

    /** The name of the current output ("Phone", "JBL Flip"…), kept up to date */
    @Composable
    fun rememberName(): String {
        val context = LocalContext.current
        val router = remember { context.getSystemService( Context.MEDIA_ROUTER_SERVICE ) as? MediaRouter }
        fun current(): String =
            router?.getSelectedRoute( MediaRouter.ROUTE_TYPE_LIVE_AUDIO )?.getName( context )?.toString().orEmpty()

        var name by remember { mutableStateOf( current() ) }
        DisposableEffect( router ) {
            val callback = object : MediaRouter.SimpleCallback() {
                override fun onRouteSelected( router: MediaRouter, type: Int, info: MediaRouter.RouteInfo ) { name = current() }
                override fun onRouteChanged( router: MediaRouter, info: MediaRouter.RouteInfo ) { name = current() }
                override fun onRouteAdded( router: MediaRouter, info: MediaRouter.RouteInfo ) { name = current() }
                override fun onRouteRemoved( router: MediaRouter, info: MediaRouter.RouteInfo ) { name = current() }
            }
            router?.addCallback( MediaRouter.ROUTE_TYPE_LIVE_AUDIO, callback )
            onDispose { router?.removeCallback( callback ) }
        }
        return name
    }

    /**
     * Opens Android's output picker for Kreate: its own call since Android 14, the
     * System UI dialog on 12-13, the settings panel on 11; Bluetooth settings before.
     */
    fun showPicker( context: Context ) {
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    if( !MediaRouter2.getInstance( context ).showSystemOutputSwitcher() ) openBluetoothSettings( context )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    context.sendBroadcast(
                        Intent( "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG" )
                            .setPackage( "com.android.systemui" )
                            .putExtra( "package_name", context.packageName )
                    )

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    context.startActivity(
                        Intent( "com.android.settings.panel.action.MEDIA_OUTPUT" )
                            .putExtra( "com.android.settings.panel.extra.PACKAGE_NAME", context.packageName )
                            .addFlags( Intent.FLAG_ACTIVITY_NEW_TASK )
                    )

                else -> openBluetoothSettings( context )
            }
        }.onFailure {
            logger.w( it ) { "no output picker, Bluetooth settings instead" }
            runCatching { openBluetoothSettings( context ) }
        }
    }

    private fun openBluetoothSettings( context: Context ) =
        context.startActivity( Intent( Settings.ACTION_BLUETOOTH_SETTINGS ).addFlags( Intent.FLAG_ACTIVITY_NEW_TASK ) )
}
