package com.Anchored.mylife.ui.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.media.MediaFileStore
import com.Anchored.mylife.data.profile.Gender
import com.Anchored.mylife.data.profile.MbtiType
import com.Anchored.mylife.data.repository.MediaRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.data.settings.HomeSectionEntry
import com.Anchored.mylife.ui.PresetTextResolver
import com.Anchored.mylife.ui.theme.RarityTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

/**
 * 演示数据生成器：给截图和试用凑一批「看起来像用了很久」的数据。
 *
 * 为什么不沿用外部脚本改数据库：那条路要连 adb、拉库、造数据、再推回去，
 * 换台电脑就得重来一遍。放在 App 里点两下就能重来，也不依赖电脑上的 Python。
 *
 * 数据是**固定种子**的假随机：看着像随手记的，但每次生成的结构完全一样，
 * 所以反复截图不会今天多一条、明天少一条。改 [SEED] 就是另一批数据。
 *
 * 写进去的东西：
 * - 22 条来自图鉴的成就（约 2/3 已完成），标题/描述按**当前语言**落库——
 *   列表和首页直接显示库里的文案，只有图鉴页会按 presetId 现查资源；
 * - 4 条自己写的成就（文案在 strings.xml 的 demo_custom_* 里）；
 * - 7 条笔记：图鉴那批用条目自带的「故事」当正文，读起来像本人写的；
 * - 4 张配图（程序画的抽象图，不是照片）+ 1 张头像；
 * - 个人资料、关注的分类、以及把首页板块恢复成全显示。
 *
 * 入口在「设置 → 开发者选项」，只对调试签名 / 可调试的包开放，判定见 [DemoAccess]。
 */
object DemoDataSeeder {

    /**
     * 随机种子。
     *
     * 固定住是为了让每次生成的数据一样：截图里的日期、封面、徽章不会变来变去。
     * 想换一批就改这个数。
     */
    private const val SEED = 20260214

    /** 从图鉴里挑多少条 */
    private const val PRESET_COUNT = 22

    /**
     * 给多少条成就写笔记。
     *
     * 固定条数而不是按比例随机：截图里「详情页有内容」这件事得稳定发生，
     * 也让 README 里那句「7 条笔记」是准的。
     */
    private const val NOTE_COUNT = 7

    /** 配图张数：正好够首页三张「最近解锁」卡片都用上封面，还多一张 */
    private const val PHOTO_COUNT = 4

    /** 最近几天连着完成（连续记录天数才不是 1） */
    private val RECENT_DAYS = listOf(0L, 1L, 2L, 3L)

    /** 演示数据里会生成多少条成就，设置页那句话要跟它对上 */
    val PREVIEW_COUNT: Int get() = PRESET_COUNT + CUSTOM.size

    /** 生成结果，用来给界面回一句话 */
    data class Summary(val achievements: Int, val completed: Int)

    /**
     * 一条准备写入的成就，附带它的笔记与配图。
     *
     * 先把要写的东西排好再落库：日期之间有约束（完成不能早于创建），
     * 排成一张表之后这些关系一眼能看全。
     */
    private data class Entry(
        val title: String,
        val description: String,
        val emoji: String,
        val presetId: Long?,
        /** 图鉴分类，用来挑「关注」哪两个分类；自建的为 null */
        val category: String? = null,
        val createdDate: Long,
        val completedDate: Long?,
        val note: String? = null,
        val photoSeed: Int? = null
    )

