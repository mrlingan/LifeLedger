package com.Anchored.mylife.data.repository

import android.content.Context
import android.net.Uri
import com.Anchored.mylife.data.backup.BackupManager
import com.Anchored.mylife.data.achievement.IconImageStore
import com.Anchored.mylife.data.background.BackgroundImageStore
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

    /**
     * 老版本的背景图存的是相册给的 content:// 地址（临时读取凭证，重启手机后读不出来），
     * 启动时把它复制一份进私有目录，换成稳定路径；复制失败就下次启动再试，偏好不动。
     *
     * **只能由 [get] 在构造完成之后调用。** 以前这一步写在 init 里直接 launch，协程
     * 会在属性初始化完成前抢跑，摸到 [settings] 时那个 `by lazy` 的委托字段还是
     * null，整个进程跟着挂掉——协程抢不抢得到是随机的，所以表现成"有时一进就闪退"
     * （日志里是 Lazy.getValue() 的 NPE，手机和模拟器都复现过）。
     */
    private fun migrateLegacyBackgroundImage() {
        CoroutineScope(Dispatchers.IO).launch {
            val legacy = settings.backgroundImagePath.value ?: return@launch
            if (!legacy.startsWith("content://")) return@launch
            val path = runCatching {
                backgroundImageStore.replace(Uri.parse(legacy), previousPath = null)
            }.getOrNull() ?: return@launch
            settings.setBackgroundImagePath(path)
        }
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
        AchievementRepository(achievementDao, pointService)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(noteDao)
    }

    val pointService: PointService by lazy { PointService(database, database.growthDao()) }
    val growthRepository: GrowthRepository by lazy { GrowthRepository(database, database.growthDao(), pointService) }

    /** 积分商城：奖励目录与兑换记录，花的是 [pointService] 那本流水账上的积分 */
    val rewardRepository: RewardRepository by lazy {
        RewardRepository(database, database.rewardDao(), pointService, settings)
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

    /** 全局背景图的私有副本 */
    val backgroundImageStore: BackgroundImageStore by lazy {
        BackgroundImageStore(appContext)
    }

    companion object {
        @Volatile
        private var instance: RepositoryProvider? = null

        fun get(context: Context): RepositoryProvider {
            return instance ?: synchronized(this) {
                instance ?: RepositoryProvider(context).also { created ->
                    instance = created
                    // 到这里构造函数已经跑完（属性初始化也结束了），协程才敢碰 settings
                    created.migrateLegacyBackgroundImage()
                }
            }
        }
    }
}
