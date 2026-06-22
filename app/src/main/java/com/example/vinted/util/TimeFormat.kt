package com.example.vinted.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Converts between Supabase `timestamptz` strings (ISO-8601 with offset, e.g.
 * `2026-06-22T10:05:20.346727+00:00`) and the values the UI needs, always in the device's local
 * time zone. Centralised so every message — sent, loaded, or streamed over Realtime — is stamped
 * and rendered consistently.
 */
object TimeFormat {

    private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

    /** The current device clock as an ISO-8601 instant, suitable for storing in a `timestamptz`. */
    fun nowIso(): String = Instant.now().toString()

    /** Renders an ISO timestamp as `HH:mm` in the device's local time zone. */
    fun toLocalTimeLabel(iso: String): String =
        runCatching {
            OffsetDateTime.parse(iso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(hourMinute)
        }.getOrDefault(iso.take(16).replace("T", " "))

    /** Epoch millis for chronological sorting; unparseable input sorts oldest (0). */
    fun toEpochMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)
}