    suspend fun generate(context: Context, repositories: RepositoryProvider): Summary =
        withContext(Dispatchers.IO) {
            val random = Random(SEED)
            val resolver = PresetTextResolver(context.resources)
            val mediaStore = MediaFileStore(context)
            val today = startOfDay(System.currentTimeMillis())

            val presets = repositories.presetAchievementRepository.getAll()
            val entries = buildList {
                addAll(
                    presetEntries(
                        presets = presets,
                        resolver = resolver,
                        random = random,
                        today = today
                    )
                )
                addAll(customEntries(context, random, today))
            }

            var completed = 0
            for (entry in entries) {
                val achievementId = repositories.achievementRepository.createAchievement(
                    title = entry.title,
                    description = entry.description,
                    iconEmoji = entry.emoji,
                    presetId = entry.presetId,
                    createdDate = entry.createdDate
                )
                val completedDate = entry.completedDate
                if (completedDate != null) {
                    repositories.achievementRepository.markCompleted(achievementId, completedDate)
                    completed++
                }

                val note = entry.note
                if (note != null) {
                    val noteId = repositories.noteRepository.createNote(
                        achievementId = achievementId,
                        content = note,
                        createdDate = completedDate ?: entry.createdDate
                    )
                    val photoSeed = entry.photoSeed
                    if (photoSeed != null) {
                        val bitmap = abstractPhoto(Random(photoSeed))
                        val path = mediaStore.saveBitmap(bitmap)
                        bitmap.recycle()
                        repositories.mediaRepository.addMedia(
                            noteId = noteId,
                            filePath = path,
                            fileType = MediaRepository.FileType.IMAGE,
                            createdDate = completedDate ?: entry.createdDate
                        )
                    }
                }
            }

            writeProfile(context, repositories, random)
            writeFavorites(repositories, entries)
            // 首页板块可能被用户关掉过几段，截图要的是完整首页
            // 演示数据要的是"首页每一段都拍得到"，所以这里显式全开，
            // 而不是用 DEFAULT_ORDER——它现在是三段（最近解锁 / 记录起点默认关）
            repositories.settings.setHomeSections(
                HomeSection.entries.map { HomeSectionEntry.primary(it) }
            )

            Summary(achievements = entries.size, completed = completed)
        }

    /**
     * 清空成就数据（图鉴保留）。
     *
     * 笔记与媒体的**记录**由外键级联删掉，磁盘上的图得自己清：
     * 数据库删完行之后，媒体目录里的文件已经没有任何引用了。
     */
    suspend fun clear(context: Context, repositories: RepositoryProvider) =
        withContext(Dispatchers.IO) {
            repositories.achievementRepository.deleteAll()
            MediaFileStore(context).clear()
            repositories.profileImageStore.clear()
            repositories.iconImageStore.clear()
            repositories.homeImageStore.clear()
            // 配图表也要清干净：文件删了、路径还留着的话，
            // 下次加回自定义图片会指向一张不存在的图
            repositories.settings.homeSectionImages.value.keys.toList().forEach { id ->
                repositories.settings.setHomeSectionImage(id, null)
            }
            repositories.settings.setProfile(
                nickname = "",
                signature = "",
                avatarPath = null,
                avatarPreset = null
            )
            // 基础信息也一起清掉：演示数据留下的性别 / MBTI 不该在"清空数据"之后还挂着
            repositories.settings.setGender(null)
            repositories.settings.setMbti(null)
            repositories.settings.setFavoriteCategories(emptySet())
        }

    // -----------------------------------------------------------------------
    // 排数据
    // -----------------------------------------------------------------------

