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
import com.Anchored.mylife.data.dao.GrowthDao
import com.Anchored.mylife.data.dao.RewardDao

@Database(
    entities = [
        Achievement::class,
        Note::class,
        Media::class,
        PresetAchievement::class,
        PointTransaction::class,
        Goal::class,
        GoalTask::class,
        DailyEvent::class,
        RewardItem::class,
        RewardRedemption::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AchievementDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun noteDao(): NoteDao
    abstract fun mediaDao(): MediaDao
    abstract fun presetAchievementDao(): PresetAchievementDao
    abstract fun growthDao(): GrowthDao
    abstract fun rewardDao(): RewardDao

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

        /** v5 -> v6: append-only growth tables; no existing user row is altered. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `point_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `type` TEXT NOT NULL, `sourceId` TEXT, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_point_transactions_createdAt` ON `point_transactions` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_point_transactions_sourceId` ON `point_transactions` (`sourceId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `goal_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goalId` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `scheduledDate` INTEGER NOT NULL, `reward` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_tasks_goalId` ON `goal_tasks` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_tasks_scheduledDate` ON `goal_tasks` (`scheduledDate`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateKey` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `type` TEXT NOT NULL, `rarity` TEXT NOT NULL, `effect` TEXT NOT NULL, `reward` INTEGER NOT NULL, `penalty` INTEGER NOT NULL, `condition` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `isDrawn` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `isFailed` INTEGER NOT NULL, `resolvedAt` INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_events_dateKey` ON `daily_events` (`dateKey`)")
            }
        }

        /**
         * v6 -> v7：新增积分商城的两张表。
         *
         * 同样是纯新增：奖励目录与兑换记录都是新表，既有数据一条都不动。
         * 内置的六条奖励不在这里写，而是第一次进商城时按当时的语言落库
         * （见 RewardRepository.seedCatalogOnce）——迁移只负责把表建出来。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `reward_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL, `category` TEXT NOT NULL, `price` INTEGER NOT NULL, `isCustom` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_items_category` ON `reward_items` (`category`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `reward_redemptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rewardId` INTEGER NOT NULL, `title` TEXT NOT NULL, `icon` TEXT NOT NULL, `category` TEXT NOT NULL, `price` INTEGER NOT NULL, `redeemedAt` INTEGER NOT NULL, `refundedAt` INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_redemptions_rewardId` ON `reward_redemptions` (`rewardId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_redemptions_redeemedAt` ON `reward_redemptions` (`redeemedAt`)")
            }
        }
    }
}
