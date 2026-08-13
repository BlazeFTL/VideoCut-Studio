package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedVideoDao {
    @Query("SELECT * FROM processed_videos ORDER BY timestamp DESC")
    fun getAllProcessedVideos(): Flow<List<ProcessedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessedVideo(video: ProcessedVideoEntity): Long

    @Query("DELETE FROM processed_videos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT SUM(originalSizeBytes - outputSizeBytes) FROM processed_videos WHERE originalSizeBytes > outputSizeBytes")
    fun getTotalBytesSaved(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM processed_videos")
    fun getTotalCount(): Flow<Int>
}
