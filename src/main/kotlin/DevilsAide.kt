import kotlinx.coroutines.Deferred
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ConcurrentHashMap

/**
 * A container for guilds and session managers, providing an interface to forward events.
 */
class DevilsAide : ListenerAdapter() {
    /* A thread-safe map to match guilds with their respective session managers. */
    private val sessionManagers = ConcurrentHashMap<Long, SessionManager>()

    /**
     * Processes a guild's [event] once it becomes ready, adding it to the cache.
     */
    override fun onGuildReady(event: GuildReadyEvent) {
        // gets the category only if one match is found
        event.guild.getCategoriesByName("Help Sessions", true).singleOrNull()
            ?.also { category ->
                // atomically adds a new session manager to the map if none exists
                sessionManagers.computeIfAbsent(event.guild.idLong) { SessionManager(category) }
            }
    }

    /**
     * Processes a slash command issued inside a guild by sending it to the appropriate session manager.
     */
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        event.guild?.idLong?.let { sessionManagers[it] }?.slashCommand(event)
    }

    /**
     * Processes a user command issued inside a guild by sending it to the appropriate session manager.
     */
    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        event.guild?.idLong?.let { sessionManagers[it] }?.userContext(event)
    }
}