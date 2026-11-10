package dev.tintwym.ui.volunteer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import dev.tintwym.data.offline.CheckInOutcome
import dev.tintwym.data.remote.EventDto
import dev.tintwym.data.remote.hhmm
import dev.tintwym.data.repo.ApiResult
import dev.tintwym.ui.checkin.QrScannerSheet
import dev.tintwym.ui.components.CheckInHintRow
import dev.tintwym.ui.components.CheckInQueuedPanel
import dev.tintwym.ui.components.CheckInSuccessPanel
import dev.tintwym.ui.components.EventCover
import dev.tintwym.ui.components.EventRow
import dev.tintwym.ui.components.FadeSwap
import dev.tintwym.ui.components.OfflineQueueBanner
import dev.tintwym.ui.components.PrimaryAction
import dev.tintwym.ui.components.QuietPanel
import dev.tintwym.ui.components.ScreenScaffold
import dev.tintwym.ui.components.SecondaryAction
import dev.tintwym.ui.components.SegmentTabs
import dev.tintwym.ui.components.StatusChip
import dev.tintwym.ui.components.SurfaceBlock
import dev.tintwym.ui.components.canSelfCheckIn
import dev.tintwym.ui.components.canWithdraw
import dev.tintwym.ui.components.capacityLine
import dev.tintwym.ui.components.isCheckedIn
import dev.tintwym.ui.components.isRegistered
import dev.tintwym.ui.components.isWaitlisted
import dev.tintwym.ui.components.subtitleLine
import dev.tintwym.ui.state.EmptyBlock
import dev.tintwym.ui.state.ErrorBlock
import dev.tintwym.ui.state.LoadState
import dev.tintwym.ui.state.LoadingBlock
import dev.tintwym.ui.theme.SleekBorder
import dev.tintwym.ui.theme.SleekPrimary
import dev.tintwym.ui.theme.SleekTextPrimary
import dev.tintwym.ui.theme.SleekTextSecondary
import dev.tintwym.ui.theme.Space
import kotlinx.coroutines.launch

