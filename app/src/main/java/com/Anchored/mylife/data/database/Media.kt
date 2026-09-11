package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"])
    ]
)
data class Media(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val noteId: Long,
    /** 应用私有目录里的绝对路径（副本），不要存 content:// */
    val filePath: String,
    /** image / video / live_photo，见 MediaRepository.FileType */
    val fileType: String,
    val createdDate: Long,
    /** 实况照片内嵌的那段视频，解析出来后单独存一个文件；普通图片 / 视频为 null */
    val motionVideoPath: String? = null
)
