package com.Anchored.mylife.data.repository

import android.content.Context
import com.Anchored.mylife.data.backup.BackupManager
import com.Anchored.mylife.data.database.DatabaseProvider
import com.Anchored.mylife.data.settings.AppSettings

/**
 * Repository 统一入口，ViewModel 里这样用：
 *
 *   class AchievementListViewModel(application: Application) : AndroidViewModel(application) {
 *       private val repositories = RepositoryProvider.get(application)
 *       private val achievementRepository = repositories.achievementRepository
 *   }
 */
class RepositoryProvider private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = DatabaseProvider.getDatabase(appContext)

    val achievementRepository: AchievementRepository by lazy {
        AchievementRepository(database.achievementDao())
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(database.noteDao())
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(database.mediaDao())
    }

    /** 预设成就图鉴（109 条） */
    val presetAchievementRepository: PresetAchievementRepository by lazy {
        PresetAchievementRepository(database.presetAchievementDao())
    }

    /** 数据备份 / 恢复 */
    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database)
    }

    /** 应用设置（深色模式等偏好），存在 SharedPreferences 里 */
    val settings: AppSettings by lazy {
        AppSettings(appContext)
    }

    companion object {
        @Volatile
        private var instance: RepositoryProvider? = null

        fun get(context: Context): RepositoryProvider {
            return instance ?: synchronized(this) {
                instance ?: RepositoryProvider(context).also { instance = it }
            }
        }
    }
}
