package com.greenrobotdev.lordofthelinks.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun assignTagToCategory(categoryTag: CategoryTagEntity)

    @Query("DELETE FROM category_tags WHERE categoryId = :categoryId AND tagId = :tagId")
    suspend fun removeTagFromCategory(categoryId: String, tagId: String)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN category_tags ct ON t.id = ct.tagId
        WHERE ct.categoryId = :categoryId
        ORDER BY t.name ASC
    """)
    fun getTagsForCategory(categoryId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM category_tags")
    fun getAllCategoryTags(): Flow<List<CategoryTagEntity>>

    @Query("DELETE FROM category_tags WHERE categoryId = :categoryId")
    suspend fun removeAllTagsFromCategory(categoryId: String)
}
