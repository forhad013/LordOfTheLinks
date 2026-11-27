package com.greenrobotdev.lordofthelinks.database.room

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v5: links keep the page's preview image URL (og:image), fetched when the link is saved. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE links ADD COLUMN imageUrl TEXT")
    }
}

/**
 * v6: forget "page has no image" markers written while the fetcher couldn't read pages (it got
 * compressed HTML), so those links are fetched again. Data only, no schema change.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE links SET imageUrl = NULL WHERE imageUrl = ''")
    }
}

/** v7: reminder timestamps on links, and a category_tags junction table. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE links ADD COLUMN reminderAt INTEGER")
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS category_tags (
                id TEXT NOT NULL PRIMARY KEY,
                categoryId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE,
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
        """.trimIndent())
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_category_tags_categoryId ON category_tags(categoryId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_category_tags_tagId ON category_tags(tagId)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_category_tags_categoryId_tagId ON category_tags(categoryId, tagId)")
    }
}

/** Every real migration, in order. Both platform builders add these. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
