package me.knighthat.internal.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals


class SkipInvalidListSerializerTest {

    @Serializable
    private data class Item( val id: String, val count: Int )

    @Serializable
    private data class Container(
        @Serializable(with = SkipInvalidListSerializer::class)
        val items: List<Item> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun keepsValidItemsAndDropsInvalidOnes() {
        val container = json.decodeFromString<Container>(
            """{ "items": [ { "id": "a", "count": 1 }, { "id": "b" }, { "id": "c", "count": 3 } ] }"""
        )

        assertEquals( listOf( Item( "a", 1 ), Item( "c", 3 ) ), container.items )
    }

    @Test
    fun nonArrayGivesEmptyList() {
        val container = json.decodeFromString<Container>( """{ "items": { "id": "a" } }""" )

        assertEquals( emptyList<Item>(), container.items )
    }

    @Test
    fun missingListUsesDefault() {
        assertEquals( emptyList<Item>(), json.decodeFromString<Container>( "{}" ).items )
    }
}
