package me.knighthat.internal.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder


/**
 * Decodes a list, dropping the elements that fail to decode instead of
 * failing the whole response.
 *
 * YouTube changes its renderers over time; with this, an item it changed
 * only disappears from its section instead of breaking the entire page.
 */
internal class SkipInvalidListSerializer<T>(
    private val elementSerializer: KSerializer<T>
) : KSerializer<List<T>> {

    private val delegate = ListSerializer( elementSerializer )

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize( encoder: Encoder, value: List<T> ) =
        delegate.serialize( encoder, value )

    override fun deserialize( decoder: Decoder ): List<T> {
        // Only JSON is ever decoded, but stay correct for any other format
        if( decoder !is JsonDecoder ) return delegate.deserialize( decoder )

        val array = decoder.decodeJsonElement() as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching {
                decoder.json.decodeFromJsonElement( elementSerializer, element )
            }.getOrNull()
        }
    }
}
