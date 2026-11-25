package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AppEventEntity)

    @Query("SELECT * FROM app_events ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 60): Flow<List<AppEventEntity>>

    @Query("SELECT COUNT(*) FROM app_events")
    fun observeTotalEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_events WHERE eventName = :eventName")
    suspend fun getEventCountByName(eventName: String): Int

    @Query(
        """
        DELETE FROM app_events
        WHERE id NOT IN (
            SELECT id FROM app_events ORDER BY timestampMs DESC, id DESC LIMIT :keepCount
        )
        """
    )
    suspend fun pruneOldEvents(keepCount: Int = 500)

    @Query("DELETE FROM app_events")
    suspend fun clearAllEvents()
}
