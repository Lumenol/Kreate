package app.kreate.di

import java.net.UnknownHostException
import app.kreate.android.utils.ProxyManager
import app.kreate.android.BuildConfig
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.enums.DohServer
import app.kreate.logging.OkHttpLogger
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import me.knighthat.innertube.Constants
import me.knighthat.utils.Toaster
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module
import java.net.InetAddress
import java.util.concurrent.TimeUnit


private const val LOGGING_TAG = "Networking"

@ExperimentalSerializationApi
private val JSON: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
    // YouTube sometimes sends `null` (or an unknown enum value) for a field
    // the models don't expect it for: fall back to its default instead of failing
    coerceInputValues = true

    // Exclude ("type": "me.knighthat.innertube.*")
    // since there's no intention to deserialize json
    // string back to the class
    classDiscriminatorMode = ClassDiscriminatorMode.NONE
}

/** Blocking: call it off the main thread ("Test DoH" setting) */
fun verifyDoH(): Boolean {
    val (resolver, addresses) = buildDoH() ?: return false
    return verifyDoH( resolver, addresses )
}

private fun buildDoH(): Pair<DnsOverHttps, List<InetAddress>>? {
    if( Preferences.DOH_SERVER.value == DohServer.NONE ) return null

    val url = Preferences.DOH_SERVER.value.url!!        // Cannot be null if other than NONE
    // IP literals: no lookup happens here
    val addresses = Preferences.DOH_SERVER.value.address.map( InetAddress::getByName )
    val resolver = DnsOverHttps.Builder()
                               // Short: a lookup that fails falls back to the system's DNS,
                               // and an unreachable server mustn't hold every connection 10 s
                               .client(
                                   OkHttpClient.Builder()
                                               .connectTimeout( 2, TimeUnit.SECONDS )
                                               .readTimeout( 2, TimeUnit.SECONDS )
                                               .build()
                               )
                               .url( url )
                               .bootstrapDnsHosts( addresses )
                               .build()
    return resolver to addresses
}

private fun verifyDoH( resolver: DnsOverHttps, addresses: List<InetAddress>, domain: String = "google.com" ): Boolean =
    runCatching {
        val results = resolver.lookup( domain )
        Logger.d( tag = LOGGING_TAG ) { "Resolved $domain to ${results.size} addresses" }

        return results.isNotEmpty()
    }.onFailure { err ->
        // Failed to resolve "google.com" with [/1.1.1.1, /1.0.0.1, /2606:4700:4700::1111, /2606:4700:4700::1001]
        Logger.e( err, LOGGING_TAG ) { "Failed to resolve \"$domain\" with $addresses" }
        Toaster.w( R.string.error_failed_to_verify_doh )
    }.getOrDefault( false )

actual val networkModule: Module = module {
    factory<Dns> {
        val (resolver, _) = buildDoH() ?: run {
            Logger.d( tag = LOGGING_TAG ) { "DoH is not enabled. Using system's DNS" }
            return@factory Dns.SYSTEM
        }
        // No check up front (it blocked the main thread at startup): when the
        // DoH server fails, the system's DNS answers instead
        Dns { hostname ->
            try {
                resolver.lookup( hostname )
            } catch( err: UnknownHostException ) {
                Logger.w( err, LOGGING_TAG ) { "DoH failed for $hostname, using the system's DNS" }
                Dns.SYSTEM.lookup( hostname )
            }
        }
    }

    single {
        val interceptor = HttpLoggingInterceptor(OkHttpLogger())
        interceptor.setLevel(
            if( BuildConfig.DEBUG )
                HttpLoggingInterceptor.Level.BODY
            else
                // Production doesn't need full body to be logged because
                // it may contain user's credential(s).
                // Basic request's destination and response code is enough
                HttpLoggingInterceptor.Level.BASIC
        )

        OkHttpClient.Builder()
                    .proxySelector( ProxyManager.proxySelector )
                    .dns( get() )
                    .addInterceptor( interceptor )
                    .build()
    }
    single {
        HttpClient(OkHttp) {
            expectSuccess = true

            @OptIn(ExperimentalSerializationApi::class)
            install( ContentNegotiation ) {
                protobuf()
                json( JSON )
            }

            install( ContentEncoding ) {
                gzip( 1f )
                deflate( 0.9F )
            }

            @OptIn(ExperimentalSerializationApi::class)
            install(WebSockets ) {
                contentConverter = KotlinxWebsocketSerializationConverter(JSON)
            }

            engine {
                preconfigured = get()
            }

            defaultRequest {
                url( Constants.YOUTUBE_MUSIC_URL )
                contentType( ContentType.Application.Json )

                url {
                    parameters.append("prettyPrint", "false")
                }
            }
        }
    }
}