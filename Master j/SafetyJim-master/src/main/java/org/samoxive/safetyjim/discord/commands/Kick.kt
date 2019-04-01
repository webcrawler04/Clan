package org.samoxive.safetyjim.discord.commands

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.samoxive.safetyjim.database.JimKick
import org.samoxive.safetyjim.database.JimSettings
import org.samoxive.safetyjim.database.awaitTransaction
import org.samoxive.safetyjim.discord.*
import java.awt.Color
import java.util.*

class Kick : Command() {
    override val usages = arrayOf("kick @user [reason] - kicks the user with the specified reason")

    override suspend fun run(bot: DiscordBot, event: GuildMessageReceivedEvent, settings: JimSettings, args: String): Boolean {
        val messageIterator = Scanner(args)
        val shard = event.jda

        val member = event.member
        val user = event.author
        val message = event.message
        val channel = event.channel
        val guild = event.guild
        val selfMember = guild.selfMember

        if (!member.hasPermission(Permission.KICK_MEMBERS)) {
            message.failMessage(bot, "You don't have enough permissions to execute this command! Required permission: Kick Members")
            return false
        }

        if (args.isEmpty()) {
            return true
        }

        val (searchResult, kickUser) = messageIterator.findUser(message)
        if (searchResult == SearchUserResult.NOT_FOUND || (kickUser == null)) {
            message.failMessage(bot, "Could not find the user to kick!")
            return false
        }

        if (searchResult == SearchUserResult.GUESSED) {
            message.askConfirmation(bot, kickUser) ?: return false
        }

        val kickMember = guild.getMember(kickUser)
        val controller = guild.controller

        if (!selfMember.hasPermission(Permission.KICK_MEMBERS)) {
            message.failMessage(bot, "I don't have enough permissions to do that!")
            return false
        }

        if (user == kickUser) {
            message.failMessage(bot, "You can't kick yourself, dummy!")
            return false
        }

        if (kickMember != null && !kickMember.isKickableBy(selfMember)) {
            message.failMessage(bot, "I don't have enough permissions to do that!")
            return false
        }

        var reason = messageIterator.seekToEnd()
        reason = if (reason == "") "No reason specified" else reason

        val now = Date()

        val embed = EmbedBuilder()
        embed.setTitle("Kicked from " + guild.name)
        embed.setColor(Color(0x4286F4))
        embed.setDescription("You were kicked from " + guild.name)
        embed.addField("Reason:", truncateForEmbed(reason), false)
        embed.setFooter("Kicked by " + user.getUserTagAndId(), null)
        embed.setTimestamp(now.toInstant())

        kickUser.trySendMessage(embed.build())

        try {
            val auditLogReason = "Kicked by ${user.getUserTagAndId()} - $reason"
            controller.kick(kickUser.id, auditLogReason).await()
            message.successReact(bot)

            val record = awaitTransaction {
                JimKick.new {
                    userid = kickUser.idLong
                    moderatoruserid = user.idLong
                    guildid = guild.idLong
                    kicktime = now.time / 1000
                    this.reason = reason
                }
            }

            message.createModLogEntry(shard, settings, kickUser, reason, "kick", record.id.value, null, false)
            channel.trySendMessage("Kicked " + kickUser.getUserTagAndId())
        } catch (e: Exception) {
            message.failMessage(bot, "Could not kick the specified user. Do I have enough permissions?")
        }

        return false
    }
}
