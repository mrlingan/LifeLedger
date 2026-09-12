package com.Anchored.mylife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.Anchored.mylife.ui.AchievementNavHost

/**
 * 必须是 AppCompatActivity：
 * - BiometricPrompt（应用锁）要求宿主是 FragmentActivity，而它正是后者的子类；
 * - 设置页里手动切换语言用的 AppCompatDelegate.setApplicationLocales()，
 *   在 Android 13 以下只对 AppCompat 的页面生效。
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 内容铺满整屏；状态栏图标的明暗由 LifeLedgerTheme 按当前主题控制
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AchievementNavHost()
        }
    }
}
