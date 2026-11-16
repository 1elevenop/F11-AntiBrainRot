package com.f11.app.data

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A recurring Max Focus window, e.g. Work Mon–Fri 09:00–18:00 or Sleep daily 23:00–07:00.
 * Times are minutes after midnight; an end at or before the start runs past midnight.
 * While a schedule is running it cannot be ended, edited or deleted.
 */
data class FocusSchedule(
    val id: Long,
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val days: Set<DayOfWeek>,
    val enabled: Boolean = true,
) {
    val overnight: Boolean get() = endMinute <= startMinute

    /** True when Max Focus should be on at [at]. Days refer to the day the window starts. */
    fun isActive(at: LocalDateTime): Boolean {
        if (!enabled || days.isEmpty()) return false
        val minute = at.hour * 60 + at.minute
        val today = at.dayOfWeek
        return if (!overnight) {
            today in days && minute >= startMinute && minute < endMinute
        } else {
            (today in days && minute >= startMinute) || (today.minus(1) in days && minute < endMinute)
        }
    }

    /** When the window running at [at] ends (only meaningful while [isActive]). */
    fun endsAt(at: LocalDateTime): LocalDateTime {
        val end = at.toLocalDate().atTime(LocalTime.of(endMinute / 60, endMinute % 60))
        return if (end.isAfter(at)) end else end.plusDays(1)
    }

    companion object {
        fun formatMinute(minute: Int): String = String.format(java.util.Locale.US, "%02d:%02d", minute / 60, minute % 60)
    }
}

object FocusSchedules {

    private const val FIELD = '|'
    private const val RECORD = ';'

    fun encode(list: List<FocusSchedule>): String = list.joinToString(RECORD.toString()) { s ->
        val mask = s.days.sumOf { 1 shl (it.value - 1) }
        val safeName = s.name.replace(FIELD, ' ').replace(RECORD, ' ').trim().take(24)
        listOf(s.id, safeName, s.startMinute, s.endMinute, mask, if (s.enabled) 1 else 0).joinToString(FIELD.toString())
    }

    fun decode(raw: String?): List<FocusSchedule> = raw.orEmpty().split(RECORD).mapNotNull { record ->
        val f = record.split(FIELD)
        if (f.size != 6) return@mapNotNull null
        val id = f[0].toLongOrNull() ?: return@mapNotNull null
        val start = f[2].toIntOrNull()?.takeIf { it in 0 until 1440 } ?: return@mapNotNull null
        val end = f[3].toIntOrNull()?.takeIf { it in 0 until 1440 } ?: return@mapNotNull null
        val mask = f[4].toIntOrNull() ?: return@mapNotNull null
        val days = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
        FocusSchedule(id, f[1].ifBlank { "Focus" }, start, end, days, f[5] == "1")
    }

    /** The running schedule that ends last, if any. */
    fun activeAt(list: List<FocusSchedule>, at: LocalDateTime): FocusSchedule? =
        list.filter { it.isActive(at) }.maxByOrNull { it.endsAt(at) }
}
