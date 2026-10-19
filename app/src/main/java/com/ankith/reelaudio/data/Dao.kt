package com.ankith.reelaudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun all(): Flow<List<Clip>>

    @Query(
        "SELECT clips.* FROM clips JOIN clip_groups ON clips.id = clip_groups.clipId " +
            "WHERE clip_groups.groupId = :groupId ORDER BY clips.createdAt DESC"
    )
    fun inGroup(groupId: Long): Flow<List<Clip>>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun get(id: Long): Clip?

    @Query("SELECT * FROM clips WHERE sourceUrl = :url")
    suspend fun byUrl(url: String): Clip?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(clip: Clip): Long

    @Update
    suspend fun update(clip: Clip)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT groupId FROM clip_groups WHERE clipId = :clipId")
    fun groupIdsFor(clipId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToGroup(link: ClipGroup)

    @Query("DELETE FROM clip_groups WHERE clipId = :clipId AND groupId = :groupId")
    suspend fun removeFromGroup(clipId: Long, groupId: Long)

    @Query("DELETE FROM clip_groups WHERE clipId = :clipId")
    suspend fun clearGroups(clipId: Long)
}

@Dao
interface GroupDao {
    @Query(
        "SELECT g.id AS id, g.name AS name, COUNT(cg.clipId) AS count FROM user_groups g " +
            "LEFT JOIN clip_groups cg ON cg.groupId = g.id GROUP BY g.id ORDER BY g.name COLLATE NOCASE"
    )
    fun allWithCounts(): Flow<List<GroupWithCount>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: Group): Long

    @Query("UPDATE user_groups SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM user_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clip_groups WHERE groupId = :id")
    suspend fun clearLinks(id: Long)
}
