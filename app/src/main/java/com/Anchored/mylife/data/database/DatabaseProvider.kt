package com.Anchored.mylife.data.database

import android.content.Context
import androidx.room.Room
import com.Anchored.mylife.data.preset.PresetAchievementSeeder
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseProvider {
    private const val DATABASE_NAME = "achievements.db"

    @Volatile
    private var instance: AchievementDatabase? = null

    /** 数据库文件的绝对路径。「数据安全」页要如实地把它显示给用户。 */
    fun databaseFile(context: Context): File =
        context.applicationContext.getDatabasePath(DATABASE_NAME)

    fun getDatabase(context: Context): AchievementDatabase {
        return instance ?: synchronized(this) {
            val appContext = context.applicationContext
            val db = Room.databaseBuilder(
                appContext,
                AchievementDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    AchievementDatabase.MIGRATION_1_2,
                    AchievementDatabase.MIGRATION_2_3,
                    AchievementDatabase.MIGRATION_3_4
                )
                .build()
            instance = db

            // 首次启动时把 assets/preset_achievements.json 里的 109 条预设成就写进数据库
            CoroutineScope(Dispatchers.IO).launch {
                PresetAchievementSeeder.seedIfNeeded(appContext, db)
            }

            db
        }
    }
}