    /**
     * 图鉴那一批。
     *
     * 两条讲究：
     * - 分类分散着挑，首页那五个分类圆环才有进度，不会全挤在一个分类里；
     * - 最近完成的那几条按稀有度岔开（传说 / 铂金 / 金 / 银），
     *   首页三张「最近解锁」卡片上的徽章才不会一模一样。
     */
    private fun presetEntries(
        presets: List<PresetAchievement>,
        resolver: PresetTextResolver,
        random: Random,
        today: Long
    ): List<Entry> {
        if (presets.isEmpty()) return emptyList()

        // 最稀有的几条先占住：首页「最近解锁」那三张卡片的徽章因此是传说 / 铂金 / 金，
        // 而不是清一色的铜牌（图鉴里 109 条的达成率大部分都很高）
        val rarest = presets.sortedBy { it.rate }.take(RECENT_DAYS.size)
        val rest = pickAcrossCategories(
            presets = presets - rarest.toSet(),
            count = PRESET_COUNT - rarest.size,
            random = random
        )
        val picked = orderForRecent(rarest + rest, random)
        val completedCount = picked.size * 2 / 3
        val photoSeeds = List(PHOTO_COUNT) { random.nextInt() }

        return picked.mapIndexed { index, preset ->
            val text = resolver.textOf(preset)
            val completedDate = if (index < completedCount) completionDate(index, random, today) else null

            // 笔记：最近完成的那几条写，正文直接用图鉴的「故事」——
            // 那份文案本来就是第一人称的复盘，当笔记读毫不违和，还省了一套演示文案
            val withNote = completedDate != null && index < NOTE_COUNT

            Entry(
                title = text.title,
                description = text.description,
                emoji = emojiFor(preset.category, random),
                presetId = preset.id,
                category = preset.category,
                createdDate = createdDate(completedDate, random, today),
                completedDate = completedDate,
                note = text.story.takeIf { withNote }?.let(::shortenStory),
                photoSeed = photoSeeds.getOrNull(index)
            )
        }
    }

    /** 自己写的那几条：文案在资源里，两边语言各一份 */
    private fun customEntries(context: Context, random: Random, today: Long): List<Entry> =
        CUSTOM.mapIndexed { index, item ->
            val title = context.getString(item.titleRes)
            // 一半已完成：列表里两种状态都有，看着像真的在用
            val completedDate = if (index % 2 == 0) {
                today - random.nextLong(6, 90) * DAY_MILLIS
            } else {
                null
            }
            Entry(
                title = title,
                description = context.getString(item.descriptionRes),
                emoji = item.emoji,
                presetId = null,
                createdDate = createdDate(completedDate, random, today),
                completedDate = completedDate
            )
        }

    /** 完成时间：最近几天连着来，其余散在过去一年里 */
    private fun completionDate(index: Int, random: Random, today: Long): Long =
        if (index < RECENT_DAYS.size) {
            today - RECENT_DAYS[index] * DAY_MILLIS
        } else {
            today - random.nextLong(6, 400) * DAY_MILLIS
        }

    /**
     * 创建时间：一定早于完成时间。
     *
     * 跨度给得大（几十到几百天）：「从 X 开始记录」和详情页的时间轴才像一段真实的历史，
     * 而不是所有事都在同一周发生。
     */
    private fun createdDate(completedDate: Long?, random: Random, today: Long): Long {
        val gap = random.nextLong(20, 300) * DAY_MILLIS
        return if (completedDate != null) {
            completedDate - gap
        } else {
            today - gap - random.nextLong(0, 60) * DAY_MILLIS
        }
    }

    /**
     * 按分类轮着挑。
     *
     * 每轮从「还有货」的分类里各取一条，于是前十几条必然落在不同分类上；
     * 分类不够多时自然停在全部分类各一条。
     */
    private fun pickAcrossCategories(
        presets: List<PresetAchievement>,
        count: Int,
        random: Random
    ): List<PresetAchievement> {
        val buckets = presets
            .groupBy { it.category }
            .mapValues { (_, items) -> items.shuffled(random).toMutableList() }
            .toMutableMap()

        val result = ArrayList<PresetAchievement>(count)
        while (result.size < count) {
            val categories = buckets.filterValues { it.isNotEmpty() }.keys
            if (categories.isEmpty()) break
            categories.shuffled(random).forEach { category ->
                val bucket = buckets.getValue(category)
                if (bucket.isNotEmpty() && result.size < count) {
                    result += bucket.removeAt(0)
                }
            }
        }
        return result
    }

