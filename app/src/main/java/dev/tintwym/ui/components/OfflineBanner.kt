package dev.tintwym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.tintwym.ui.theme.SleekOnPrimaryContainer
import dev.tintwym.ui.theme.SleekOnTertiaryContainer
import dev.tintwym.ui.theme.SleekPrimaryContainer
import dev.tintwym.ui.theme.SleekPrimarySoft
import dev.tintwym.ui.theme.SleekTertiary
import dev.tintwym.ui.theme.SleekTertiaryContainer
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space

@Composable
fun OfflineQueueBanner(
  count: Int,
  syncing: Boolean = false,
  onSyncNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (count <= 0) return
  Row(
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusMd))
      .background(SleekTertiaryContainer)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      Icons.Outlined.CloudOff,
      contentDescription = null,
      tint = SleekTertiary,
      modifier = Modifier.size(22.dp),
    )
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
      Text(
        if (count == 1) "1 check-in waiting to sync"
        else "$count check-ins waiting to sync",
        style = MaterialTheme.typography.titleSmall,
        color = SleekOnTertiaryContainer,
      )
      Text(
        if (syncing) "Syncing now…" else "We’ll send them when you’re back online.",
        style = MaterialTheme.typography.bodySmall,
        color = SleekOnTertiaryContainer.copy(alpha = 0.85f),
      )
    }
    TextButton(onClick = onSyncNow, enabled = !syncing) {
      if (syncing) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = SleekTertiary,
        )
      } else {
        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleekTertiary)
      }
      Spacer(Modifier.width(4.dp))
      Text(if (syncing) "…" else "Sync", color = SleekTertiary)
    }
  }
}

@Composable
fun CheckInSuccessPanel(message: String) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekPrimaryContainer)
      .padding(Space.lg),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      "You’re in",
      style = MaterialTheme.typography.titleMedium,
      color = SleekOnPrimaryContainer,
    )
    Text(
      message,
      style = MaterialTheme.typography.bodyMedium,
      color = SleekOnPrimaryContainer.copy(alpha = 0.85f),
    )
  }
}

@Composable
fun CheckInQueuedPanel(message: String) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekTertiaryContainer)
      .padding(Space.lg),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      "Saved offline",
      style = MaterialTheme.typography.titleMedium,
      color = SleekOnTertiaryContainer,
    )
    Text(
      message,
      style = MaterialTheme.typography.bodyMedium,
      color = SleekOnTertiaryContainer.copy(alpha = 0.9f),
    )
  }
}

@Composable
fun CheckInHintRow(text: String) {
  Row(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(
      Modifier
        .width(3.dp)
        .height(14.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(SleekPrimarySoft),
    )
    Spacer(Modifier.width(10.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
  }
}
