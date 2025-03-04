/*
 * Copyright (c) 2019-2021 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.commands.chats

import io.github.starwishsama.comet.api.command.CommandProps
import io.github.starwishsama.comet.api.command.interfaces.ChatCommand
import io.github.starwishsama.comet.api.command.interfaces.UnDisableableCommand
import io.github.starwishsama.comet.i18n.LocalizationManager
import io.github.starwishsama.comet.objects.CometUser
import io.github.starwishsama.comet.objects.enums.UserLevel
import io.github.starwishsama.comet.service.command.AdminService.addPermission
import io.github.starwishsama.comet.service.command.AdminService.giveCommandTime
import io.github.starwishsama.comet.service.command.AdminService.listPermissions
import io.github.starwishsama.comet.service.command.AdminService.removePermission
import io.github.starwishsama.comet.utils.CometUtil.toMessageChain
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain

@Suppress("SpellCheckingInspection")
object AdminCommand : ChatCommand, UnDisableableCommand {
    override suspend fun execute(event: MessageEvent, args: List<String>, user: CometUser): MessageChain {
        if (!hasPermission(user, event)) {
            return LocalizationManager.getLocalizationText("message.no-permission").toMessageChain()
        }

        return if (args.isEmpty()) {
            getHelp().toMessageChain()
        } else {
            when (args[0]) {
                "help", "帮助" -> getHelp().toMessageChain()
                "permlist", "权限列表", "qxlb" -> listPermissions(user, args.getOrElse(1) { "" }, event.message)
                "permadd", "添加权限", "tjqx" -> addPermission(user, args.getOrElse(1) { "" }, args.getOrElse(2) { "" }, event.message)
                "permdel", "删除权限", "scqx", "sqx" -> removePermission(user, args.getOrElse(1) { "" }, args.getOrElse(2) { "" }, event.message)
                "give", "增加次数" -> giveCommandTime(event, args)
                else -> "命令不存在, 使用 /admin help 查看更多".toMessageChain()
            }
        }
    }

    override val props: CommandProps =
        CommandProps("admin", arrayListOf("管理", "管", "gl"), "机器人管理员命令", UserLevel.ADMIN)

    override fun getHelp(): String = """
        /admin help 展示此帮助列表
        /admin permlist [用户] 查看用户拥有的权限
        /admin permadd [用户] [权限名] 给一个用户添加权限
        /admin give [用户] [命令条数] 给一个用户添加命令条数
    """.trimIndent()

    private fun hasPermission(user: CometUser, e: MessageEvent): Boolean {
        if (user.hasPermission(props.permissionNodeName)) return true
        if (e is GroupMessageEvent && e.sender.permission != MemberPermission.MEMBER) return true
        return false
    }
}