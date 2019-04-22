package io.paulocosta.twitbooks.service

import arrow.core.Either
import io.paulocosta.twitbooks.entity.SyncResult
import io.paulocosta.twitbooks.ratelimit.RateLimitKeeper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Service dedicated to sync messages from friends. I'm still
 * unsure on the exact method of obtaining entity. I could start
 * from the timelines and going down on the tweet chain but I would
 * miss the new tweets that way.
 *
 * On the other hand I could try finding the oldest tweet from the
 * friend and going up from there. But that way I might take too long
 * to synchronize the entity.
 *
 * For an MVP one I think the first approach could be better.
 * **/

private val logger = KotlinLogging.logger {}

@Service
class SyncService @Autowired constructor(
        val userService: UserService,
        val syncFriendsService: SyncFriendsService,
        val messageService: MessageService,
        val rateLimitService: RateLimitService) {

    @Value("\${sync.enabled}")
    var syncEnabled: Boolean = false

    companion object {
        const val SYNC_DELAY_MILLIS = 4L * 36L * 100000L
    }

    @Scheduled(fixedDelay = SYNC_DELAY_MILLIS)
    fun sync() {
        if (syncEnabled) {
            logger.info { "Starting sync" }
            syncUsers()
            syncMessages()
        } else {
            logger.info { "Sync disabled by config property" }
        }
    }

    private fun syncMessages() {

        val rateLimit = when(val eitherLimit = rateLimitService.getTimelineRateLimits()) {
            is Either.Left -> eitherLimit.a
            else -> {
                logger.info { "Rate limit exceeded. Stopping message sync" }
                return
            }
        }

        val rateLimitKeeper = RateLimitKeeper(rateLimit)

        for (it in userService.getAllUsers()) {
            if (messageService.syncMessages(it, rateLimitKeeper) == SyncResult.ERROR) {
                break
            }
        }
    }

    private fun syncUsers() {
        when (syncFriendsService.sync()) {
            SyncResult.SUCCESS -> {
                logger.info { "User sync finished successfully" }
            }
            SyncResult.ERROR -> {
                logger.info { "User sync finished with error" }
            }
        }
    }

}