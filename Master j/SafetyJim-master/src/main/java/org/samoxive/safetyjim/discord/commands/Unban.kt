package org.samoxive.safetyjim.discord.commands

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.samoxive.safetyjim.database.JimBan
import org.samoxive.safetyjim.database.JimBanTable
import org.samoxive.safetyjim.database.JimSettings
import org.samoxive.safetyjim.database.awaitTransaction
import org.samoxive.safetyjim.discord.*
import java.util.*

class Unban : Command() {
    override val usages = arrayOf("unban @user - unbans specified user")

    override suspend fun run(bot: DiscordBot, event: GuildMessageReceivedEvent, settings: JimSettings, args: String): Boolean {
        val messageIterator = Scanner(args)

        val member = event.member
        val message = event.message
        val guild = event.guild
        val selfMember = guild.selfMember
        val controller = guild.controller

        if (!member.hasPermission(Permission.BAN_MEMBERS)) {
            message.failMessage(bot, "You don't have enough permissions to execute this command! Required permission: Ban Members")
            return false
        }

        if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
            message.failMessage(bot, "I do not have enough permissions to do that!")
            return false
        }

        if (args.isEmpty()) {
            return true
        }

        val (searchResult, targetUser) = messageIterator.findBannedUser(message)
        if (searchResult == SearchUserResult.NOT_FOUND || (targetUser == null)) {
            message.failMessage(bot, "Could not find the user to unban!")
            return false
        }

        if (searchResult == SearchUserResult.GUESSED) {
            message.askConfirmation(bot, targetUser) ?: return false
        }

        controller.unban(targetUser).await()

        awaitTransaction {
            JimBan.find {
                (JimBanTable.guildid eq guild.idLong) and (JimBanTable.userid eq targetUser.idLong)
            }.forUpdate().forEach { it.unbanned = true }
        }

        message.successReact(bot)

        return false
    }
}
