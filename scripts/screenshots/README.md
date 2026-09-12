# 界面截图流水线

README 里的 `docs/screenshots/zh|en/` 就是这套脚本拍出来的。它**只在模拟器上跑**：
清空的是模拟器里应用的数据，真机和用户自己的数据完全不受影响。

所有临时产物（数据库、照片、截图原图、UI dump）都落在 `scripts/screenshots/work/`，
这个目录不进版本库。

## 前置条件

- Android SDK + platform-tools（`adb` 在 PATH 里，或者设置 `ANDROID_HOME`）
- Python 3 + Pillow（`pip install pillow`）
- 一台已经跑起来的模拟器（默认 `emulator-5554`，可以用 `-Serial` 换）
- 模拟器上装好了应用（debug 包就行）

## 流程

### 1. 让应用回到「刚安装」的状态

```bash
adb -s emulator-5554 shell pm clear com.Anchored.mylife
adb -s emulator-5554 shell am start -n com.Anchored.mylife/.MainActivity
```

在界面上选「用 109 条预设成就开始」，等它写完，然后退出：

```bash
adb -s emulator-5554 shell am force-stop com.Anchored.mylife
```

### 2. 生成照片素材

```bash
python scripts/screenshots/make_photos.py scripts/screenshots/work/media
```

会生成 5 张抽象风景（卡片封面、详情页配图）和 1 张头像。**这些是程序画出来的，
不是真实照片**，只用于截图。

### 3. 取基线数据，再按语言播种

先把数据库连 WAL 一起拉下来：

```bash
adb -s emulator-5554 exec-out run-as com.Anchored.mylife cat databases/achievements.db     > scripts/screenshots/work/db/achievements.db
adb -s emulator-5554 exec-out run-as com.Anchored.mylife cat databases/achievements.db-wal > scripts/screenshots/work/db/achievements.db-wal
adb -s emulator-5554 exec-out run-as com.Anchored.mylife cat databases/achievements.db-shm > scripts/screenshots/work/db/achievements.db-shm
python scripts/screenshots/make_baseline.py scripts/screenshots/work/db/achievements.db
```

`make_baseline.py` 会把库退回「刚走完首次启动」的样子（只剩 109 条预设、没有笔记和媒体），
并把自增序列退回 109，这样中英两次播种拿到的是同一批 id。

```bash
python scripts/screenshots/seed_random.py scripts/screenshots/work/db/zh.db zh
python scripts/screenshots/seed_random.py scripts/screenshots/work/db/en.db en
```

`seed_random.py` 用固定随机种子把 109 条里的约三分之一标成已完成（日期随机散在过去一年多，
最近四天连着完成几条），再插入 6 条自建成就、13 条笔记、5 张配图。**中英两遍结构完全相同，
只有文案不同**——中文那遍会顺带把预设条目的标题/描述换成图鉴里的中文原文（成就表存的是
入库当时的文案，只有图鉴页才会按 `presetId` 现查多语言资源）。

如果要的是别的数据，改 `CUSTOM` / `NOTES` / `PROFILE` 三张表即可。

### 4. 写个人资料、推回模拟器

```bash
adb -s emulator-5554 exec-out run-as com.Anchored.mylife cat shared_prefs/lifeledger_settings.xml > scripts/screenshots/work/prefs.xml
python scripts/screenshots/set_prefs.py scripts/screenshots/work/prefs.xml zh /data/user/0/com.Anchored.mylife/files/profile/avatar_random.jpg

pwsh -File scripts/screenshots/push_demo.ps1 -Db scripts/screenshots/work/db/zh.db -Prefs scripts/screenshots/work/prefs.xml -WithMedia
```

`push_demo.ps1` 会先 force-stop 应用，再把数据库、偏好和照片写进去，最后删掉旧的
WAL/SHM——不这么做，Room 可能读到半新半旧的库。

切语言用系统级的「按应用设置语言」就行，不用点界面：

```bash
adb -s emulator-5554 shell cmd locale set-app-locales com.Anchored.mylife --locales zh
adb -s emulator-5554 shell am force-stop com.Anchored.mylife
adb -s emulator-5554 shell am start -n com.Anchored.mylife/.MainActivity
```

### 5. 拍

```bash
pwsh -File scripts/screenshots/shot.ps1 -Name zh_home -ScrollTop 3
```

`-ScrollTop N` 表示先往下滚 N 次（首页要从顶部开始，就传 3 次上滑回来）。
截图落在 `work/shots/<name>.png`，同时把 UI 树存成 `work/ui_<name>.xml`。

六张的名字固定为：`<lang>_home`、`<lang>_home_dark`、`<lang>_all`、`<lang>_detail`、
`<lang>_codex`、`<lang>_settings`。深色那张要先在「设置 → 主题」里切到深色，拍完切回来。

### 6. 缩放并写进仓库

```bash
python scripts/screenshots/process_shots.py scripts/screenshots/work/shots docs/screenshots
```

把 1080×2400 的原图等比缩到 600×1333 覆盖 `docs/screenshots/` 里的旧图。

## 拍完顺手验一下

这些脚本不能替你"看"图，但能把明显的错拦住：

```bash
python scripts/screenshots/check_final_shots.py docs/screenshots        # 尺寸 / 模式 / 是否重复
python scripts/screenshots/check_layout.py work/ui_zh_home.xml 已完成 进行中 总记录 坚持天数
python scripts/screenshots/dump_texts.py work/ui_zh_home.xml           # 整页文案，顺便查有没有密文漏到界面
python scripts/screenshots/find_node.py  work/ui_zh_home.xml 双语达人   # 某个文案的坐标
python scripts/screenshots/inspect_db.py work/db/zh.db                 # 库里的行数
```

`check_layout.py` 是专门为「核心数据四列居中」写的：它会比较每一列标签和数字的中心、
四个中心之间的间距，以及左右留白是否对称。

## 几个坑

- **日期是相对当天生成的**，所以重跑一遍，截图里的日期和上一条提交不会逐字一致；
  真正需要一致的是"结构"（哪些条目完成、几列数字、文案），这些由固定种子保证。
- **中文那遍必须最后跑**（或者跑完再把语言切回中文），否则成就表里会残留英文文案。
- 应用在**数据加密开关打开**的状态下，界面文案照旧是明文（读路径永远解密），
  所以开不开加密都不影响截图；但 `dump_texts.py` 会顺带检查有没有 `enc1:`/`enc2:` 漏到界面。
- 模拟器时区是 GMT 时，`seed_random.py` 的日界线按 UTC 算，和设备的 `startOfDay` 一致。
