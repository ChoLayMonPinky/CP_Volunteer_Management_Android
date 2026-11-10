package dev.tintwym.ui.volunteer

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.tintwym.appGraph
import dev.tintwym.data.remote.EventBucket
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.HourLogDto
import dev.tintwym.data.remote.MySummaryDto
import dev.tintwym.data.remote.NotificationDto
import dev.tintwym.data.remote.RegistrationDto
import dev.tintwym.data.remote.TaskDto
import dev.tintwym.data.remote.inBucket
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.components.AvatarInitials
import dev.tintwym.ui.components.EventCard
import dev.tintwym.ui.components.EventRow
import dev.tintwym.ui.components.FocusBand
import dev.tintwym.ui.components.OfflineQueueBanner
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.components.QuietPanel
import dev.tintwym.ui.components.ScreenScaffold
import dev.tintwym.ui.components.SecondaryAction
import dev.tintwym.ui.components.SectionLabel
import dev.tintwym.ui.components.SegmentTabs
import dev.tintwym.ui.components.SoftDivider
import dev.tintwym.ui.components.StatusChip
import dev.tintwym.ui.components.isRegistered
import dev.tintwym.ui.state.EmptyBlock
import dev.tintwym.ui.state.ErrorBlock
import dev.tintwym.ui.state.LoadState
import dev.tintwym.ui.state.LoadingBlock
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.SleekTextTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerHomeScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  val pendingCount by graph.checkIns.pendingCount.collectAsState()
  val flushing by graph.checkIns.flushing.collectAsState()
  var summary by remember { mutableStateOf<LoadState<MySummaryDto>>(LoadState.Loading) }
  var regs by remember { mutableStateOf<LoadState<List<RegistrationDto>>>(LoadState.Loading) }
  var tasks by remember { mutableStateOf<LoadState<List<TaskDto>>>(LoadState.Loading) }
  var refreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  RequestNotificationPermission()

  fun reload() {
    scope.launch {
      refreshing = true
      graph.checkIns.requestFlush()
      summary = when (val r = graph.me.summary()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      regs = when (val r = graph.me.registrations()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "CANCELLED" && it.status != "NO_SHOW" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      tasks = when (val r = graph.me.tasks()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "DONE" && it.status != "CANCELLED" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      refreshing = false
    }
  }

  LaunchedEffect(Unit) { reload() }
  val first = session?.fullName?.substringBefore(" ") ?: "there"

  PullToRefreshBox(isRefreshing = refreshing, onRefresh = { reload() }) {
    ScreenScaffold(title = "Home", subtitle = "Your volunteering") {
      when (val s = summary) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
        is LoadState.Ready -> item {
          FocusBand(
            title = "Hi, $first",
            subtitle = "${s.data.totalHours ?: 0} hours · ${s.data.currentBadge?.title ?: "Keep going"}",
            meta = "${s.data.upcomingRegistrations} upcoming · ${s.data.unreadNotifications} unread · ${s.data.openTasks} tasks",
          )
        }
      }
      if (pendingCount > 0) {
        item {
          OfflineQueueBanner(
            count = pendingCount,
            syncing = flushing,
            onSyncNow = { graph.checkIns.requestFlush() },
          )
        }
      }
      item { SectionLabel("Next up") }
      when (val r = regs) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(r.message) }
        is LoadState.Ready -> {
          if (r.data.isEmpty()) {
            item {
              EmptyBlock(
                "No registrations yet. Browse Explore to join an event.",
              )
            }
          } else {
            items(r.data.take(5), key = { it.id }) { reg ->
              EventRow(
                title = reg.eventName ?: "Event",
                subtitle = "${reg.eventDate.orEmpty()} · ${reg.startTime?.take(5).orEmpty()} · ${reg.status.replace('_', ' ')}",
                trailing = "Open",
                onClick = { onOpenEvent(reg.eventId) },
              )
            }
          }
        }
      }
      item { SectionLabel("Your tasks") }
      when (val t = tasks) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(t.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          if (t.data.isEmpty()) item { EmptyBlock("No open tasks assigned to you.") }
          else items(t.data.take(5), key = { it.id }) { task ->
            EventRow(
              title = task.title,
              subtitle = "${task.priority ?: "MEDIUM"} · ${task.eventName ?: "General"} · ${task.status}",
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerExploreScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  var bucket by remember { mutableStateOf("Open") }
  var category by remember { mutableStateOf("All") }
  var state by remember { mutableStateOf<LoadState<List<EventDto>>>(LoadState.Loading) }
  var refreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      refreshing = true
      state = when (val r = graph.events.list(upcoming = false)) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "CANCELLED" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      refreshing = false
    }
  }

  LaunchedEffect(Unit) { reload() }

  val bucketEnum = when (bucket) {
    "Past" -> EventBucket.Past
    else -> EventBucket.Open
  }
  val filtered = (state as? LoadState.Ready)?.data
    ?.inBucket(bucketEnum)
    ?.filter { category == "All" || it.category.equals(category, ignoreCase = true) }
    .orEmpty()

  PullToRefreshBox(isRefreshing = refreshing, onRefresh = { reload() }) {
    ScreenScaffold(title = "Explore", subtitle = "Find opportunities") {
      item { SegmentTabs(listOf("Open", "Past"), bucket) { bucket = it } }
      item { SegmentTabs(listOf("All", "Environment", "Community", "Education"), category) { category = it } }
      when (val s = state) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          if (filtered.isEmpty()) {
            item {
              EmptyBlock(
                if (bucket == "Open") "No open events right now. Check Past or ask your organiser to publish."
                else "No past events in this filter.",
              )
            }
          }
          items(filtered, key = { it.id }) { event ->
            EventCard(event, onClick = { onOpenEvent(event.id) }, actionLabel = if (event.isRegistered()) "View registration" else "View & register")
          }
        }
      }
    }
  }
}

