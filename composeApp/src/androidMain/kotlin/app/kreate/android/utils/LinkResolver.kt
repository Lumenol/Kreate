package app.kreate.android.utils

import android.net.Uri
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.java.KoinJavaComponent.get

/** What shared or opened YouTube links need before Kreate can show them */
object LinkResolver {

    private val logger = Logger.withTag( "LinkResolver" )
    private val URL = Regex( """https?://\S+""" )

    /**
     * The link in a shared text: apps share "Listen to this: https://…", not the link
     * alone. The text itself when it has none (a plain link, or a search).
     */
    fun linkIn( text: String ): Uri =
        (URL.find( text )?.value?.trimEnd( '.', ',', ')', '»', '"', '\'' ) ?: text.trim()).toUri()

    /**
     * The channel id ("UC…") behind a channel link by name: "/@name", "/c/name",
     * "/user/name". YouTube resolves it (`navigation/resolve_url`); `null` when it can't.
     */
    suspend fun channelIdOf( link: Uri ): String? = withContext( Dispatchers.IO ) {
        runCatching {
            val body = buildJsonObject {
                putJsonObject( "context" ) {
                    putJsonObject( "client" ) {
                        put( "clientName", "WEB" )
                        put( "clientVersion", "2.20251015.00.00" )
                    }
                }
                // Always on www.youtube.com: music.youtube.com doesn't resolve these
                put( "url", "https://www.youtube.com${link.path.orEmpty()}" )
            }.toString()
            val request = Request.Builder()
                                 .url( "https://www.youtube.com/youtubei/v1/navigation/resolve_url?prettyPrint=false" )
                                 .post( body.toRequestBody( "application/json".toMediaType() ) )
                                 .build()
            get<OkHttpClient>( OkHttpClient::class.java ).newCall( request ).execute().use { response ->
                val json = Json.parseToJsonElement( response.body.string() ).jsonObject
                (json["endpoint"] as? JsonObject)
                    ?.get( "browseEndpoint" )?.jsonObject
                    ?.get( "browseId" )?.jsonPrimitive?.content
                    ?.takeIf { it.startsWith( "UC" ) }
            }
        }.onFailure { logger.w( it ) { "can't resolve the channel of $link" } }
         .getOrNull()
    }
}
