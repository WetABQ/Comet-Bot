/*
 * Copyright (c) 2019-2022 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.startup

import cn.hutool.cron.CronUtil
import io.github.starwishsama.comet.BuildConfig
import io.github.starwishsama.comet.CometApplication
import io.github.starwishsama.comet.CometVariables
import io.github.starwishsama.comet.CometVariables.cfg
import io.github.starwishsama.comet.CometVariables.comet
import io.github.starwishsama.comet.CometVariables.cometServiceServer
import io.github.starwishsama.comet.CometVariables.consoleCommandLogger
import io.github.starwishsama.comet.CometVariables.daemonLogger
import io.github.starwishsama.comet.CometVariables.logger
import io.github.starwishsama.comet.api.command.CommandManager
import io.github.starwishsama.comet.api.command.MessageHandler
import io.github.starwishsama.comet.api.command.MessageHandler.attachHandler
import io.github.starwishsama.comet.api.thirdparty.bilibili.DynamicApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.LiveApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.SearchApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.UserApi
import io.github.starwishsama.comet.api.thirdparty.bilibili.VideoApi
import io.github.starwishsama.comet.api.thirdparty.jikipedia.JikiPediaApi
import io.github.starwishsama.comet.api.thirdparty.noabbr.NoAbbrApi
import io.github.starwishsama.comet.api.thirdparty.rainbowsix.R6StatsApi
import io.github.starwishsama.comet.api.thirdparty.twitter.TwitterApi
import io.github.starwishsama.comet.commands.chats.AdminCommand
import io.github.starwishsama.comet.commands.chats.ArkNightCommand
import io.github.starwishsama.comet.commands.chats.BangumiCommand
import io.github.starwishsama.comet.commands.chats.BiliBiliCommand
import io.github.starwishsama.comet.commands.chats.CheckInCommand
import io.github.starwishsama.comet.commands.chats.DiceCommand
import io.github.starwishsama.comet.commands.chats.DivineCommand
import io.github.starwishsama.comet.commands.chats.GaokaoCommand
import io.github.starwishsama.comet.commands.chats.GithubCommand
import io.github.starwishsama.comet.commands.chats.GroupConfigCommand
import io.github.starwishsama.comet.commands.chats.GuessNumberCommand
import io.github.starwishsama.comet.commands.chats.HelpCommand
import io.github.starwishsama.comet.commands.chats.InfoCommand
import io.github.starwishsama.comet.commands.chats.JikiPediaCommand
import io.github.starwishsama.comet.commands.chats.KeyWordCommand
import io.github.starwishsama.comet.commands.chats.KickCommand
import io.github.starwishsama.comet.commands.chats.MinecraftCommand
import io.github.starwishsama.comet.commands.chats.MusicCommand
import io.github.starwishsama.comet.commands.chats.MuteCommand
import io.github.starwishsama.comet.commands.chats.NoAbbrCommand
import io.github.starwishsama.comet.commands.chats.PictureSearchCommand
import io.github.starwishsama.comet.commands.chats.PusherCommand
import io.github.starwishsama.comet.commands.chats.R6SCommand
import io.github.starwishsama.comet.commands.chats.RConCommand
import io.github.starwishsama.comet.commands.chats.RSPCommand
import io.github.starwishsama.comet.commands.chats.RollCommand
import io.github.starwishsama.comet.commands.chats.TwitterCommand
import io.github.starwishsama.comet.commands.chats.UnMuteCommand
import io.github.starwishsama.comet.commands.chats.VersionCommand
import io.github.starwishsama.comet.commands.console.BroadcastCommand
import io.github.starwishsama.comet.commands.console.DebugCommand
import io.github.starwishsama.comet.commands.console.StopCommand
import io.github.starwishsama.comet.file.DataSaveHelper
import io.github.starwishsama.comet.file.DataSetup
import io.github.starwishsama.comet.listeners.AutoReplyListener
import io.github.starwishsama.comet.listeners.BiliBiliShareListener
import io.github.starwishsama.comet.listeners.BotGroupStatusListener
import io.github.starwishsama.comet.listeners.GroupMemberChangedListener
import io.github.starwishsama.comet.listeners.GroupRequestListener
import io.github.starwishsama.comet.listeners.NormalizeMessageSendListener
import io.github.starwishsama.comet.listeners.RepeatListener
import io.github.starwishsama.comet.listeners.register
import io.github.starwishsama.comet.logger.HinaLogLevel
import io.github.starwishsama.comet.logger.YabapiLogRedirecter
import io.github.starwishsama.comet.managers.GroupConfigManager
import io.github.starwishsama.comet.managers.NetworkRequestManager
import io.github.starwishsama.comet.objects.tasks.GroupFileAutoRemover
import io.github.starwishsama.comet.objects.tasks.HitokotoUpdater
import io.github.starwishsama.comet.service.RetrofitLogger
import io.github.starwishsama.comet.service.gacha.GachaService
import io.github.starwishsama.comet.service.pusher.PusherManager
import io.github.starwishsama.comet.service.server.CometServiceServer
import io.github.starwishsama.comet.utils.FileUtil
import io.github.starwishsama.comet.utils.LoggerAppender
import io.github.starwishsama.comet.utils.RuntimeUtil
import io.github.starwishsama.comet.utils.StringUtil.getLastingTimeAsString
import io.github.starwishsama.comet.utils.TaskUtil
import io.github.starwishsama.comet.utils.gaokaoDateTime
import io.github.starwishsama.comet.utils.network.NetUtil
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import net.kronos.rkon.core.Rcon
import net.mamoe.mirai.Bot
import okhttp3.OkHttpClient
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


object CometRuntime {
    fun postSetup() {
        CometVariables.startTime = LocalDateTime.now()
        CometVariables.loggerAppender = LoggerAppender(FileUtil.getLogLocation())
        CometVariables.miraiLoggerAppender = LoggerAppender(FileUtil.getLogLocation("mirai"))
        CometVariables.miraiNetLoggerAppender = LoggerAppender(FileUtil.getLogLocation("mirai-net"))

        Runtime.getRuntime().addShutdownHook(Thread { shutdownTask() })

        logger.info(
            """
        
           ______                     __ 
          / ____/___  ____ ___  ___  / /_
         / /   / __ \/ __ `__ \/ _ \/ __/
        / /___/ /_/ / / / / / /  __/ /_  
        \____/\____/_/ /_/ /_/\___/\__/  


    """
        )

        CometVariables.client = OkHttpClient().newBuilder()
            .callTimeout(3, TimeUnit.SECONDS)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .followRedirects(true)
            .hostnameVerifier { _, _ -> true }
            .also {
                if (cfg.proxySwitch) {
                    if (NetUtil.checkProxyUsable()) {
                        it.proxy(Proxy(cfg.proxyType, InetSocketAddress(cfg.proxyUrl, cfg.proxyPort)))
                    }
                }
            }
            .addInterceptor(RetrofitLogger())
            .build()

        DataSetup.init()

        CommandManager.setupCommands(
            arrayOf(
                AdminCommand,
                ArkNightCommand,
                BangumiCommand,
                BiliBiliCommand,
                CheckInCommand,
                io.github.starwishsama.comet.commands.chats.DebugCommand,
                DivineCommand,
                GaokaoCommand,
                GenshinGachaCommand,
                GuessNumberCommand,
                HelpCommand,
                InfoCommand,
                MusicCommand,
                MuteCommand,
                UnMuteCommand,
                PictureSearchCommand,
                R6SCommand,
                RConCommand,
                KickCommand,
                TwitterCommand,
                VersionCommand,
                GroupConfigCommand,
                RSPCommand,
                RollCommand,
                MinecraftCommand,
                PusherCommand,
                GithubCommand,
                DiceCommand,
                NoAbbrCommand,
                JikiPediaCommand,
                KeyWordCommand,
                // Console Command
                StopCommand,
                DebugCommand,
                io.github.starwishsama.comet.commands.console.AdminCommand,
                BroadcastCommand
            )
        )

        YabapiLogRedirecter.initYabapi()

        logger.info("[命令] 已注册 " + CommandManager.countCommands() + " 个命令")
    }

    private fun shutdownTask() {
        logger.info("[Bot] 正在关闭 Bot...")
        CronUtil.stop()
        DataSetup.saveAllResources()
        PusherManager.stopPushers()
        cometServiceServer?.stop()

        TaskUtil.service.shutdown()

        CometVariables.rCon?.disconnect()
        CometVariables.miraiLoggerAppender.close()
        CometVariables.loggerAppender.close()
    }

    fun setupBot(bot: Bot) {
        bot.attachHandler()

        /** 监听器 */
        val listeners = arrayOf(
            BiliBiliShareListener,
            RepeatListener,
            BotGroupStatusListener,
            AutoReplyListener,
            GroupMemberChangedListener,
            GroupRequestListener,
            NormalizeMessageSendListener
        )

        listeners.forEach { it.register(bot) }

        DataSetup.initPerGroupSetting(bot)

        runCatching {
            setupRCon()
        }.onFailure {
            daemonLogger.warning("无法连接至 rcon 服务器", it)
        }

        runScheduleTasks()

        PusherManager.startPushers()

        GachaPoolManager.init()

        startupServer()

        TaskUtil.scheduleAtFixedRate(5, 5, TimeUnit.SECONDS) {
            runBlocking { NetworkRequestManager.schedule() }
        }

        logger.info("彗星 Bot 启动成功, 版本 ${BuildConfig.version}, 耗时 ${CometVariables.startTime.getLastingTimeAsString()}")

        TaskUtil.schedule { GachaService.loadAllPools() }
    }

    fun setupRCon() {
        val address = cfg.rConUrl
        val password = cfg.rConPassword
        if (address != null && password != null && CometVariables.rCon == null) {
            CometVariables.rCon = Rcon(address, cfg.rConPort, password.toByteArray())
            daemonLogger.info("成功连接至 rcon 服务器")
        }
    }

    @Suppress("HttpUrlsUsage")
    private fun startupServer() {
        if (!cfg.webHookSwitch) {
            return
        }

        try {
            val customSuffix = cfg.webHookAddress.replace("http://", "").replace("https://", "").split("/")
            cometServiceServer = CometServiceServer(cfg.webHookPort, customSuffix.last())
        } catch (e: Exception) {
            daemonLogger.warning("Comet 服务端启动失败", e)
        }
    }

    private fun runScheduleTasks() {
        TaskUtil.schedule { DataSaveHelper.checkOldFiles() }

        val apis =
            arrayOf(DynamicApi, JikiPediaApi, LiveApi, NoAbbrApi, R6StatsApi, SearchApi, TwitterApi, UserApi, VideoApi)

        /** 定时任务 */
        DataSaveHelper.scheduleBackup()
        DataSaveHelper.scheduleSave()

        apis.forEach {
            if (it.duration > 0) {
                TaskUtil.scheduleAtFixedRate(it.duration.toLong(), it.duration.toLong(), TimeUnit.HOURS) {
                    it.resetTime()
                }
            }
        }

        TaskUtil.scheduleAtFixedRate(1, 1, TimeUnit.HOURS) {
            RuntimeUtil.forceGC()
            GroupFileAutoRemover.execute()
            HitokotoUpdater.run()
        }

        CronUtil.schedule("0 0 8 * * ?", Runnable {
            GroupConfigManager.getAllConfigs().filter { it.gaokaoPushEnabled }.forEach {
                runBlocking {
                    comet.getBot().getGroup(it.id)?.sendMessage(
                        "现在距离${LocalDateTime.now().year}年普通高等学校招生全国统一考试还有${
                            gaokaoDateTime.getLastingTimeAsString(TimeUnit.DAYS)
                        }。"
                    )
                }
            }
        })

        CronUtil.start()
    }

    fun handleConsoleCommand() {
        TaskUtil.schedule {
            consoleCommandLogger.log(HinaLogLevel.Info, "后台已启用", prefix = "后台管理")

            while (comet.getBot().isActive) {
                var line: String

                try {
                    line = CometApplication.console.readLine(">")
                    val result = MessageHandler.dispatchConsoleCommand(line)
                    if (result.isNotEmpty()) {
                        consoleCommandLogger.info(result)
                    }
                } catch (ignored: EndOfFileException) {
                } catch (ignored: UserInterruptException) {
                }

            }
        }
    }
}
