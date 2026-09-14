package com.Anchored.mylife.data.profile

/**
 * 个人资料里的两项基础信息：性别与 MBTI。
 *
 * 这两个值目前只出现在个人资料页那一张「基础信息」卡上（首页和「我的」都不读它们），
 * 但它们回答的是和昵称、头像同一个问题——"我是谁"，所以和昵称一样存在本地偏好里：
 * 不加密、不上传，换台设备就重新设（这一页的定位见 ProfileScreen 的副标题）。
 *
 * 存的是枚举名，认不出来的值一律当作"没设过"，界面回落到「未设置」——
 * 手改坏偏好文件、或者以后删掉某个类型，都不能让界面停在一条认不出的记录上，
 * 和 [AvatarPreset.fromName] 是同一个约定。
 *
 * 两个枚举都**不认识资源 id**：中文/英文怎么写在界面层映射，和
 * [com.Anchored.mylife.data.settings.HomeSection] 一样。
 */

/** 性别。三个选项都是"用户自己说的"，这里不做任何推断或补全 */
enum class Gender {
    MALE,
    FEMALE,
    /** 不公开：选了它就在资料页上留白，而不是留一个空值 */
    UNDISCLOSED;

    companion object {
        /** 按落盘的名字还原；认不出来（含 null 和空串）当作"没设过" */
        fun fromName(name: String?): Gender? =
            name?.let { value -> entries.firstOrNull { it.name == value } }
    }
}

/**
 * MBTI 类型。
 *
 * 声明顺序就是选择器里的顺序：先按第一组（I / E）分块，块内按剩下三组的习惯顺序排，
 * 十六个标签铺在弹层里时，相邻两个只差一位——找起来比按字母表排要快。
 */
enum class MbtiType {
    INTJ,
    INTP,
    ENTJ,
    ENTP,
    INFJ,
    INFP,
    ENFJ,
    ENFP,
    ISTJ,
    ISFJ,
    ESTJ,
    ESFJ,
    ISTP,
    ISFP,
    ESTP,
    ESFP;

    companion object {
        /** 按落盘的名字还原；认不出来（含 null 和空串）当作"没设过" */
        fun fromName(name: String?): MbtiType? =
            name?.let { value -> entries.firstOrNull { it.name == value } }
    }
}
