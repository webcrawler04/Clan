package org.samoxive.safetyjim.discord.commands

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.ocpsoft.prettytime.PrettyTime
import org.samoxive.safetyjim.config.JimConfig
import org.samoxive.safetyjim.database.JimBanTable
import org.samoxive.safetyjim.database.JimSettings
import org.samoxive.safetyjim.database.awaitTransaction
import org.samoxive.safetyjim.discord.*
import java.awt.Color
import java.util.*

class Info : Command() {
    override val usages = arrayOf("info - displays some information about the bot")
    private val supportServer = "https://discord.io/safetyjim"
    private val githubLink = "https://github.com/samoxive/safetyjim"
    private val botInviteLink = "https://discordapp.com/oauth2/authorize?client_id=313749262687141888&permissions=268446790&scope=bot"
    private val patreonLink = "https://www.patreon.com/safetyjim"
    private val prettyTime = PrettyTime()

    override suspend fun run(bot: DiscordBot, event: GuildMessageReceivedEvent, settings: JimSettings, args: String): Boolean {
        val config = bot.config
        val currentShard = event.jda
        val shards = bot.shards.map { shard -> shard.jda }
        val guild = event.guild
        val selfUser = currentShard.selfUser
        val message = event.message
        val channel = event.channel

        val shardCount = shards.size
        val shardId = getShardIdFromGuildId(guild.idLong, shardCount)
        val shardString = getShardString(shardId, shardCount)

        val uptimeString = prettyTime.format(bot.startTime)

        val guildCount = bot.guildCount
        val userCount = shards
                .asSequence()
                .map { shard -> shard.users.size }
                .sum()
        val pingShard = currentShard.ping
        val pingAverage = shards
                .asSequence()
                .map { shard -> shard.ping }
                .sum() / shardCount

        val runtime = Runtime.getRuntime()
        val ramTotal = runtime.totalMemory() / (1024 * 1024)
        val ramUsed = ramTotal - runtime.freeMemory() / (1024 * 1024)

        val lastBanRecord = awaitTransaction {
            JimBanTable.select { JimBanTable.guildid eq guild.idLong }
                    .orderBy(JimBanTable.bantime to SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
        }

        var daysSince = "\u221E" // Infinity symbol

        if (lastBanRecord != null) {
            val now = Date()
            val dayCount = (now.time / 1000 - lastBanRecord[JimBanTable.bantime]) / (60 * 60 * 24)
            daysSince = dayCount.toString()
        }

        val embed = EmbedBuilder()
        embed.setAuthor("Safety Jim - v${config[JimConfig.version]} - Shard $shardString", null, selfUser.avatarUrl)
        embed.setDescription("Lifting the :hammer: since $uptimeString")
        embed.addField("Server Count", guildCount.toString(), true)
        embed.addField("User Count", userCount.toString(), true)
        embed.addBlankField(true)
        embed.addField("Websocket Ping", "Shard $shardString: ${pingShard}ms\nAverage: ${pingAverage}ms", true)
        embed.addField("RAM usage", "${ramUsed}MB / ${ramTotal}MB", true)
        embed.addBlankField(true)
        embed.addField("Links", "[Support]($supportServer) | [Github]($githubLink) | [Invite]($botInviteLink) | [Patreon]($patreonLink)", true)
        embed.setFooter("Made by Samoxive#8634. | Days since last incident: $daysSince", null)
        embed.setColor(Color(0x4286F4))

        message.successReact(bot)
        channel.trySendMessage(embed.build())

        return false
    }
}
