package com.greenrobotdev.lordofthelinks.database.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_tags",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["tagId"]),
        Index(value = ["categoryId", "tagId"], unique = true)
    ]
)
data class CategoryTagEntity(
    @PrimaryKey
    val id: String,
    val categoryId: String,
    val tagId: String,
    val createdAt: Long = System.currentTimeMillis()
)
