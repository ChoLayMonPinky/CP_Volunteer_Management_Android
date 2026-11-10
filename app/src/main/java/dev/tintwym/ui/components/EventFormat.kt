package dev.tintwym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.friendlyWhen
import androidx.compose.foundation.border
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.SleekTextTertiary
import dev.tintwym.ui.theme.Space

fun EventDto.subtitleLine(): String {
  val place = venue?.takeIf { it.isNotBlank() } ?: address ?: if (remote) "Remote" else "TBA"
  return "${friendlyWhen()} · $place"
}

fun EventDto.capacityLine(): String = "$registeredCount/$capacity spots · ${status.replace('_', ' ')}"

fun EventDto.isRegistered(): Boolean {
  val s = myRegistrationStatus ?: return false
  return s != "CANCELLED"
}

/** Active signup that can still be withdrawn (not attendance terminal states). */
fun EventDto.canWithdraw(): Boolean {
  val s = myRegistrationStatus ?: return false
  return s == "CONFIRMED" || s == "WAITLIST"
}

/** True when the volunteer can self check-in (backend allows CONFIRMED + NO_SHOW). */
fun EventDto.canSelfCheckIn(): Boolean =
  myRegistrationStatus == "CONFIRMED" || myRegistrationStatus == "NO_SHOW"

fun EventDto.isWaitlisted(): Boolean = myRegistrationStatus == "WAITLIST"

fun EventDto.isCheckedIn(): Boolean =
  myRegistrationStatus == "CHECKED_IN" || myRegistrationStatus == "COMPLETED"

@Composable
fun EventCover(imageUrl: String?, category: String?, modifier: Modifier = Modifier, tall: Boolean = false) {
  val brush = remember(category) {
    val colors = when (category?.lowercase()) {
      "environment" -> listOf(Color(0xFF065F46), Color(0xFF10B981))
      "community" -> listOf(Color(0xFF1E3A5F), Color(0xFF38BDF8))
      "education" -> listOf(Color(0xFF4C1D95), Color(0xFFA78BFA))
      else -> listOf(Color(0xFF064E3B), Color(0xFF059669))
    }
    Brush.horizontalGradient(colors)
  }
  Box(
    modifier
      .fillMaxWidth()
      .height(if (tall) 120.dp else 88.dp)
      .clip(RoundedCornerShape(Space.radiusMd)),
  ) {
    Box(Modifier.matchParentSize().background(brush))
    if (!imageUrl.isNullOrBlank()) {
      AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )
    }
    if (!category.isNullOrBlank()) {
      Text(
        category,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(10.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.35f))
          .padding(horizontal = 8.dp, vertical = 3.dp),
      )
    }
  }
}

@Composable
fun EventCard(
  event: EventDto,
  onClick: () -> Unit,
  actionLabel: String = "Open",
) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(Space.radiusLg))
      .clickable(onClick = onClick)
      .padding(12.dp),
  ) {
    EventCover(event.imageUrl, event.category)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(
        event.name,
        style = MaterialTheme.typography.titleMedium,
        color = SleekTextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      StatusChip(event.myRegistrationStatus?.replace('_', ' ') ?: event.status)
    }
    Spacer(Modifier.height(4.dp))
    Text(event.subtitleLine(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
    Text(event.capacityLine(), style = MaterialTheme.typography.bodySmall, color = SleekTextTertiary)
    Spacer(Modifier.height(6.dp))
    Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = SleekPrimary)
  }
}
