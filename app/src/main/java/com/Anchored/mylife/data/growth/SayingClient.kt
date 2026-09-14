package com.Anchored.mylife.data.growth

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 取回来的一条一言。免费接口只给正文，出处/作者那边没有。 */
data class RemoteSaying(val text: String)

/**
 * 零依赖的小客户端（不引 OkHttp / Retrofit，保持包体积）。
 *
 * 走 UAPI 的免费接口 `GET https://uapis.cn/api/v1/saying`，返回 `{"text": "..."}`，
 * **每次调用都是随机一条**；「每天固定同一条」不靠接口，由
 * [com.Anchored.mylife.data.repository.GrowthRepository] 落库缓存实现。
 *
 * 只有开发者选项里显式打开网络开关后才会被执行。
 */
object SayingClient {
    private const val ENDPOINT = "https://uapis.cn/api/v1/saying"

    fun fetch(): RemoteSaying? = runCatching {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            // 限流（429）和语料库异常（500）都是非 2xx，一律当作本次失败，
            // 交给上层保留已有内容，不做重试风暴。
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val text = JSONObject(body).optString("text").trim()
            RemoteSaying(text).takeIf { it.text.isNotEmpty() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
