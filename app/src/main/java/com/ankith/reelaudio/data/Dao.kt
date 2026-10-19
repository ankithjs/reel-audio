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

    @Query("SELECT * FROM clips")
    suspend fun allOnce(): List<Clip>

    @Query("SELECT * FROM clip_groups")
    suspend fun allLinks(): List<ClipGroup>

    @Query("SELECT id FROM clips WHERE status = 'READY' AND transcriptStatus IN (:statuses)")
    suspend fun idsWithTranscript(statuses: List<String>): List<Long>

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
        "SELECT g.id AS id, g.name AS name, " +
            "(SELECT COUNT(*) FROM clip_groups WHERE groupId = g.id) + " +
            "(SELECT COUNT(*) FROM link_groups WHERE groupId = g.id) AS count " +
            "FROM user_groups g ORDER BY g.name COLLATE NOCASE"
    )
    fun allWithCounts(): Flow<List<GroupWithCount>>

    @Query("SELECT * FROM user_groups")
    suspend fun allOnce(): List<Group>

    @Query("SELECT * FROM user_groups WHERE name = :name")
    suspend fun byName(name: String): Group?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: Group): Long

    @Query("UPDATE user_groups SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM user_groups WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clip_groups WHERE groupId = :id")
    suspend fun clearLinks(id: Long)

    @Query("DELETE FROM link_groups WHERE groupId = :id")
    suspend fun clearLinkMembers(id: Long)
}

@Dao
interface LinkDao {
    @Query("SELECT * FROM links ORDER BY createdAt DESC")
    fun all(): Flow<List<SavedLink>>

    @Query(
        "SELECT links.* FROM links JOIN link_groups ON links.id = link_groups.linkId " +
            "WHERE link_groups.groupId = :groupId ORDER BY links.createdAt DESC"
    )
    fun inGroup(groupId: Long): Flow<List<SavedLink>>

    @Query("SELECT * FROM links WHERE sourceClipId = :clipId ORDER BY createdAt")
    fun forClip(clipId: Long): Flow<List<SavedLink>>

    @Query("SELECT * FROM links")
    suspend fun allOnce(): List<SavedLink>

    @Query("SELECT * FROM links WHERE id = :id")
    suspend fun get(id: Long): SavedLink?

    @Query("SELECT * FROM links WHERE url = :url")
    suspend fun byUrl(url: String): SavedLink?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: SavedLink): Long

    @Update
    suspend fun update(link: SavedLink)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT groupId FROM link_groups WHERE linkId = :linkId")
    fun groupIdsFor(linkId: Long): Flow<List<Long>>

    @Query("SELECT * FROM link_groups")
    suspend fun allMemberships(): List<LinkGroup>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToGroup(link: LinkGroup)

    @Query("DELETE FROM link_groups WHERE linkId = :linkId AND groupId = :groupId")
    suspend fun removeFromGroup(linkId: Long, groupId: Long)

    @Query("DELETE FROM link_groups WHERE linkId = :linkId")
    suspend fun clearGroups(linkId: Long)
}
