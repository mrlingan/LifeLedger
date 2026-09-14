package com.Anchored.mylife.ui.demo

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * 「设置 → 开发者选项」什么时候可见。
 *
 * 那一组里有一个「清空成就数据」，普通用户点进去就是真的丢数据，
 * 所以默认只对自己人开放，判定条件按可信度排：
 *
 * 1. **调试签名**：包是用 Android 默认的调试证书（`CN=Android Debug`）签的。
 *    这一条认的是"谁签的"，不是"怎么编的"——出「release 构建 + 演示数据」的
 *    截图包时正好接住：构建类型是 release，但签名还是本机的调试证书；
 * 2. **可调试的测试包**：`android:debuggable="true"`，也就是 Android Studio
 *    直接跑出来的那种 debug 包；
 * 3. [FORCE_SHOW]：两条都不占、又确实需要（例如用正式 keystore 出截图包），
 *    把它改成 true 重新编译即可。
 *
 * 三条都不满足（用户装的正式包就是这种）时，设置页里**连这一组都不渲染**，
 * 不是渲染出来再禁用——少一个入口就少一次误触。
 */
object DemoAccess {

    /**
     * 无视签名与构建类型，强制显示开发者选项。
     *
     * 出截图包用的后门：正式 keystore 签的包也要能生成演示数据时改成 true。
     * 平时保持 false——改真了，正式包就会带上这个入口。
     */
    private const val FORCE_SHOW = false

    /** 判定结果，顺便说明是哪一条命中的，设置页标题上会标出来，方便自查 */
    enum class Reason {
        /** 调试签名（Android 默认调试证书） */
        DEBUG_SIGNATURE,

        /** 可调试的测试包 */
        DEBUGGABLE,

        /** 手工强制打开 */
        FORCED,

        /** 都不满足：不显示 */
        HIDDEN
    }

    fun reason(context: Context): Reason = when {
        FORCE_SHOW -> Reason.FORCED
        isDebugSigned(context) -> Reason.DEBUG_SIGNATURE
        isDebuggable(context) -> Reason.DEBUGGABLE
        else -> Reason.HIDDEN
    }

    fun isVisible(context: Context): Boolean = reason(context) != Reason.HIDDEN

    /**
     * 签名证书是不是 Android 的调试证书。
     *
     * 调试证书的主体固定是 `CN=Android Debug, O=Android, C=US`：
     * 只要把主体拿出来看一眼就够了，不用去比指纹（换台机器、
     * 重新生成 debug.keystore 之后指纹会变，主体不会）。
     */
    private fun isDebugSigned(context: Context): Boolean = runCatching {
        signaturesOf(context).orEmpty().any { signature ->
            val subject = certificateOf(signature).subjectX500Principal?.name
            subject?.contains(DEBUG_CERT_CN) == true
        }
    }.getOrDefault(false)

    private fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun signaturesOf(context: Context): Array<Signature>? {
        val manager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo
                ?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures
        }
    }

    private fun certificateOf(signature: Signature): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(signature.toByteArray())) as X509Certificate

    private const val DEBUG_CERT_CN = "CN=Android Debug"
}
