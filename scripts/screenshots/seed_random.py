"""Fill the LifeLedger database with random (but reproducible) demo data.

    python seed_random.py <db path> <zh|en>

同一个 SEED 下，中英两次运行得到的**结构完全相同**（同样哪些图鉴条目已完成、
同样的日期、同样的笔记位置），只有文案不同——这样两套截图展示的是同一个应用状态。

注意：成就表里存的是**入库那一刻的文案**（图鉴页才按 presetId 现查多语言资源），
所以中文这一遍要把预设条目的标题/描述换成 preset_achievements 里的中文原文；
英文那一遍从 baseline.db（英文界面下首次启动生成的）出发，标题本身就是英文。
"""

import hashlib
import random
import sqlite3
import sys
import time

DAY = 86_400_000
SEED = 20260912

# 自建成就（presetId 为 NULL 的那些），中英各 10 条，顺序一一对应
CUSTOM = {
    "zh": [
        ("把阳台改成小花园", "从配土换盆开始，现在有 11 盆还活着", "🪴"),
        ("学会做提拉米苏", "第一次就成功，手指饼干是自己烤的", "🍰"),
        ("跑完第一个半程马拉松", "2 小时 07 分，最后 3 公里全靠意志", "🏃"),
        ("带爸妈做一次全面体检", "提前一周预约，报告都正常", "🩺"),
        ("写满一整本手账", "365 页，一天没落下", "📓"),
        ("学会用胶片相机拍照", "拍废三卷，第四卷开始有感觉", "📷"),
        ("一个人去看海", "坐了六小时高铁，住了一晚青旅", "🌊"),
        ("换掉用了十年的沙发", "自己量尺寸、自己装起来", "🛋️"),
        ("学会自由泳换气", "从呛水到能连着游 500 米", "🏊"),
        ("把工作电脑彻底整理一遍", "归档、备份、清出 200G", "💻"),
    ],
    "en": [
        ("Turned the balcony into a small garden", "Mixed my own soil and repotted everything — 11 plants still alive", "🪴"),
        ("Learned to make tiramisu", "Worked the first time; I baked the ladyfingers myself", "🍰"),
        ("Ran my first half marathon", "2:07 — the last 3 km were pure willpower", "🏃"),
        ("Took my parents for a full health check", "Booked a week ahead, every result normal", "🩺"),
        ("Filled a whole journal", "365 pages, not a single day missed", "📓"),
        ("Learned to shoot film", "Ruined three rolls; the fourth finally had something", "📷"),
        ("Went to see the sea alone", "Six hours on the train, one night in a hostel", "🌊"),
        ("Replaced the ten-year-old sofa", "Measured it myself, assembled it myself", "🛋️"),
        ("Learned to breathe in freestyle", "From choking on water to 500 m without stopping", "🏊"),
        ("Cleaned up my work laptop properly", "Archived, backed up, freed 200 GB", "💻"),
    ],
}

NOTES = {
    "zh": [
        "今天风很大，但还是按计划做完了。",
        "比想象中难，中间有两次想放弃。",
        "记一笔：下次提前一周准备，不然太赶。",
        "和爸妈一起做这件事，比什么都值。",
        "照片是路上随手拍的，留个纪念。",
        "第一次手忙脚乱，第二次就顺了。",
        "下次想试试更难的版本。",
        "谢谢当时没有放弃的自己。",
    ],
    "en": [
        "Windy today, but I still finished what I planned.",
        "Harder than I expected — I nearly gave up twice.",
        "Note to self: start a week earlier next time.",
        "Doing this with my parents was worth everything.",
        "Snapped this on the way; keeping it here as a memento.",
        "The first attempt was chaos, the second one clicked.",
        "Next time I want to try the harder version.",
        "Grateful that past me did not quit.",
    ],
}

PROFILE = {
    "zh": ("小满", "慢慢来，比较快。"),
    "en": ("May", "Slow is smooth, smooth is fast."),
}

