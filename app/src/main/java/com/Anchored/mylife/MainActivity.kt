package com.Anchored.mylife

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.Anchored.mylife.ui.AchievementNavHost

/**
 * 用 FragmentActivity 而不是 ComponentActivity：
 * BiometricPrompt（应用锁）要求宿主是 FragmentActivity。
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 内容铺满整屏；状态栏图标的明暗由 LifeLedgerTheme 按当前主题控制
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AchievementNavHost()
        }
    }
}
