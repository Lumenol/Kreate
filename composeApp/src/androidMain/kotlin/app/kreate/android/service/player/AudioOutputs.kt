package app.kreate.android.service.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.kreate.android.Preferences
import app.kreate.android.R
import co.touchlab.kermit.Logger
import me.knighthat.utils.Toaster

/**
 * Where Kreate's own sound goes: the phone's default output, or one of the outputs
 * connected now (headphones, Bluetooth speaker, USB…). Only Kreate is moved, the rest
 * of the phone keeps Android's output.
 *
 * The choice is forgotten when its device goes away (switched off, out of range):
 * playback goes back to the default output, it never stays on a device that's gone.
 * After a restart it's applied again only if that device is there.
 */
object AudioOutputs {

    /** A connected output; [key] identifies the device across connections */
    data class Output( val id: Int, val type: Int, val name: String, val key: String )

    private val logger = Logger.withTag( "AudioOutputs" )

    /** Outputs connected now, the phone's speaker first */
    val outputs = mutableStateListOf<Output>()
    /** Kreate's own output; `null`: the phone's default one */
    var selected by mutableStateOf<Output?>( null )
        private set

    private var audioManager: AudioManager? = null
    private var player: StatefulPlayer? = null
    private var callback: AudioDeviceCallback? = null

    // Music outputs only: not the earpiece, calls (SCO) or internal routes
    private val MUSIC_TYPES = buildSet {
        add( AudioDeviceInfo.TYPE_BUILTIN_SPEAKER )
        add( AudioDeviceInfo.TYPE_BLUETOOTH_A2DP )
        add( AudioDeviceInfo.TYPE_WIRED_HEADSET )
        add( AudioDeviceInfo.TYPE_WIRED_HEADPHONES )
        add( AudioDeviceInfo.TYPE_USB_HEADSET )
        add( AudioDeviceInfo.TYPE_USB_DEVICE )
        add( AudioDeviceInfo.TYPE_LINE_ANALOG )
        add( AudioDeviceInfo.TYPE_LINE_DIGITAL )
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) add( AudioDeviceInfo.TYPE_HEARING_AID )
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {
            add( AudioDeviceInfo.TYPE_BLE_HEADSET )
            add( AudioDeviceInfo.TYPE_BLE_SPEAKER )
        }
    }

    /** From the player service: follows the outputs as they come and go */
    fun start( context: Context, player: StatefulPlayer ) {
        if( callback != null ) return
        this.player = player
        val manager = context.getSystemService( Context.AUDIO_SERVICE ) as? AudioManager ?: return
        audioManager = manager
        refresh( context )

        // A choice from a previous run, applied only if its device is here
        val saved = Preferences.AUDIO_OUTPUT_CHOICE.value
        if( saved.isNotBlank() ) {
            val output = outputs.firstOrNull { it.key == saved }
            if( output != null ) select( context, output ) else forget()
        }

        callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded( added: Array<AudioDeviceInfo> ) = refresh( context )

            override fun onAudioDevicesRemoved( removed: Array<AudioDeviceInfo> ) {
                val current = selected
                refresh( context )
                if( current != null && outputs.none { it.key == current.key } ) {
                    logger.i { "${current.name} is gone: back to the default output" }
                    select( context, null )
                    Toaster.i( context.getString( R.string.audio_output_gone, current.name ) )
                }
            }
        }
        manager.registerAudioDeviceCallback( callback, Handler( Looper.getMainLooper() ) )
    }

    fun stop() {
        callback?.let { audioManager?.unregisterAudioDeviceCallback( it ) }
        callback = null
        player = null
    }

    private fun refresh( context: Context ) {
        val devices = audioManager?.getDevices( AudioManager.GET_DEVICES_OUTPUTS ).orEmpty()
        val list = devices.filter { it.isSink && it.type in MUSIC_TYPES }
                          .map { device -> Output( device.id, device.type, labelOf( context, device ), keyOf( device ) ) }
                          // Bluetooth devices are often listed twice
                          .distinctBy { it.key }
                          .sortedBy { if( it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ) 0 else 1 }
        outputs.clear()
        outputs.addAll( list )
    }

    private fun keyOf( device: AudioDeviceInfo ): String {
        val address = if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) device.address.orEmpty() else ""
        return "${device.type}|$address|${device.productName}"
    }

    private fun labelOf( context: Context, device: AudioDeviceInfo ): String = when( device.type ) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> context.getString( R.string.audio_output_phone_speaker )
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> context.getString( R.string.audio_output_wired )
        else -> device.productName?.toString()?.takeIf { it.isNotBlank() } ?: context.getString( R.string.audio_output )
    }

    /** Sends Kreate's sound to [output]; `null`: back to the phone's default output */
    fun select( context: Context, output: Output? ) {
        val device = output?.let { o -> audioManager?.getDevices( AudioManager.GET_DEVICES_OUTPUTS )?.firstOrNull { it.id == o.id } }
        if( output != null && device == null ) {
            // Gone between the list and the touch
            refresh( context )
            select( context, null )
            return
        }
        player?.setPreferredAudioDevice( device )
        selected = output
        Preferences.AUDIO_OUTPUT_CHOICE.value = output?.key.orEmpty()
    }

    private fun forget() {
        selected = null
        Preferences.AUDIO_OUTPUT_CHOICE.value = ""
    }
}
