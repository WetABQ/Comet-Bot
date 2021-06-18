/*
 * Copyright (c) 2019-2021 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.service.gacha

import com.charleskorn.kaml.Yaml
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.objects.gacha.custom.CustomPool
import io.github.starwishsama.comet.objects.gacha.pool.ArkNightPool
import io.github.starwishsama.comet.objects.gacha.pool.GachaPool
import io.github.starwishsama.comet.utils.*
import io.github.starwishsama.comet.utils.math.MathUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException
import java.util.stream.Collectors

object GachaService {
    val gachaPools = mutableSetOf<GachaPool>()
    private var arkNightUsable = true
    private val poolPath = FileUtil.getChildFolder("gacha")

    fun loadGachaData(arkNight: File) {
        loadArkNightData(arkNight)

        if (arkNightUsable) {
            loadDefaultArkNightData()
        }
    }

    fun loadAllPools() {
        // 载入默认卡池
        if (arkNightUsable) {
            gachaPools.add(ArkNightPool())
        }

        if (!poolPath.exists()) {
            poolPath.mkdirs()
            return
        }

        if (poolPath.listFiles()?.isEmpty() == true) {
            return
        }

        poolPath.listFiles()?.forEach {
            addPoolFromFile(it)
        }

        BotVariables.daemonLogger.info("成功载入了 ${gachaPools.size} 个自定义卡池!")
    }

    fun addPool(gachaPool: CustomPool): Boolean {
        val exists = gachaPools.parallelStream().filter { it.name == gachaPool.poolName }.findAny().isPresent

        if (exists) {
            BotVariables.daemonLogger.warning("已有相同名称的卡池存在! 请检查是否忘记删除了旧文件: ${gachaPool.poolName}")
            return false
        }

        return when (gachaPool.gameType) {
            CustomPool.GameType.ARKNIGHT -> {
                parseArkNightPool(gachaPool).let { gachaPools.add(it) }
            }
        }
    }

    @Suppress("unchecked_cast")
    inline fun <reified T> getPoolsByType(): List<T> {
        return gachaPools.stream().filter { it is T }.collect(Collectors.toList()) as List<T>
    }

    fun addPoolFromFile(poolFile: File) {
        require(poolFile.exists()) { "${poolFile.absolutePath} isn't exists" }

        // 不处理非 YAML 类型文件
        if (!poolFile.name.endsWith("yml") || !poolFile.name.endsWith("yaml")) {
            return
        }

        try {
            val pool = Yaml.default.decodeFromString<CustomPool>(poolFile.getContext())
            addPool(pool)
        } catch (e: IOException) {
            FileUtil.createErrorReportFile("解析卡池信息失败", "gacha", e, "", e.message ?: "")
        }
    }

    fun isArkNightUsable(): Boolean {
        return arkNightUsable
    }

    private fun parseArkNightPool(customPool: CustomPool): ArkNightPool {
        val pool = ArkNightPool(
            customPool.poolName,
            customPool.displayPoolName,
            customPool.poolDescription
        ) {
            (GachaUtil.hasOperator(this.name) || customPool.modifiedGachaItems.stream().filter { it.name == this.name }
                .findAny().isPresent) &&
                    (if (customPool.condition.isNotEmpty()) !customPool.condition.contains(obtain) else true)
        }

        customPool.modifiedGachaItems.forEach { item ->
            val result = pool.poolItems.stream().filter { it.name == item.name }.findAny()

            result.ifPresent {
                if (item.isHidden) {
                    pool.poolItems.remove(it)
                    return@ifPresent
                }

                if (item.probability > 0) {
                    if (item.weight <= 1) {
                        pool.highProbabilityItems[it] = item.probability
                    } else {
                        pool.highProbabilityItems[it] = MathUtil.calculateWeight(
                            pool.poolItems.size,
                            pool.poolItems.filter { poolItem -> poolItem.rare == result.get().rare }.size,
                            item.weight
                        )
                    }
                }
            }.also {
                if (!result.isPresent) {
                    BotVariables.daemonLogger.warning("名为 ${item.name} 的抽卡物品不存在于游戏数据中")
                }
            }
        }

        return pool
    }

    @Deprecated("PCR have no enough data to maintain")
    private fun loadPCRData() {
        BotVariables.daemonLogger.info("公主连结抽卡模拟器已停止维护.")
        /**if (data.exists()) {
        BotVariables.pcr.addAll(DataFiles.pcrData.file.parseAsClass())
        BotVariables.daemonLogger.info("成功载入公主连结游戏数据, 共 ${BotVariables.pcr.size} 个角色")
        } else {
        BotVariables.daemonLogger.info("未检测到公主连结游戏数据, 对应游戏抽卡模拟器将无法使用")
        pcrUsable = false
        }*/
    }

    private fun loadArkNightData(data: File) {
        try {
            GachaUtil.arkNightDataCheck(data)
        } catch (e: IOException) {
            BotVariables.daemonLogger.warning("解析明日方舟游戏数据失败, ${e.message}\n注意: 数据来源于 Github, 国内用户无法下载请自行下载替换\n替换位置: ./res/arkNights.json\n链接: ${GachaUtil.arkNightData}")
        }

        if (data.exists()) {
            BotVariables.mapper.readTree(data).elements().forEach { t ->
                BotVariables.arkNight.add(BotVariables.mapper.readValue(t.traverse()))
            }

            BotVariables.daemonLogger.info("成功载入明日方舟游戏数据, 共 ${BotVariables.arkNight.size} 个干员")
            if (BotVariables.cfg.arkDrawUseImage) {
                if (System.getProperty("java.awt.headless") != "true" && RuntimeUtil.getOsName().lowercase()
                        .contains("linux")
                ) {
                    BotVariables.daemonLogger.info("检测到 Linux 系统, 正在启用无头模式")
                    System.setProperty("java.awt.headless", "true")
                }

                TaskUtil.runAsync {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            GachaUtil.downloadArkNightImage()
                        }
                    }
                }
            }
        } else {
            BotVariables.daemonLogger.info("未检测到明日方舟游戏数据, 抽卡模拟器将无法使用")
            arkNightUsable = false
        }

        BotVariables.daemonLogger.info("[配置] 成功载入配置文件")
    }

    private fun loadDefaultArkNightData() {
        val default = File(FileUtil.getResourceFolder(), "default_arknight.json")

        if (!default.exists()) {
            BotVariables.daemonLogger.warning("无法加载默认明日方舟数据: ${default.name} 不存在")
            return
        }

        val node = BotVariables.mapper.readTree(default)

        node.forEach {
            it.forEach { inside ->
                GachaConstants.arkNightDefault.add(inside.asText())
            }
        }

        BotVariables.daemonLogger.info("加载默认明日方舟数据成功, 共 ${GachaConstants.arkNightDefault.size} 个干员")
    }
}