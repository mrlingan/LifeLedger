package com.Anchored.mylife.data.preset

import android.content.Context
import com.Anchored.mylife.data.database.AchievementDatabase
import com.Anchored.mylife.data.database.PresetAchievement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * 预设成就导入器。
 *
 * 数据源：app/src/main/assets/preset_achievements.json
 * 时机：DatabaseProvider 第一次拿到数据库实例时（也就是 App 首次启动）
 *
 * 行为：
 * - 已经导入过就跳过（记录在 SharedPreferences 里），所以用户删掉的预设不会又被塞回来；
 * - 解析失败也不会让 App 崩溃，只是这一次不导入。
 *
 * 想重新导入：清掉 App 数据，或者把 SharedPreferences 里的 preset_achievements_seeded 置为 false。
 */
object PresetAchievementSeeder {

    private const val ASSET_NAME = "preset_achievements.json"
    private const val PREFS_NAME = "lifeledger_prefs"
    private const val KEY_SEEDED = "preset_achievements_seeded"

    suspend fun seedIfNeeded(context: Context, database: AchievementDatabase) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_SEEDED, false)) return

        val dao = database.presetAchievementDao()
        if (dao.count() > 0) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            return
        }

        val items = withContext(Dispatchers.IO) { readFromAssets(context) }
        if (items.isEmpty()) return

        dao.insertAll(items)
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private fun readFromAssets(context: Context): List<PresetAchievement> = runCatching {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val result = ArrayList<PresetAchievement>(array.length())

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            result += PresetAchievement(
                id = item.getLong("id"),
                title = item.getString("title"),
                description = item.optString("desc"),
                story = item.optString("story"),
                category = item.optString("category"),
                rarity = item.optString("rarity"),
                rate = item.optDouble("rate", 0.0),
                iconEmoji = "",
                isUnlocked = false,
                unlockedDate = null
            )
        }
        result
    }.getOrElse { emptyList() }
}
