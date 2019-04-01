package org.samoxive.safetyjim.server.endpoints

import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.json.Json
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import org.samoxive.safetyjim.database.awaitTransaction
import org.samoxive.safetyjim.database.createGuildSettings
import org.samoxive.safetyjim.database.deleteGuildSettings
import org.samoxive.safetyjim.database.getGuildSettings
import org.samoxive.safetyjim.discord.DiscordBot
import org.samoxive.safetyjim.server.AuthenticatedGuildEndpoint
import org.samoxive.safetyjim.server.Result
import org.samoxive.safetyjim.server.Status
import org.samoxive.safetyjim.server.endJson
import org.samoxive.safetyjim.server.entities.GuildSettingsEntity
import org.samoxive.safetyjim.server.entities.toChannelEntity
import org.samoxive.safetyjim.server.entities.toGuildEntity
import org.samoxive.safetyjim.server.entities.toRoleEntity
import org.samoxive.safetyjim.tryhard
import org.samoxive.safetyjim.tryhardAsync

class GetGuildSettingsEndpoint(bot: DiscordBot) : AuthenticatedGuildEndpoint(bot) {
    override val route = "/guilds/:guildId/settings"
    override val method = HttpMethod.GET

    override suspend fun handle(event: RoutingContext, request: HttpServerRequest, response: HttpServerResponse, user: User, guild: Guild, member: Member): Result {
        val guildSettingsDb = getGuildSettings(guild, bot.config)
        val holdingRoomRole = if (guildSettingsDb.holdingroomroleid != null) guild.getRoleById(guildSettingsDb.holdingroomroleid!!) else null // kotlin pls
        val settings = GuildSettingsEntity(
                guild.toGuildEntity(),
                guild.textChannels.map { it.toChannelEntity() },
                guild.roles.map { it.toRoleEntity() },
                guildSettingsDb.modlog,
                guild.getTextChannelById(guildSettingsDb.modlogchannelid)?.toChannelEntity()
                        ?: return Result(Status.SERVER_ERROR),
                guildSettingsDb.holdingroom,
                holdingRoomRole?.toRoleEntity(),
                guildSettingsDb.holdingroomminutes,
                guildSettingsDb.invitelinkremover,
                guildSettingsDb.welcomemessage,
                guildSettingsDb.message,
                guild.getTextChannelById(guildSettingsDb.welcomemessagechannelid)?.toChannelEntity()
                        ?: return Result(Status.SERVER_ERROR),
                guildSettingsDb.prefix,
                guildSettingsDb.silentcommands,
                guildSettingsDb.nospaceprefix,
                guildSettingsDb.statistics,
                guildSettingsDb.joincaptcha
        )

        response.endJson(Json.stringify(GuildSettingsEntity.serializer(), settings))
        return Result(Status.OK)
    }
}

class PostGuildSettingsEndpoint(bot: DiscordBot) : AuthenticatedGuildEndpoint(bot) {
    override val route = "/guilds/:guildId/settings"
    override val method = HttpMethod.POST

    override suspend fun handle(event: RoutingContext, request: HttpServerRequest, response: HttpServerResponse, user: User, guild: Guild, member: Member): Result {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            return Result(Status.FORBIDDEN, "You need to be an administrator to change server settings!")
        }
        val bodyString = event.bodyAsString ?: return Result(Status.BAD_REQUEST)
        val newSettings = tryhard { Json.parse(GuildSettingsEntity.serializer(), bodyString) }
                ?: return Result(Status.BAD_REQUEST)

        guild.textChannels.find { it.id == newSettings.modLogChannel.id }
                ?: return Result(Status.BAD_REQUEST, "Selected moderator log channel doesn't exist!")
        guild.textChannels.find { it.id == newSettings.welcomeMessageChannel.id }
                ?: return Result(Status.BAD_REQUEST, "Selected welcome message channel doesn't exist!")
        if (newSettings.holdingRoomRole != null) {
            guild.roles.find { it.id == newSettings.holdingRoomRole.id }
                    ?: return Result(Status.BAD_REQUEST, "Selected holding room role doesn't exist!")
        } else {
            if (newSettings.joinCaptcha || newSettings.holdingRoom) {
                return Result(Status.BAD_REQUEST, "You can't enable join captcha or holding room without setting a holding room role!")
            }
        }

        if (newSettings.joinCaptcha && newSettings.holdingRoom) {
            return Result(Status.BAD_REQUEST, "You can't enable both holding room and join captcha at the same time!")
        }

        val message = newSettings.message
        val prefix = newSettings.prefix
        if (message.isEmpty() || prefix.isEmpty()) {
            return Result(Status.BAD_REQUEST, "")
        } else {
            if (prefix.split(" ").size != 1) {
                return Result(Status.BAD_REQUEST, "Prefix can't be multiple words!")
            }

            if (prefix.length >= 1000) {
                return Result(Status.BAD_REQUEST, "Prefix can't be too long!")
            }

            if (message.length >= 1750) {
                return Result(Status.BAD_REQUEST, "Welcome message can't be too long!")
            }
        }

        if (newSettings.statistics) {
            return Result(Status.BAD_REQUEST, "Statistics option isn't open to public yet!")
        }

        if (newSettings.guild.id != guild.id) {
            return Result(Status.BAD_REQUEST)
        }

        val guildSettingsDb = getGuildSettings(guild, bot.config)
        tryhardAsync {
            awaitTransaction {
                guildSettingsDb.modlog = newSettings.modLog
                guildSettingsDb.modlogchannelid = newSettings.modLogChannel.id.toLong()
                guildSettingsDb.holdingroom = newSettings.holdingRoom
                guildSettingsDb.holdingroomroleid = newSettings.holdingRoomRole?.id?.toLong()
                guildSettingsDb.holdingroomminutes = newSettings.holdingRoomMinutes
                guildSettingsDb.invitelinkremover = newSettings.inviteLinkRemover
                guildSettingsDb.welcomemessage = newSettings.welcomeMessage
                guildSettingsDb.message = newSettings.message
                guildSettingsDb.welcomemessagechannelid = newSettings.welcomeMessageChannel.id.toLong()
                guildSettingsDb.prefix = newSettings.prefix
                guildSettingsDb.silentcommands = newSettings.silentCommands
                guildSettingsDb.nospaceprefix = newSettings.noSpacePrefix
                guildSettingsDb.statistics = newSettings.statistics
                guildSettingsDb.joincaptcha = newSettings.joinCaptcha
            }
        } ?: return Result(Status.SERVER_ERROR)

        response.end()
        return Result(Status.OK)
    }
}

class ResetGuildSettingsEndpoint(bot: DiscordBot) : AuthenticatedGuildEndpoint(bot) {
    override val route = "/guilds/:guildId/settings"
    override val method = HttpMethod.DELETE

    override suspend fun handle(event: RoutingContext, request: HttpServerRequest, response: HttpServerResponse, user: User, guild: Guild, member: Member): Result {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            return Result(Status.FORBIDDEN)
        }

        deleteGuildSettings(guild)
        createGuildSettings(guild, bot.config)
        response.end()
        return Result(Status.OK)
    }
}