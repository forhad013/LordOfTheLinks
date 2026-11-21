package com.greenrobotdev.lordofthelinks.database

import com.greenrobotdev.lordofthelinks.database.room.Converters
import kotlin.test.Test
import kotlin.test.assertEquals

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun stringListRoundTrips() {
        val list = listOf("kotlin", "has \"quotes\", commas", "")
        assertEquals(list, converters.toStringList(converters.fromStringList(list)))
        assertEquals(emptyList(), converters.toStringList(converters.fromStringList(emptyList())))
    }

    @Test
    fun unreadableColumnBecomesEmptyList() {
        assertEquals(emptyList(), converters.toStringList("not json"))
        assertEquals(emptyList(), converters.toStringList(""))
    }
}