@Composable
fun VolunteerMyEventsScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  var tab by remember { mutableStateOf("Active") }
  var state by remember { mutableStateOf<LoadState<List<RegistrationDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      state = when (val r = graph.me.registrations()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }

  val list = (state as? LoadState.Ready)?.data.orEmpty().filter {
    when (tab) {
      "Done" -> it.status in setOf("COMPLETED", "NO_SHOW", "CANCELLED")
      else -> it.status !in setOf("COMPLETED", "NO_SHOW", "CANCELLED")
    }
  }

  ScreenScaffold(title = "My Events", subtitle = "Registered") {
    item { SegmentTabs(listOf("Active", "Done"), tab) { tab = it } }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (list.isEmpty()) {
          item {
            EmptyBlock(
              if (tab == "Active") "Nothing active. Explore opportunities to register."
              else "No completed registrations yet.",
            )
          }
        }
        items(list, key = { it.id }) { reg ->
          EventRow(
            title = reg.eventName ?: "Event",
            subtitle = "${reg.eventDate.orEmpty()} · ${reg.startTime?.take(5).orEmpty()}",
            trailing = reg.status.replace('_', ' '),
            accent = if (reg.status == "CHECKED_IN") SleekPrimary else SleekTextTertiary,
            onClick = { onOpenEvent(reg.eventId) },
          )
        }
      }
    }
  }
}

@Composable
fun VolunteerNotificationsScreen() {
  val graph = LocalContext.current.appGraph()
  var state by remember { mutableStateOf<LoadState<List<NotificationDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      state = when (val r = graph.me.notifications()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.items)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }

  ScreenScaffold(title = "Notifications", subtitle = "Updates") {
    item {
      SecondaryAction("Mark all read") {
        scope.launch {
          graph.me.markAllRead()
          reload()
        }
      }
    }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (s.data.isEmpty()) item { EmptyBlock("You’re all caught up.") }
        items(s.data, key = { it.id }) { n ->
          EventRow(
            title = n.title,
            subtitle = n.body.orEmpty(),
            trailing = if (n.read) null else "New",
            accent = if (n.read) SleekTextTertiary else SleekPrimary,
            onClick = {
              scope.launch {
                graph.me.markRead(n.id)
                reload()
              }
            },
          )
          SoftDivider()
        }
      }
    }
  }
}

@Composable
fun VolunteerProfileScreen(onLogout: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  var hours by remember { mutableStateOf<LoadState<List<HourLogDto>>>(LoadState.Loading) }

  LaunchedEffect(Unit) {
    hours = when (val r = graph.me.hours()) {
      is ApiResult.Ok -> LoadState.Ready(r.value)
      is ApiResult.Err -> LoadState.Failed(r.message)
    }
  }

  ScreenScaffold(title = "Profile") {
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AvatarInitials(session?.fullName ?: "V", size = 56.dp)
        Column {
          Text(session?.fullName ?: "Volunteer", style = MaterialTheme.typography.headlineSmall)
          Text(session?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
          Text(session?.organizationName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = SleekTextTertiary)
          Spacer(Modifier.height(6.dp))
          StatusChip("Volunteer")
        }
      }
    }
    item {
      QuietPanel {
        Text("Recent hours", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        when (val h = hours) {
          LoadState.Loading -> LoadingBlock()
          is LoadState.Failed -> ErrorBlock(h.message)
          is LoadState.Ready -> {
            if (h.data.isEmpty()) EmptyBlock("No hour logs yet — check in at an event to earn hours.")
            h.data.take(6).forEach { log ->
              EventRow(
                title = log.eventName ?: log.workDate.orEmpty(),
                subtitle = "${log.hours ?: 0} hrs · ${log.status} · ${log.source}",
              )
            }
          }
        }
      }
    }
    item { PrimaryAction("Sign out", onClick = onLogout) }
  }
}

@Composable
private fun RequestNotificationPermission() {
  if (Build.VERSION.SDK_INT >= 33) {
    RequestNotificationPermissionApi33()
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermissionApi33() {
  val permission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
  LaunchedEffect(Unit) {
    if (!permission.status.isGranted) permission.launchPermissionRequest()
  }
}
