import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.cache.CacheFlag

fun main() {
    light(System.getenv("DISCORD_TOKEN"), true) {
        enableCache(CacheFlag.MEMBER_OVERRIDES)
        addEventListeners(DevilsAide())
    }.apply { registerCommands() }
}

fun JDA.registerCommands() = updateCommands {
    slash("session", "Control your help session") {
        isGuildOnly = true
        defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND)

        subcommand("start", "Start a new help session")
        subcommand("end", "End your current help session")
    }

    Commands.user("Add to session")
        .apply {
            isGuildOnly = true
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
        }.also { addCommands(it) }

    Commands.user("Remove from session")
        .apply {
            isGuildOnly = true
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
        }.also { addCommands(it) }
}.queue()