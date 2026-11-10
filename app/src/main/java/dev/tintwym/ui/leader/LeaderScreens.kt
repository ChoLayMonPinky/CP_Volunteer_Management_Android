package dev.tintwym.ui.leader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.tintwym.appGraph
import dev.tintwym.data.remote.EventBucket
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.IncidentDto
import dev.tintwym.data.remote.MemberDto
import dev.tintwym.data.remote.MessageDto
import dev.tintwym.data.remote.TaskDto
import dev.tintwym.data.remote.inBucket
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.components.AvatarInitials
import dev.tintwym.ui.components.EventCard
import dev.tintwym.ui.components.EventRow
import dev.tintwym.ui.components.FocusBand
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.components.QuietPanel
import dev.tintwym.ui.components.ScreenScaffold
import dev.tintwym.ui.components.SecondaryAction
import dev.tintwym.ui.components.SectionLabel
import dev.tintwym.ui.components.SegmentTabs
import dev.tintwym.ui.components.SoftDivider
import dev.tintwym.ui.components.StatusChip
import dev.tintwym.ui.components.SurfaceBlock
import dev.tintwym.ui.components.capacityLine
import dev.tintwym.ui.state.EmptyBlock
import dev.tintwym.ui.state.ErrorBlock
import dev.tintwym.ui.state.LoadState
import dev.tintwym.ui.state.LoadingBlock
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderHomeScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  var events by remember { mutableStateOf<LoadState<List<EventDto>>>(LoadState.Loading) }
  var tasks by remember { mutableStateOf<LoadState<List<TaskDto>>>(LoadState.Loading) }
  var refreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      refreshing = true
      events = when (val r = graph.events.list(false)) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      tasks = when (val r = graph.work.tasks()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "DONE" && it.status != "CANCELLED" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      refreshing = false
    }
  }

  LaunchedEffect(Unit) { reload() }
  val open = (events as? LoadState.Ready)?.data?.inBucket(EventBucket.Open).orEmpty()

  PullToRefreshBox(isRefreshing = refreshing, onRefresh = { reload() }) {
    ScreenScaffold(title = "Home", subtitle = "Ops board") {
      when (val e = events) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(e.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          item {
            FocusBand(
              title = "Today’s focus",
              subtitle = open.firstOrNull()?.name ?: "No open events — check Past or drafts",
              meta = "${open.size} open · ${(tasks as? LoadState.Ready)?.data?.size ?: 0} open tasks",
            )
          }
          item { SectionLabel("Open events") }
          if (open.isEmpty()) {
            item { EmptyBlock("No open events. Switch to My Events → Past to review completed shifts.") }
          } else {
            items(open.take(5), key = { it.id }) { event ->
              EventCard(event, onClick = { onOpenEvent(event.id) }, actionLabel = "Open ops")
            }
          }
        }
      }
      item { SectionLabel("Team tasks") }
      when (val t = tasks) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { EmptyBlock(t.message) }
        is LoadState.Ready -> {
          if (t.data.isEmpty()) item { EmptyBlock("No open tasks.") }
          else items(t.data.take(6), key = { it.id }) { task ->
            EventRow(
              title = task.title,
              subtitle = "${task.assigneeName ?: "Unassigned"} · ${task.priority} · ${task.status}",
              trailing = "Done",
              onClick = {
                scope.launch {
                  graph.work.setTaskStatus(task.id, "DONE")
                  reload()
                }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun LeaderMyEventsScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  var tab by remember { mutableStateOf("Open") }
  var state by remember { mutableStateOf<LoadState<List<EventDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      state = when (val r = graph.events.list(false)) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "CANCELLED" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }
  val bucket = if (tab == "Past") EventBucket.Past else EventBucket.Open
  val list = (state as? LoadState.Ready)?.data?.inBucket(bucket).orEmpty()

  ScreenScaffold(title = "My Events", subtitle = "Operational view") {
    item { SegmentTabs(listOf("Open", "Past"), tab) { tab = it } }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (list.isEmpty()) item { EmptyBlock("No events in $tab.") }
        items(list, key = { it.id }) { event ->
          SurfaceBlock {
            Text(event.name, style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(event.capacityLine(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
            if (!event.checkInCode.isNullOrBlank()) {
              Text("Code ${event.checkInCode}", style = MaterialTheme.typography.labelLarge, color = SleekPrimary)
            }
            Spacer(Modifier.height(12.dp))
            SecondaryAction("Open ops board") { onOpenEvent(event.id) }
          }
        }
      }
    }
  }
}

@Composable
fun LeaderMyTeamScreen() {
  val graph = LocalContext.current.appGraph()
  var tab by remember { mutableStateOf("People") }
  var members by remember { mutableStateOf<LoadState<List<MemberDto>>>(LoadState.Loading) }
  var incidents by remember { mutableStateOf<LoadState<List<IncidentDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      members = when (val r = graph.members.list()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.role != "ORGANIZER" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      incidents = when (val r = graph.work.incidents()) {
        is ApiResult.Ok -> LoadState.Ready(r.value.filter { it.status != "RESOLVED" })
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }

  ScreenScaffold(title = "My Team", subtitle = "People & incidents") {
    item { SegmentTabs(listOf("People", "Incidents"), tab) { tab = it } }
    if (tab == "People") {
      when (val s = members) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          if (s.data.isEmpty()) item { EmptyBlock("No team members found.") }
          items(s.data, key = { it.id }) { member ->
            Row(
              Modifier.fillMaxWidth().height(56.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              AvatarInitials(member.fullName)
              Column(Modifier.weight(1f)) {
                Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                Text(
                  "${member.role.replace('_', ' ')} · ${member.totalHours ?: 0} hrs",
                  color = SleekTextSecondary,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
              StatusChip(if (member.active) "Active" else "Inactive")
            }
            SoftDivider()
          }
        }
      }
    } else {
      when (val s = incidents) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          if (s.data.isEmpty()) item { EmptyBlock("No open incidents.") }
          items(s.data, key = { it.id }) { inc ->
            EventRow(
              title = inc.title,
              subtitle = "${inc.severity} · ${inc.eventName ?: "General"} · ${inc.reportedByName ?: ""}",
              trailing = inc.status,
            )
          }
        }
      }
    }
  }
}

@Composable
fun LeaderMessagesScreen() {
  val graph = LocalContext.current.appGraph()
  var channel by remember { mutableStateOf("ANNOUNCEMENTS") }
  var state by remember { mutableStateOf<LoadState<List<MessageDto>>>(LoadState.Loading) }
  var draft by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  val channels = listOf("ANNOUNCEMENTS", "LEADERS", "GENERAL")

  fun reload() {
    scope.launch {
      state = LoadState.Loading
      state = when (val r = graph.work.messages(channel)) {
        is ApiResult.Ok -> LoadState.Ready(r.value.asReversed())
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(channel) { reload() }

  ScreenScaffold(title = "Messages", subtitle = "Channels") {
    item { SegmentTabs(channels, channel) { channel = it } }
    item {
      QuietPanel {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          label = { Text("New message") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        PrimaryAction("Post") {
          if (draft.isBlank()) return@PrimaryAction
          scope.launch {
            when (val r = graph.work.postMessage(channel, draft.trim())) {
              is ApiResult.Ok -> {
                draft = ""
                error = null
                reload()
              }
              is ApiResult.Err -> error = r.message
            }
          }
        }
        if (error != null) {
          Spacer(Modifier.height(6.dp))
          Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (s.data.isEmpty()) item { EmptyBlock("No messages yet — post the first update.") }
        items(s.data, key = { it.id }) { msg ->
          EventRow(
            title = msg.senderName ?: "Someone",
            subtitle = msg.content,
            trailing = if (msg.urgent) "Urgent" else null,
          )
        }
      }
    }
  }
}

@Composable
fun LeaderProfileScreen(onLogout: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  ScreenScaffold(title = "Profile") {
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AvatarInitials(session?.fullName ?: "L", size = 56.dp)
        Column {
          Text(session?.fullName ?: "Leader", style = MaterialTheme.typography.headlineSmall)
          Text(session?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
          Spacer(Modifier.height(6.dp))
          StatusChip("Volunteer leader")
        }
      }
    }
    item { PrimaryAction("Sign out", onClick = onLogout) }
  }
}
