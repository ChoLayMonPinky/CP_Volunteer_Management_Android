package dev.tintwym.data.remote

import java.time.LocalDate

enum class EventBucket { Open, Past, Draft, Other }

fun EventDto.bucket(today: LocalDate = LocalDate.now()): EventBucket {
  val status = status.uppercase()
  return when (status) {
    "DRAFT" -> EventBucket.Draft
    "COMPLETED", "CANCELLED" -> EventBucket.Past
    "OPEN", "IN_PROGRESS", "CLOSED" -> EventBucket.Open
    else -> try {
      val d = LocalDate.parse(date.take(10))
      if (d.isBefore(today)) EventBucket.Past else EventBucket.Open
    } catch (_: Exception) {
      EventBucket.Other
    }
  }
}

fun List<EventDto>.sortedByDateDesc(): List<EventDto> =
  sortedByDescending { it.date }

fun List<EventDto>.sortedByDateAsc(): List<EventDto> =
  sortedBy { it.date }

fun List<EventDto>.inBucket(bucket: EventBucket): List<EventDto> =
  filter { it.bucket() == bucket }.let {
    if (bucket == EventBucket.Past) it.sortedByDateDesc() else it.sortedByDateAsc()
  }

fun EventDto.friendlyWhen(): String {
  val time = startTime.hhmm()
  return try {
    val d = LocalDate.parse(date.take(10))
    val today = LocalDate.now()
    when {
      d == today -> "Today · $time"
      d == today.plusDays(1) -> "Tomorrow · $time"
      d.isBefore(today) -> "$date · $time"
      else -> "$date · $time"
    }
  } catch (_: Exception) {
    "$date · $time"
  }
}

/** Safe HH:mm slice for "09:30:00" or short strings. */
fun String.hhmm(): String = if (length >= 5) take(5) else this
