package dev.tintwym.ui.organiser

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
import dev.tintwym.data.remote.EventCreateRequest
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.HourLogDto
import dev.tintwym.data.remote.MemberDto
import dev.tintwym.data.remote.OrganizerSummaryDto
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
import dev.tintwym.ui.components.StatStrip
import dev.tintwym.ui.components.StatusChip
import dev.tintwym.ui.state.EmptyBlock
import dev.tintwym.ui.state.ErrorBlock
import dev.tintwym.ui.state.LoadState
import dev.tintwym.ui.state.LoadingBlock
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganiserDashboardScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  var dash by remember { mutableStateOf<LoadState<OrganizerSummaryDto>>(LoadState.Loading) }
  var events by remember { mutableStateOf<LoadState<List<EventDto>>>(LoadState.Loading) }
  var pending by remember { mutableStateOf<LoadState<List<HourLogDto>>>(LoadState.Loading) }
  var refreshing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      refreshing = true
      dash = when (val r = graph.members.dashboard()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      events = when (val r = graph.events.list(false)) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      pending = when (val r = graph.members.pendingHours()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      refreshing = false
    }
  }

  LaunchedEffect(Unit) { reload() }
  val first = session?.fullName?.substringBefore(" ") ?: "there"
  val open = (events as? LoadState.Ready)?.data?.inBucket(EventBucket.Open).orEmpty()

  PullToRefreshBox(isRefreshing = refreshing, onRefresh = { reload() }) {
    ScreenScaffold(title = "Dashboard", subtitle = session?.organizationName ?: "Organisation") {
      when (val d = dash) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { ErrorBlock(d.message, onRetry = { reload() }) }
        is LoadState.Ready -> {
          item {
            FocusBand(
              title = "Good day, $first",
              subtitle = "Live programme pulse for your organisation.",
              meta = "${open.size} open · ${d.data.activeVolunteers} volunteers · ${d.data.pendingHourApprovals} hour approvals",
            )
          }
          item {
            StatStrip(
              listOf(
                Triple("Open", "${open.size}", "events"),
                Triple("People", "${d.data.activeVolunteers}", "active"),
                Triple("Hours", "${d.data.totalApprovedHours ?: 0}", "approved"),
              ),
            )
          }
        }
      }
      item { SectionLabel("Open events") }
      if (open.isEmpty()) {
        item { EmptyBlock("No open events. Create one from the Events tab.") }
      } else {
        items(open.take(4), key = { it.id }) { event ->
          EventCard(event, onClick = { onOpenEvent(event.id) })
        }
      }
      item { SectionLabel("Pending hour approvals") }
      when (val p = pending) {
        LoadState.Loading -> item { LoadingBlock() }
        is LoadState.Failed -> item { EmptyBlock(p.message) }
        is LoadState.Ready -> {
          if (p.data.isEmpty()) item { EmptyBlock("No pending hour logs.") }
          else items(p.data.take(5), key = { it.id }) { log ->
            QuietPanel {
              Text(log.userName ?: "Volunteer", style = MaterialTheme.typography.titleMedium)
              Text(
                "${log.hours ?: 0} hrs · ${log.eventName ?: log.workDate} · ${log.description ?: ""}",
                color = SleekTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Approve", modifier = Modifier.weight(1f)) {
                  scope.launch {
                    graph.members.reviewHour(log.id, true)
                    reload()
                  }
                }
                SecondaryAction("Reject", modifier = Modifier.weight(1f)) {
                  scope.launch {
                    graph.members.reviewHour(log.id, false)
                    reload()
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun OrganiserEventsScreen(onOpenEvent: (String) -> Unit) {
  val graph = LocalContext.current.appGraph()
  var tab by remember { mutableStateOf("Open") }
  var state by remember { mutableStateOf<LoadState<List<EventDto>>>(LoadState.Loading) }
  var showCreate by remember { mutableStateOf(false) }
  var name by remember { mutableStateOf("") }
  var venue by remember { mutableStateOf("") }
  var capacity by remember { mutableStateOf("20") }
  var notice by remember { mutableStateOf<String?>(null) }
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
  val bucket = when (tab) {
    "Past" -> EventBucket.Past
    "Draft" -> EventBucket.Draft
    else -> EventBucket.Open
  }
  val list = (state as? LoadState.Ready)?.data?.inBucket(bucket).orEmpty()

  ScreenScaffold(title = "Events", subtitle = "Plan and publish") {
    item {
      PrimaryAction(if (showCreate) "Hide form" else "Create event") { showCreate = !showCreate }
    }
    if (showCreate) {
      item {
        QuietPanel {
          Text("New event", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(venue, { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(8.dp))
          OutlinedTextField(capacity, { capacity = it.filter { c -> c.isDigit() } }, label = { Text("Capacity") }, modifier = Modifier.fillMaxWidth())
          Spacer(Modifier.height(12.dp))
          PrimaryAction("Publish") {
            val date = LocalDate.now().plusDays(7).toString()
            val start = LocalTime.of(9, 0).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            val end = LocalTime.of(12, 0).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            scope.launch {
              when (
                val r = graph.events.create(
                  EventCreateRequest(
                    name = name.ifBlank { "Untitled event" },
                    description = "Created from CommonGround Android",
                    category = "Community",
                    venue = venue.ifBlank { "TBA" },
                    date = date,
                    startTime = start,
                    endTime = end,
                    capacity = capacity.toIntOrNull() ?: 20,
                    status = "OPEN",
                  ),
                )
              ) {
                is ApiResult.Ok -> {
                  notice = "Created ${r.value.name}"
                  showCreate = false
                  name = ""
                  venue = ""
                  tab = "Open"
                  reload()
                }
                is ApiResult.Err -> notice = r.message
              }
            }
          }
        }
      }
    }
    if (notice != null) item { Text(notice!!, color = MaterialTheme.colorScheme.primary) }
    item { SegmentTabs(listOf("Open", "Past", "Draft"), tab) { tab = it } }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (list.isEmpty()) item { EmptyBlock("No $tab events.") }
        items(list, key = { it.id }) { event ->
          EventCard(event, onClick = { onOpenEvent(event.id) })
        }
      }
    }
  }
}

@Composable
fun OrganiserVolunteersScreen() {
  val graph = LocalContext.current.appGraph()
  var tab by remember { mutableStateOf("All") }
  var state by remember { mutableStateOf<LoadState<List<MemberDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      state = when (val r = graph.members.list()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }
  val filtered = (state as? LoadState.Ready)?.data.orEmpty().filter {
    when (tab) {
      "Leaders" -> it.role == "VOLUNTEER_LEADER"
      "Volunteers" -> it.role == "VOLUNTEER"
      else -> true
    }
  }

  ScreenScaffold(title = "Volunteers", subtitle = "Directory") {
    item { SegmentTabs(listOf("All", "Leaders", "Volunteers"), tab) { tab = it } }
    when (val s = state) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(s.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        if (filtered.isEmpty()) item { EmptyBlock("No members in this filter.") }
        items(filtered, key = { it.id }) { v ->
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarInitials(v.fullName)
            Column(Modifier.weight(1f)) {
              Text(v.fullName, style = MaterialTheme.typography.titleMedium)
              Text(
                "${v.role.replace('_', ' ')} · ${v.totalHours ?: 0} hrs",
                color = SleekTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            StatusChip(if (v.active) "Active" else "Inactive")
          }
          SoftDivider()
        }
      }
    }
  }
}

@Composable
fun OrganiserReportsScreen() {
  val graph = LocalContext.current.appGraph()
  var dash by remember { mutableStateOf<LoadState<OrganizerSummaryDto>>(LoadState.Loading) }
  var pending by remember { mutableStateOf<LoadState<List<HourLogDto>>>(LoadState.Loading) }
  val scope = rememberCoroutineScope()

  fun reload() {
    scope.launch {
      dash = when (val r = graph.members.dashboard()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
      pending = when (val r = graph.members.pendingHours()) {
        is ApiResult.Ok -> LoadState.Ready(r.value)
        is ApiResult.Err -> LoadState.Failed(r.message)
      }
    }
  }

  LaunchedEffect(Unit) { reload() }

  ScreenScaffold(title = "Reports", subtitle = "Programme health") {
    when (val d = dash) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { ErrorBlock(d.message, onRetry = { reload() }) }
      is LoadState.Ready -> {
        item {
          StatStrip(
            listOf(
              Triple("Pending", "${d.data.pendingHourApprovals}", "hours"),
              Triple("Incidents", "${d.data.openIncidents}", "open"),
              Triple("Tasks", "${d.data.openTasks}", "open"),
            ),
          )
        }
        item {
          QuietPanel {
            Text("Snapshot", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
            Spacer(Modifier.height(8.dp))
            EventRow("Completed events", "${d.data.completedEvents}")
            EventRow("Active leaders", "${d.data.activeLeaders}")
            EventRow("Approved hours", "${d.data.totalApprovedHours ?: 0}")
            Spacer(Modifier.height(8.dp))
            EmptyBlock("PDF/CSV exports stay on web. Approvals below are live.")
          }
        }
      }
    }
    item { SectionLabel("Approve hours") }
    when (val p = pending) {
      LoadState.Loading -> item { LoadingBlock() }
      is LoadState.Failed -> item { EmptyBlock(p.message) }
      is LoadState.Ready -> {
        if (p.data.isEmpty()) item { EmptyBlock("Queue clear.") }
        items(p.data, key = { it.id }) { log ->
          QuietPanel {
            Text("${log.userName} · ${log.hours} hrs", style = MaterialTheme.typography.titleMedium)
            Text(log.eventName ?: log.workDate.orEmpty(), color = SleekTextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              PrimaryAction("Approve", modifier = Modifier.weight(1f)) {
                scope.launch { graph.members.reviewHour(log.id, true); reload() }
              }
              SecondaryAction("Reject", modifier = Modifier.weight(1f)) {
                scope.launch { graph.members.reviewHour(log.id, false); reload() }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun OrganiserProfileScreen(onLogout: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  val session by graph.sessionStore.sessionFlow.collectAsState(initial = null)
  ScreenScaffold(title = "Profile", subtitle = "Organisation") {
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AvatarInitials(session?.fullName ?: "O", size = 56.dp)
        Column {
          Text(session?.fullName ?: "Organiser", style = MaterialTheme.typography.headlineSmall)
          Text(session?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
          Text(session?.organizationName.orEmpty(), color = SleekTextSecondary)
          Spacer(Modifier.height(6.dp))
          StatusChip("Organiser")
        }
      }
    }
    item { PrimaryAction("Sign out", onClick = onLogout) }
  }
}