    /**
     * 把前几条排成「不同稀有度」。
     *
     * 列表靠前的会成为最近完成的（见 [completionDate]），也就是首页那三张卡片。
     * 从最稀有往下各取一条摆在前面，卡片上就是传说 / 铂金 / 金三种徽章。
     */
    private fun orderForRecent(
        picked: List<PresetAchievement>,
        random: Random
    ): List<PresetAchievement> {
        val byTier = picked.groupBy { RarityTier.fromRate(it.rate) }
        val head = RarityTier.entries
            .reversed()
            .mapNotNull { tier -> byTier[tier]?.random(random) }
        val recent = head.toSet()

        return head + picked.filterNot { it in recent }
    }

    /** 图鉴条目的「故事」当笔记正文；太长就截断，详情页里不至于一屏全是字 */
    private fun shortenStory(story: String): String {
        val trimmed = story.trim()
        return if (trimmed.length <= STORY_LIMIT) trimmed else trimmed.take(STORY_LIMIT) + "…"
    }

    private fun emojiFor(category: String, random: Random): String {
        val options = EMOJI_BY_CATEGORY[category] ?: DEFAULT_EMOJIS
        return options[random.nextInt(options.size)]
    }

    /** 个人资料：昵称、签名、一张画出来的头像，以及「基础信息」里的两项 */
    private fun writeProfile(
        context: Context,
        repositories: RepositoryProvider,
        random: Random
    ) {
        val nickname = context.getString(R.string.demo_profile_nickname)
        val signature = context.getString(R.string.demo_profile_signature)
        val bitmap = avatarImage(nickname, random)
        val path = repositories.profileImageStore.save(
            bitmap = bitmap,
            previousPath = repositories.settings.avatarPath.value
        )
        bitmap.recycle()
        repositories.settings.setProfile(
            nickname = nickname,
            signature = signature,
            avatarPath = path,
            // 演示数据画的就是一张图，所以内置头像那一栏留空
            avatarPreset = null
        )
        // 性别与 MBTI 是死的两个值，不跟着 random 走：演示数据每次生成都该是一个样子，
        // 截图和试用才比对得上
        repositories.settings.setGender(Gender.FEMALE)
        repositories.settings.setMbti(MbtiType.INFP)
    }

    /**
     * 关注的分类：挑已经解锁最多的两个。
     *
     * 首页和列表都会把它们排到最前面，于是首页那五个圆环里，
     * 排头两个是「真的在积累」的分类，而不是空环。
     */
    private fun writeFavorites(repositories: RepositoryProvider, entries: List<Entry>) {
        val completedPresetIds = entries
            .filter { it.completedDate != null }
            .mapNotNull { it.presetId }
            .toSet()
        if (completedPresetIds.isEmpty()) return

        val categories = entries
            .filter { it.presetId != null && it.presetId in completedPresetIds }
            .mapNotNull { it.category }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }

