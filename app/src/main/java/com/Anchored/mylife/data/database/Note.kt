package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Achievement::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["achievementId"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val achievementId: Long,
    val content: String,
    val createdDate: Long,
    val updatedDate: Long
)