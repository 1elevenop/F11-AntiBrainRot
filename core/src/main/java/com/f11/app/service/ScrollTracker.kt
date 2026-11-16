package com.f11.app.service

/**
 * Pure, clock-free swipe limiter. Callers pass event timestamps (ms), which keeps it
 * deterministic and unit-testable.
 *
 * - A burst of scroll events closer together than [debounceMs] is one swipe.
 * - The user may make [swipeLimit] swipes; the next one raises the shield for [cooldownMs].
 * - A session resets after [sessionIdleResetMs] without swipes.
 */
class ScrollTracker(
    swipeLimit: Int,
    cooldownMs: Long,
    val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    val sessionIdleResetMs: Long = DEFAULT_SESSION_IDLE_RESET_MS,
    initialBlockedUntil: Long = 0L,
) {

    sealed interface Result {
        /** A new swipe was counted. */
        data class Counted(val count: Int, val remaining: Int) : Result

        /** Part of a swipe that was already counted. */
        data object Debounced : Result

        /** This swipe exceeded the limit; the shield should go up until [blockedUntil]. */
        data class LimitReached(val blockedUntil: Long) : Result

        /** A cooldown is already running. */
        data class Blocked(val blockedUntil: Long) : Result
    }

    var swipeLimit: Int = swipeLimit
        set(value) {
            require(value > 0) { "swipeLimit must be > 0" }
            field = value
        }

    var cooldownMs: Long = cooldownMs
        set(value) {
            require(value > 0) { "cooldownMs must be > 0" }
            field = value
        }

    var swipeCount: Int = 0
        private set

    var blockedUntil: Long = initialBlockedUntil
        private set

    private var lastEventAt: Long = NONE
    private var lastSwipeAt: Long = NONE

    init {
        require(debounceMs >= 0) { "debounceMs must be >= 0" }
        require(sessionIdleResetMs > 0) { "sessionIdleResetMs must be > 0" }
        this.swipeLimit = swipeLimit
        this.cooldownMs = cooldownMs
    }

    fun isBlocked(now: Long): Boolean = now < blockedUntil

    fun remainingCooldownMs(now: Long): Long = (blockedUntil - now).coerceAtLeast(0L)

    fun onScrollEvent(now: Long): Result {
        if (isBlocked(now)) {
            lastEventAt = now
            return Result.Blocked(blockedUntil)
        }
        if (blockedUntil != 0L) {
            // Cooldown finished: start a fresh session.
            blockedUntil = 0L
            swipeCount = 0
            lastSwipeAt = NONE
        }

        val sameGesture = lastEventAt != NONE && now >= lastEventAt && now - lastEventAt < debounceMs
        lastEventAt = now
        if (sameGesture) return Result.Debounced

        if (lastSwipeAt != NONE && now - lastSwipeAt >= sessionIdleResetMs) {
            swipeCount = 0
        }
        lastSwipeAt = now
        swipeCount++

        if (swipeCount > swipeLimit) {
            blockedUntil = now + cooldownMs
            swipeCount = 0
            return Result.LimitReached(blockedUntil)
        }
        return Result.Counted(swipeCount, swipeLimit - swipeCount)
    }

    /** Clears the session and any running cooldown. */
    fun reset() {
        swipeCount = 0
        blockedUntil = 0L
        lastEventAt = NONE
        lastSwipeAt = NONE
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 500L
        const val DEFAULT_SESSION_IDLE_RESET_MS = 10 * 60 * 1000L
        private const val NONE = Long.MIN_VALUE
    }
}

/**
 * Debounce-only swipe detector, used to keep counting swipes when the guard is off
 * (awareness without enforcement).
 */
class SwipeDebouncer(val debounceMs: Long = ScrollTracker.DEFAULT_DEBOUNCE_MS) {
    private var lastEventAt: Long = Long.MIN_VALUE

    /** True when [now] starts a new swipe rather than continuing the current one. */
    fun isNewSwipe(now: Long): Boolean {
        val sameGesture = lastEventAt != Long.MIN_VALUE && now >= lastEventAt && now - lastEventAt < debounceMs
        lastEventAt = now
        return !sameGesture
    }
}
