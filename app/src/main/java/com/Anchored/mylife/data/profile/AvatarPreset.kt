package com.Anchored.mylife.data.profile

/**
 * 内置头像。
 *
 * 不想上传照片的用户不该只能空着，所以这里预备了一排画出来的头像；
 * 画成什么样归界面（见 ui/components/PresetAvatar.kt），数据层只认这一串标识，
 * 和 [com.Anchored.mylife.data.settings.HomeSection] 一样不认识资源 id。
 *
 * 声明顺序就是选择器里的顺序：暖色到冷色，相邻两个的图案和底色都岔开，
 * 一排看过去不会觉得是同一个头像换了个色。
 *
 * 一个都没挑、也没上传图片时，界面用昵称的第一个字当头像（见 AppAvatar），
 * 所以"没设过"是一个正常的常态，不是缺省值。
 */
enum class AvatarPreset {
    /** 日出：地平线上一轮太阳 */
    SUNRISE,

    /** 海浪：两道弧线 */
    WAVE,

    /** 山峦：两座山峰 */
    MOUNTAIN,

    /** 树叶：一片叶子 */
    LEAF,

    /** 月夜：一弯月牙 */
    MOON,

    /** 云朵：三团云 */
    CLOUD,

    /** 星芒：一颗四角星 */
    SPARKLE,

    /** 花瓣：五瓣花 */
    PETAL;

    companion object {
        /**
         * 按落盘的名字还原。
         *
         * 认不出来（偏好文件被手改坏，或者以后删掉了某个头像）一律当作"没挑过"，
         * 界面自己会回落到昵称首字，不会出现一个空圈。
         */
        fun fromName(name: String?): AvatarPreset? =
            name?.let { value -> entries.firstOrNull { it.name == value } }
    }
}
