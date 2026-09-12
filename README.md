# LifeLedger · 人生账本

**简体中文** | [English](README.en.md)

> 把人生当成一款游戏，记录、整理、回顾你走过的每一步。

一个**完全离线**的 Android 个人成就记录应用。没有账号、没有服务器、没有埋点，所有数据只存在你自己的手机上。

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-4285F4)
![Room](https://img.shields.io/badge/Room-2.7.2-4285F4)
![minSdk](https://img.shields.io/badge/minSdk-24-orange)
![Offline](https://img.shields.io/badge/Offline-100%25-brightgreen)
![License](https://img.shields.io/badge/License-MIT-green)

> **不想自己编译？** 到 [Releases](https://github.com/mrlingan/LifeLedger/releases/latest) 直接下载 APK，装到手机上就能用。Android 7.0 及以上。

由 **[mrlingan](https://github.com/mrlingan)** 开发与维护。

---

## 这是什么

大部分「记录类」App 最后都会变成待办清单：加一条、划掉、忘记。

这个项目想做的事稍微不一样——它把**已经发生的事**当成成就来收藏，而不是把**还没做的事**当成压力列在屏幕上。所以：

- 首页是「人生仪表盘」，第一眼看到的是**你已经完成了多少**，而不是还剩多少没做
- 每一条成就都能写下当时的细节、附上照片和视频，成为一条可以回看的记录
- 内置一份 109 条的成就图鉴，可以像收集一样去解锁
- 完成时间可以手动指定——因为「什么时候完成的」本来就该由你说了算

---

## 功能

### 🚀 第一次打开

- 先问一句「从哪里开始」：**默认**把 109 条预设成就加进「我的成就」列表，之后一条条去达成；**自定义**则从空白开始，只记录自己写下的成就
- 两种选择都不影响图鉴——109 条随时可以浏览、挑选、解锁
- 已经在用的用户（升级上来的）不会被问第二次

### 📊 首页 · 人生仪表盘

- 问候语 + 日期 + 一句激励语（设了昵称和签名就显示你自己的），右上角是「记录成就」
- 配过昵称或头像之后，头像会出现在左上角，点一下直接进个人资料
- **人生进度**：阶段（每完成 5 条向上一个阶段）+ 进度条 + 百分比并排，下面写明距离下一阶段还差几条
- **核心数据**：已完成 / 进行中 / 总记录 / 坚持天数（附当前连续记录），一行四个数字，标签在上、数字在下
- **分类进度**：图鉴各分类的收集进度，一行五个小圆环 + 「查看全部」进图鉴
- **最近解锁**：最近 3 条做成卡片，带封面（配过图就显示那张图，没有就用标题首字）、名称、描述与稀有度；具体时间不在这里露面，点进详情才看
- 收尾一行「从 X 开始记录 · 已记录 N 天」
- 首页只做总览，**不放成就列表**：记录再多，首页的长度也不会变

### 🏆 成就管理 · 独立于首页

- **全部成就**：独立的列表页，支持「全部 / 已完成 / 进行中」筛选（筛选器固定在顶部），列表项右侧可一键切换完成状态
- 新建成就：可以从图鉴挑一条预填，也可以完全自己写（标题、描述、图标）
- 编辑与删除（删除会级联清理它下面的笔记和媒体）
- 标记完成 / 取消完成，**完成日期可以手动指定**（系统日历选择器）
- 标记完成的那一刻有一次触感反馈和一声轻音（`res/raw/complete.wav`）；取消完成保持安静，也不需要额外权限
- 成就详情页：大图标 Hero 区、解锁时间线、相关数据、记事本

### 📖 成就图鉴 · 人生经历档案馆

- 内置 **109 条**预设成就，覆盖成长、生活、旅行、学业、情感、娱乐、社交、兴趣、技能、职业、家庭、新手村、健康、财务 14 个分类
- 五档稀有度：青铜 / 白银 / 黄金 / 白金 / 传奇（按达成率自动推导）
- **档案索引式陈列**：徽记在左，右侧第一行是名称、第二行是介绍，再下面是稀有度；手机一列，宽屏自动变两列。未解锁的额外标出达成率，完成时间只在详情里显示
- **三重筛选**：关键词搜索、全部 / 已解锁 / 未解锁、按分类
- 已解锁用稀有度色描边的徽记和主文字色；未解锁保留名称、描述和达成率，整体降一档对比度——不做大锁，也不做问号
- 支持「从图鉴挑选」→ 自动预填标题、描述、图标到新建页
- **图鉴的达成状态与首页共用同一份数据**：在首页取消完成，图鉴会同步变回未解锁

### 📝 记事本与媒体

- 每条成就下可以写多条笔记，支持编辑、删除
- 笔记可以附图片、视频、**实况照片**
- 实况照片会自动解析出内嵌的那段视频并单独保存，长按图片即可播放一次
- 所有媒体文件都会复制进应用私有目录，不依赖相册原图

### 💾 数据备份

- 一键导出为 `.zip`：包含全部结构化数据、原始图片视频，以及个人资料（昵称、签名、头像）
- 导出时**可以给整份备份设一个口令**（PBKDF2-HMAC-SHA256 派生密钥 + AES-256-GCM）：留空就是普通 zip，设了口令则连文件名之外的一切都是密文；导入时识别到加密会自动索要口令
- 导入恢复，带二次确认（覆盖式恢复）
- 备份文件里的媒体路径是相对路径，**换机之后也能正确还原**

### ⚙️ 设置

- **成就设置**：新成就的默认图标、标记完成前的二次确认、关注的分类（关注的分类在首页和列表里排前面）
- **主题**：跟随系统 / 浅色 / 深色
- **语言**：跟随系统 / 简体中文 / English
- **显示**：列表密度（紧凑 / 标准 / 宽松）、字号缩放
- **动画**：完整 / 精简 / 关闭
- **备份与恢复**：导出、导入，以及当前数据量
- **每日提醒**：设定时间与频率（每天 / 工作日），到点用系统闹钟在本机弹一条通知；不引入 WorkManager
- **应用锁**：指纹、面容或锁屏密码；离开超过 30 秒自动重新锁定
- **数据安全**：数据存放位置、**本地加密开关**、备份入口
- **个人资料**：昵称、头像、个人签名；昵称会出现在首页问候语里，签名出现在问候语下面。头像复制进应用私有目录，不依赖相册原图
- **关于**：版本号与开源许可

### 🎨 界面

- 完整的自定义设计系统（颜色 / 字体 / 间距 / 圆角 / 动效）
- **没有使用任何 Material 默认观感的组件**：卡片、按钮、输入框、弹窗、开关、筛选器全部自己实现
- **底部导航**：首页 / 成就 / 图鉴 / 设置四个入口，进详情、新建、备份这类二级页面时自动收起；选中态只用强调色，没有选中色块和指示器
- 深色模式独立调色，不是简单的黑白反转
- **中英双语**：跟随系统语言切换，图鉴的 109 条标题 / 描述 / 故事都有完整英文版

---

## 截图

| 首页 | 全部成就 | 成就详情 |
|:---:|:---:|:---:|
| ![首页](docs/screenshots/zh/home.png) | ![全部成就](docs/screenshots/zh/all-achievements.png) | ![成就详情](docs/screenshots/zh/detail.png) |
| 成就图鉴 | 设置 | 深色模式 |
| ![成就图鉴](docs/screenshots/zh/codex.png) | ![设置](docs/screenshots/zh/settings.png) | ![深色模式](docs/screenshots/zh/home-dark.png) |

> 截图来自真机运行。英文界面截图见 [README.en.md](README.en.md)。

---

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose（Material 3 仅作为部分组件的宿主） |
| 架构 | MVVM + Repository |
| 本地存储 | Room 2.7.2（数据库版本 4，含 1→2→3→4 三次迁移） |
| 导航 | Navigation Compose 2.8.9 |
| 异步 | Coroutines + Flow |
| 图片 | 系统照片选择器（无需存储权限） |
| 安全 | AndroidX Biometric 1.1.0 |
| 加密 | Android Keystore + AES-256-GCM；本地数据用信封加密，备份可选口令加密（PBKDF2-HMAC-SHA256） |
| 构建 | AGP 9.3.0 · Gradle 9.5 · JDK 17 |
| 最低版本 | Android 7.0（API 24） |

**没有引入任何网络库**——这个项目从设计上就不联网。

---

## 设计系统

在写页面之前先建立了完整的设计系统，所有页面只通过它取样式。

```
ui/theme/
├── Color.kt     语义色板：中性灰阶 + 单一青绿强调色 + 五档稀有度金属色
├── Type.kt      10 档字体，数字单独成体系并使用等宽数字（tnum）
├── Dimens.kt    间距 4/8/12/16/24/32/48/64，圆角 8/12/16/20
├── Shape.kt     形状由圆角 token 推导
├── Motion.kt    动效 150/200/250/300ms + 三种缓动
└── Theme.kt     Material 角色到语义色的映射
```

几个刻意遵守的规则：

- 页面里**不允许出现硬编码颜色**（全部集中在一个文件里）
- 页面里**不允许出现体系外的 dp 值**
- 层级靠「表面色差 + 1dp 描边」建立，**不用阴影**
- 动效只有 8dp 位移动画和淡入淡出，没有弹跳

公共组件位于 `ui/components/`：`AppTopBar`、`AppBottomBar`、`AppCard`、`AppButton`、`AppIconButton`、`AppTextLink`、`AppTextField`、`AppDialog`、`AppSnackbar`、`AppChip`、`AppSwitch`、`AppSettingRow`、`SegmentedControl`、`AchievementCard`、`RarityBadge`、`StatusBadge`、`MetricNumber` / `StatTile`、`ProgressBar` / `ProgressRing` / `IndeterminateBar`、`SectionHeader` / `TimelineItem`、`EmptyState`、`AppearAnimation`。

---

## 快速开始

### 直接安装

到 [Releases](https://github.com/mrlingan/LifeLedger/releases/latest) 下载最新的 `LifeLedger-vX.Y.Z.apk`，在手机设置里允许「安装未知来源的应用」，安装即可。不需要账号，也不申请网络权限；唯一可能用到的是「每日提醒」的通知权限，而且只在你打开提醒开关时才会申请。

> 当前版本使用 APK Signature Scheme v2 签名，对应 `minSdk 24`（Android 7.0）及以上。校验用的 SHA-256 写在每个 Release 的说明里。

### 从源码运行

环境要求：

- Android Studio（建议最新稳定版）
- JDK 17
- Android SDK Platform 35

```bash
git clone https://github.com/mrlingan/LifeLedger.git
cd LifeLedger
```

用 Android Studio 打开项目根目录 → 等待 Gradle Sync 完成 → 点 Run。

> **国内网络提示**：项目已经在 `settings.gradle.kts` 里配置了阿里云镜像，官方源作为兜底。如果同步仍然很慢，可以把 `C:\Users\<you>\.gradle` 和项目目录加入 Windows Defender 的排除项——实时扫描是这类项目构建慢的主要元凶。

### 打包发布前记得改

- `app/src/main/res/values/strings.xml` 里的应用名
- 应用图标（`res/mipmap-*`）
- 如果你是 fork 之后自己维护，建议把 `app/build.gradle.kts` 里的 `applicationId`（`com.Anchored.mylife`）换成你自己的——**改了之后系统会把它当成一个新应用，旧数据不会自动迁移**，需要用备份导出再导入

---

## 项目结构

```
app/src/main/
├── java/com/Anchored/mylife/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── backup/        备份与恢复（zip 打包 / 解包 / 可选口令加密）
│   │   ├── crypto/        本地字段加密（信封加密 + 逐行迁移）
│   │   ├── dao/           Room DAO
│   │   ├── database/      实体、数据库、迁移、数据库提供者
│   │   ├── media/         媒体文件复制、实况照片解析
│   │   ├── preset/        预设成就导入器
│   │   ├── profile/       头像的私有副本
│   │   ├── reminder/      每日提醒（系统闹钟 + 通知 + 开机重排）
│   │   ├── repository/    仓库层与统一入口
│   │   └── settings/      应用设置（SharedPreferences）
│   └── ui/
│       ├── components/    公共 UI 组件
│       ├── home/          首页区块（问候、人生进度、核心数据、分类进度、最近解锁）
│       ├── codex/         图鉴条目与进度区块
│       ├── theme/         设计系统
│       ├── AchievementNavHost.kt    导航、主题、应用锁
│       ├── HomeScreen.kt / HomeViewModel.kt        首页总览（不放列表）
│       ├── OnboardingScreen.kt      首次启动：选择从哪里开始
│       ├── AllAchievementsScreen.kt / AchievementListViewModel.kt   全部成就列表
│       ├── AchievementRow.kt        成就列表项（列表观感只有这一处实现）
│       ├── AddOptionsSheet.kt       「从图鉴挑选 / 自己写」二选一弹层
│       ├── AchievementDetailScreen.kt / ViewModel
│       ├── AddAchievementScreen.kt
│       ├── PresetAchievementScreen.kt / ViewModel
│       ├── ProfileScreen.kt / ProfileViewModel.kt        个人资料
│       ├── AchievementSettingsScreen.kt / ViewModel      成就设置
│       ├── ReminderScreen.kt / ReminderViewModel.kt      每日提醒
│       ├── DataSecurityScreen.kt / DataSecurityViewModel.kt  数据安全
│       ├── SettingsScreen.kt / ViewModel
│       ├── BackupScreen.kt / ViewModel
│       ├── MediaComponents.kt
│       ├── CompletionFeedback.kt    标记完成时的音效与触感
│       ├── AppLockGate.kt
│       └── PresetText.kt / PresetStringRes.kt   图鉴文案本地化
├── assets/
│   └── preset_achievements.json   109 条预设成就（中文原文，作为稳定标识）
└── res/
    ├── values/           英文（默认语言）
    └── values-zh/        中文

scripts/preset_i18n/      图鉴文案生成脚本（改完成就内容后重新生成 string 资源）
docs/screenshots/         README 用的界面截图（zh/ 与 en/ 各一套）
```

---

## 数据与隐私

**完全离线。** 应用不申请网络权限，也不包含任何联网代码。

| 数据 | 存放位置 |
|---|---|
| 成就、笔记、媒体记录、图鉴进度 | Room 数据库（应用私有目录） |
| 成就标题 / 描述、笔记正文 | 打开「本地加密」后以密文落盘；密钥由系统 Keystore 保管，不跟数据库文件一起走 |
| 图片、视频、实况照片副本 | `files/media/` |
| 头像副本 | `files/profile/` |
| 主题、应用锁等偏好 | SharedPreferences |

权限只有两个，且都只服务于「每日提醒」：`POST_NOTIFICATIONS`（通知，仅在你打开提醒开关时申请）和 `RECEIVE_BOOT_COMPLETED`（重启后重排闹钟）。不申请网络权限。

卸载应用会连同数据一起删除，所以**换机前请先在设置里导出备份**。

备份文件是一个 zip：

```
lifeledger_backup_20260912_1040.zip
├── backup.json         全部结构化数据
├── media/              原始图片与视频
└── profile/            头像副本
```

导出时可以选择给整份备份设一个口令（不设就是普通 zip）。导入时会先清空当前数据再写入备份内容，媒体文件按文件名重新落地到当前设备的私有目录——所以跨设备恢复也能正确对上。

---

## 内置成就库

图鉴里的 109 条成就来自开源项目 **[EarthOnline-Achievement](https://github.com/LKlingkong/EarthOnline-Achievement)**（原项目 README 中声明为 MIT License），包含每条成就的标题、描述、故事全文、分类与达成率。

原始数据被完整保留，只有两处调整：

- **稀有度**：原数据只有四档（普通 / 稀有 / 史诗 / 传说），且存在标注与达成率相互矛盾的情况。本项目改为按达成率统一推导五档（青铜 / 白银 / 黄金 / 白金 / 传奇），不改动原始数据本身
- **数据结构**：原项目把「一句话描述」与「完整故事」分开存放，本项目沿用了这个结构，分别用于列表与详情页

### 多语言

图鉴正文在数据库里保存的是中文原文，它只作为**稳定标识**使用（id 对应、备份、导入导出都靠它）；界面上显示什么语言，由 `res/values/preset_strings.xml`（英文，默认）和 `res/values-zh/preset_strings.xml`（中文）决定。所以切换系统语言立刻生效，不需要迁移数据库，也不会动用户已有的数据。

改完成就内容之后，跑一次生成脚本重新产出这批 string 资源：

```bash
pwsh -File scripts/preset_i18n/generate_preset_i18n.ps1
```

脚本会读取同目录下的 `source.tsv`（中文原文）和 `en.json`（英文翻译），把结果输出到 `out/`，覆盖回 `app/src/main/res/values*/preset_strings.xml` 和 `app/src/main/java/com/Anchored/mylife/ui/PresetStringRes.kt` 即可。资源里缺哪条只会退回显示中文原文，不会崩。

---

## 路线图

- [x] 成就的增删改查与完成状态
- [x] 自定义完成日期
- [x] 笔记与媒体（图片 / 视频 / 实况照片）
- [x] 109 条成就图鉴与解锁
- [x] 数据备份与恢复
- [x] 设计系统与全站重绘
- [x] 深色模式与应用锁
- [x] 本地数据加密（手动开关，信封加密 + 逐行迁移）
- [ ] 媒体文件（图片 / 视频）的本地加密
- [ ] 图标体系：`iconEmoji` → `iconKey`，改用统一矢量图标（含数据库迁移与旧数据映射）
- [x] 每日提醒（系统闹钟 + 通知，不引入 WorkManager）
- [ ] 数据统计页：年度回顾、成长曲线、时间线
- [x] 显示与动画强度设置
- [x] 个人资料（昵称、头像、个人签名）

---

## 已知限制

- **成就图标目前是 Emoji**。数据模型里存的是 emoji 字符，不同厂商的系统字体渲染效果不同，长期会迁移到内置矢量图标（见路线图）。
- 图鉴里尚未配图标的条目，暂时以标题首字占位。
- 应用锁依赖系统的指纹 / 面容 / 锁屏密码；设备未设置任何验证方式时无法开启（这是刻意的，避免把用户关在外面）。
- 本地字段加密是**手动开关**（设置 → 数据安全 → 加密本地数据），默认关闭。开启后成就标题/描述与笔记正文加密存储，密钥由系统 Keystore 保管、不跟数据库文件一起走，能挡住"把库文件拷走"这类离线读取；挡不住已 root 且能在同机运行代码的人。媒体文件（图片、视频）目前仍未加密。
- 没有接入 SQLCipher 之类的整库加密，图鉴内容、媒体路径等本来就不敏感的字段仍以明文存储。
- **「导入 / 导出」里的选择性导出还没做**：设置里那一项仍标着「即将支持」，目前的导出和导入都是整库级别的。

---

## 贡献

欢迎 Issue 和 Pull Request。提交前建议：

1. 跑一次 `./gradlew assembleDebug` 确认构建通过
2. 新增 UI 时复用 `ui/components/` 里的组件，不要在页面里直接写颜色和尺寸
3. 修改数据库结构时**必须补迁移**，并且保证老数据可以正常升级

---

## 许可证

[MIT License](LICENSE) © 2026 mrlingan

你可以自由使用、修改、分发这个项目，包括商业用途，只需保留版权声明。

---

## 致谢

- [EarthOnline-Achievement](https://github.com/LKlingkong/EarthOnline-Achievement) —— 109 条预设成就的数据来源
- [AndroidX](https://developer.android.com/jetpack) —— Room / Compose / Navigation / Biometric
- [Material Symbols](https://fonts.google.com/icons) —— 界面功能图标

---

**每个人都是自己人生的主角。**
