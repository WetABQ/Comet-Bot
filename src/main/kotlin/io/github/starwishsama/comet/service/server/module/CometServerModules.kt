package io.github.starwishsama.comet.service.server.module

import cn.hutool.core.net.URLDecoder
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.readValue
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.BotVariables.cfg
import io.github.starwishsama.comet.api.thirdparty.github.data.events.PushEvent
import io.github.starwishsama.comet.logger.HinaLogLevel
import io.github.starwishsama.comet.service.pusher.instances.GithubPusher
import io.github.starwishsama.comet.service.server.ServerUtil
import io.github.starwishsama.comet.utils.FileUtil
import io.github.starwishsama.comet.utils.json.isUsable

/**
 * [GithubWebHookHandler]
 *
 * 处理 Github Webhook 请求.
 */
class GithubWebHookHandler : HttpHandler {
    private val signature256 = "X-Hub-Signature-256"

    override fun handle(he: HttpExchange) {
        val signature = he.requestHeaders[signature256]

        if (cfg.webHookSecret.isNotEmpty() && signature == null) {
            BotVariables.netLogger.log(HinaLogLevel.Debug, "收到新事件, 未通过安全验证. 请求的签名为: 无", prefix = "WebHook")
            he.sendResponseHeaders(500, 0)
            return
        }

        val request = String(he.requestBody.readBytes())

        if (cfg.webHookSecret.isNotEmpty() && signature != null && !ServerUtil.checkSignature(signature[0], request)) {
            BotVariables.netLogger.log(HinaLogLevel.Debug, "收到新事件, 未通过安全验证. 请求的签名为: $signature", prefix = "WebHook")
            he.sendResponseHeaders(500, 0)
            return
        }

        BotVariables.netLogger.log(HinaLogLevel.Debug, "收到新事件", prefix = "WebHook")

        FileUtil.createTempFile(request)

        if (!request.startsWith("payload")) {
            BotVariables.netLogger.log(HinaLogLevel.Debug, "无效请求", prefix = "WebHook")
            he.sendResponseHeaders(403, 0)
            return
        }

        val payload = URLDecoder.decode(request.replace("payload=", ""), Charsets.UTF_8)

        val validate = BotVariables.mapper.readTree(payload).isUsable()

        if (validate) {
            BotVariables.netLogger.log(HinaLogLevel.Warn, "解析请求失败, 回调的 JSON 不合法.\n${payload}", prefix = "WebHook")
            he.sendResponseHeaders(403, 0)
            return
        }

        try {
            val info = BotVariables.mapper.readValue<PushEvent>(payload)
            GithubPusher.push(info)
            BotVariables.netLogger.log(HinaLogLevel.Debug, "推送 WebHook 消息成功", prefix = "WebHook")
        } catch (e: JsonParseException) {
            BotVariables.netLogger.log(HinaLogLevel.Debug, "推送 WebHook 消息失败, 不支持的事件类型", prefix = "WebHook")
        } catch (e: Exception) {
            BotVariables.netLogger.log(HinaLogLevel.Warn, "推送 WebHook 消息失败", e, prefix = "WebHook")
        }

        val response = if (cfg.webHookSecret.isEmpty()) {
            "Comet 已收到事件, 推荐使用密钥以保证安全".toByteArray()
        } else {
            "Comet 已收到事件".toByteArray()
        }

        he.responseHeaders.add("content-type", "text/plain; charset=UTF-8")
        he.sendResponseHeaders(200, response.size.toLong())

        he.responseBody.use {
            it.write(response)
            it.flush()
        }
    }
}