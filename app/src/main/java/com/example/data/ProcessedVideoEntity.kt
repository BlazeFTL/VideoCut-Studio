package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_videos")
data class ProcessedVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val operationType: String, // "SINGLE_CUT", "MULTI_CUT", "COMPRESS"
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val durationMs: Long,
    val resolution: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
