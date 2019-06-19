package io.paulocosta.twitbooks.service

import arrow.core.Either
import io.paulocosta.twitbooks.auth.TwitterProvider
import io.paulocosta.twitbooks.entity.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.social.twitter.api.TwitterProfile
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SyncFriendsService @Autowired constructor(
        val friendService: FriendService,
        val friendSyncStatusService: FriendSyncStatusService,
        val rateLimitService: RateLimitService,
        val twitterProvider: TwitterProvider
) {

    fun sync(user: User): SyncResult {
        logger.info { "Starting to sync users" }
        val rateLimit = when (val limitResult = rateLimitService.getFriendRateLimits(user.accessToken)) {
            is Either.Left -> limitResult.a
            is Either.Right -> return SyncResult.ERROR
        }

        return if (rateLimit.exceeded()) {
            logger.info { "Rate limit has been exceeded for the list friends API" }
            SyncResult.ERROR
        } else {
            val latestSync = friendSyncStatusService.getLatestFriendSyncStatus(user)
            return when (latestSync.status) {
                Status.ABSENT -> {
                    logger.info { "No previous user sync status present. Starting full sync" }
                    doSync(user, rateLimit, null)
                }
                Status.SUCCESS -> {
                    logger.info { "Previous user sync was successful. Skipping further synchronization" }
                    SyncResult.SUCCESS
                }
                Status.FAILED -> {
                    logger.info { "Previous sync failed. Attempt to synchronize with previous cursor" }
                    doSync(user, rateLimit, latestSync.cursorId)
                }
            }
        }
    }

    private fun doSync(user: User, rateLimit: RateLimit, cursor: Long?): SyncResult {
        var hits = 0
        var lastCursor = cursor
        while (hits < rateLimit.remainingHits) {
            val profiles = if (hits == 0 && cursor == null) {
                twitterProvider.getTwitter(user.accessToken).friendOperations().friends
            } else {
                twitterProvider.getTwitter(user.accessToken).friendOperations().getFriendsInCursor(lastCursor!!)
            }

            hits++

            friendService.saveFriends(profiles.map { toFriend(it, user) })
            lastCursor = profiles.nextCursor

            if (!profiles.hasNext()) {
                friendSyncStatusService.createSuccessEvent(user)
                return SyncResult.SUCCESS
            }
        }
        friendSyncStatusService.createSyncFailedEvent(user, lastCursor)
        return SyncResult.ERROR
    }

    private fun toFriend(profile: TwitterProfile, user: User): Friend {
        return Friend(
                profile.id,
                profile.name,
                profile.screenName,
                MessageSyncStrategy.DEPTH,
                user)
    }

}
