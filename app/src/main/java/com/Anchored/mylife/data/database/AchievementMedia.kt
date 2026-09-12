package com.Anchored.mylife.data.database

/**
 * 首页「最近解锁」卡片用的一行：某条成就下的一张图片。
 *
 * media 表本身只记 noteId，这条查询把 notes 连上，拿到 achievementId。
 * 只取图片（视频和实况照片暂时不在这里当封面用）。
 */
data class AchievementMedia(
    val achievementId: Long,
    val filePath: String,
    val fileType: String
)
