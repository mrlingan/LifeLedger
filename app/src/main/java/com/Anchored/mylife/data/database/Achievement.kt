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
     * 这条成就属于哪个分类。
     *
     * 从图鉴带过来时跟着图鉴条目的分类走；自己手写的成就可以自己挑一个，
     * 挑不到合适的还能在新建 / 编辑成就的那一栏当场新建一个。
     * 空字符串 = 没分类，首页的分类进度不计入它。
     *
     * 存的是**名字**而不是 id：分类本来就是用户自己起的名字，用名字当标识
     * 最直接，也免得为了一个标签再维护一张表、再多一层外键。
     */
    val category: String = "",
    /**
     * 这条成就对应图鉴里的哪一条（preset_achievements.id），自己手写的为 null。
     *
     * 图鉴里的「已达成」= 存在一条 presetId 相同、且 isCompleted 为 true 的成就。
     * 首页和图书共用这一份状态，所以两边永远对得上：
     * 在首页取消完成，图鉴里也会跟着变回未达成。
     */
    val presetId: Long? = null
)
