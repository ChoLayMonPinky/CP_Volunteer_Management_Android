package dev.tintwym.ui.state

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space

sealed class LoadState<out T> {
  data object Loading : LoadState<Nothing>()
  data class Ready<T>(val data: T) : LoadState<T>()
  data class Failed(val message: String) : LoadState<Nothing>()
}

@Composable
fun LoadingBlock() {
  Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = SleekPrimary, strokeWidth = 3.dp)
  }
}

@Composable
fun ErrorBlock(message: String, onRetry: (() -> Unit)? = null) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(Space.radiusLg))
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Couldn’t load", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
    Spacer(Modifier.height(6.dp))
    Text(message, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
    if (onRetry != null) {
      Spacer(Modifier.height(12.dp))
      PrimaryAction("Try again", onClick = onRetry)
    }
  }
}

@Composable
fun EmptyBlock(
  message: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(Space.radiusLg))
      .padding(20.dp),
  ) {
    Text(message, style = MaterialTheme.typography.bodyLarge, color = SleekTextSecondary)
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(12.dp))
      PrimaryAction(actionLabel, onClick = onAction)
    }
  }
}
