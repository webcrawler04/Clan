package org.samoxive.safetyjim.discord

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.samoxive.safetyjim.database.JimSettings

abstract class Command {
    abstract val usages: Array<String>
    abstract suspend fun run(bot: DiscordBot, event: GuildMessageReceivedEvent, settings: JimSettings, args: String): Boolean
}