MEDIA_DIR = "/data/user/0/com.Anchored.mylife/files/media"
PROFILE_DIR = "/data/user/0/com.Anchored.mylife/files/profile"
AVATAR_FILE = "avatar_random.jpg"


def start_of_day(millis):
    # 模拟器时区是 GMT，日界线按 UTC 算，和设备的 startOfDay 保持一致
    return millis - millis % DAY


def main(db_path, lang):
    now = int(time.time() * 1000)
    today = start_of_day(now)
    # 任何写进去的时间都不能落在"现在"之后，否则界面会显示未来的时间
    def clamp(stamp):
        return min(stamp, now - 5 * 60_000)

    rng = random.Random(SEED)
    con = sqlite3.connect(db_path)
    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.execute("DELETE FROM media")
    con.execute("DELETE FROM notes")
    con.execute("DELETE FROM achievements WHERE presetId IS NULL")

    if lang == "zh":
        # 预设条目在图鉴表里存着中文原文，用它覆写成就表里的文案
        con.execute(
            "UPDATE achievements SET"
            " title = (SELECT title FROM preset_achievements WHERE id = achievements.presetId),"
            " description = (SELECT description FROM preset_achievements WHERE id = achievements.presetId)"
            " WHERE presetId IS NOT NULL"
        )

    preset_ids = [
        r[0]
        for r in con.execute(
            "SELECT id FROM achievements WHERE presetId IS NOT NULL ORDER BY id"
        )
    ]

    # 1) 最近四天连续完成，让「连续记录」不为零、首页有内容可放
    recent_four = rng.sample(preset_ids, 4)
    completed = {}
    for offset, achievement_id in enumerate(recent_four):
        if offset == 0:
            completed[achievement_id] = now - 2 * 3600_000 - rng.randint(0, 1800) * 1000
        else:
            completed[achievement_id] = (
                today - offset * DAY + 11 * 3600_000 + rng.randint(0, 3000) * 1000
            )

    # 2) 其余条目约三分之一在最近一年多里完成
    for achievement_id in preset_ids:
        if achievement_id in completed:
            continue
        if rng.random() < 0.34:
            completed[achievement_id] = (
                today - rng.randint(4, 420) * DAY + rng.randint(8, 22) * 3600_000
            )

    for achievement_id in preset_ids:
        done_at = completed.get(achievement_id)
        if done_at is None:
            created = today - rng.randint(1, 300) * DAY + rng.randint(7, 23) * 3600_000
            con.execute(
                "UPDATE achievements SET createdDate=?, completedDate=NULL, isCompleted=0 WHERE id=?",
                (created, achievement_id),
            )
        else:
            created = done_at - rng.randint(10, 180) * DAY
            con.execute(
                "UPDATE achievements SET createdDate=?, completedDate=?, isCompleted=1 WHERE id=?",
                (created, done_at, achievement_id),
            )

    # 3) 自建成就：6 条（前 4 条已完成、后 2 条进行中）
    picked_custom = rng.sample(range(len(CUSTOM[lang])), 6)
    custom_rows = []
    for index, pool_index in enumerate(picked_custom):
        title, description, icon = CUSTOM[lang][pool_index]
        if index < 4:
            done_at = today - rng.randint(4, 300) * DAY + rng.randint(8, 22) * 3600_000
            created = done_at - rng.randint(15, 150) * DAY
            cur = con.execute(
                "INSERT INTO achievements (title,description,createdDate,completedDate,isCompleted,iconEmoji,presetId)"
                " VALUES (?,?,?,?,1,?,NULL)",
                (title, description, created, done_at, icon),
            )
        else:
            created = today - rng.randint(3, 90) * DAY + rng.randint(8, 22) * 3600_000
            cur = con.execute(
                "INSERT INTO achievements (title,description,createdDate,completedDate,isCompleted,iconEmoji,presetId)"
                " VALUES (?,?,?,NULL,0,?,NULL)",
                (title, description, created, icon),
            )
        custom_rows.append((cur.lastrowid, title, index < 4))

    # 4) 笔记：均匀撒在已完成的条目上
    finished = [
        r[0]
        for r in con.execute(
            "SELECT id FROM achievements WHERE isCompleted=1 ORDER BY completedDate DESC"
        )
    ]
    note_targets = rng.sample(finished, min(len(NOTES[lang]), len(finished)))
    for position, achievement_id in enumerate(note_targets):
        text = NOTES[lang][position % len(NOTES[lang])]
        stamp = con.execute(
            "SELECT completedDate FROM achievements WHERE id=?", (achievement_id,)
        ).fetchone()[0] + rng.randint(1, 8) * 3600_000
        stamp = clamp(stamp)
        con.execute(
            "INSERT INTO notes (achievementId, content, createdDate, updatedDate) VALUES (?,?,?,?)",
            (achievement_id, text, stamp, stamp),
        )

    # 5) 封面图：最近完成的三条各挂一张（空笔记 + 一张图，取最早的一张当封面）
    for position, achievement_id in enumerate(recent_four[:3]):
        stamp = clamp(completed[achievement_id] - 60_000)
        cur = con.execute(
            "INSERT INTO notes (achievementId, content, createdDate, updatedDate) VALUES (?,?,?,?)",
            (achievement_id, "", stamp, stamp),
        )
        con.execute(
            "INSERT INTO media (noteId, filePath, fileType, createdDate, motionVideoPath)"
            " VALUES (?,?,'image',?,NULL)",
            (cur.lastrowid, f"{MEDIA_DIR}/photo_{position + 1}.jpg", stamp),
        )

    # 6) 详情页那条：两条带图的笔记（详情截图用）
    detail_id = recent_four[0]
    # 用最通用的两条文案：这两条会出现在详情截图里，不能带"和爸妈""海边"这类
    # 只适合某一条成就的措辞
    for order, photo in ((4, "photo_4.jpg"), (5, "photo_5.jpg")):
        stamp = clamp(completed[detail_id] + order * 3600_000)
        cur = con.execute(
            "INSERT INTO notes (achievementId, content, createdDate, updatedDate) VALUES (?,?,?,?)",
            (detail_id, NOTES[lang][order - 4], stamp, stamp),
        )
        con.execute(
            "INSERT INTO media (noteId, filePath, fileType, createdDate, motionVideoPath)"
            " VALUES (?,?,'image',?,NULL)",
            (cur.lastrowid, f"{MEDIA_DIR}/{photo}", stamp),
        )

    con.commit()

    rows = list(
        con.execute(
            "SELECT id, presetId, isCompleted, createdDate, completedDate FROM achievements"
            " ORDER BY id, presetId"
        )
    )
    digest = hashlib.sha256(repr(rows).encode()).hexdigest()[:16]
    done_count = con.execute("SELECT COUNT(*) FROM achievements WHERE isCompleted=1").fetchone()[0]
    days = {start_of_day(r[0]) for r in con.execute("SELECT completedDate FROM achievements WHERE completedDate IS NOT NULL")}
    streak = 0
    cursor = start_of_day(now)
    if cursor not in days:
        cursor -= DAY
    while cursor in days:
        streak += 1
        cursor -= DAY

    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.close()

    print(f"language      : {lang}")
    print(f"structure hash: {digest}")
    print(f"achievements  : {len(rows)}  (completed {done_count})")
    print(f"custom rows   : {[r[0] for r in custom_rows]}")
    print(f"recent four   : {recent_four}  -> detail target id={detail_id}")
    print(f"notes         : {len(note_targets) + 3 + 2}")
    print(f"media         : 5")
    print(f"streak        : {streak}  distinct completion days: {len(days)}")
    print(f"profile       : {PROFILE[lang][0]} / {PROFILE[lang][1]}")
    print(f"avatar path   : {PROFILE_DIR}/{AVATAR_FILE}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "en")
