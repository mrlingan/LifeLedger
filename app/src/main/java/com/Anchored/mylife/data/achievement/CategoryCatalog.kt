package com.Anchored.mylife.data.achievement

/**
 * 分类清单。
 *
 * 分类有三个来源，界面上看到的永远是三者的并集：
 * 1. 图鉴里已有的分类（内置，跟着 109 条预设一起来）
 * 2. 用户自己新建的（[com.Anchored.mylife.data.settings.AppSettings.customCategories]）
 * 3. 用户在自己的成就上写过的（成就的 category 字段）
 *
 * 第 3 条是关键：它让"自定义分类"不用非有一张自己的表——只要有一条成就挂着这个名字，
 * 这个分类就存在。备份恢复之后即使偏好文件没带过来，分类也不会凭空消失。
 *
 * 顺序：内置的在前（按条目数排），然后是用户新建的，最后是只在成就上出现过的。
 * 去重按名字，第一次出现的位置说了算。
 */
object CategoryCatalog {

    fun merge(
        presetCategories: Collection<String>,
        customCategories: Collection<String>,
        usedInAchievements: Collection<String>
    ): List<String> {
        val result = LinkedHashSet<String>()
        presetCategories.forEach { if (it.isNotBlank()) result += it }
        customCategories.sorted().forEach { if (it.isNotBlank()) result += it }
        usedInAchievements.sorted().forEach { if (it.isNotBlank()) result += it }
        return result.toList()
    }
}
