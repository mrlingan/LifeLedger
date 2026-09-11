package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val createdDate: Long,
    val completedDate: Long? = null,
    val isCompleted: Boolean = false,
    val iconEmoji: String,
    /**
     * 这条成就对应图鉴里的哪一条（preset_achievements.id），自己手写的为 null。
     *
     * 图鉴里的「已达成」= 存在一条 presetId 相同、且 isCompleted 为 true 的成就。
     * 首页和图书共用这一份状态，所以两边永远对得上：
     * 在首页取消完成，图鉴里也会跟着变回未达成。
     */
    val presetId: Long? = null
)