        if (categories.isNotEmpty()) {
            repositories.settings.setFavoriteCategories(categories.toSet())
        }
    }

    // -----------------------------------------------------------------------
    // 画图
    // -----------------------------------------------------------------------

    /**
     * 一张抽象配图：底色渐变 + 几团柔光。
     *
     * 这些色值不放进 theme/Color.kt：它们不是界面配色，而是**图像内容**，
     * 和用户自己拍的照片是一回事，只是这台机器上没有相册可用。
     */
    private fun abstractPhoto(random: Random): Bitmap {
        val palette = PHOTO_PALETTES[random.nextInt(PHOTO_PALETTES.size)]
        val width = 1440
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            palette[0], palette[1], Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        repeat(random.nextInt(4, 7)) {
            val radius = width * (0.18f + random.nextFloat() * 0.32f)
            val centerX = random.nextFloat() * width
            val centerY = random.nextFloat() * height
            val glow = palette[random.nextInt(palette.size)]
            paint.shader = RadialGradient(
                centerX, centerY, radius,
                glow and 0x00FFFFFF or (0x66 shl 24),
                glow and 0x00FFFFFF,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(centerX, centerY, radius, paint)
        }

        // 斜着一条细亮线：纯渐变的图看着太"塑料"，加一道痕就有点照片的意思
        paint.shader = null
        paint.strokeWidth = 3f
        paint.color = 0x33FFFFFF
        val offset = random.nextFloat() * height
        canvas.drawLine(0f, offset, width.toFloat(), offset * 0.4f, paint)

        return bitmap
    }

    /** 头像：强调色系的径向渐变 + 昵称首字，和没配头像时的占位是同一个路数 */
    private fun avatarImage(nickname: String, random: Random): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val palette = AVATAR_PALETTES[random.nextInt(AVATAR_PALETTES.size)]
        paint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            palette[0], palette[1], Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        val glyph = nickname.trim().firstOrNull()?.toString().orEmpty()
        if (glyph.isNotEmpty()) {
            paint.shader = null
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = size * 0.42f
            val baseline = size / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(glyph, size / 2f, baseline, paint)
        }
        return bitmap
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // -----------------------------------------------------------------------
    // 素材表
    // -----------------------------------------------------------------------

    private const val STORY_LIMIT = 140

    /** 自己写的成就：标题、描述都在资源里（中英各一份） */
    private data class Custom(val titleRes: Int, val descriptionRes: Int, val emoji: String)

    private val CUSTOM = listOf(
        Custom(R.string.demo_custom_1_title, R.string.demo_custom_1_desc, "🎹"),
        Custom(R.string.demo_custom_2_title, R.string.demo_custom_2_desc, "🏔️"),
        Custom(R.string.demo_custom_3_title, R.string.demo_custom_3_desc, "🍲"),
        Custom(R.string.demo_custom_4_title, R.string.demo_custom_4_desc, "📖")
    )

    /** 图鉴分类 → 图标，让列表里的图标看着是有意的，而不是一排默认奖杯 */
    private val EMOJI_BY_CATEGORY = mapOf(
        "生活" to listOf("🏡", "🍳", "🌿"),
        "职业" to listOf("💼", "📈", "🧑‍💻"),
        "兴趣" to listOf("🎨", "🎸", "📷"),
        "旅行" to listOf("✈️", "🏔️", "🗺️"),
        "成长" to listOf("🌱", "📚", "🧭"),
        "财务" to listOf("💰", "🏦", "🧾"),
        "技能" to listOf("🛠️", "🧩", "🎯"),
        "社交" to listOf("🥂", "🫂", "🎤"),
        "学业" to listOf("🎓", "📖", "🔬"),
        "健康" to listOf("💪", "🏃", "🧘"),
        "娱乐" to listOf("🎮", "🎬", "🎧"),
        "家庭" to listOf("🏠", "👨‍👩‍👧", "🍲"),
        "情感" to listOf("💗", "💌", "🌈"),
        "新手村" to listOf("🌟", "🐣", "✨")
    )

    private val DEFAULT_EMOJIS = listOf("🏆", "🌟", "🎉")

    private val PHOTO_PALETTES = listOf(
        listOf(0xFF0B1E3A.toInt(), 0xFF1F6F8B.toInt(), 0xFF64D2FF.toInt()),
        listOf(0xFF2B1B3D.toInt(), 0xFF7A3E6B.toInt(), 0xFFFF9F0A.toInt()),
        listOf(0xFF10261F.toInt(), 0xFF2F6B4F.toInt(), 0xFFD4B45A.toInt()),
        listOf(0xFF1A1A2E.toInt(), 0xFF3E4A89.toInt(), 0xFFA7B0BB.toInt())
    )

    /** 头像用强调色那一族的蓝，和 App 的强调色是一家 */
    private val AVATAR_PALETTES = listOf(
        listOf(0xFF007AFF.toInt(), 0xFF003A75.toInt()),
        listOf(0xFF64D2FF.toInt(), 0xFF0066CC.toInt())
    )
}
