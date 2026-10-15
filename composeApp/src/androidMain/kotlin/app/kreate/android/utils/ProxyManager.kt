package app.kreate.android.utils

import app.kreate.android.Preferences
import co.touchlab.kermit.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Proxy of the app's HTTP client, read from the settings for each connection.
 *
 * The client used to check the proxy when it was created, blocking the thread
 * that created it (the main one at startup) for up to 5 s, and kept that result
 * until the app restarted (#1535). Now nothing is checked on the way: with the
 * proxy on, connections go through it (never around it), and the "Test proxy"
 * setting checks it on demand. From Craeckie/Kreate d28d608c, simplified.
 */
object ProxyManager {

    private const val TAG = "ProxyManager"

    /** `null` when off, or when the settings can't make a proxy ("Direct" mode, port out of range...) */
    private fun configuredProxy(): Proxy? {
        if( !Preferences.IS_PROXY_ENABLED.value ) return null
        val type = Preferences.PROXY_SCHEME.value
        if( type == Proxy.Type.DIRECT ) return null
        val host = Preferences.PROXY_HOST.value.takeIf( String::isNotBlank ) ?: return null
        return runCatching {
            Proxy( type, InetSocketAddress.createUnresolved( host, Preferences.PROXY_PORT.value ) )
        }.onFailure { Logger.e( it, TAG ) { "invalid proxy settings" } }
         .getOrNull()
    }

    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select( uri: URI? ): List<Proxy> = listOf( configuredProxy() ?: Proxy.NO_PROXY )

        override fun connectFailed( uri: URI?, sa: SocketAddress?, ioe: IOException? ) {
            Logger.w( ioe, TAG ) { "connection to $uri through proxy $sa failed" }
        }
    }

    /**
     * Blocking: call it off the main thread.
     *
     * @return `true` when a request goes through the configured proxy
     */
    fun test(): Boolean {
        val proxy = configuredProxy() ?: return false
        return runCatching {
            OkHttpClient.Builder()
                        .proxy( proxy )
                        .connectTimeout( 3, TimeUnit.SECONDS )
                        .callTimeout( 5, TimeUnit.SECONDS )
                        .build()
                        .newCall( Request.Builder().head().url( "https://www.youtube.com/generate_204" ).build() )
                        .execute()
                        .use { it.isSuccessful }
        }.onFailure { Logger.e( it, TAG ) { "proxy $proxy unreachable" } }
         .getOrDefault( false )
    }
}
