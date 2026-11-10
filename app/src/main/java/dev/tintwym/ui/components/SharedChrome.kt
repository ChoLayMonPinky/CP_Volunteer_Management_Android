package dev.tintwym.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekHairline
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekPrimarySoft
import dev.tintwym.ui.theme.SleekPrimaryStrong
import dev.tintwym.ui.theme.SleekPrimaryContainer
import dev.tintwym.ui.theme.SleekSurface
import dev.tintwym.ui.theme.SleekSurfaceMuted
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.SleekTextTertiary
import dev.tintwym.ui.theme.Space

/** Cheap one-shot fade for tab panels — 160ms, no slide (keeps scroll jank low). */
@Composable
fun FadeSwap(target: String, content: @Composable (String) -> Unit) {
  AnimatedContent(
    targetState = target,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "fadeSwap",
    content = { content(it) },
  )
}

@Composable
fun ScreenScaffold(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  actions: @Composable (() -> Unit)? = null,
  content: LazyListScope.() -> Unit,
) {
  Column(Modifier.fillMaxSize()) {
    Row(
      Modifier
        .fillMaxWidth()
        .padding(start = if (onBack != null) 4.dp else Space.screen, end = Space.screen, top = 16.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (onBack != null) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SleekTextPrimary)
        }
      }
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = SleekTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
          Spacer(Modifier.height(3.dp))
          Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
      }
      actions?.invoke()
    }
    HorizontalDivider(color = SleekHairline.copy(alpha = 0.5f), thickness = 1.dp)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = Space.screen,
        top = Space.lg,
        end = Space.screen,
        bottom = 28.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(Space.md),
      content = content,
    )
  }
}

@Composable
fun PageIntro(eyebrow: String? = null, headline: String, body: String? = null) {
  Column(Modifier.fillMaxWidth()) {
    if (eyebrow != null) {
      Text(
        eyebrow.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = SleekPrimary,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(6.dp))
    }
    Text(headline, style = MaterialTheme.typography.headlineMedium, color = SleekTextPrimary)
    if (body != null) {
      Spacer(Modifier.height(6.dp))
      Text(body, style = MaterialTheme.typography.bodyLarge, color = SleekTextSecondary)
    }
  }
}

@Composable
fun StatStrip(stats: List<Triple<String, String, String?>>) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(Space.radiusLg))
      .padding(vertical = 14.dp, horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    stats.forEachIndexed { index, (label, value, hint) ->
      if (index > 0) {
        Box(
          Modifier
            .width(1.dp)
            .height(36.dp)
            .background(SleekHairline),
        )
      }
      Column(
        Modifier.weight(1f).padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = SleekTextPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
        if (hint != null) {
          Text(hint, style = MaterialTheme.typography.labelSmall, color = SleekTextTertiary)
        }
      }
    }
  }
}

@Composable
fun FocusBand(title: String, subtitle: String, meta: String? = null) {
  val brush = remember {
    Brush.horizontalGradient(listOf(SleekPrimaryStrong, SleekPrimary, SleekPrimarySoft))
  }
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(brush)
      .padding(Space.xl),
  ) {
    Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f))
    if (meta != null) {
      Spacer(Modifier.height(10.dp))
      Text(meta, style = MaterialTheme.typography.labelLarge, color = SleekPrimarySoft)
    }
  }
}

@Composable
fun SectionLabel(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.titleSmall,
    color = SleekTextSecondary,
    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
  )
}

@Composable
fun QuietPanel(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurfaceMuted)
      .padding(Space.lg),
    content = content,
  )
}

@Composable
fun SurfaceBlock(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusLg))
      .background(SleekSurface)
      .border(1.dp, SleekBorder, RoundedCornerShape(Space.radiusLg))
      .padding(Space.lg),
    content = content,
  )
}

@Composable
fun EventRow(
  title: String,
  subtitle: String,
  trailing: String? = null,
  accent: Color = SleekPrimary,
  onClick: (() -> Unit)? = null,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusMd))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = 12.dp, horizontal = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier
        .width(3.dp)
        .height(36.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(accent),
    )
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(Modifier.height(2.dp))
      Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (trailing != null) {
      Text(trailing, style = MaterialTheme.typography.labelMedium, color = SleekPrimary, modifier = Modifier.padding(start = 8.dp))
    }
    if (onClick != null) {
      Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = SleekTextTertiary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun PersonRow(
  name: String,
  detail: String,
  trailing: String? = null,
  onClick: (() -> Unit)? = null,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Space.radiusMd))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AvatarInitials(name)
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(name, style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
      Text(detail, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
    }
    if (trailing != null) StatusChip(trailing)
  }
}

@Composable
fun StatusChip(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelMedium,
    color = SleekPrimary,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(SleekPrimaryContainer)
      .padding(horizontal = 9.dp, vertical = 4.dp),
  )
}

@Composable
fun SegmentTabs(tabs: List<String>, selected: String, onSelect: (String) -> Unit) {
  Row(
    Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .clip(RoundedCornerShape(Space.radiusMd))
      .background(SleekSurfaceMuted)
      .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    tabs.forEach { label ->
      val active = label == selected
      Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (active) SleekTextPrimary else SleekTextSecondary,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
          .clip(RoundedCornerShape(Space.radiusSm))
          .background(if (active) SleekSurface else Color.Transparent)
          .then(
            if (active) Modifier.border(1.dp, SleekBorder.copy(alpha = 0.7f), RoundedCornerShape(Space.radiusSm))
            else Modifier,
          )
          .clickable { onSelect(label) }
          .padding(horizontal = 14.dp, vertical = 9.dp),
      )
    }
  }
}

@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(Space.radiusMd),
    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
  ) {
    Text(text, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun SecondaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(Space.radiusMd),
    border = androidx.compose.foundation.BorderStroke(1.dp, SleekBorder),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextPrimary),
  ) {
    Text(text)
  }
}

@Composable
fun EmptyHint(text: String) {
  Text(text, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
}

@Composable
fun AvatarInitials(name: String, size: androidx.compose.ui.unit.Dp = 42.dp) {
  val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
  Box(
    Modifier
      .size(size)
      .clip(CircleShape)
      .background(SleekPrimaryContainer),
    contentAlignment = Alignment.Center,
  ) {
    Text(initials, color = SleekPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
  }
}

@Composable
fun SoftDivider() {
  HorizontalDivider(color = SleekHairline, thickness = 1.dp)
}

// Back-compat aliases used while screens migrate
@Composable
fun HeroBanner(title: String, subtitle: String) = FocusBand(title, subtitle)

@Composable
fun MetricTile(label: String, value: String, hint: String? = null) {
  QuietPanel {
    Text(label, style = MaterialTheme.typography.labelLarge, color = SleekTextSecondary)
    Spacer(Modifier.height(4.dp))
    Text(value, style = MaterialTheme.typography.headlineMedium, color = SleekTextPrimary)
    if (hint != null) {
      Spacer(Modifier.height(2.dp))
      Text(hint, style = MaterialTheme.typography.bodySmall, color = SleekTextTertiary)
    }
  }
}

@Composable
fun SectionCard(
  title: String? = null,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  SurfaceBlock(modifier) {
    if (title != null) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
      Spacer(Modifier.height(10.dp))
    }
    content()
  }
}

@Composable
fun ListRow(
  title: String,
  subtitle: String,
  trailing: String? = null,
  onClick: (() -> Unit)? = null,
) {
  EventRow(title = title, subtitle = subtitle, trailing = trailing, onClick = onClick)
}
