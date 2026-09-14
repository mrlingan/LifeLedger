package com.Anchored.mylife.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.Anchored.mylife.data.dao.AchievementDao
import com.Anchored.mylife.data.dao.MediaDao
import com.Anchored.mylife.data.dao.NoteDao
import com.Anchored.mylife.data.dao.PresetAchievementDao

@Database(
    entities = [
        Achievement::class,
        Note::class,
        Media::class,
        PresetAchievement::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AchievementDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun noteDao(): NoteDao
    abstract fun mediaDao(): MediaDao
    abstract fun presetAchievementDao(): PresetAchievementDao

    companion object {
        /**
         * v1 -> v2：新增预设成就图鉴表。
         * 老用户升级时保留原有成就数据，只补一张新表。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `preset_achievements` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `story` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `rarity` TEXT NOT NULL,
                        `rate` REAL NOT NULL,
                        `iconEmoji` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `unlockedDate` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v2 -> v3：media 表新增 motionVideoPath，存放实况照片里抽出来的那段视频。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `media` ADD COLUMN `motionVideoPath` TEXT")
            }
        }

        /**
         * v3 -> v4：成就表新增 presetId，用来和图书条目对应。
         * 图鉴的达成状态从此由成就表推导，不再单独存一份。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `achievements` ADD COLUMN `presetId` INTEGER")
            }
        }

        /**
         * v4 -> v5：成就表新增 category，用户自己写的成就也能挂一个分类。
         *
         * 默认空串（= 没分类），老数据一条都不会被动到；图鉴带过来的那些在
         * 创建时就写好了分类，首页的分类进度因此能把自建成就也算进去。
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `achievements` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
