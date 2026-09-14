package com.Anchored.mylife.data.backup

import android.content.Context
import android.net.Uri
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.AchievementDatabase
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.database.Note
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.achievement.AchievementIcon
import com.Anchored.mylife.data.achievement.IconImageStore
import com.Anchored.mylife.data.home.HomeImageStore
import com.Anchored.mylife.data.crypto.decrypted
import com.Anchored.mylife.data.crypto.encrypted
import com.Anchored.mylife.data.profile.ProfileImageStore
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.profile.Gender
import com.Anchored.mylife.data.profile.MbtiType
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.HomeSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 备份 / 恢复的结果概览，也用来在界面上显示数据统计 */
data class BackupSummary(
    val achievementCount: Int = 0,
    val noteCount: Int = 0,
    val mediaCount: Int = 0
)

/**
 * 数据备份与恢复。
 *
 * 备份文件是一个 zip：
 *   backup.json    全部结构化数据（成就、笔记、媒体记录、图鉴进度）
 *   media/xxx.jpg  图片 / 视频 / 实况照片的原文件
 *
 * 因为媒体文件在库里存的是绝对路径，导出时统一转成 media/文件名 这样的相对路径，
 * 导入时再还原成当前设备上的私有目录路径，换机之后也能对上。
 */
class BackupManager(
    private val context: Context,
    private val database: AchievementDatabase,
    private val settings: AppSettings,
    private val profileImages: ProfileImageStore,
    private val iconImages: IconImageStore,
    private val homeImages: HomeImageStore
) {

    private val mediaDir: File
        get() = File(context.filesDir, MEDIA_DIR_NAME)

    private val profileDir: File
        get() = File(context.filesDir, PROFILE_DIR_NAME)

    private val iconDir: File
        get() = File(context.filesDir, ICON_DIR_NAME)

    private val homeDir: File
        get() = File(context.filesDir, HOME_DIR_NAME)

    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(Date())
        return "lifeledger_backup_$stamp.zip"
    }

    suspend fun stats(): BackupSummary = withContext(Dispatchers.IO) {
        BackupSummary(
            achievementCount = database.achievementDao().countAchievements(),
            noteCount = database.noteDao().countNotes(),
            mediaCount = database.mediaDao().countMedia()
        )
    }

    /** 导出到用户选中的位置（SAF 返回的 Uri），不需要任何存储权限 */
    suspend fun exportTo(uri: Uri, passphrase: CharArray? = null): BackupSummary = withContext(Dispatchers.IO) {
        // 备份文件里必须是明文：换机之后要能被另一台设备的密钥重新加密。
        // 这一步与本地加密开关无关——开关关着时行本来就是明文，decrypted() 原样返回。
        val achievements = database.achievementDao().getAllAchievements().map { it.decrypted() }
        val notes = database.noteDao().getAllNotes().map { it.decrypted() }
        val mediaList = database.mediaDao().getAllMedia()
        val presets = database.presetAchievementDao().getAll()
        // 头像是文件，路径单独记，压缩包里放到 profile/ 下
        val avatarFile = settings.avatarPath.value?.let { File(it) }?.takeIf { it.isFile }
        // 自定义成就图标同理：校验一下确实在图标目录里，免得把别处的文件也打进包
        val iconFiles = achievements
            .mapNotNull { AchievementIcon.customPath(it.iconEmoji) }
            .map { File(it) }
            .filter { it.isFile && it.parentFile?.canonicalPath == iconDir.canonicalPath }
        // 首页那张自定义图片
        // 首页自定义图片可以有好几张（这个板块在白名单里，能重复添加），所以是一张表：
        // 板块实例 id → 文件。只带出确实躺在 files/home/ 里的那些，别的路径一律不碰
        val homeImageFiles = settings.homeSectionImages.value
            .mapValues { (_, path) -> File(path) }
            .filterValues { it.isFile && it.parentFile?.canonicalPath == homeDir.canonicalPath }

        val json = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put(
                "profile",
                JSONObject().apply {
                    put("nickname", settings.nickname.value)
                    put("signature", settings.signature.value)
                    put(
                        "avatar",
                        avatarFile?.let { "$PROFILE_DIR_NAME/${it.name}" } ?: JSONObject.NULL
                    )
                    // 内置头像：老备份里没有这一项，恢复时当作"没挑过"，
                    // 界面会回落到昵称首字
                    put("avatarPreset", settings.avatarPreset.value?.name ?: JSONObject.NULL)
                    // 资料页「基础信息」里的两枚选项：没设过就写 null，
                    // 恢复时读回 null，界面照旧显示「未设置」
                    put("gender", settings.gender.value?.name ?: JSONObject.NULL)
                    put("mbti", settings.mbti.value?.name ?: JSONObject.NULL)
                }
            )
            put("achievements", achievements.toAchievementJsonArray())
            put("notes", notes.toNoteJsonArray())
            put("media", mediaList.toMediaJsonArray())
            put("presets", presets.toPresetJsonArray())
            // 分类偏好（自建分类名 + 首页显示哪五个）。老备份里没有这一段，
            // 恢复时按"没有"处理：成就自己带的分类还在，清单会自动重建
            put(
                "categories",
                JSONObject().apply {
                    put(
                        "custom",
                        JSONArray().apply { settings.customCategories.value.forEach { put(it) } }
                    )
                    put(
                        "home",
                        JSONArray().apply { settings.homeCategories.value.forEach { put(it) } }
                    )
                    put(
                        "colors",
                        JSONObject().apply {
                            settings.categoryColors.value.forEach { (name, color) ->
                                put(name, color)
                            }
                        }
                    )
                }
            )
            put(
                "home",
                JSONObject().apply {
                    put(
                        "image",
                        // 老字段照写：旧版导入新备份时至少还认得出第一张
                        homeImageFiles[HomeSection.CUSTOM_IMAGE.name]
                            ?.let { "$HOME_DIR_NAME/${it.name}" } ?: JSONObject.NULL
                    )
                    put(
                        "images",
                        JSONObject().apply {
                            homeImageFiles.forEach { (id, file) ->
                                put(id, "$HOME_DIR_NAME/${file.name}")
                            }
                        }
                    )
                }
            )
        }

        val output = context.contentResolver.openOutputStream(uri)
            ?: error("无法写入所选位置")

        output.use { raw ->
            // 要加密就先套一层：头写完再往里写 zip，GCM 的校验段在关闭时补上
            val target = if (passphrase != null) {
                BackupCrypto.encryptedOutput(raw, passphrase)
            } else {
                raw
            }
            ZipOutputStream(BufferedOutputStream(target)).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                mediaList.forEach { media ->
                    addFileToZip(zip, media.filePath)
                    addFileToZip(zip, media.motionVideoPath)
                }

                avatarFile?.let { addFileToZip(zip, it.absolutePath, PROFILE_DIR_NAME) }

                iconFiles.forEach { addFileToZip(zip, it.absolutePath, ICON_DIR_NAME) }

                homeImageFiles.values.forEach { addFileToZip(zip, it.absolutePath, HOME_DIR_NAME) }
            }
        }

        BackupSummary(
            achievementCount = achievements.size,
            noteCount = notes.size,
            mediaCount = mediaList.size
        )
    }

    /** 从备份文件恢复，会先清空当前数据 */
    /**
     * 导入。
     *
     * 加密备份先在缓存目录里解成临时 zip，再走原来的流程；
     * 临时文件在 finally 里删掉，不管成功失败都不留在手机上。
     */
    suspend fun importFrom(uri: Uri, passphrase: CharArray? = null): BackupSummary =
        withContext(Dispatchers.IO) {
            if (!isEncrypted(uri)) return@withContext importFromSource(uri)

            val pass = passphrase ?: error("ENCRYPTED_BACKUP_NEEDS_PASSPHRASE")
            val temp = File(context.cacheDir, "lifeledger_import.zip")
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("无法读取所选文件")
                input.use { source ->
                    temp.outputStream().use { target ->
                        BackupCrypto.decrypt(source, target, pass)
                    }
                }
                importFromSource(Uri.fromFile(temp))
            } finally {
                temp.delete()
            }
        }

    /** 只读文件头几个字节，判断这份备份是不是加密的 */
    fun isEncrypted(uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(BackupCrypto.MAGIC.size)
            val read = input.read(header)
            read == header.size && BackupCrypto.isEncrypted(header)
        } ?: false
    }.getOrDefault(false)

    private suspend fun importFromSource(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        // 第一趟：只读 backup.json，先知道这份备份里都有什么
        val json = JSONObject(readJsonEntry(uri))

        val achievements = json.optJSONArray("achievements").toAchievementList()
        val notes = json.optJSONArray("notes").toNoteList()
        val mediaList = json.optJSONArray("media").toMediaList()
        val presets = json.optJSONArray("presets").toPresetList()
        val profile = json.optJSONObject("profile")

        // 清空旧数据。注意这一步会删掉旧的媒体文件，
        // 所以必须放在解包新文件之前，否则会把刚恢复出来的文件一起删掉。
        clearUserData()

        // 第二趟：把媒体文件解出来，此时目录已经是干净的
        extractMediaEntries(uri)

        if (achievements.isNotEmpty()) {
            database.achievementDao().insertAchievements(
                achievements.map { it.encrypted(settings.dataEncryptionEnabled.value) }
            )
        }
        if (notes.isNotEmpty()) {
            database.noteDao().insertNotes(
                notes.map { it.encrypted(settings.dataEncryptionEnabled.value) }
            )
        }
        if (mediaList.isNotEmpty()) {
            database.mediaDao().insertMediaList(mediaList)
        }
        if (presets.isNotEmpty()) {
            // 图鉴内容本身不用恢复，这里只把「已解锁」和图标状态覆盖回来
            database.presetAchievementDao().insertAll(presets)
        }

        restoreProfile(profile)
        restoreCategoryPreferences(json.optJSONObject("categories"))
        restoreHomeImage(json.optJSONObject("home"))

        BackupSummary(
            achievementCount = achievements.size,
            noteCount = notes.size,
            mediaCount = mediaList.size
        )
    }

    private suspend fun clearUserData() {
        database.mediaDao().deleteAllMedia()
        database.noteDao().deleteAllNotes()
        database.achievementDao().deleteAllAchievements()
        // 旧的媒体文件一并清掉，避免留下孤儿文件
        mediaDir.listFiles()?.forEach { it.delete() }
        profileImages.clear()
        iconImages.clear()
        homeImages.clear()
    }

    /**
     * 恢复个人资料。
     *
     * 老版本的备份里没有 profile 字段：这时昵称和签名保持不动，
     * 但头像路径必须清掉——上面已经把头像目录清空了，留着就是个死路径。
     * 性别与 MBTI 是后来才进包的，同理：包里没有就保持本机现在的值，
     * 只有包里明确写了（或者明确写了 null）才动它们。
     *
     * 头像的两个来源在包里各占一项：图片能解出来就用图片，内置头像只在没有图片时生效
     * ——和 [com.Anchored.mylife.data.settings.AppSettings.setProfile] 里那份"二选一"是同一个约定。
     */
    private fun restoreProfile(profile: JSONObject?) {
        if (profile == null) {
            settings.setProfile(
                nickname = settings.nickname.value,
                signature = settings.signature.value,
                avatarPath = null,
                avatarPreset = null
            )
            return
        }

        val relativeAvatar = if (profile.isNull("avatar")) {
            null
        } else {
            profile.optString("avatar").takeIf { it.isNotBlank() }
        }
        val avatarPath = relativeAvatar
            ?.let { File(context.filesDir, it) }
            ?.takeIf { it.isFile }
            ?.absolutePath

        val avatarPreset = if (profile.isNull("avatarPreset")) {
            null
        } else {
            AvatarPreset.fromName(profile.optString("avatarPreset").takeIf { it.isNotBlank() })
        }

        settings.setProfile(
            nickname = profile.optString("nickname"),
            signature = profile.optString("signature"),
            avatarPath = avatarPath,
            avatarPreset = avatarPreset.takeIf { avatarPath == null }
        )

        // 老备份里没有这两项：`has` 为 false 时一个字都不动，本机的选择留着
        if (profile.has("gender")) {
            settings.setGender(readProfileEnum(profile, "gender", Gender::fromName))
        }
        if (profile.has("mbti")) {
            settings.setMbti(readProfileEnum(profile, "mbti", MbtiType::fromName))
        }
    }

    /**
     * 读包里的一项枚举选项：JSON null（或空串）读成 null，也就是「未设置」。
     *
     * 认不出来的名字由 [fromName] 兜住，同样落回 null——手改过的备份不该让这一页崩掉。
     */
    private fun <T> readProfileEnum(
        json: JSONObject,
        key: String,
        fromName: (String?) -> T?
    ): T? = if (json.isNull(key)) null else fromName(json.optString(key).takeIf { it.isNotBlank() })

    /**
     * 恢复分类偏好。
     *
     * 自建分类名、首页显示哪五个、每个分类的圆环颜色都在这里。
     * 老备份没有这一段（返回 null）时什么都不动，成就自带的分类会让清单自动补回来。
     */
    private fun restoreCategoryPreferences(categories: JSONObject?) {
        if (categories == null) return

        categories.optJSONArray("custom")?.let { array ->
            val names = buildSet {
                for (index in 0 until array.length()) {
                    val name = array.optString(index).trim()
                    if (name.isNotEmpty()) add(name)
                }
            }
            settings.setCustomCategories(names)
        }

        categories.optJSONArray("home")?.let { array ->
            val names = buildList {
                for (index in 0 until array.length()) {
                    val name = array.optString(index).trim()
                    if (name.isNotEmpty()) add(name)
                }
            }
            settings.setHomeCategories(names)
        }

        categories.optJSONObject("colors")?.let { colors ->
            colors.keys().forEach { name ->
                settings.setCategoryColor(name, colors.optString(name).takeIf { it.isNotBlank() })
            }
        }
    }

    /**
     * 恢复首页的自定义图片。
     *
     * 新备份存的是 `images` 那张表（板块实例 id → 相对路径），因为首页可以有好几张图；
     * 老备份只有单个 `image` 字段，按"第一份自定义图片"接住。
     *
     * 两份都没有（json 里没有 home，或者用户从没上传过）→ 整张表清空：
     * 上面已经把图片目录清空了，继续留着一个绝对路径就是个死链。
     */
    private fun restoreHomeImage(home: JSONObject?) {
        settings.homeSectionImages.value.keys.toList().forEach { id ->
            settings.setHomeSectionImage(id, null)
        }
        if (home == null) return

        val images = home.optJSONObject("images")
        if (images != null) {
            images.keys().forEach { id ->
                val relative = images.optString(id).takeIf { it.isNotBlank() }
                if (relative != null) {
                    settings.setHomeSectionImage(id, File(context.filesDir, relative).absolutePath)
                }
            }
            return
        }

        // 老备份：单张图，当时它一定就是那一份「自定义图片」
        val relative = if (home.isNull("image")) {
            null
        } else {
            home.optString("image").takeIf { it.isNotBlank() }
        }
        val absolute = relative?.let { File(context.filesDir, it).absolutePath }
        if (absolute != null) settings.setHomeSectionImage(HomeSection.CUSTOM_IMAGE.name, absolute)
    }

    /** 只读 zip 里的 backup.json，读完就结束，不用把整包解出来 */
    private fun readJsonEntry(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")

        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == JSON_ENTRY) {
                        return zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        error("备份文件里没有 $JSON_ENTRY")
    }

    /** 把 zip 里 media/ 与 profile/ 下的文件解到应用私有目录 */
    private fun extractMediaEntries(uri: Uri) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")

        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val isContent = name.startsWith("$MEDIA_DIR_NAME/") ||
                        name.startsWith("$PROFILE_DIR_NAME/") ||
                        name.startsWith("$ICON_DIR_NAME/") ||
                        name.startsWith("$HOME_DIR_NAME/")
                    if (isContent && !entry.isDirectory) {
                        val target = File(context.filesDir, name)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun addFileToZip(
        zip: ZipOutputStream,
        path: String?,
        directory: String = MEDIA_DIR_NAME
    ) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.isFile) return
        zip.putNextEntry(ZipEntry("$directory/${file.name}"))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    // ---------- 导出用：实体 -> JSON ----------

    // 这几个扩展函数的名字必须各不相同：
    // 它们的接收者都是 List，泛型擦除后 JVM 签名相同，
    // 编译器为 lambda 访问私有成员生成的 access$ 桥接方法会撞名。
    private fun List<Achievement>.toAchievementJsonArray(): JSONArray = JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("createdDate", item.createdDate)
                    put("completedDate", item.completedDate ?: JSONObject.NULL)
                    put("isCompleted", item.isCompleted)
                    put("iconEmoji", relativeIconValue(item.iconEmoji))
                    put("category", item.category)
                    put("presetId", item.presetId ?: JSONObject.NULL)
                }
            )
        }
    }

    private fun List<Note>.toNoteJsonArray(): JSONArray = JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("achievementId", item.achievementId)
                    put("content", item.content)
                    put("createdDate", item.createdDate)
                    put("updatedDate", item.updatedDate)
                }
            )
        }
    }

    private fun List<Media>.toMediaJsonArray(): JSONArray = JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("noteId", item.noteId)
                    put("filePath", relativeMediaName(item.filePath))
                    put("fileType", item.fileType)
                    put("createdDate", item.createdDate)
                    put("motionVideoPath", item.motionVideoPath?.let { relativeMediaName(it) })
                }
            )
        }
    }

    private fun List<PresetAchievement>.toPresetJsonArray(): JSONArray = JSONArray().apply {
        forEach { item ->
            put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("story", item.story)
                    put("category", item.category)
                    put("rarity", item.rarity)
                    put("rate", item.rate)
                    put("iconEmoji", item.iconEmoji)
                    put("isUnlocked", item.isUnlocked)
                    put("unlockedDate", item.unlockedDate ?: JSONObject.NULL)
                }
            )
        }
    }

    private fun relativeMediaName(path: String): String = "$MEDIA_DIR_NAME/${File(path).name}"

    /**
     * 图标落进备份时的值。
     *
     * emoji 原样保留；自定义图片换成 `icons/文件名`——绝对路径在另一台设备上没意义，
     * 换机后私有目录的路径本来就不一样。只认图标目录里的文件，
     * 别的路径（万一被手改过）原样带走，恢复时至少还能看出它原来指向哪儿。
     */
    private fun relativeIconValue(icon: String): String {
        val file = AchievementIcon.customPath(icon)?.let { File(it) } ?: return icon
        if (file.parentFile?.canonicalPath != iconDir.canonicalPath) return icon
        return "$ICON_DIR_NAME/${file.name}"
    }

    /** 恢复时的反向翻译：`icons/文件名` → `file:<当前设备的绝对路径>` */
    private fun absoluteIconValue(value: String): String {
        if (!value.startsWith("$ICON_DIR_NAME/")) return value
        return AchievementIcon.custom(File(context.filesDir, value).absolutePath)
    }

    private fun absoluteMediaPath(relative: String?): String? {
        if (relative.isNullOrBlank()) return null
        return File(context.filesDir, relative).absolutePath
    }

    // ---------- 导入用：JSON -> 实体 ----------

    private fun JSONArray?.toAchievementList(): List<Achievement> {
        if (this == null) return emptyList()
        val result = ArrayList<Achievement>(length())
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            result += Achievement(
                id = item.optLong("id"),
                title = item.optString("title"),
                description = item.optString("description"),
                createdDate = item.optLong("createdDate"),
                completedDate = if (item.isNull("completedDate")) null else item.optLong("completedDate"),
                isCompleted = item.optBoolean("isCompleted"),
                iconEmoji = absoluteIconValue(item.optString("iconEmoji")),
                category = item.optString("category"),
                presetId = if (item.has("presetId") && !item.isNull("presetId")) {
                    item.optLong("presetId")
                } else {
                    null
                }
            )
        }
        return result
    }

    private fun JSONArray?.toNoteList(): List<Note> {
        if (this == null) return emptyList()
        val result = ArrayList<Note>(length())
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            result += Note(
                id = item.optLong("id"),
                achievementId = item.optLong("achievementId"),
                content = item.optString("content"),
                createdDate = item.optLong("createdDate"),
                updatedDate = item.optLong("updatedDate")
            )
        }
        return result
    }

    private fun JSONArray?.toMediaList(): List<Media> {
        if (this == null) return emptyList()
        val result = ArrayList<Media>(length())
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            val filePath = absoluteMediaPath(item.optString("filePath")) ?: continue
            result += Media(
                id = item.optLong("id"),
                noteId = item.optLong("noteId"),
                filePath = filePath,
                fileType = item.optString("fileType"),
                createdDate = item.optLong("createdDate"),
                motionVideoPath = absoluteMediaPath(
                    if (item.isNull("motionVideoPath")) null else item.optString("motionVideoPath")
                )
            )
        }
        return result
    }

    private fun JSONArray?.toPresetList(): List<PresetAchievement> {
        if (this == null) return emptyList()
        val result = ArrayList<PresetAchievement>(length())
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            result += PresetAchievement(
                id = item.optLong("id"),
                title = item.optString("title"),
                description = item.optString("description"),
                story = item.optString("story"),
                category = item.optString("category"),
                rarity = item.optString("rarity"),
                rate = item.optDouble("rate", 0.0),
                iconEmoji = item.optString("iconEmoji"),
                isUnlocked = item.optBoolean("isUnlocked"),
                unlockedDate = if (item.isNull("unlockedDate")) null else item.optLong("unlockedDate")
            )
        }
        return result
    }

    private companion object {
        const val BACKUP_VERSION = 1
        const val JSON_ENTRY = "backup.json"
        const val MEDIA_DIR_NAME = "media"
        const val PROFILE_DIR_NAME = ProfileImageStore.DIRECTORY_NAME
        const val ICON_DIR_NAME = IconImageStore.DIRECTORY_NAME
        const val HOME_DIR_NAME = HomeImageStore.DIRECTORY_NAME
    }
}
