package org.samoxive.safetyjim.discord.commands

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.jetbrains.exposed.sql.and
import org.samoxive.safetyjim.database.JimMute
import org.samoxive.safetyjim.database.JimMuteTable
import org.samoxive.safetyjim.database.JimSettings
import org.samoxive.safetyjim.database.awaitTransaction
import org.samoxive.safetyjim.discord.*
import java.util.*

class Unmute : Command() {
    override val usages = arrayOf("unmute @user - unmutes specified user")

    override suspend fun run(bot: DiscordBot, event: GuildMessageReceivedEvent, settings: JimSettings, args: String): Boolean {
        val messageIterator = Scanner(args)
        val member = event.member
        val message = event.message
        val guild = event.guild
        val controller = guild.controller

        if (!member.hasPermission(Permission.MANAGE_ROLES)) {
            message.failMessage(bot, "You don't have enough permissions to execute this command! Required permission: Manage Roles")
            return false
        }

        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) {
            message.failMessage(bot, "I don't have enough permissions do this action!")
            return false
        }

        if (args.isEmpty()) {
            return true
        }

        val mutedRoles = guild.getRolesByName("Muted", false)
        if (mutedRoles.size == 0) {
            message.failMessage(bot, "Could not find a role called Muted, please create one yourself or mute a user to set it up automatically.")
            return false
        }

        val (searchResult, unmuteUser) = messageIterator.findUser(message)
        if (searchResult == SearchUserResult.NOT_FOUND || (unmuteUser == null)) {
            message.failMessage(bot, "Could not find the user to unmute!")
            return false
        }

        if (searchResult == SearchUserResult.GUESSED) {
            message.askConfirmation(bot, unmuteUser) ?: return false
        }

        val muteRole = mutedRoles[0]
        val unmuteMember = guild.getMember(unmuteUser)
        try {
            controller.removeSingleRoleFromMember(unmuteMember, muteRole).await()
        } catch (e: Exception) {
            message.failMessage(bot, "Could not unmute the user: \"${unmuteUser.name}\". Do I have enough permissions or is Muted role below me?")
            return false
        }

        awaitTransaction {
            JimMute.find {
                (JimMuteTable.guildid eq guild.idLong) and (JimMuteTable.userid eq unmuteUser.idLong)
            }.forUpdate().forEach { it.unmuted = true }
        }

        message.successReact(bot)
        return false
    }
}
