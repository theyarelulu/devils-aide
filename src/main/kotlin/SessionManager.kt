import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Category
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A container which assigned for each guild to facilitate session management.
 */
class SessionManager(category: Category) {
    /** A property to store the category channel's id. */
    private val categoryId = category.idLong
    /** A thread-safe counter for the number of sessions created. */
    private val sessionCounter = AtomicInteger()
    /* A thread-safe map to match users with their respective session channels. */
    private val sessions = ConcurrentHashMap<Long, Deferred<Long>>()
    /** A coroutine scope to launch asynchronous tasks, also attached to an exception handler. */
    private val scope = CoroutineScope(Dispatchers.IO) + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    init {
        scope.launch {
            // add session channels to the map, associating them by user id
            category.textChannels
                .associateTo(sessions) { it.memberPermissionOverrides.single().idLong to CompletableDeferred(it.idLong) }
        }
    }

    /**
     * Processes a slash command issued by the user.
     */
    fun slashCommand(event: SlashCommandInteractionEvent) {
        when(event.subcommandName) {
            "start" -> userInitiatedSessionStart(event)
            "end" -> userInitiatedSessionEnd(event)
        }
    }

    /**
     * Processes a user context menu command issued by an admin.
     */
    fun userContext(event: UserContextInteractionEvent) {
        when(event.name) {
            "Add to session" -> adminInitiatedSessionStart(event)
            "Remove from session" -> adminInitiatedSessionEnd(event)
        }
    }

    /**
     * Processes start requests initiated by admins.
     */
    private fun adminInitiatedSessionStart(event: UserContextInteractionEvent) = scope.launch {
        val target = event.interaction.targetMember!!
        val sessionId = sessions.computeIfAbsent(target.idLong) { event.jda.createChannelAsync(event.member!!, target) }

        event.takeUnless { sessionId.isCompleted } // when session doesn't exist
            ?.deferReply(true)?.await() // defer the reply
            ?.editOriginal("${target.asMention}'s new session is ready: <#${sessionId.await()}>")?.await()
            ?: event.reply("${target.asMention} already has an active session: <#${sessionId.await()}>")
                .setEphemeral(true).await()
    }

    /**
     * Processes close requests initiated by admin.
     */
    private fun adminInitiatedSessionEnd(event: UserContextInteractionEvent) = scope.launch {
        val target = event.interaction.targetMember!!
        val sessionId = sessions.remove(target.idLong)?.await()

        val interaction = sessionId?.let { event.deferReply(true).await() } // when session exists, defer reply
            ?.also { event.jda.deleteChannelAsync(sessionId, event.member!!, target).await() } // delete the channel
            ?: event.reply("${target.asMention} does not have an active session").setEphemeral(true).await()

        sessionId?.let { interaction }
            ?.takeUnless { event.channel?.idLong == sessionId } // protection against replying in deleted channels
            ?.editOriginal("${target.asMention}'s session has been successfully ended")?.await()
    }

    /**
     * Processes session start requests initiated by the user.
     */
    private fun userInitiatedSessionStart(event: SlashCommandInteractionEvent) = scope.launch {
        val sessionId = sessions.computeIfAbsent(event.member!!.idLong) { event.jda.createChannelAsync(event.member!!) }

        event.takeUnless { sessionId.isCompleted } // when session doesn't exist
            ?.deferReply(true)?.await() // defer the reply
            ?.editOriginal("Your new session is ready: <#${sessionId.await()}>")?.await()
            ?: event.reply("You already have an active session: <#${sessionId.await()}>") // when session exists
                .setEphemeral(true).await()
    }

    /**
     * Processes session close requests initiated by the user.
     */
    private fun userInitiatedSessionEnd(event: SlashCommandInteractionEvent) = scope.launch {
        val sessionId = sessions.remove(event.user.idLong)?.await()

        val interaction = sessionId?.let { event.deferReply(true).await() } // when session exists, defer reply
            ?.also { event.jda.deleteChannelAsync(sessionId, event.member!!).await() } // then delete the channel
            ?: event.reply("You do not have an active session").setEphemeral(true).await()

        sessionId?.let { interaction }
            ?.takeUnless { event.channel.idLong == sessionId } // protection against replying in deleted channels
            ?.editOriginal("Your session has been successfully ended")?.await()
    }

    /**
     * Creates a channel for [target], as requested by [issuer].
     */
    private fun JDA.createChannelAsync(issuer: Member, target: Member) = scope.async {
        var reason = "Requested by ${issuer.effectiveName} (${issuer.idLong})"

        if (issuer.idLong != target.idLong) // if issued by an admin, update the reason accordingly
            reason += " on behalf of ${target.effectiveName} (${target.idLong})"

        getCategoryById(categoryId)!! // get category or throw an exception if it doesn't exist
            // create a channel with the counter number
            .createTextChannel("session-${sessionCounter.incrementAndGet()}")
            // only allow the issuing user to see the channel
            .addMemberPermissionOverride(target.idLong, mutableListOf(Permission.VIEW_CHANNEL), null)
            .reason(reason)
            .await() // wait for the request to complete
            .idLong // return the channel's id
    }
    
    /**
     * An overload for user initiated channel creates.
     */
    private fun JDA.createChannelAsync(issuer: Member) =
        createChannelAsync(issuer, issuer)

    /**
     * Deletes a [target] if requested by an [issuer] on behalf of [target]
     */
    private fun JDA.deleteChannelAsync(targetChannel: Long, issuer: Member, target: Member) = scope.async {
        var reason = "Requested by ${issuer.effectiveName} (${issuer.idLong})"

        if (issuer.idLong != target.idLong) // if issued by an admin, update the reason accordingly
            reason += " on behalf of ${target.effectiveName} (${target.idLong})"

        getTextChannelById(targetChannel)?.delete()?.reason(reason)?.await()

    }

    /**
     * An overload for user initiated channel deletes.
     */
    private fun JDA.deleteChannelAsync(targetChannel: Long, issuer: Member) =
        deleteChannelAsync(targetChannel, issuer, issuer)
}