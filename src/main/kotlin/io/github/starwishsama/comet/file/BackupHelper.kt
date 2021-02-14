package io.github.starwishsama.comet.file

import cn.hutool.core.io.file.FileWriter
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.BotVariables.cfg
import io.github.starwishsama.comet.BotVariables.daemonLogger
import io.github.starwishsama.comet.utils.FileUtil
import io.github.starwishsama.comet.utils.NumberUtil.toLocalDateTime
import io.github.starwishsama.comet.utils.TaskUtil
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

object BackupHelper {
    private val location: File = FileUtil.getChildFolder("backups")
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

    private fun createBackup(){
        try {
            if (!location.exists()) {
                location.mkdirs()
            }

            val backupTime = LocalDateTime.now()
            val backupFileName = "backup-${dateFormatter.format(backupTime)}.json"

            val backupFile = File(location, backupFileName)
            backupFile.createNewFile()
            FileWriter.create(backupFile, Charsets.UTF_8)
                    .write(BotVariables.nullableGson.toJson(BotVariables.users))
            BotVariables.logger.info("[备份] 备份成功! 文件名是 $backupFileName")
        } catch (e: Exception) {
            BotVariables.logger.error("[备份] 尝试备份时发生了异常", e)
        }
    }

    fun scheduleBackup() =
        TaskUtil.runScheduleTaskAsync(cfg.autoSaveTime, cfg.autoSaveTime, TimeUnit.MINUTES, BackupHelper::createBackup)

    @OptIn(ExperimentalTime::class)
    fun checkOldFiles() {
        if (cfg.autoCleanDuration < 1) return

        var counter = 0
        var totalSize = 0L

        val files = mutableListOf<File>()

        files.addAll(FileUtil.getChildFolder("logs").listFiles() ?: arrayOf())
        files.addAll(FileUtil.getErrorReportFolder().listFiles() ?: arrayOf())
        files.addAll(location.listFiles() ?: arrayOf())

        files.forEach { f ->
            val modifiedTime = f.lastModified().toLocalDateTime(true)
            val currentTime = LocalDateTime.now()
            if (Duration.between(modifiedTime, currentTime).toKotlinDuration().inDays > cfg.autoCleanDuration) {
                try {
                    totalSize += f.length()
                    f.delete()
                    counter++
                } catch (e: Exception) {
                    daemonLogger.warning("删除旧文件失败", e)
                }
            }
        }

        if (counter > 0) daemonLogger.info("已成功清理 $counter 个旧文件, 节省了 ${totalSize / 1024 / 1024} MB")
    }
}