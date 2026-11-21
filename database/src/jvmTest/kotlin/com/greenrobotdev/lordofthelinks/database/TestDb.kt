package com.greenrobotdev.lordofthelinks.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.database.room.LinkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun inMemoryDb(): LinkDatabase =
    Room.inMemoryDatabaseBuilder<LinkDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

fun link(id: String, createdAt: Long = 0L, title: String = id) =
    LinkEntity(id = id, title = title, url = "https://example.com/$id", description = "", isFavorite = false, createdAt = createdAt)

/**
 * Collects [flow] on a real thread (Room emits off the test dispatcher) into a channel, so a test can
 * receive the first emission, change the data, then receive the re-emission. Cancel via [scope].
 */
fun <T> CoroutineScope.emissionsOf(flow: Flow<T>): Channel<T> {
    val channel = Channel<T>(Channel.UNLIMITED)
    launch(Dispatchers.Default) { flow.collect { channel.send(it) } }
    return channel
}
