package com.Anchored.mylife.data.backup

import android.content.Context
import android.net.Uri
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.AchievementDatabase
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.database.Note
import com.Anchored.mylife.data.database.PresetAchievement
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
    private val database: AchievementDatabase
) {

    private val mediaDir: File
        get() = File(context.filesDir, MEDIA_DIR_NAME)

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
    suspend fun exportTo(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val achievements = database.achievementDao().getAllAchievements()
        val notes = database.noteDao().getAllNotes()
        val mediaList = database.mediaDao().getAllMedia()
        val presets = database.presetAchievementDao().getAll()

        val json = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("achievements", achievements.toAchievementJsonArray())
            put("notes", notes.toNoteJsonArray())
            put("media", mediaList.toMediaJsonArray())
            put("presets", presets.toPresetJsonArray())
        }

        val output = context.contentResolver.openOutputStream(uri)
            ?: error("无法写入所选位置")

        output.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(json.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                mediaList.forEach { media ->
                    addFileToZip(zip, media.filePath)
                    addFileToZip(zip, media.motionVideoPath)
                }
            }
        }

        BackupSummary(
            achievementCount = achievements.size,
            noteCount = notes.size,
            mediaCount = mediaList.size
        )
    }

    /** 从备份文件恢复，会先清空当前数据 */
    suspend fun importFrom(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        // 第一趟：只读 backup.json，先知道这份备份里都有什么
        val json = JSONObject(readJsonEntry(uri))

        val achievements = json.optJSONArray("achievements").toAchievementList()
        val notes = json.optJSONArray("notes").toNoteList()
        val mediaList = json.optJSONArray("media").toMediaList()
        val presets = json.optJSONArray("presets").toPresetList()

        // 清空旧数据。注意这一步会删掉旧的媒体文件，
        // 所以必须放在解包新文件之前，否则会把刚恢复出来的文件一起删掉。
        clearUserData()

        // 第二趟：把媒体文件解出来，此时目录已经是干净的
        extractMediaEntries(uri)

        if (achievements.isNotEmpty()) {
            database.achievementDao().insertAchievements(achievements)
        }
        if (notes.isNotEmpty()) {
            database.noteDao().insertNotes(notes)
        }
        if (mediaList.isNotEmpty()) {
            database.mediaDao().insertMediaList(mediaList)
        }
        if (presets.isNotEmpty()) {
            // 图鉴内容本身不用恢复，这里只把「已解锁」和图标状态覆盖回来
            database.presetAchievementDao().insertAll(presets)
        }

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

    /** 把 zip 里 media/ 下的文件解到应用私有目录 */
    private fun extractMediaEntries(uri: Uri) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")

        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("$MEDIA_DIR_NAME/") && !entry.isDirectory) {
                        val target = File(mediaDir, File(name).name)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.isFile) return
        zip.putNextEntry(ZipEntry("$MEDIA_DIR_NAME/${file.name}"))
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
                    put("iconEmoji", item.iconEmoji)
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
                iconEmoji = item.optString("iconEmoji"),
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
    }
}