@Composable
fun VolunteerEventDetailScreen(eventId: String, onBack: () -> Unit) {
  val graph = LocalContext.current.appGraph()
  var state by remember { mutableStateOf<LoadState<EventDto>>(LoadState.Loading) }
  var tab by remember { mutableStateOf("Overview") }
  var code by remember { mutableStateOf("") }
  var actionMsg by remember { mutableStateOf<String?>(null) }
  var actionTone by remember { mutableStateOf(ActionTone.Info) }
  var busy by remember { mutableStateOf(false) }
  var showScanner by remember { mutableStateOf(false) }
  val pendingCount by graph.checkIns.pendingCount.collectAsState()
  val flushing by graph.checkIns.flushing.collectAsState()
  val pendingItems by graph.offlineQueue.pendingFlow.collectAsState(initial = emptyList())
  val pendingForEvent = pendingItems.any { it.eventId == eventId }
  var wasPendingForEvent by remember(eventId) { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val tabs = listOf("Overview", "Schedule", "Check-in", "Contact")

  suspend fun softReload() {
    when (val r = graph.events.get(eventId)) {
      is ApiResult.Ok -> state = LoadState.Ready(r.value)
      is ApiResult.Err -> {
        if (state !is LoadState.Ready) state = LoadState.Failed(r.message)
        else {
          actionMsg = r.message
          actionTone = ActionTone.Error
        }
      }
    }
  }

  fun initialLoad() {
    scope.launch {
      if (state !is LoadState.Ready) state = LoadState.Loading
      softReload()
    }
  }

  fun submitCheckIn(event: EventDto, rawCode: String) {
    if (busy) return
    val trimmed = rawCode.trim()
    if (trimmed.isBlank()) {
      actionMsg = "Enter or scan a check-in code."
      actionTone = ActionTone.Error
      tab = "Check-in"
      return
    }
    scope.launch {
      busy = true
      tab = "Check-in"
      when (val outcome = graph.checkIns.selfCheckIn(eventId, trimmed, event.name)) {
        CheckInOutcome.Success -> {
          actionMsg = "Checked in — enjoy the event."
          actionTone = ActionTone.Success
          code = ""
          softReload()
        }
        is CheckInOutcome.Queued -> {
          actionMsg = outcome.message
          actionTone = ActionTone.Queued
        }
        is CheckInOutcome.Failed -> {
          actionMsg = outcome.message
          actionTone = ActionTone.Error
        }
      }
      busy = false
    }
  }

  LaunchedEffect(eventId) { initialLoad() }

  // When offline sync clears for this event, refresh so check-in UI matches server.
  LaunchedEffect(pendingForEvent) {
    if (wasPendingForEvent && !pendingForEvent) {
      softReload()
      actionMsg = "Check-in synced."
      actionTone = ActionTone.Success
      tab = "Check-in"
    }
    wasPendingForEvent = pendingForEvent
  }

  LaunchedEffect(eventId) {
    graph.checkIns.syncedEventIds.collect { syncedId ->
      if (syncedId == eventId) {
        softReload()
        actionMsg = "Check-in synced."
        actionTone = ActionTone.Success
        tab = "Check-in"
      }
    }
  }

  if (showScanner) {
    QrScannerSheet(
      onDismiss = { showScanner = false },
      onCode = { scanned ->
        code = scanned
        val event = (state as? LoadState.Ready)?.data
        if (event != null) submitCheckIn(event, scanned)
      },
    )
  }

  when (val s = state) {
    LoadState.Loading -> ScreenScaffold(title = "Event", onBack = onBack) { item { LoadingBlock() } }
    is LoadState.Failed -> ScreenScaffold(title = "Event", onBack = onBack) {
      item { ErrorBlock(s.message, onRetry = { initialLoad() }) }
    }
    is LoadState.Ready -> {
      val event = s.data
      ScreenScaffold(
        title = event.name,
        subtitle = "${event.date} · ${event.category ?: event.status}",
        onBack = onBack,
      ) {
        item { EventCover(event.imageUrl, event.category) }
        if (pendingCount > 0) {
          item {
            OfflineQueueBanner(
              count = pendingCount,
              syncing = flushing,
              onSyncNow = { graph.checkIns.requestFlush() },
            )
          }
        }
        item {
          QuietPanel {
            Text(event.description ?: "No description.", style = MaterialTheme.typography.bodyLarge, color = SleekTextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(event.subtitleLine(), style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)
            Text(event.capacityLine(), style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
            Spacer(Modifier.height(10.dp))
            StatusChip(event.myRegistrationStatus?.replace('_', ' ') ?: event.status)
          }
        }
        item {
          when {
            event.canWithdraw() -> SecondaryAction(if (busy) "Working…" else "Withdraw registration") {
              if (busy) return@SecondaryAction
              scope.launch {
                busy = true
                when (val r = graph.events.cancel(eventId)) {
                  is ApiResult.Ok -> {
                    actionMsg = "Registration withdrawn"
                    actionTone = ActionTone.Info
                    softReload()
                  }
                  is ApiResult.Err -> {
                    actionMsg = r.message
                    actionTone = ActionTone.Error
                  }
                }
                busy = false
              }
            }
            !event.isRegistered() -> PrimaryAction(if (busy) "Working…" else "Register for event") {
              if (busy) return@PrimaryAction
              scope.launch {
                busy = true
                when (val r = graph.events.register(eventId)) {
                  is ApiResult.Ok -> {
                    actionMsg = "Registered — open Check-in when you arrive."
                    actionTone = ActionTone.Success
                    tab = "Check-in"
                    softReload()
                  }
                  is ApiResult.Err -> {
                    actionMsg = r.message
                    actionTone = ActionTone.Error
                  }
                }
                busy = false
              }
            }
          }
        }
        item { SegmentTabs(tabs, tab) { tab = it } }
        item {
          FadeSwap(tab) { current ->
            when (current) {
              "Overview" -> QuietPanel {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(event.capacityLine(), color = SleekTextSecondary)
                CheckInHintRow("Leaders share a QR or short code at the venue for check-in.")
              }
              "Schedule" -> QuietPanel {
                EventRow("Date", event.date)
                EventRow("Session", "${event.startTime.hhmm()} – ${event.endTime.hhmm()}")
                EventRow("Duration", "${event.durationHours} hours")
              }
              "Check-in" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (actionTone) {
                  ActionTone.Success -> actionMsg?.let { CheckInSuccessPanel(it) }
                  ActionTone.Queued -> actionMsg?.let { CheckInQueuedPanel(it) }
                  ActionTone.Error -> actionMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                  }
                  ActionTone.Info -> actionMsg?.let {
                    Text(it, color = SleekPrimary, style = MaterialTheme.typography.bodyMedium)
                  }
                }
                CheckInPanel(
                  event = event,
                  code = code,
                  onCodeChange = { code = it },
                  busy = busy,
                  pendingSync = pendingForEvent,
                  onScan = { showScanner = true },
                  onCheckIn = { submitCheckIn(event, code) },
                  onCheckOut = {
                    if (busy) return@CheckInPanel
                    scope.launch {
                      busy = true
                      when (val r = graph.events.selfCheckOut(eventId)) {
                        is ApiResult.Ok -> {
                          actionMsg = "Checked out — thanks for volunteering."
                          actionTone = ActionTone.Success
                          softReload()
                        }
                        is ApiResult.Err -> {
                          actionMsg = r.message
                          actionTone = ActionTone.Error
                        }
                      }
                      busy = false
                    }
                  },
                )
              }
              "Contact" -> QuietPanel {
                EventRow("Venue", event.venue ?: "TBA")
                EventRow("Address", event.address ?: if (event.remote) "Remote" else "—")
                if (!event.meetingLink.isNullOrBlank()) {
                  EventRow("Meeting link", event.meetingLink)
                }
              }
            }
          }
        }
      }
    }
  }
}

private enum class ActionTone { Info, Success, Queued, Error }

@Composable
private fun CheckInPanel(
  event: EventDto,
  code: String,
  onCodeChange: (String) -> Unit,
  busy: Boolean,
  pendingSync: Boolean,
  onScan: () -> Unit,
  onCheckIn: () -> Unit,
  onCheckOut: () -> Unit,
) {
  when {
    event.canSelfCheckIn() -> SurfaceBlock {
      Text("Arrive & check in", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
      Spacer(Modifier.height(6.dp))
      Text(
        "Scan the leader’s QR or type the short code. Works offline — we’ll sync when you’re back online.",
        style = MaterialTheme.typography.bodyMedium,
        color = SleekTextSecondary,
      )
      if (pendingSync) {
        Spacer(Modifier.height(10.dp))
        StatusChip("Pending sync")
      }
      Spacer(Modifier.height(16.dp))
      PrimaryAction(
        text = if (busy) "Working…" else "Scan QR code",
        onClick = { if (!busy) onScan() },
      )
      Spacer(Modifier.height(10.dp))
      Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Icon(
          Icons.Outlined.QrCodeScanner,
          contentDescription = null,
          tint = SleekTextSecondary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("or enter code manually", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
      }
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        label = { Text("Check-in code") },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Space.radiusMd),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = SleekPrimary,
          unfocusedBorderColor = SleekBorder,
        ),
      )
      Spacer(Modifier.height(12.dp))
      SecondaryAction(
        text = if (busy) "Working…" else "Check in with code",
        onClick = { if (!busy && code.isNotBlank()) onCheckIn() },
      )
    }
    !event.isRegistered() -> QuietPanel {
      EmptyBlock("Register before checking in.")
    }
    event.isWaitlisted() -> QuietPanel {
      EmptyBlock("You’re on the waitlist. Check-in opens once you’re confirmed.")
    }
    event.isCheckedIn() && event.myRegistrationStatus != "CHECKED_IN" -> QuietPanel {
      EmptyBlock("Attendance recorded as ${event.myRegistrationStatus?.replace('_', ' ')}.")
    }
    event.myRegistrationStatus == "CHECKED_IN" -> SurfaceBlock {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Outlined.CheckCircle,
          contentDescription = null,
          tint = SleekPrimary,
          modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text("You’re checked in", style = MaterialTheme.typography.titleMedium, color = SleekTextPrimary)
          Text(
            "Check out when you leave so hours can be logged.",
            color = SleekTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      Spacer(Modifier.height(16.dp))
      SecondaryAction(if (busy) "Working…" else "Check out") {
        if (!busy) onCheckOut()
      }
    }
    else -> QuietPanel {
      EmptyBlock("Check-in isn’t available for your registration status yet.")
    }
  }
}
