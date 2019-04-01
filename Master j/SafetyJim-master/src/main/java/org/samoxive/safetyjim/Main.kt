package org.samoxive.safetyjim

import com.uchuhimo.konf.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.reactiverse.kotlin.pgclient.queryAwait
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.log4j.*
import org.samoxive.safetyjim.config.DatabaseConfig
import org.samoxive.safetyjim.config.JimConfig
import org.samoxive.safetyjim.config.OauthConfig
import org.samoxive.safetyjim.config.ServerConfig
import org.samoxive.safetyjim.database.*
import org.samoxive.safetyjim.discord.DiscordBot
import org.samoxive.safetyjim.server.Server
import org.slf4j.LoggerFactory
import java.io.OutputStreamWriter

fun main() {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    setupLoggers()

    val config = Config {
        addSpec(JimConfig)
        addSpec(DatabaseConfig)
        addSpec(OauthConfig)
        addSpec(ServerConfig)
    }.from.toml.file("config.toml")

    initPgPool(config)
    runBlocking { print(BansTable.insertBan(BanEntity(
            userId = -10,
            moderatorUserId = 10,
            guildId = 10,
            banTime = 0,
            reason = "",
            expireTime = null,
            expires = true,
            unbanned = false
    )))
    }
    val bot = DiscordBot(config)
    Server(bot)
}

fun setupLoggers() {
    val layout = EnhancedPatternLayout("%d{ISO8601} [%-5p] [%t]: %m%n")
    val ca = ConsoleAppender(layout)
    ca.setWriter(OutputStreamWriter(System.out))

    val log = LoggerFactory.getLogger("main")
    val fa = try {
        DailyRollingFileAppender(layout, "logs/jim.log", "'.'yyyy-MM-dd")
    } catch (e: Exception) {
        log.error("Could not access log files!", e)
        return System.exit(1)
    }

    Logger.getLogger("com.joestelmach.natty.Parser").level = Level.WARN
    Logger.getLogger("org.jooq.Constants").level = Level.WARN
    Logger.getRootLogger().addAppender(fa)
    Logger.getRootLogger().addAppender(ca)
    Logger.getRootLogger().level = Level.INFO
}