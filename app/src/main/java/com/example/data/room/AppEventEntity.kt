package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_events")
data class AppEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventName: String,
    val category: String,
    val screen: String,
    val metadataJson: String = "{}",
    val timestampMs: Long = System.currentTimeMillis(),
    val sessionId: String
)
