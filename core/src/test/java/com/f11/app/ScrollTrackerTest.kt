package com.f11.app

import com.f11.app.service.ScrollTracker
import com.f11.app.service.ScrollTracker.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollTrackerTest {

    private val cooldown = 15 * 60 * 1000L

    private fun tracker(limit: Int = 3) = ScrollTracker(
        swipeLimit = limit,
        cooldownMs = cooldown,
        debounceMs = 500L,
        sessionIdleResetMs = 60_000L,
    )

    /** Swipes spaced 2 s apart starting at [start]; returns the timestamp of the last one. */
    private fun ScrollTracker.swipe(times: Int, start: Long = 0L): Long {
        var t = start
        repeat(times) { i ->
            t = start + i * 2_000L
            onScrollEvent(t)
        }
        return t
    }

    @Test
    fun countsDistinctSwipes() {
        val t = tracker()
        assertEquals(Result.Counted(1, 2), t.onScrollEvent(0L))
        assertEquals(Result.Counted(2, 1), t.onScrollEvent(2_000L))
        assertEquals(2, t.swipeCount)
    }

    @Test
    fun burstOfEventsIsOneSwipe() {
        val t = tracker()
        assertEquals(Result.Counted(1, 2), t.onScrollEvent(0L))
        assertEquals(Result.Debounced, t.onScrollEvent(100L))
        assertEquals(Result.Debounced, t.onScrollEvent(450L))
        // Each event extends the gesture, so 800 is still within 500 ms of 450.
        assertEquals(Result.Debounced, t.onScrollEvent(800L))
        assertEquals(1, t.swipeCount)
        assertEquals(Result.Counted(2, 1), t.onScrollEvent(1_400L))
    }

    @Test
    fun allowsExactlyLimitSwipesThenRaisesShield() {
        val t = tracker(limit = 3)
        val last = t.swipe(3)
        assertFalse(t.isBlocked(last))
        val result = t.onScrollEvent(last + 2_000L)
        assertEquals(Result.LimitReached(last + 2_000L + cooldown), result)
        assertTrue(t.isBlocked(last + 2_001L))
    }

    @Test
    fun scrollsDuringCooldownReportBlocked() {
        val t = tracker(limit = 1)
        t.onScrollEvent(0L)
        val reached = t.onScrollEvent(2_000L) as Result.LimitReached
        val blocked = t.onScrollEvent(10_000L)
        assertEquals(Result.Blocked(reached.blockedUntil), blocked)
        assertEquals(reached.blockedUntil - 10_000L, t.remainingCooldownMs(10_000L))
    }

    @Test
    fun cooldownExpiryStartsFreshSession() {
        val t = tracker(limit = 2)
        t.swipe(2)
        val reached = t.onScrollEvent(4_000L) as Result.LimitReached
        val after = reached.blockedUntil + 1_000L
        assertFalse(t.isBlocked(after))
        assertEquals(Result.Counted(1, 1), t.onScrollEvent(after))
        assertEquals(0L, t.remainingCooldownMs(after))
    }

    @Test
    fun idleGapResetsSession() {
        val t = tracker(limit = 3)
        val last = t.swipe(3)
        assertEquals(Result.Counted(1, 2), t.onScrollEvent(last + 60_000L))
    }

    @Test
    fun restoresPersistedCooldown() {
        val t = ScrollTracker(swipeLimit = 5, cooldownMs = cooldown, initialBlockedUntil = 50_000L)
        assertTrue(t.isBlocked(10_000L))
        assertEquals(Result.Blocked(50_000L), t.onScrollEvent(10_000L))
    }

    @Test
    fun loweringLimitAppliesToNextSwipe() {
        val t = tracker(limit = 10)
        val last = t.swipe(4)
        t.swipeLimit = 3
        assertTrue(t.onScrollEvent(last + 2_000L) is Result.LimitReached)
    }

    @Test
    fun resetClearsSessionAndCooldown() {
        val t = tracker(limit = 1)
        t.swipe(2)
        assertTrue(t.isBlocked(3_000L))
        t.reset()
        assertFalse(t.isBlocked(3_000L))
        assertEquals(Result.Counted(1, 0), t.onScrollEvent(3_000L))
    }

    @Test
    fun clockGoingBackwardsCountsAsNewSwipe() {
        val t = tracker()
        t.onScrollEvent(5_000L)
        assertEquals(Result.Counted(2, 1), t.onScrollEvent(4_900L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroLimit() {
        tracker(limit = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveCooldown() {
        ScrollTracker(swipeLimit = 3, cooldownMs = 0L)
    }
}
