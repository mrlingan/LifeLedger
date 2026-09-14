package com.Anchored.mylife.data.repository

import android.content.Context
import com.Anchored.mylife.data.backup.BackupManager
import com.Anchored.mylife.data.achievement.IconImageStore
import com.Anchored.mylife.data.home.HomeImageStore
import com.Anchored.mylife.data.database.DatabaseProvider
import com.Anchored.mylife.data.crypto.DataCipher
import com.Anchored.mylife.data.crypto.DataEncryptionMigration
import com.Anchored.mylife.data.crypto.EncryptedAchievementDao
import com.Anchored.mylife.data.crypto.EncryptedNoteDao
import com.Anchored.mylife.data.profile.ProfileImageStore
import com.Anchored.mylife.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    init {
        // 信封加密：启动时把数据密钥解出来缓存到内存，之后加解密不再碰 Keystore。
        // 放在后台线程做，避免第一次读列表时在主线程等 IPC。
        DataCipher.init(appContext)
        CoroutineScope(Dispatchers.IO).launch { DataCipher.warmUp() }
    }

    // 用户自己写的内容：读库时一律解密，写库时按设置里的开关决定加不加密。
    // 装饰在 DAO 这一层：仓库、备份、迁移都从这里过，上层一行都不用改。
    private val achievementDao = EncryptedAchievementDao(database.achievementDao()) {
        settings.dataEncryptionEnabled.value
    }
    private val noteDao = EncryptedNoteDao(database.noteDao()) {
        settings.dataEncryptionEnabled.value
    }

    /**
     * 明文迁移：用户打开加密开关时，把库里已有的明文行逐行改写成密文。
     *
     * 用的是原始 DAO，因为迁移要判断的正是"落盘的值长什么样"。
     */
    val dataEncryptionMigration: DataEncryptionMigration by lazy {
        DataEncryptionMigration(database.achievementDao(), database.noteDao())
    }

    val achievementRepository: AchievementRepository by lazy {
        AchievementRepository(achievementDao)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(noteDao)
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
        BackupManager(
            appContext,
            database,
            settings,
            profileImageStore,
            iconImageStore,
            homeImageStore
        )
    }

    /** 应用设置（深色模式等偏好），存在 SharedPreferences 里 */
    val settings: AppSettings by lazy {
        AppSettings(appContext)
    }

    /** 头像文件的私有副本 */
    val profileImageStore: ProfileImageStore by lazy {
        ProfileImageStore(appContext)
    }

    /** 自定义成就图标的私有副本 */
    val iconImageStore: IconImageStore by lazy {
        IconImageStore(appContext)
    }

    /** 首页自定义图片的私有副本 */
    val homeImageStore: HomeImageStore by lazy {
        HomeImageStore(appContext)
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
