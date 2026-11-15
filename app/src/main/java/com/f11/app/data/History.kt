package com.f11.app.data

/** One day of usage. [epochDay] is LocalDate.toEpochDay(). */
data class DayStat(val epochDay: Long, val swipes: Int, val shields: Int)

/** Pure helpers for the compact on-disk history format "day:swipes:shields;...". */
object History {

    const val MAX_DAYS = 90

    fun decode(raw: String?): List<DayStat> =
        raw.orEmpty().split(';').mapNotNull { entry ->
            val parts = entry.split(':')
            if (parts.size != 3) return@mapNotNull null
            val day = parts[0].toLongOrNull() ?: return@mapNotNull null
            val swipes = parts[1].toIntOrNull() ?: return@mapNotNull null
            val shields = parts[2].toIntOrNull() ?: return@mapNotNull null
            DayStat(day, swipes.coerceAtLeast(0), shields.coerceAtLeast(0))
        }.sortedBy { it.epochDay }

    fun encode(days: List<DayStat>): String =
        days.sortedBy { it.epochDay }
            .takeLast(MAX_DAYS)
            .joinToString(";") { "${it.epochDay}:${it.swipes}:${it.shields}" }

    /** Replaces (or adds) the entry for [stat]'s day. */
    fun upsert(days: List<DayStat>, stat: DayStat): List<DayStat> =
        (days.filterNot { it.epochDay == stat.epochDay } + stat).sortedBy { it.epochDay }.takeLast(MAX_DAYS)

    /** The last [count] days ending at [today], with zero-filled gaps. */
    fun window(days: List<DayStat>, today: Long, count: Int): List<DayStat> {
        val byDay = days.associateBy { it.epochDay }
        return (today - count + 1..today).map { byDay[it] ?: DayStat(it, 0, 0) }
    }

    /**
     * Consecutive days, counting back from [today], on which the shield never went up.
     * Days before [firstDay] (before F11 was installed) don't count.
     */
    fun focusStreak(days: List<DayStat>, today: Long, firstDay: Long): Int {
        val byDay = days.associateBy { it.epochDay }
        var streak = 0
        var day = today
        while (day >= firstDay && (byDay[day]?.shields ?: 0) == 0) {
            streak++
            day--
        }
        return streak
    }
}
